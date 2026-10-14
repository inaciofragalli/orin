
package dev.orin.desktop.audio;

import dev.orin.desktop.network.UdpAudioReceiver;
import dev.orin.desktop.protocol.AudioPacketCodec;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DesktopAudioPipelineTest {

    @Test
    void shouldDecodeAndPlayReceivedPcm() throws Exception {
        UdpAudioReceiver receiver = mock(UdpAudioReceiver.class);
        PcmAudioOutput output = mock(PcmAudioOutput.class);

        byte[] pcm = new byte[AudioPacketCodec.PCM_FRAME_SIZE];
        pcm[0] = 42;
        pcm[1] = 15;

        byte[] datagram = AudioPacketCodec.encode(1, 480, pcm);
        when(receiver.receive()).thenReturn(datagram);

        DesktopAudioPipeline pipeline =
            new DesktopAudioPipeline(receiver, output);

        pipeline.receiveAndPlayNext();

        ArgumentCaptor<byte[]> captor =
            ArgumentCaptor.forClass(byte[].class);

        verify(output).write(captor.capture());
        assertArrayEquals(pcm, captor.getValue());
    }

    @Test
    void shouldNotPlayInvalidDatagram() throws Exception {
        UdpAudioReceiver receiver = mock(UdpAudioReceiver.class);
        PcmAudioOutput output = mock(PcmAudioOutput.class);

        byte[] invalid = new byte[AudioPacketCodec.DATAGRAM_SIZE + 1];
        when(receiver.receive()).thenReturn(invalid);

        DesktopAudioPipeline pipeline =
            new DesktopAudioPipeline(receiver, output);

        assertThrows(
            IllegalArgumentException.class,
            pipeline::receiveAndPlayNext
        );

        verifyNoInteractions(output);
    }

    @Test
    void shouldProcessNextCallAfterInvalidPacket() throws Exception {
        UdpAudioReceiver receiver = mock(UdpAudioReceiver.class);
        PcmAudioOutput output = mock(PcmAudioOutput.class);
        byte[] pcm = new byte[AudioPacketCodec.PCM_FRAME_SIZE];
        pcm[0] = 42;
        pcm[1] = 15;
        byte[] valid = AudioPacketCodec.encode(1, 480, pcm);
        byte[] invalid = valid.clone();
        invalid[4] = 2;
        when(receiver.receive()).thenReturn(invalid, valid);
        DesktopAudioPipeline pipeline = new DesktopAudioPipeline(receiver, output);

        assertThrows(IllegalArgumentException.class, pipeline::receiveAndPlayNext);
        verifyNoInteractions(output);

        pipeline.receiveAndPlayNext();

        ArgumentCaptor<byte[]> captor = ArgumentCaptor.forClass(byte[].class);
        verify(output).write(captor.capture());
        assertArrayEquals(pcm, captor.getValue());
        verify(receiver, times(2)).receive();
    }

    @Test
    void shouldPropagateReceiveTimeout() throws Exception {
        UdpAudioReceiver receiver = mock(UdpAudioReceiver.class);
        PcmAudioOutput output = mock(PcmAudioOutput.class);

        when(receiver.receive())
            .thenThrow(new SocketTimeoutException("Timeout"));

        DesktopAudioPipeline pipeline =
            new DesktopAudioPipeline(receiver, output);

        assertThrows(
            SocketTimeoutException.class,
            pipeline::receiveAndPlayNext
        );

        verifyNoInteractions(output);
    }


    @Test
    void shouldReceiveDecodeAndPlayOverLoopback() throws Exception {
        try (UdpAudioReceiver receiver = new UdpAudioReceiver(0, 1000);
             DatagramSocket sender = new DatagramSocket()) {

            PcmAudioOutput output = mock(PcmAudioOutput.class);
            DesktopAudioPipeline pipeline =
                new DesktopAudioPipeline(receiver, output);

            byte[] pcm = new byte[AudioPacketCodec.PCM_FRAME_SIZE];
            pcm[0] = 42;
            pcm[1] = 15;

            byte[] encoded = AudioPacketCodec.encode(1, 480, pcm);

            DatagramPacket packet = new DatagramPacket(
                encoded,
                encoded.length,
                InetAddress.getLoopbackAddress(),
                receiver.getPort()
            );

            sender.send(packet);
            pipeline.receiveAndPlayNext();

            ArgumentCaptor<byte[]> captor =
                ArgumentCaptor.forClass(byte[].class);

            verify(output).write(captor.capture());
            assertArrayEquals(pcm, captor.getValue());
        }
    }

}
