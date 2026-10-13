package dev.orin.desktop.protocol;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AudioPacketCodecTest {
    @Test
    void shouldEncodeAudioPacket() {
        byte[] pcm = new byte[960];
        byte[] encoded = AudioPacketCodec.encode(1, 8, pcm);

        assertEquals(976, encoded.length);
    }

    @Test
    void shouldEncodeCorrectHeader() {
        byte[] pcm = new byte[AudioPacketCodec.PCM_FRAME_SIZE];
        byte[] encoded = AudioPacketCodec.encode(42, 480, pcm);

        ByteBuffer buffer = ByteBuffer.wrap(encoded)
            .order(ByteOrder.BIG_ENDIAN);


        assertEquals(0x4F52494E, buffer.getInt());
        assertEquals(1, buffer.get());
        assertEquals(1, buffer.get());
        assertEquals(42, buffer.getInt());
        assertEquals(480, buffer.getInt());
        assertEquals(960, buffer.getShort());
    }

    @Test
    void shouldEncodeHeaderInBigEndianWithoutChangingPcmBytes() {
        byte[] pcm = new byte[AudioPacketCodec.PCM_FRAME_SIZE];
        pcm[0] = 0x34;
        pcm[1] = 0x12;
        pcm[959] = (byte) 0xFF;

        byte[] encoded = AudioPacketCodec.encode(0x01020304, 0x05060708, pcm);
        byte[] expectedHeader = {
            0x4F, 0x52, 0x49, 0x4E, 1, 1,
            1, 2, 3, 4, 5, 6, 7, 8, 0x03, (byte) 0xC0
        };

        assertArrayEquals(expectedHeader, Arrays.copyOfRange(encoded, 0, 16));
        assertArrayEquals(pcm, Arrays.copyOfRange(encoded, 16, encoded.length));
    }

    @Test
    void shouldRejectNullPcm() {
        assertThrows(NullPointerException.class, () -> AudioPacketCodec.encode(1, 8, null));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 959, 961})
    void shouldRejectInvalidPcmSize(int length) {
        assertThrows(IllegalArgumentException.class,
            () -> AudioPacketCodec.encode(1, 8, new byte[length]));
    }






}
