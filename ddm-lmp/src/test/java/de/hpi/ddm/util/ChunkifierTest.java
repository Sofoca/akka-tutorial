package de.hpi.ddm.util;

import de.hpi.ddm.actors.LargeMessageProxy;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class ChunkifierTest {
    private static final int CHUNK_SIZE = 4;

    @Test
    public void canSerializeAndDeserialize() {
        String messageContent = "itsATrap";
        LargeMessageProxy.LargeMessage<String> msg = new LargeMessageProxy.LargeMessage<>(messageContent, null);

        Class class_ = msg.getClass();

        List<byte[]> chunks = Chunkifier.chunkify(msg, CHUNK_SIZE);

        Object blob = Chunkifier.unchunkify(chunks, CHUNK_SIZE, class_);

        assertEquals(msg, blob);
        assertEquals(msg.getMessage(), ((LargeMessageProxy.LargeMessage<String>) blob).getMessage());
    }
}