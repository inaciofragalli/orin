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

    @Test
    void shouldDecodeIndependentDatagram() {
        byte[] datagram = new byte[976];
        byte[] header = {
            0x4F, 0x52, 0x49, 0x4E, 1, 1,
            1, 2, 3, 4, 5, 6, 7, 8, 0x03, (byte) 0xC0
        };
        System.arraycopy(header, 0, datagram, 0, header.length);
        datagram[16] = 0x34;
        datagram[17] = 0x12;
        datagram[975] = (byte) 0xFF;

        AudioPacket decoded = AudioPacketCodec.decode(datagram);

        assertEquals(0x01020304, decoded.sequence());
        assertEquals(0x05060708, decoded.timestamp());
        assertArrayEquals(Arrays.copyOfRange(datagram, 16, 976), decoded.pcm());
    }

    @Test
    void shouldRejectNullDatagram() {
        assertThrows(NullPointerException.class, () -> AudioPacketCodec.decode(null));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 15, 975, 977})
    void shouldRejectInvalidDatagramSize(int length) {
        assertThrows(IllegalArgumentException.class,
            () -> AudioPacketCodec.decode(new byte[length]));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 4, 5})
    void shouldRejectInvalidHeader(int offset) {
        byte[] datagram = AudioPacketCodec.encode(1, 8, new byte[960]);
        datagram[offset] ^= 1;

        assertThrows(IllegalArgumentException.class, () -> AudioPacketCodec.decode(datagram));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 959, 961})
    void shouldRejectInvalidDeclaredPcmLength(int length) {
        byte[] datagram = AudioPacketCodec.encode(1, 8, new byte[960]);
        ByteBuffer.wrap(datagram).order(ByteOrder.BIG_ENDIAN).putShort(14, (short) length);

        assertThrows(IllegalArgumentException.class, () -> AudioPacketCodec.decode(datagram));
    }

    @Test
    void shouldDecodeEncodedAudioPacket() {
        byte[] pcm = new byte[AudioPacketCodec.PCM_FRAME_SIZE];
        pcm[0] = 42;
        pcm[959] = 99;

        byte[] encoded = AudioPacketCodec.encode(7, 480, pcm);
        AudioPacket decoded = AudioPacketCodec.decode(encoded);

        assertEquals(7, decoded.sequence());
        assertEquals(480, decoded.timestamp());
        assertArrayEquals(pcm, decoded.pcm());
    }
}
