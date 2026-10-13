package dev.orin.desktop.audio;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import java.util.Objects;

public final class PcmAudioOutput implements AutoCloseable{
    private final AudioFormat format;
    private SourceDataLine line;
    private final AudioLineFactory lineFactory;

    public PcmAudioOutput(AudioFormat format) {
        this(format, AudioSystem::getSourceDataLine);
    }

    PcmAudioOutput(AudioFormat format, AudioLineFactory lineFactory) {
        this.format = Objects.requireNonNull(format);
        this.lineFactory = Objects.requireNonNull(lineFactory);
    }

    public void open() throws LineUnavailableException {
        if (line != null && line.isOpen()) {
            throw new IllegalStateException("Audio output is already open");
        }

        SourceDataLine newLine = lineFactory.create(format);

        try {
            newLine.open(format);
            newLine.start();
            line = newLine;
        } catch (LineUnavailableException | RuntimeException e) {
            newLine.close();
            throw e;
        }
    }

    public void write(byte[] pcm) {
        Objects.requireNonNull(pcm, "PCM data cannot be null");

        if (line == null || !line.isOpen()) {
            throw new IllegalStateException("Audio output is not open");
        }

        if (pcm.length == 0) {
            return;
        }

        int offset = 0;
        while (offset < pcm.length) {
            if (!line.isOpen()) {
                throw new IllegalStateException("Audio output is not open");
            }
            int written = line.write(pcm, offset, pcm.length - offset);
            if (written <= 0) {
                throw new IllegalStateException("Audio output made no progress");
            }
            offset += written;
        }
    }

    public void drain() {
        if (line == null || !line.isOpen()) {
            throw new IllegalStateException("Audio output is not open");
        }
        line.drain();
    }

    @Override
    public void close() {
        if (line == null) {
            return;
        }

        line.stop();
        line.flush();
        line.close();
        line = null;
    }
}
