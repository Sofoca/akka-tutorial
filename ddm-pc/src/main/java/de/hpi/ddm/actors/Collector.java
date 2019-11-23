package de.hpi.ddm.actors;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

import akka.actor.AbstractLoggingActor;
import akka.actor.Props;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

public class Collector extends AbstractLoggingActor {

    ////////////////////////
    // Actor Construction //
    ////////////////////////

    public static final String DEFAULT_NAME = "collector";

    public static Props props() {
        return Props.create(Collector.class);
    }

    ////////////////////
    // Actor Messages //
    ////////////////////

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class CollectMessage implements Serializable {
        private static final long serialVersionUID = -102767440935270949L;
        private Collection<String> result;
    }

    @Data
    static class PrintMessage implements Serializable {
        private static final long serialVersionUID = -267778464637901383L;
    }

    /////////////////
    // Actor State //
    /////////////////

    private List<String> results = new ArrayList<>();

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
                .match(CollectMessage.class, this::handle)
                .match(PrintMessage.class, this::handle)
                .matchAny(object -> this.log().info("Received unknown message: \"{}\"", object.toString()))
                .build();
    }

    private void handle(CollectMessage message) {
        this.results.addAll(message.getResult());
    }

    private void handle(PrintMessage message) {
        this.results.forEach(result -> this.log().info("{}", result));
    }
}
