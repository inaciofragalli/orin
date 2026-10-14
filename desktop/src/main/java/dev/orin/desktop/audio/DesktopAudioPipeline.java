package dev.orin.desktop.audio;

import dev.orin.desktop.network.UdpAudioReceiver;
import dev.orin.desktop.protocol.AudioPacketCodec;

import java.io.IOException;
import java.util.Objects;

public final class DesktopAudioPipeline {
    private final UdpAudioReceiver receiver;
    private final PcmAudioOutput output;

    public DesktopAudioPipeline(
        UdpAudioReceiver receiver,
        PcmAudioOutput output
    ) {
        this.receiver = Objects.requireNonNull(receiver);
        this.output = Objects.requireNonNull(output);
    }

    public void receiveAndPlayNext() throws IOException {
        byte[] datagram = receiver.receive();
        byte[] pcm = AudioPacketCodec.decode(datagram).pcm();
        output.write(pcm);
    }
}
