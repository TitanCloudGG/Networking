package dev.f4ls3.titancloud.networking.packets;

import dev.f4ls3.titancloud.networking.Packet;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.channel.ChannelHandlerContext;

import java.io.IOException;

public class ConnectionClosedPacket extends Packet {

    public ConnectionClosedPacket() {
        super(2);
    }

    @Override
    public void receive(ByteBufInputStream buffer, ChannelHandlerContext ctx) throws IOException {
        ctx.channel().close();
    }

    @Override
    public void send(ByteBufOutputStream buffer, ChannelHandlerContext ctx) throws IOException {}
}
