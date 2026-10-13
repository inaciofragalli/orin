package dev.orin.desktop.audio;

import org.junit.jupiter.api.Test;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class PcmAudioOutputTest {
    @Test
    void shouldWritePcmDataToAudioLine() throws Exception {
        AudioFormat format = new AudioFormat(48000, 16, 1, true, false);

        SourceDataLine mockLine = mock(SourceDataLine.class);
        when(mockLine.isOpen()).thenReturn(true);

        byte[] pcm = {10, 20, 30, 40};
        when(mockLine.write(pcm, 0, pcm.length)).thenReturn(pcm.length);

        try (PcmAudioOutput output = new PcmAudioOutput(format, ignored -> mockLine)) {
            output.open();
            output.write(pcm);
        }

        verify(mockLine).write(pcm, 0, pcm.length);
    }

    @Test
    void shouldRejectWriteBeforeOpen() {
        AudioFormat format = new AudioFormat(48000, 16, 1, true, false);

        PcmAudioOutput output = new PcmAudioOutput(format);

        assertThrows(IllegalStateException.class, () -> output.write(new byte[]{10, 20}));
    }

    @Test
    void shouldRejectNullPcmData() {
        AudioFormat format = new AudioFormat(48000, 16, 1, true, false);

        PcmAudioOutput output = new PcmAudioOutput(format);

        assertThrows(NullPointerException.class, () -> output.write(null));
    }

    @Test
    void shouldRejectWriteAfterClose() throws LineUnavailableException {
        AudioFormat format = new AudioFormat(48000, 16, 1, true, false);

        SourceDataLine mockLine = mock(SourceDataLine.class);
        when(mockLine.isOpen()).thenReturn(true);

        PcmAudioOutput output = new PcmAudioOutput(format, ignored -> mockLine);

        output.open();
        output.close();

        verify(mockLine).close();
        var order = inOrder(mockLine);
        order.verify(mockLine).stop();
        order.verify(mockLine).flush();
        order.verify(mockLine).close();

        assertThrows(IllegalStateException.class,
            () -> output.write(new byte[]{10, 20}));
    }

    @Test
    void shouldRejectOpeningTwice() throws LineUnavailableException {
        AudioFormat format = new AudioFormat(48000, 16, 1, true, false);

        SourceDataLine mockLine = mock(SourceDataLine.class);
        when(mockLine.isOpen()).thenReturn(true);

        AudioLineFactory factory = mock(AudioLineFactory.class);
        when(factory.create(format)).thenReturn(mockLine);

        try (PcmAudioOutput output = new PcmAudioOutput(format, factory)) {
            output.open();
            assertThrows(IllegalStateException.class, output::open);
            verify(factory, times(1)).create(format);
        }
    }

    @Test
    void shouldOpenAndStartLineWithRequestedFormat() throws Exception {
        AudioFormat format = new AudioFormat(48000, 16, 1, true, false);
        SourceDataLine line = mock(SourceDataLine.class);
        AudioLineFactory factory = mock(AudioLineFactory.class);
        when(factory.create(format)).thenReturn(line);

        try (PcmAudioOutput output = new PcmAudioOutput(format, factory)) {
            output.open();
            var order = inOrder(factory, line);
            order.verify(factory).create(format);
            order.verify(line).open(format);
            order.verify(line).start();
        }
    }

    @Test
    void shouldAllowRetryAfterFactoryFailure() throws Exception {
        AudioFormat format = new AudioFormat(48000, 16, 1, true, false);
        AudioLineFactory factory = mock(AudioLineFactory.class);
        SourceDataLine line = mock(SourceDataLine.class);
        LineUnavailableException failure = new LineUnavailableException("Unavailable");
        when(factory.create(format)).thenThrow(failure).thenReturn(line);
        when(line.isOpen()).thenReturn(true);
        byte[] pcm = {10, 20};
        when(line.write(pcm, 0, pcm.length)).thenReturn(pcm.length);

        try (PcmAudioOutput output = new PcmAudioOutput(format, factory)) {
            assertSame(failure, assertThrows(LineUnavailableException.class, output::open));
            assertThrows(IllegalStateException.class, () -> output.write(pcm));
            output.open();
            output.write(pcm);
            verify(line).write(pcm, 0, pcm.length);
        }
    }

    @Test
    void shouldCloseFailedLineAndRetryAfterOpenFailure() throws Exception {
        AudioFormat format = new AudioFormat(48000, 16, 1, true, false);
        SourceDataLine failedLine = mock(SourceDataLine.class);
        SourceDataLine nextLine = mock(SourceDataLine.class);
        AudioLineFactory factory = mock(AudioLineFactory.class);
        when(factory.create(format)).thenReturn(failedLine, nextLine);
        LineUnavailableException failure = new LineUnavailableException("Unavailable");
        doThrow(failure).when(failedLine).open(format);

        try (PcmAudioOutput output = new PcmAudioOutput(format, factory)) {
            assertSame(failure, assertThrows(LineUnavailableException.class, output::open));
            verify(failedLine).close();
            verify(failedLine, never()).start();
            assertThrows(IllegalStateException.class, () -> output.write(new byte[]{10, 20}));
            output.open();
            verify(nextLine).open(format);
            verify(nextLine).start();
        }
    }

    @Test
    void shouldCloseFailedLineAndRetryAfterStartFailure() throws Exception {
        AudioFormat format = new AudioFormat(48000, 16, 1, true, false);
        SourceDataLine failedLine = mock(SourceDataLine.class);
        SourceDataLine nextLine = mock(SourceDataLine.class);
        AudioLineFactory factory = mock(AudioLineFactory.class);
        when(factory.create(format)).thenReturn(failedLine, nextLine);
        IllegalStateException failure = new IllegalStateException("Cannot start");
        doThrow(failure).when(failedLine).start();

        try (PcmAudioOutput output = new PcmAudioOutput(format, factory)) {
            assertSame(failure, assertThrows(IllegalStateException.class, output::open));
            verify(failedLine).close();
            assertThrows(IllegalStateException.class, () -> output.write(new byte[]{10, 20}));
            output.open();
            verify(nextLine).start();
        }
    }

    @Test
    void shouldSkipEmptyPcmData() throws Exception {
        AudioFormat format = new AudioFormat(48000, 16, 1, true, false);
        SourceDataLine line = mock(SourceDataLine.class);
        when(line.isOpen()).thenReturn(true);
        try (PcmAudioOutput output = new PcmAudioOutput(format, ignored -> line)) {
            output.open();
            output.write(new byte[0]);
            verify(line, never()).write(any(byte[].class), anyInt(), anyInt());
        }
    }

    @Test
    void shouldRejectWriteWhenLineBecomesUnavailable() throws Exception {
        AudioFormat format = new AudioFormat(48000, 16, 1, true, false);
        SourceDataLine line = mock(SourceDataLine.class);
        try (PcmAudioOutput output = new PcmAudioOutput(format, ignored -> line)) {
            output.open();
            assertThrows(IllegalStateException.class, () -> output.write(new byte[]{10, 20}));
            verify(line, never()).write(any(byte[].class), anyInt(), anyInt());
        }
    }

    @Test
    void shouldCompletePartialWritesWithoutRepeatingBytes() throws Exception {
        AudioFormat format = new AudioFormat(48000, 16, 1, true, false);
        SourceDataLine line = mock(SourceDataLine.class);
        when(line.isOpen()).thenReturn(true);
        byte[] pcm = {10, 20, 30, 40, 50, 60};
        when(line.write(pcm, 0, 6)).thenReturn(2);
        when(line.write(pcm, 2, 4)).thenReturn(2);
        when(line.write(pcm, 4, 2)).thenReturn(2);

        try (PcmAudioOutput output = new PcmAudioOutput(format, ignored -> line)) {
            output.open();
            output.write(pcm);
            var order = inOrder(line);
            order.verify(line).write(pcm, 0, 6);
            order.verify(line).write(pcm, 2, 4);
            order.verify(line).write(pcm, 4, 2);
            verify(line, times(3)).write(eq(pcm), anyInt(), anyInt());
        }
    }

    @Test
    void shouldFailWhenPartialWriteMakesNoProgress() throws Exception {
        AudioFormat format = new AudioFormat(48000, 16, 1, true, false);
        SourceDataLine line = mock(SourceDataLine.class);
        when(line.isOpen()).thenReturn(true);
        byte[] pcm = {10, 20, 30, 40};
        when(line.write(pcm, 0, 4)).thenReturn(2);
        when(line.write(pcm, 2, 2)).thenReturn(0);

        try (PcmAudioOutput output = new PcmAudioOutput(format, ignored -> line)) {
            output.open();
            assertThrows(IllegalStateException.class, () -> output.write(pcm));
            verify(line, times(2)).write(eq(pcm), anyInt(), anyInt());
        }
    }

    @Test
    void shouldAllowCloseBeforeOpen() {
        AudioFormat format = new AudioFormat(48000, 16, 1, true, false);
        AudioLineFactory factory = mock(AudioLineFactory.class);
        PcmAudioOutput output = new PcmAudioOutput(format, factory);
        output.close();
        verifyNoInteractions(factory);
    }

    @Test
    void shouldCloseLineOnlyOnce() throws Exception {
        AudioFormat format = new AudioFormat(48000, 16, 1, true, false);
        SourceDataLine line = mock(SourceDataLine.class);
        PcmAudioOutput output = new PcmAudioOutput(format, ignored -> line);
        output.open();
        output.close();
        output.close();
        verify(line, times(1)).stop();
        verify(line, times(1)).flush();
        verify(line, times(1)).close();
    }

    @Test
    void shouldReopenWithAnotherLineAfterClose() throws Exception {
        AudioFormat format = new AudioFormat(48000, 16, 1, true, false);
        SourceDataLine firstLine = mock(SourceDataLine.class);
        SourceDataLine nextLine = mock(SourceDataLine.class);
        AudioLineFactory factory = mock(AudioLineFactory.class);
        when(factory.create(format)).thenReturn(firstLine, nextLine);
        when(nextLine.isOpen()).thenReturn(true);
        byte[] pcm = {10, 20};
        when(nextLine.write(pcm, 0, pcm.length)).thenReturn(pcm.length);

        try (PcmAudioOutput output = new PcmAudioOutput(format, factory)) {
            output.open();
            output.close();
            output.open();
            output.write(pcm);
            verify(firstLine).close();
            verify(nextLine).write(pcm, 0, pcm.length);
        }
    }

    @Test
    void shouldDrainPendingAudio() throws Exception {
        AudioFormat format = new AudioFormat(48000, 16, 1, true, false);
        SourceDataLine line = mock(SourceDataLine.class);
        when(line.isOpen()).thenReturn(true);
        byte[] pcm = {10, 20};
        when(line.write(pcm, 0, pcm.length)).thenReturn(pcm.length);

        try (PcmAudioOutput output = new PcmAudioOutput(format, ignored -> line)) {
            output.open();
            output.write(pcm);
            output.drain();
        }
        var order = inOrder(line);
        order.verify(line).write(pcm, 0, pcm.length);
        order.verify(line).drain();
        order.verify(line).stop();
        order.verify(line).flush();
        order.verify(line).close();
    }

    @Test
    void shouldRejectDrainBeforeOpen() {
        AudioFormat format = new AudioFormat(48000, 16, 1, true, false);
        PcmAudioOutput output = new PcmAudioOutput(format);
        assertThrows(IllegalStateException.class, output::drain);
    }

    @Test
    void shouldRejectDrainAfterClose() throws Exception {
        AudioFormat format = new AudioFormat(48000, 16, 1, true, false);
        SourceDataLine line = mock(SourceDataLine.class);
        when(line.isOpen()).thenReturn(true);
        PcmAudioOutput output = new PcmAudioOutput(format, ignored -> line);
        output.open();
        output.close();
        assertThrows(IllegalStateException.class, output::drain);
        verify(line, never()).drain();
    }
}
