package dev.orin.desktop.diagnostics;

import dev.orin.desktop.audio.PcmAudioOutput;

import javax.sound.sampled.AudioFormat;

public class AudioOutputManualCheck {
    public static void main(String[] args) throws Exception {
        int sampleRate = 48_000;
        int duration = 1;
        int frequency = 440;

        AudioFormat format = new AudioFormat(sampleRate, 16, 1, true, false);

        byte[] pcm = new byte[sampleRate * duration * 2];

        for (int i = 0; i < sampleRate * duration; i++) {
            double wave = Math.sin(2 * Math.PI * frequency * i / sampleRate);

            short sample = (short) (wave * Short.MAX_VALUE * 0.1);

            pcm[i * 2] = (byte) sample;
            pcm[i * 2 + 1] = (byte) (sample >> 8);
        }

        try (PcmAudioOutput output = new PcmAudioOutput(format)) {
            output.open();
            output.write(pcm);
            output.drain();
        }
    }
}
