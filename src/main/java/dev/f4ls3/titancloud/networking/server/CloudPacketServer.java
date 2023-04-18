package dev.f4ls3.titancloud.networking.server;

import dev.f4ls3.titancloud.event.EventManager;
import dev.f4ls3.titancloud.networking.*;
import dev.f4ls3.titancloud.networking.packets.AuthenticationPacket;
import dev.f4ls3.titancloud.networking.packets.ConnectionClosedPacket;
import dev.f4ls3.titancloud.networking.packets.HeartbeatPacket;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.EmptyByteBuf;
import io.netty.channel.*;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.MessageToByteEncoder;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CloudPacketServer {

    private static final boolean EPOLL = Epoll.isAvailable();

    private final int port;

    private EventLoopGroup bossGroup, workerGroup;
    private ServerBootstrap bootstrap;
    private ChannelFuture future;

    public CloudPacketServer(final int port) {
        this.port = port;
    }

    public void configure() {
        Logger.getLogger("io.netty").setLevel(Level.OFF);
        System.err.close();
        System.setErr(System.out);

        this.bossGroup = EPOLL ? new EpollEventLoopGroup() : new NioEventLoopGroup();
        this.workerGroup = EPOLL ? new EpollEventLoopGroup() : new NioEventLoopGroup();

        this.bootstrap = new ServerBootstrap();

        bootstrap.group(bossGroup, workerGroup);
        bootstrap.channel(EPOLL ? EpollServerSocketChannel.class : NioServerSocketChannel.class);
        bootstrap.childHandler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
                ch
                        .pipeline()
                        .addLast(new ByteToMessageDecoder() {
                            @Override
                            protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
                                if(in instanceof EmptyByteBuf) return;

                                ByteBufInputStream buffer = new ByteBufInputStream(in);
                                int id = buffer.readInt();
                                if (id == -1) return;

                                Packet packet = PacketRegistry.get(id);

                                if (packet == null) return;
                                packet.receive(buffer, ctx);
                                out.add(packet);
                                EventManager.fire("network_packet_decoded", packet);
                            }
                        })
                        .addLast(new MessageToByteEncoder<Packet>() {
                            @Override
                            protected void encode(ChannelHandlerContext ctx, Packet packet, ByteBuf out) throws Exception {
                                if (packet.getPacketID() == -1) return;

                                ByteBufOutputStream buffer = new ByteBufOutputStream(out);
                                buffer.writeInt(packet.getPacketID());
                                packet.send(buffer, ctx);
                                EventManager.fire("network_packet_encoded", packet);
                            }
                        })
                        .addLast(new SimpleChannelInboundHandler<Packet>() {
                            @Override
                            public void channelActive(ChannelHandlerContext ctx) throws Exception {
                                Networking.getLogger().info("Channel connected", ctx.channel().id() + ";", "Waiting for authentication");
                                EventManager.fire("network_channel_connected", ctx);
                            }

                            @Override
                            public void channelInactive(ChannelHandlerContext ctx) throws Exception {
                                final Minion minion = MinionRegistry.get(ctx);
                                if(minion == null) return;

                                Networking.getLogger().info(minion.getMinionName(), "disconnected");

                                EventManager.fire("network_channel_disconnected", minion);
                                MinionRegistry.unregister(minion);
                            }

                            @Override
                            protected void messageReceived(ChannelHandlerContext ctx, Packet packet) throws Exception {
                                final Minion minion = MinionRegistry.get(ctx);
                                if(minion == null && !(packet instanceof AuthenticationPacket)) {
                                    ctx.channel().writeAndFlush(new ConnectionClosedPacket()).addListener(ChannelFutureListener.CLOSE);
                                    return;
                                }

                                if(packet instanceof HeartbeatPacket heartbeat) minion.setMinionPing(heartbeat.getPing());

                                EventManager.fire("network_packet_received", minion, packet);
                            }

                            @Override
                            public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
                                EventManager.fire("network_exception_caught", cause);
                            }
                        });
                }
        });

        bootstrap.option(ChannelOption.SO_BACKLOG, 50);
        bootstrap.option(ChannelOption.SO_KEEPALIVE, true);

        bootstrap.childOption(ChannelOption.SO_KEEPALIVE, true);
    }

    public void run() {
        try {
            this.future = bootstrap.bind(this.port);
            Networking.getLogger().info("Server started on port", this.port);

            new Thread(() -> {
                while(true) {
                    try {
                        if (!this.future.await().isSuccess()) {
                            stop();
                            break;
                        }
                    } catch (InterruptedException e) {
                        EventManager.fire("network_exception_caught", e.getCause());
                    }
                }
            }).start();

            EventManager.fire("network_server_started", future, this.port);

        } catch(Exception e) {
            EventManager.fire("network_exception_caught", e.getCause());
        }
    }

    public void stop() {
        try {
            this.future.channel()
                    .writeAndFlush(new ConnectionClosedPacket())
                    .await()
                    .channel()
                    .close();

            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public int getPort() {
        return port;
    }
}
