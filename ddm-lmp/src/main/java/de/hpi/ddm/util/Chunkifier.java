package de.hpi.ddm.util;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import de.hpi.ddm.actors.LargeMessageProxy;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;

public class Chunkifier {
    private static final Kryo kryo = new Kryo();

    public static byte[] blobify(List<byte[]> data, int chunkSize) {
        int fullChunkSize = chunkSize * (data.size() - 1);
        int lastChunkSize = data.get(data.size() - 1).length;

        ByteBuffer writeBuffer = ByteBuffer.wrap(new byte[fullChunkSize + lastChunkSize]);
        data.forEach(writeBuffer::put);

        return writeBuffer.array();

    }

    public static List<byte[]> chunkify(LargeMessageProxy.LargeMessage<?> msg, int chunkSize) {
        Output output = new Output(new ByteArrayOutputStream());
        kryo.writeObject(output, msg.getMessage());
        output.close();

        ByteBuffer byteBuffer = ByteBuffer.wrap(((ByteArrayOutputStream) output.getOutputStream()).toByteArray()); // doesn't copy
        List<byte[]> chunks = new LinkedList<>();
        while (byteBuffer.hasRemaining()) {
            byte[] chunk = new byte[chunkSize];
            byteBuffer.get(chunk); // copies
            chunks.add(chunk);
        }

        return chunks;
    }
}
