package de.hpi.ddm.actors;

import static org.junit.Assert.assertTrue;

import java.io.Serializable;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

import akka.cluster.Cluster;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import akka.actor.AbstractLoggingActor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.testkit.javadsl.TestKit;
import de.hpi.ddm.MasterSystem;
import de.hpi.ddm.configuration.Configuration;
import de.hpi.ddm.configuration.ConfigurationSingleton;
import scala.concurrent.Await;

public class SystemTest {

    private static ActorSystem system;
    private Configuration c;

    private static class TestCollector extends AbstractLoggingActor {

        ////////////////////////
        // Actor Construction //
        ////////////////////////

        public static final String DEFAULT_NAME = "collector";
        private final ActorRef parent;

        public static Props props(ActorRef parent) {
            return Props.create(TestCollector.class, () -> new TestCollector(parent));
        }

        TestCollector(ActorRef parent) {
            this.parent = parent;
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
                    .match(de.hpi.ddm.actors.Collector.CollectMessage.class, this::handle)
                    .match(de.hpi.ddm.actors.Collector.PrintMessage.class, this::handle)
                    .matchAny(object -> this.log().info("Received unknown message: \"{}\"", object.toString()))
                    .build();
        }

        private void handle(de.hpi.ddm.actors.Collector.CollectMessage message) {
            this.results.addAll(message.getResult());
        }

        private void handle(de.hpi.ddm.actors.Collector.PrintMessage message) {
            results.forEach(result -> parent.tell(this.results, getSelf()));
        }
    }


    @Before
    public void setUp() throws Exception {
        c = ConfigurationSingleton.get();

        final Config config = ConfigFactory.parseString(
                "akka.remote.artery.canonical.hostname = \"" + c.getHost() + "\"\n" +
                        "akka.remote.artery.canonical.port = " + c.getPort() + "\n" +
                        "akka.cluster.roles = [" + MasterSystem.MASTER_ROLE + "]\n" +
                        "akka.cluster.seed-nodes = [\"akka://" + c.getActorSystemName() + "@" + c.getMasterHost() + ":" + c.getMasterPort() + "\"]")
                .withFallback(ConfigFactory.load("application"));

        system = ActorSystem.create(c.getActorSystemName(), config);
    }

    @After
    public void tearDown() throws Exception {
        TestKit.shutdownActorSystem(system);
    }

    @Test @Ignore
    public void testSystem() {
        new TestKit(system) {
            {
                ActorRef collector = system.actorOf(TestCollector.props(getRef()), Collector.DEFAULT_NAME);
                MasterSystem.start();

				within(Duration.ofSeconds(2), () -> {
					// Test if a large message gets passed from one proxy to the other

					this.expectMsg(null);

					// Will wait for the rest of the within duration
					expectNoMessage();
					return null;
				});
            }
        };
    }
}
