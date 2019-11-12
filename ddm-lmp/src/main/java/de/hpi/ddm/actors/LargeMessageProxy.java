package de.hpi.ddm.actors;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletionStage;

import akka.NotUsed;
import akka.actor.*;
import akka.pattern.Patterns;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import de.hpi.ddm.util.Chunkifier;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import akka.stream.*;
import akka.stream.javadsl.*;
import scala.concurrent.ExecutionContextExecutor;
import scala.concurrent.duration.FiniteDuration;

public class LargeMessageProxy extends AbstractLoggingActor {

	////////////////////////
	// Actor Construction //
	////////////////////////

	public static final String DEFAULT_NAME = "largeMessageProxy";
	public static final int CHUNK_SIZE = 1024;

	public static Props props() {
		return Props.create(LargeMessageProxy.class);
	}

	////////////////////
	// Actor Messages //
	////////////////////

	@Data @NoArgsConstructor @AllArgsConstructor
	public static class LargeMessage<T> implements Serializable {
		private static final long serialVersionUID = 2940665245810221108L;
		private T message;
		private ActorRef receiver;
	}

	@Data @NoArgsConstructor @AllArgsConstructor
	public static class BytesMessage<T> implements Serializable {
		private static final long serialVersionUID = 4057807743872319842L;
		private T bytes;
		private ActorRef sender;
		private ActorRef receiver;
	}

	@Data @NoArgsConstructor @AllArgsConstructor
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
		List<byte[]> chunks = new LinkedList<byte[]>();
		sourceSaysHello.getSourceRef().getSource().runWith(Sink.foreach(chunk -> chunks.add(chunk)), ActorMaterializer.create(this.getContext()));
		Object objectAnswer = Chunkifier.unchunkify(chunks, CHUNK_SIZE, sourceSaysHello.dataType);
		LargeMessage message = (LargeMessage) objectAnswer;
		message.getReceiver().tell(message, sourceSaysHello.sender);
	}

	private void handle(LargeMessage<?> message) {
		ActorRef receiver = message.getReceiver();
		ActorSelection receiverProxy = this.context().actorSelection(receiver.path().child(DEFAULT_NAME));
		final Source<byte[], NotUsed> source = Source.from(Chunkifier.chunkify(message, CHUNK_SIZE));
		ActorMaterializer mat = ActorMaterializer.create(this.getContext());
		final CompletionStage<SourceRef<byte[]>> completionStage = source.runWith(StreamRefs.sourceRef(), mat);
		Patterns.pipe(completionStage.thenApply((ref) -> new SourceSaysHello(ref, message.getClass(), this.getSender(), message.getReceiver())), getContext().getDispatcher()).to(receiverProxy);

		// This will definitely fail in a distributed setting if the serialized message is large!
		// Solution options:
		// 1. Serialize the object and send its bytes batch-wise (make sure to use artery's side channel then).
		// 2. Serialize the object and send its bytes via Akka streaming.
		// 3. Send the object via Akka's http client-server component.
		// 4. Other ideas ...

		// receiverProxy.tell(new BytesMessage<>(message.getMessage(), this.sender(), message.getReceiver()), this.self());
	}

//	private void handle(BytesMessage<?> message) {
//		// Reassemble the message content, deserialize it and/or load the content from some local location before forwarding its content.
//		message.getReceiver().tell(message.getBytes(), message.getSender());
//	}
}
