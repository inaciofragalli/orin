package dev.orin.desktop.diagnostics;

import dev.orin.desktop.protocol.AudioPacketCodec;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

public class UdpAudioSenderCheck {
    private static final int PORT = 50000;
    private static final int SAMPLE_RATE = 48_000;
    private static final int SAMPLES_PER_FRAME = 480;
    private static final int FRAME_COUNT = 300;

    public static void main(String[] args) throws Exception {
        InetAddress destination =
            InetAddress.getLoopbackAddress();

        try (DatagramSocket sender = new DatagramSocket()) {
            long nextFrameTime = System.nanoTime();

            for (int frame = 0; frame < FRAME_COUNT; frame++) {
                byte[] pcm = generateTone(frame);

                byte[] encoded = AudioPacketCodec.encode(
                    frame,
                    frame * SAMPLES_PER_FRAME,
                    pcm
                );

                DatagramPacket packet = new DatagramPacket(
                    encoded,
                    encoded.length,
                    destination,
                    PORT
                );

                sender.send(packet);

                nextFrameTime += TimeUnit.MILLISECONDS.toNanos(10);
                long wait = nextFrameTime - System.nanoTime();

                if (wait > 0) {
                    LockSupport.parkNanos(wait);
                }
            }
        }

        System.out.println("Envio concluído!");
    }

    private static byte[] generateTone(int frame) {
        byte[] pcm = new byte[AudioPacketCodec.PCM_FRAME_SIZE];

        for (int i = 0; i < SAMPLES_PER_FRAME; i++) {
            int sampleIndex = frame * SAMPLES_PER_FRAME + i;

            double wave = Math.sin(
                2 * Math.PI * 440 * sampleIndex / SAMPLE_RATE
            );

            short sample = (short) (
                wave * Short.MAX_VALUE * 0.1
            );

            pcm[i * 2] = (byte) sample;
            pcm[i * 2 + 1] = (byte) (sample >> 8);
        }

        return pcm;
    }
}
