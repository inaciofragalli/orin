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

}
