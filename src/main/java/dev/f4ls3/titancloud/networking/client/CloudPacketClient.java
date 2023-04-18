package dev.f4ls3.titancloud.networking.client;

import dev.f4ls3.titancloud.event.EventManager;
import dev.f4ls3.titancloud.networking.*;
import dev.f4ls3.titancloud.networking.packets.AuthenticationPacket;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.EmptyByteBuf;
import io.netty.channel.*;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.MessageToByteEncoder;

import java.net.SocketException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CloudPacketClient {

    private static final boolean EPOLL = Epoll.isAvailable();

    private final String host;
    private final int port;

    private final String token;

    private EventLoopGroup workerGroup;
    private Bootstrap bootstrap;

    public CloudPacketClient(final String host, final int port, final String token) {
        this.host = host;
        this.port = port;
        this.token = token;
    }

    public void configure() {
        Logger.getLogger("io.netty").setLevel(Level.OFF);

        EventManager.on("network_client_reconnect", objs -> {
            int type = (int) objs[0];
            if(type == 0) Networking.getLogger().warning("Couldn't connect to Master; Trying to reconnect in 10s");
            else Networking.getLogger().warning("Lost connection to Master; Trying to reconnect in 10s");
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            run();
        });

        this.workerGroup = EPOLL ? new EpollEventLoopGroup() : new NioEventLoopGroup();

        this.bootstrap = new Bootstrap();

        bootstrap.group(workerGroup);
        bootstrap.channel(EPOLL ? EpollSocketChannel.class : NioSocketChannel.class);
        bootstrap.handler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) {
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
                            public void channelActive(ChannelHandlerContext ctx) {
                                EventManager.fire("network_connected", ctx);
                            }

                            @Override
                            public void channelInactive(ChannelHandlerContext ctx) {
                                EventManager.fire("network_disconnected", ctx);
                            }

                            @Override
                            protected void messageReceived(ChannelHandlerContext ctx, Packet packet) {
                                EventManager.fire("network_packet_received", ctx, packet);
                            }

                            @Override
                            public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
                                if(cause instanceof SocketException) {
                                    EventManager.fire("network_client_reconnect", 1);
                                    return;
                                }

                                EventManager.fire("network_exception_caught", cause);
                            }
                        });
            }
        });

        bootstrap.option(ChannelOption.SO_KEEPALIVE, true);
    }

    public void run() {
        try {
            final ChannelFuture future = bootstrap.connect(this.host, this.port).await();
            if(future.isSuccess() && future.channel().isOpen()) {
                Networking.getLogger().info("Client connected; Sending authentication");
                future.channel().writeAndFlush(new AuthenticationPacket(this.token));

                new Thread(() -> {
                    while (future.channel().isOpen());
                }).start();

                EventManager.fire("network_client_started", future);
            }
            else EventManager.fire("network_client_reconnect", 0);

        } catch(Exception e) {
            EventManager.fire("network_exception_caught", e.getCause());
        }
    }

    public int getPort() {
        return port;
    }

    public String getHost() {
        return host;
    }
}
