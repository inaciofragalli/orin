package dev.orin.desktop.protocol;

public record AudioPacket(
    int sequence,
    int timestamp,
    byte[] pcm
){
}
