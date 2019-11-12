package de.hpi.ddm.actors;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

import akka.actor.*;
import de.hpi.ddm.util.Chunkifier;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import akka.stream.*;
import akka.stream.javadsl.*;

public class LargeMessageProxy extends AbstractLoggingActor {

    ////////////////////////
    // Actor Construction //
    ////////////////////////

    public static final String DEFAULT_NAME = "largeMessageProxy";
    public static final int CHUNK_SIZE = 1024;

    private final ActorMaterializer materializer = ActorMaterializer.create(getContext());

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
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SourceSaysHello implements Serializable {
        private static final long serialVersionUID = 9059032848123489012L;
        private SourceRef<byte[]> sourceRef;
        private Class dataType;
        private ActorRef sender;
        private ActorRef receiver;
    }

    /////////////////
    // Actor State //
    /////////////////

    /////////////////////
    // Actor Lifecycle //
    /////////////////////

    ////////////////////
    // Actor Behavior //
    ////////////////////

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(LargeMessage.class, this::handle)
                .match(SourceSaysHello.class, this::handle)
                .matchAny(object -> this.log().info("Received unknown message: \"{}\"", object.toString()))
                .build();
    }

    private void handle(SourceSaysHello sourceSaysHello) {
        List<byte[]> chunks = new LinkedList<>();

        sourceSaysHello.getSourceRef().getSource()
                .runForeach(chunks::add, materializer)
                .thenApply(ignore -> Chunkifier.unchunkify(chunks, CHUNK_SIZE, sourceSaysHello.dataType))
                .thenAccept(data -> sourceSaysHello.getReceiver().tell(data, sourceSaysHello.getSender()));
    }

    private void handle(LargeMessage<?> message) {
        ActorRef receiver = message.getReceiver();
        ActorSelection receiverProxy = this.context().actorSelection(receiver.path().child(DEFAULT_NAME));
		Source.from(Chunkifier.chunkify(message.getMessage(), CHUNK_SIZE))
				.runWith(StreamRefs.sourceRef(), materializer)
				.thenApply((sourceRef) -> new SourceSaysHello(
						sourceRef,
						message.getMessage().getClass(),
						this.getSender(),
						message.getReceiver()
				))
				.thenAccept(msg -> receiverProxy.tell(msg, this.getSender()));
        }
}
