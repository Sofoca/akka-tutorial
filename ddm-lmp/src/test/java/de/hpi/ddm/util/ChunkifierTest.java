package de.hpi.ddm.util;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
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
        List<byte[]> chunks = Chunkifier.chunkify(msg, CHUNK_SIZE);
        byte[] blob = Chunkifier.blobify(chunks, CHUNK_SIZE);
        String result = new Kryo().readObject(new Input(blob), String.class);

        assertEquals(result, messageContent);
    }
}