package dev.orin.desktop.diagnostics;

import dev.orin.desktop.audio.DesktopAudioPipeline;
import dev.orin.desktop.audio.PcmAudioOutput;
import dev.orin.desktop.network.UdpAudioReceiver;

import javax.sound.sampled.AudioFormat;

public class UdpAudioReceiverCheck {
    private static final int PORT = 50000;
    private static final int FRAME_COUNT = 300;

    public static void main(String[] args) throws Exception {
        AudioFormat format = new AudioFormat(
            48_000, 16, 1, true, false
        );

        try (UdpAudioReceiver receiver =
                 new UdpAudioReceiver(PORT, 30_000);
             PcmAudioOutput output =
                 new PcmAudioOutput(format)) {

            DesktopAudioPipeline pipeline =
                new DesktopAudioPipeline(receiver, output);

            output.open();

            System.out.println("Aguardando áudio na porta " + PORT);

            for (int i = 0; i < FRAME_COUNT; i++) {
                pipeline.receiveAndPlayNext();
            }

            output.drain();
            System.out.println("Reprodução concluída!");
        }
    }
}
