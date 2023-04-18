package dev.f4ls3.titancloud.networking.packets;

import dev.f4ls3.titancloud.networking.*;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;

import java.io.IOException;

public class AuthenticationPacket extends Packet {

    private String token;

    public AuthenticationPacket() {
        super(0);
    }
    public AuthenticationPacket(final String token) {
        super(0);
        this.token = token;
    }

    @Override
    public void receive(ByteBufInputStream buffer, ChannelHandlerContext ctx) throws IOException {
        final String authToken = buffer.readUTF();

        if(!AuthenticationManager.verifyTokenSignature(authToken)) {
            ctx.channel().close();
            return;
        }

        final Minion minion = AuthenticationManager.resolveMinionFromToken(authToken);
        if(minion == null) {
            ctx.channel().writeAndFlush(new ConnectionClosedPacket()).addListener(ChannelFutureListener.CLOSE);
            Networking.getLogger().warning("Minion couldn't be resolved; Connection closed.");
            return;
        }

        minion.setCurrentContext(ctx);
        MinionRegistry.register(minion);

        Networking.getLogger().info("Authentication successful;", minion.getMinionName(), "connected");
        minion.getCurrentContext().channel().writeAndFlush(new SuccessPacket("Authentication successful"));
        minion.getCurrentContext().channel().writeAndFlush(new HeartbeatPacket());
    }

    @Override
    public void send(ByteBufOutputStream buffer, ChannelHandlerContext ctx) throws IOException {
        if(this.token == null) throw new IOException("Token can't be null");
        buffer.writeUTF(this.token);
    }
}
