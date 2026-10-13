package dev.orin.desktop.network;

import org.junit.jupiter.api.Test;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

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

}
