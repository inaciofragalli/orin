package dev.orin.desktop.protocol;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Objects;

public final class AudioPacketCodec {
    public static final int HEADER_SIZE = 16;
    public static final int PCM_FRAME_SIZE = 960;
    public static final int DATAGRAM_SIZE = HEADER_SIZE + PCM_FRAME_SIZE;

    private AudioPacketCodec() {
    }

    public static byte[] encode(int sequence, int timestamp, byte[] pcm) {
        Objects.requireNonNull(pcm, "PCM cannot be null");

        if (pcm.length != PCM_FRAME_SIZE) {
            throw new IllegalArgumentException("Invalid PCM frame size");
        }

        ByteBuffer buffer = ByteBuffer.allocate(DATAGRAM_SIZE)
            .order(ByteOrder.BIG_ENDIAN);

        buffer.putInt(0x4F52494E);          // "ORIN"
        buffer.put((byte) 1);                     // Version
        buffer.put((byte) 1);                     // Audio packet type
        buffer.putInt(sequence);
        buffer.putInt(timestamp);
        buffer.putShort((short) pcm.length);
        buffer.put(pcm);

        return buffer.array();
    }

    public static AudioPacket decode(byte[] datagram) {
        Objects.requireNonNull(datagram, "Datagram cannot be null");

        if (datagram.length != DATAGRAM_SIZE) {
            throw new IllegalArgumentException("Invalid Datagram size");
        }

        ByteBuffer buffer = ByteBuffer.wrap(datagram)
            .order(ByteOrder.BIG_ENDIAN);

        int magic = buffer.getInt();
        int version = Byte.toUnsignedInt(buffer.get());
        int type = Byte.toUnsignedInt(buffer.get());

        if (magic != 0x4F52494E || version != 1 || type != 1) {
            throw new IllegalArgumentException("Invalid packet header");
        }

        int sequence = buffer.getInt();
        int timestamp = buffer.getInt();
        int length = Short.toUnsignedInt(buffer.getShort());

        if (length != PCM_FRAME_SIZE) {
            throw new IllegalArgumentException("Invalid PCM length");
        }

        byte[] pcm = new byte[length];
        buffer.get(pcm);

        return new AudioPacket(sequence, timestamp, pcm);
    }
}
