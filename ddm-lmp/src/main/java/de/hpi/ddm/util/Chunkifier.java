package de.hpi.ddm.util;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;

public class Chunkifier {
    private static final Kryo kryo = new Kryo();

    public static Object unchunkify(List<byte[]> data, int chunkSize, Class tClass) {
        int fullChunkSize = chunkSize * (data.size() - 1);
        int lastChunkSize = data.get(data.size() - 1).length;

        ByteBuffer writeBuffer = ByteBuffer.wrap(new byte[fullChunkSize + lastChunkSize]);
        data.forEach(writeBuffer::put);
        //noinspection unchecked
        return new Kryo().readObject(new Input(writeBuffer.array()), tClass);
    }

    public static List<byte[]> chunkify(Object object, int chunkSize) {
        Output output = new Output(new ByteArrayOutputStream());
        kryo.writeObject(output, object);
        output.close();

        ByteBuffer byteBuffer = ByteBuffer.wrap(((ByteArrayOutputStream) output.getOutputStream()).toByteArray()); // doesn't copy
        List<byte[]> chunks = new LinkedList<>();
        while (byteBuffer.hasRemaining()) {
            byte[] chunk = new byte[Math.min(byteBuffer.remaining(), chunkSize)];
            byteBuffer.get(chunk); // copies
            chunks.add(chunk);
        }

        return chunks;
    }
}
