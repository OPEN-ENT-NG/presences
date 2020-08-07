package fr.openent.presences.common.eventbus;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.MessageCodec;

import java.io.*;

public class GenericCodec<T> implements MessageCodec<T, T> {
    private final Class<T> cls;

    public GenericCodec(Class<T> cls) {
        super();
        this.cls = cls;
    }


    @Override
    public void encodeToWire(Buffer buffer, T s) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutput out = null;
        try {
            out = new ObjectOutputStream(bos);
            out.writeObject(s);
            out.flush();
            byte[] yourBytes = bos.toByteArray();
            buffer.appendInt(yourBytes.length);
            buffer.appendBytes(yourBytes);
            out.close();
        } catch (IOException e) {
        } finally {
            try {
                bos.close();
            } catch (IOException ex) {
            }
        }
    }

    @Override
    public T decodeFromWire(int pos, Buffer buffer) {
        // My custom message starting from this *position* of buffer
        int _pos = pos;

        // Length of JSON
        int length = buffer.getInt(_pos);

        // Jump 4 because getInt() == 4 bytes
        byte[] yourBytes = buffer.getBytes(_pos += 4, _pos += length);
        ByteArrayInputStream bis = new ByteArrayInputStream(yourBytes);
        try {
            ObjectInputStream ois = new ObjectInputStream(bis);
            @SuppressWarnings("unchecked")
            T msg = (T) ois.readObject();
            ois.close();
            return msg;
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("Listen failed " + e.getMessage());
        } finally {
            try {
                bis.close();
            } catch (IOException e) {
            }
        }
        return null;
    }

    @Override
    public T transform(T customMessage) {
        // If a message is sent *locally* across the event bus.
        // This example sends message just as is
        return customMessage;
    }

    @Override
    public String name() {
        // Each codec must have a unique name.
        // This is used to identify a codec when sending a message and for unregistering
        // codecs.
        return cls.getSimpleName() + "Codec";
    }

    @Override
    public byte systemCodecID() {
        // Always -1
        return -1;
    }
}
