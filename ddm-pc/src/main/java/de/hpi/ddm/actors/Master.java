package de.hpi.ddm.actors;

import java.io.Serializable;
import java.util.*;

import akka.actor.AbstractLoggingActor;
import akka.actor.ActorRef;
import akka.actor.PoisonPill;
import akka.actor.Props;
import akka.actor.Terminated;
import de.hpi.ddm.util.workmanager.WorkManager;
import de.hpi.ddm.util.workmanager.tasks.HintSolution;
import de.hpi.ddm.util.workmanager.tasks.PasswordSolution;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

public class Master extends AbstractLoggingActor {

    ////////////////////////
    // Actor Construction //
    ////////////////////////

    public static final String DEFAULT_NAME = "master";
    private final WorkManager workManager;
    private boolean startedWorkers = false;
    private char[] possibleChars = null;


    public static Props props(final ActorRef reader, final ActorRef collector) {
        return Props.create(Master.class, () -> new Master(reader, collector));
    }

    private Master(final ActorRef reader, final ActorRef collector) {
        this.reader = reader;
        this.collector = collector;
        this.workers = new ArrayList<>();
        this.workManager = new WorkManager();
    }

    ////////////////////
    // Actor Messages //
    ////////////////////
    @Data
    public static class StartMessage implements Serializable {
        private static final long serialVersionUID = -50374816448627600L;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WorkerStartMessage implements Serializable {
        private static final long serialVersionUID = -50374816448627600L;

        public char[] possibleChars;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class BatchMessage implements Serializable {
        private static final long serialVersionUID = 8343040942748609598L;
        private List<Reader.PasswordLine> lines;
    }

    @Data
    static class RegistrationMessage implements Serializable {
        private static final long serialVersionUID = 3303081601659723997L;
    }

    /////////////////
    // Actor State //
    /////////////////

    private final ActorRef reader;
    private final ActorRef collector;
    private final List<ActorRef> workers;

    private long startTime;

    /////////////////////
    // Actor Lifecycle //
    /////////////////////

    @Override
    public void preStart() {
        Reaper.watchWithDefaultReaper(this);
    }

    ////////////////////
    // Actor Behavior //
    ////////////////////

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(StartMessage.class, this::handle)
                .match(BatchMessage.class, this::handle)
                .match(Terminated.class, this::handle)
                .match(HintSolution.class, this::handle)
                .match(PasswordSolution.class, this::handle)
                .match(Worker.WorkRequest.class, this::handle)
                .match(RegistrationMessage.class, this::handle)
                .matchAny(object -> this.log().info("Received unknown message: \"{}\"", object.toString()))
                .build();
    }

    private void handle(Worker.WorkRequest workRequest) {
        log().info("new workrequest");
        if (workManager.isFinished()) {
            log().info("workmanager is finished for now");
            this.reader.tell(new Reader.ReadMessage(), this.self());
            log().info("sent ReadMessage to reader");
        } else {
            log().info("workmanager not finished");
            if (workManager.hasTasks()) {
                workRequest.getWorker().tell(workManager.getWork(), this.getSelf());
                log().info("sent work to worker");
            } else {
                log().info("no work left to send to worker");
            }
        }
    }

    private void handle(HintSolution hintSolution) {
        log().info("Got hint solution " + hintSolution.getHint());
        workManager.addHintSolution(hintSolution);
    }

    private void handle(PasswordSolution passwordSolution) {
        log().info("Got password solution " + passwordSolution.getPassword());
        workManager.addPasswordSolution(passwordSolution);
    }

    private void handle(StartMessage message) {
        this.startTime = System.currentTimeMillis();

        this.reader.tell(new Reader.ReadMessage(), this.self());
    }

    private void handle(BatchMessage message) {

        ///////////////////////////////////////////////////////////////////////////////////////////////////////
        // The input file is read in batches for two reasons: /////////////////////////////////////////////////
        // 1. If we distribute the batches early, we might not need to hold the entire input data in memory. //
        // 2. If we process the batches early, we can achieve latency hiding. /////////////////////////////////
        ///////////////////////////////////////////////////////////////////////////////////////////////////////

        if (message.getLines().isEmpty()) {
            if (workManager.isFinished()) {
                log().info("workmanager is finished, collecting and terminating");
                this.collector.tell(new Collector.CollectMessage(workManager.getResults()), this.self());
                this.collector.tell(new Collector.PrintMessage(), this.self());
                this.terminate();
            }

            return;
        } else {
            possibleChars = message.getLines().get(0).getPasswordChars();
        }

        message.getLines().forEach(workManager::addWork);

        if (!startedWorkers) {
            workers.forEach(worker -> worker.tell(new WorkerStartMessage(possibleChars), this.self()));
            startedWorkers = true;
        }
    }

    private void terminate() {
        this.reader.tell(PoisonPill.getInstance(), ActorRef.noSender());
        this.collector.tell(PoisonPill.getInstance(), ActorRef.noSender());

        for (ActorRef worker : this.workers) {
            // this.context().unwatch(worker);
            this.getContext().stop(worker); // ungracefully stop actors
            // worker.tell(PoisonPill.getInstance(), ActorRef.noSender());
        }

        this.self().tell(PoisonPill.getInstance(), ActorRef.noSender());

        long executionTime = System.currentTimeMillis() - this.startTime;
        this.log().info("Algorithm finished in {} ms", executionTime);
        this.getContext().stop(this.self());
        this.getContext().getSystem().terminate();
    }

    private void handle(RegistrationMessage message) {
        this.context().watch(this.sender());
        this.workers.add(this.sender());
        if (startedWorkers) {
            this.sender().tell(new WorkerStartMessage(possibleChars), this.self());
        }
//		this.log().info("Registered {}", this.sender());
    }

    private void handle(Terminated message) {
        this.context().unwatch(message.getActor());
        this.workers.remove(message.getActor());
//		this.log().info("Unregistered {}", message.getActor());
    }
}
