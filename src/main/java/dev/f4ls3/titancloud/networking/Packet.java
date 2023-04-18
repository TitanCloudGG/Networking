package dev.f4ls3.titancloud.networking;

import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.channel.ChannelHandlerContext;

import java.io.IOException;

public abstract class Packet {

    private final int packetID;

    public Packet(final int packetID) {
        this.packetID = packetID;
    }

    public void configure() {}

    public abstract void receive(ByteBufInputStream buffer, ChannelHandlerContext ctx) throws IOException;
    public abstract void send(ByteBufOutputStream buffer, ChannelHandlerContext ctx) throws IOException;

    public int getPacketID() {
        return packetID;
    }
}
