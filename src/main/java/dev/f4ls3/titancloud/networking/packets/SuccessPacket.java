package dev.f4ls3.titancloud.networking.packets;

import dev.f4ls3.titancloud.networking.Networking;
import dev.f4ls3.titancloud.networking.Packet;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.channel.ChannelHandlerContext;

import java.io.IOException;

public class SuccessPacket extends Packet {

    private String message;

    public SuccessPacket() {
        super(3);
    }

    public SuccessPacket(final String message) {
        super(3);
        this.message = message;
    }

    @Override
    public void receive(ByteBufInputStream buffer, ChannelHandlerContext ctx) throws IOException {
        Networking.getLogger().info(buffer.readUTF());
    }

    @Override
    public void send(ByteBufOutputStream buffer, ChannelHandlerContext ctx) throws IOException {
        buffer.writeUTF(this.message);
    }
}
