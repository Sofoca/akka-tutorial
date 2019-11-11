package de.hpi.ddm.actors;

import akka.actor.*;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import scala.concurrent.duration.Duration;

import java.io.ByteArrayOutputStream;
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

public class LargeMessageProxy extends AbstractLoggingActor {

    ////////////////////////
    // Actor Construction //
    ////////////////////////

    public static final String DEFAULT_NAME = "largeMessageProxy";
    private Kryo kryo = new Kryo();
    private static final int CHUNK_MAX_BYTES = 1024;
    private List<ChunkMessage> currentMessageBuffer;
    private int currentMessageCount = 0;
    private Map<UUID, Map<Integer, Cancellable>> resendTimers = new HashMap<>();
    private Map<UUID, List<ChunkMessage>> messagesToBeSent = new HashMap<>();

    public static Props props() {
        return Props.create(LargeMessageProxy.class);
    }

    ////////////////////
    // Actor Messages //
    ////////////////////

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LargeMessage<T> implements Serializable {
        private static final long serialVersionUID = 2940665245810221108L;
        private T message;
        private ActorRef receiver;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ChunkMessage implements Serializable {
        private static final long serialVersionUID = 1L;
        private byte[] bytes = new byte[CHUNK_MAX_BYTES];
        private ActorRef sender;
        private ActorRef receiver;
        private int sequenceNumber;
        private int sequenceLength;
        private Class dataClass;
        private String parentMessageId;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class AdmiralAckbar implements Serializable {
        private static final long serialVersionUID = 2L;
        private ActorRef sender;
        private int sequenceNumber;
        private String parentMessageId;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BytesMessage<T> implements Serializable {
        private static final long serialVersionUID = 4057807743872319842L;
        private T bytes;
        private ActorRef sender;
        private ActorRef receiver;
    }

    /////////////////
    // Actor State //
    /////////////////

    /////////////////////
    // Actor Lifecycle //
    /////////////////////

    public LargeMessageProxy() {
        kryo.register(LargeMessage.class);
    }

    ////////////////////
    // Actor Behavior //
    ////////////////////

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(LargeMessage.class, this::handle)
                .match(BytesMessage.class, this::handle)
                .match(ChunkMessage.class, this::handle)
                .match(AdmiralAckbar.class, this::handle)
                .matchAny(object -> this.log().info("Received unknown message: \"{}\"", object.toString()))
                .build();
    }

    private void handle(AdmiralAckbar admiralAckbar) {
        UUID messageId = UUID.fromString(admiralAckbar.getParentMessageId());
        resendTimers
                .get(messageId)
                .get(admiralAckbar.getSequenceNumber())
                .cancel();
        resendTimers
                .get(messageId)
                .remove(admiralAckbar.getSequenceNumber());
        if (resendTimers.get(messageId).isEmpty()) {
            if (!messagesToBeSent.get(messageId).isEmpty()) {
                ChunkMessage nextChunk = messagesToBeSent.get(messageId).remove(0);
                ActorSelection receiverProxy = this.context().actorSelection(nextChunk.getReceiver().path().child(DEFAULT_NAME));
                resendTimers.get(messageId).put(nextChunk.getSequenceNumber(),
                        this.getContext().getSystem().getScheduler().schedule(
                                Duration.create(0, TimeUnit.SECONDS),
                                Duration.create(3, TimeUnit.SECONDS),
                                () -> receiverProxy.tell(nextChunk, this.self()),
                                this.getContext().getDispatcher()
                        )
                );
            }
        }
    }

    private void handle(LargeMessage<?> message) {
        ActorSelection receiverProxy = this.context().actorSelection(message.getReceiver().path().child(DEFAULT_NAME));

        // This will definitely fail in a distributed setting if the serialized message is large!
        // Solution options:
        // 1. Serialize the object and send its bytes batch-wise (make sure to use artery's side channel then).
        // 2. Serialize the object and send its bytes via Akka streaming.
        // 3. Send the object via Akka's http client-server component.
        // 4. Other ideas ...

        UUID messageId = UUID.randomUUID();
        List<ChunkMessage> chunkMessages = chunkifyByteArray(this.sender(), message.getReceiver(), message, messageId);

        chunkMessages.forEach(msg -> {
            messagesToBeSent.putIfAbsent(messageId, new ArrayList<>());
            messagesToBeSent.get(messageId).add(msg);
        });

        resendTimers.putIfAbsent(messageId, new HashMap<>());
        ChunkMessage firstMessage = messagesToBeSent.get(messageId).remove(0);
        resendTimers.get(messageId).put(firstMessage.getSequenceNumber(),
                this.getContext().getSystem().getScheduler().schedule(
                        Duration.create(0, TimeUnit.SECONDS),
                        Duration.create(3, TimeUnit.SECONDS),
                        () -> receiverProxy.tell(firstMessage, this.self()),
                        this.getContext().getDispatcher()
                )
        );
    }

    private void handle(ChunkMessage message) {
        if (currentMessageBuffer == null) {
            currentMessageBuffer = new ArrayList<>(message.getSequenceLength());
            IntStream.range(0, message.getSequenceLength()).forEach(i -> currentMessageBuffer.add(null));
            currentMessageCount = 0;
        }

        if (currentMessageBuffer.set(message.getSequenceNumber(), message) == null) {
            currentMessageCount++;
        }

        // acknowledge the message
        ActorSelection senderProxy = this.context().actorSelection(message.getSender().path().child(DEFAULT_NAME));
        senderProxy.tell(
                new AdmiralAckbar(this.getSender(), message.getSequenceNumber(), message.getParentMessageId()),
                this.sender()
        );

        if (currentMessageCount == message.getSequenceLength()) {
            message.getReceiver().tell(reassembleChunks(), message.getSender());
        }
    }

    private Object reassembleChunks() {
        int fullBytesSize = CHUNK_MAX_BYTES * (currentMessageCount - 1);
        ChunkMessage lastMessage = currentMessageBuffer.get(currentMessageCount - 1);
        int lastBytesSize = lastMessage.getBytes().length;
        byte[] reassembledMessage = new byte[fullBytesSize + lastBytesSize];
        for (int i = 0; i < currentMessageCount - 1; i++) {
            System.arraycopy(currentMessageBuffer.get(i).getBytes(), 0, reassembledMessage, i * CHUNK_MAX_BYTES, CHUNK_MAX_BYTES);
        }
        System.arraycopy(lastMessage.getBytes(), 0, reassembledMessage, CHUNK_MAX_BYTES * (currentMessageCount - 1), lastMessage.getBytes().length);

        return kryo.readObject(new Input(reassembledMessage), lastMessage.getDataClass());
    }

    private List<ChunkMessage> chunkifyByteArray(ActorRef sender, ActorRef receiver, LargeMessage<?> message, UUID messageId) {
        ByteArrayOutputStream data = freezeMessage(message);
        int chunkCount = data.size() / CHUNK_MAX_BYTES + 1;

        List<ChunkMessage> chunkMessages = new ArrayList<>();
        for (int i = 0; i < chunkCount; i++) {
            int start = i * CHUNK_MAX_BYTES;
            int copySize = CHUNK_MAX_BYTES;
            if (i == chunkCount - 1) {
                copySize = (data.size() - start) % CHUNK_MAX_BYTES;
            }

            chunkMessages.add(
                    new ChunkMessage(
                            Arrays.copyOfRange(data.toByteArray(), start, start + copySize),
                            sender,
                            receiver,
                            i,
                            chunkCount,
                            message.message.getClass(),
                            messageId.toString()
                    )
            );
        }

        return chunkMessages;
    }

    private ByteArrayOutputStream freezeMessage(LargeMessage<?> message) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        Output output = new Output(byteArrayOutputStream);
        kryo.writeObject(output, message.getMessage());
        output.flush();
        output.close();
        return byteArrayOutputStream;
    }

    private void handle(BytesMessage<?> message) {
        // Reassemble the message content, deserialize it and/or load the content from some local location before forwarding its content.
        message.getReceiver().tell(message.getBytes(), message.getSender());
    }
}
