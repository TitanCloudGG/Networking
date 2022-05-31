package dev.f4ls3.titancloud.networking.server;

import dev.f4ls3.titancloud.event.EventManager;
import dev.f4ls3.titancloud.networking.Packet;
import dev.f4ls3.titancloud.networking.PacketRegistry;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.MessageToByteEncoder;

import java.util.List;

public class CloudPacketServer {

    private static final boolean EPOLL = Epoll.isAvailable();

    private final int port;

    private EventLoopGroup bossGroup, workerGroup;
    private ServerBootstrap bootstrap;

    public CloudPacketServer(final int port) {
        this.port = port;
    }

    public void configure() {
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
                                int id = in.readInt();
                                if (id == -1) return;

                                Packet packet = PacketRegistry.get(id);
                                ByteBufInputStream buffer = new ByteBufInputStream(in);

                                if (packet == null) return;
                                packet.read(buffer);
                                out.add(packet);
                                EventManager.fire("packet_decoded", packet);
                            }
                        })
                        .addLast(new MessageToByteEncoder<Packet>() {
                            @Override
                            protected void encode(ChannelHandlerContext ctx, Packet packet, ByteBuf out) throws Exception {
                                if (packet.getPacketID() == -1) return;
                                out.writeInt(packet.getPacketID());

                                ByteBufOutputStream buffer = new ByteBufOutputStream(out);
                                packet.write(buffer);
                                EventManager.fire("packet_encoded", packet);
                            }
                        });
                }
        });
    }

    public int getPort() {
        return port;
    }
}
