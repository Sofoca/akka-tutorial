package de.hpi.ddm.actors;

import akka.actor.*;
import akka.cluster.Cluster;
import akka.cluster.ClusterEvent.CurrentClusterState;
import akka.cluster.ClusterEvent.MemberRemoved;
import akka.cluster.ClusterEvent.MemberUp;
import akka.cluster.Member;
import akka.cluster.MemberStatus;
import de.hpi.ddm.MasterSystem;
import de.hpi.ddm.util.HintCracker;
import de.hpi.ddm.util.PasswordCracker;
import de.hpi.ddm.util.workmanager.tasks.HintSolution;
import de.hpi.ddm.util.workmanager.tasks.HintTask;
import de.hpi.ddm.util.workmanager.tasks.PasswordSolution;
import de.hpi.ddm.util.workmanager.tasks.PasswordTask;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

public class Worker extends AbstractLoggingActor {

    ////////////////////////
    // Actor Construction //
    ////////////////////////

    public static final String DEFAULT_NAME = "worker";
    private ActorSelection masterActor;
    private HintCracker hintCracker = null;

    public static Props props() {
        return Props.create(Worker.class);
    }

    public Worker() {
        this.cluster = Cluster.get(this.context().system());
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    public static class WorkRequest implements Serializable {
        private static final long serialVersionUID = 3951510940074392007L;
        public ActorRef worker;
    }

    ////////////////////
    // Actor Messages //
    ////////////////////

    /////////////////
    // Actor State //
    /////////////////

    private Member masterSystem;
    private final Cluster cluster;

    /////////////////////
    // Actor Lifecycle //
    /////////////////////

    @Override
    public void preStart() {
        Reaper.watchWithDefaultReaper(this);

        this.cluster.subscribe(this.self(), MemberUp.class, MemberRemoved.class);
    }

    @Override
    public void postStop() {
        this.cluster.unsubscribe(this.self());
    }

    ////////////////////
    // Actor Behavior //
    ////////////////////

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(CurrentClusterState.class, this::handle)
                .match(MemberUp.class, this::handle)
                .match(MemberRemoved.class, this::handle)
                .match(Master.WorkerStartMessage.class, this::handle)
                //.match(PasswordSolution.class, this::handle)
                .match(PasswordTask.class, this::handle)
                .match(HintTask.class, this::handle)
                //.match(HintSolution.class, this::handle)
                .matchAny(object -> this.log().info("Received unknown message: \"{}\"", object.toString()))
                .build();
    }

    private void handle(Master.WorkerStartMessage startMessage) {
        hintCracker = new HintCracker(startMessage.getPossibleChars());
        this.masterActor.tell(new WorkRequest(this.getSelf()), this.self());
    }

    private void handle(PasswordTask passwordTask) {
        PasswordCracker passwordCracker = new PasswordCracker(
                passwordTask.passwordChars,
                passwordTask.passwordLength,
                passwordTask.passwordHash,
                passwordTask.hints
        );

        masterActor.tell(
                new PasswordSolution(passwordCracker.crack(), passwordTask.passwordId),
                this.self()
        );
        this.masterActor.tell(new WorkRequest(this.getSelf()), this.self());
    }

    private void handle(HintTask hintTask) {
        String solution = hintCracker.crack(hintTask.target);
        masterActor.tell(
                new HintSolution(solution, hintTask.hintId, hintTask.passwordId),
                this.self()
        );
        log().info("got hint solution " + solution + " for " + hintTask.hintId);
        this.masterActor.tell(new WorkRequest(this.getSelf()), this.self());
    }

    private void handle(CurrentClusterState message) {
        message.getMembers().forEach(member -> {
            if (member.status().equals(MemberStatus.up()))
                this.register(member);
        });
    }

    private void handle(MemberUp message) {
        this.register(message.member());
    }

    private void register(Member member) {
        if ((this.masterSystem == null) && member.hasRole(MasterSystem.MASTER_ROLE)) {
            this.masterSystem = member;

            this.masterActor =
                    this.getContext().actorSelection(member.address() + "/user/" + Master.DEFAULT_NAME);

            this.masterActor.tell(new Master.RegistrationMessage(), this.self());
        }
    }

    private void handle(MemberRemoved message) {
        if (this.masterSystem.equals(message.member()))
            this.self().tell(PoisonPill.getInstance(), ActorRef.noSender());
    }
}