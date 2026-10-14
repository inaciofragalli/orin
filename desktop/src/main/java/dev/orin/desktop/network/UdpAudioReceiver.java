package dev.orin.desktop.network;

import dev.orin.desktop.protocol.AudioPacketCodec;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.Arrays;

public final class UdpAudioReceiver implements AutoCloseable {
    private final DatagramSocket socket;

    public UdpAudioReceiver(int port) throws SocketException {
        this.socket = new DatagramSocket(port);
    }

    public UdpAudioReceiver(int port, int timeoutMillis) throws SocketException {
        this.socket = new DatagramSocket(port);
        this.socket.setSoTimeout(timeoutMillis);
    }

    public byte[] receive() throws IOException {
        byte[] buffer = new byte[AudioPacketCodec.DATAGRAM_SIZE + 1];

        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        socket.receive(packet);

        return Arrays.copyOf(packet.getData(), packet.getLength());
    }

    public int getPort() {
        return socket.getLocalPort();
    }

    @Override
    public void close() {
        socket.close();
    }
}
