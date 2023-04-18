package dev.f4ls3.titancloud.networking.packets;

import dev.f4ls3.titancloud.networking.Packet;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.channel.ChannelHandlerContext;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class HeartbeatPacket extends Packet {

    private int ping;

    public HeartbeatPacket() {
        super(1);
    }

    @Override
    public void receive(ByteBufInputStream buffer, ChannelHandlerContext ctx) throws IOException {
        this.ping = (int) (System.currentTimeMillis() - buffer.readLong());
        ctx.executor().schedule(() -> ctx.channel().writeAndFlush(new HeartbeatPacket()), 5, TimeUnit.SECONDS);
    }

    @Override
    public void send(ByteBufOutputStream buffer, ChannelHandlerContext ctx) throws IOException {
        buffer.writeLong(System.currentTimeMillis());
    }

    public int getPing() {
        return ping;
    }
}
