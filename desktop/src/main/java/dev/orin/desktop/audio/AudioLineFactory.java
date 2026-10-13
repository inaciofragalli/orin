package dev.orin.desktop.audio;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

@FunctionalInterface
public interface AudioLineFactory {
    SourceDataLine create(AudioFormat audioFormat) throws LineUnavailableException;
}
