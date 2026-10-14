package dev.orin.desktop.network;

import dev.orin.desktop.protocol.AudioPacket;
import dev.orin.desktop.protocol.AudioPacketCodec;
import org.junit.jupiter.api.Test;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class UdpAudioReceiverTest {
    @Test
    void shouldReceiveUdpPacket() throws Exception {
        try (var executor = Executors.newSingleThreadExecutor();
             UdpAudioReceiver receiver = new UdpAudioReceiver(0);
             DatagramSocket sender = new DatagramSocket()) {
            byte[] expected = {10, 20, 30, 40};
            var received = executor.submit(receiver::receive);

            DatagramPacket packet = new DatagramPacket(
                expected,
                expected.length,
                InetAddress.getLoopbackAddress(),
                receiver.getPort()
            );

            sender.send(packet);

            byte[] actual = received.get(2, TimeUnit.SECONDS);

            assertArrayEquals(expected, actual);
        }
    }

    @Test
    void shouldTimeOutWhenNoPacketArrives() throws Exception {
        try (var executor = Executors.newSingleThreadExecutor();
             UdpAudioReceiver receiver = new UdpAudioReceiver(0, 100)) {
            var received = executor.submit(receiver::receive);

            ExecutionException failure = assertThrows(ExecutionException.class,
                () -> received.get(2, TimeUnit.SECONDS));

            assertInstanceOf(SocketTimeoutException.class, failure.getCause());
        }
    }

    @Test
    void shouldReceiveCompleteAudioDatagram() throws Exception {
        try (var executor = Executors.newSingleThreadExecutor();
             UdpAudioReceiver receiver = new UdpAudioReceiver(0);
             DatagramSocket sender = new DatagramSocket()) {

            byte[] pcm = new byte[AudioPacketCodec.PCM_FRAME_SIZE];
            byte[] expected = AudioPacketCodec.encode(1, 0, pcm);
            var received = executor.submit(receiver::receive);

            DatagramPacket packet = new DatagramPacket(
                expected,
                expected.length,
                InetAddress.getLoopbackAddress(),
                receiver.getPort()
            );

            sender.send(packet);

            byte[] actual = received.get(2, TimeUnit.SECONDS);

            assertArrayEquals(expected, actual);
        }
    }

    @Test
    void shouldRejectOversizedAudioDatagram() throws Exception {
        try (var executor = Executors.newSingleThreadExecutor();
             UdpAudioReceiver receiver = new UdpAudioReceiver(0);
             DatagramSocket sender = new DatagramSocket()) {

            byte[] oversized = new byte[AudioPacketCodec.DATAGRAM_SIZE + 1];
            byte[] expected = new byte[AudioPacketCodec.DATAGRAM_SIZE + 1];
            var received = executor.submit(receiver::receive);

            DatagramPacket packet = new DatagramPacket(
                expected,
                expected.length,
                InetAddress.getLoopbackAddress(),
                receiver.getPort()
            );

            sender.send(packet);

            byte[] actual = received.get(2, TimeUnit.SECONDS);

            assertEquals(977, actual.length);

            assertThrows(
                IllegalArgumentException.class,
                () -> AudioPacketCodec.decode(actual)
            );
        }
    }

    @Test
    void shouldRejectTruncatedDatagramWithValidPrefix() throws Exception {
        try (var executor = Executors.newSingleThreadExecutor();
             UdpAudioReceiver receiver = new UdpAudioReceiver(0);
             DatagramSocket sender = new DatagramSocket()) {
            byte[] valid = AudioPacketCodec.encode(1, 480,
                new byte[AudioPacketCodec.PCM_FRAME_SIZE]);
            byte[] oversized = Arrays.copyOf(valid, valid.length + 32);
            Arrays.fill(oversized, valid.length, oversized.length, (byte) 42);
            var received = executor.submit(receiver::receive);

            sender.send(new DatagramPacket(
                oversized,
                oversized.length,
                InetAddress.getLoopbackAddress(),
                receiver.getPort()
            ));

            byte[] actual = received.get(2, TimeUnit.SECONDS);

            assertEquals(AudioPacketCodec.DATAGRAM_SIZE + 1, actual.length);
            assertArrayEquals(valid, Arrays.copyOf(actual, valid.length));
            assertEquals((byte) 42, actual[valid.length]);
            assertThrows(IllegalArgumentException.class,
                () -> AudioPacketCodec.decode(actual));
        }
    }

    @Test
    void shouldDecodeReceivedAudioDatagram() throws Exception {
        try (var executor = Executors.newSingleThreadExecutor();
             UdpAudioReceiver receiver = new UdpAudioReceiver(0);
             DatagramSocket sender = new DatagramSocket()) {

            byte[] pcm = new byte[AudioPacketCodec.PCM_FRAME_SIZE];
            pcm[0] = 10;
            pcm[1] = 20;
            pcm[2] = 30;

            int sequence = 5;
            int timestamp = 480;

            byte[] expected = AudioPacketCodec.encode(sequence, timestamp, pcm);

            var received = executor.submit(receiver::receive);

            DatagramPacket packet = new DatagramPacket(
                expected,
                expected.length,
                InetAddress.getLoopbackAddress(),
                receiver.getPort()
            );

            sender.send(packet);

            byte[] actual = received.get(2, TimeUnit.SECONDS);

            AudioPacket decoded = AudioPacketCodec.decode(actual);

            assertEquals(sequence, decoded.sequence());
            assertEquals(timestamp, decoded.timestamp());
            assertArrayEquals(pcm, decoded.pcm());
        }
    }
}
