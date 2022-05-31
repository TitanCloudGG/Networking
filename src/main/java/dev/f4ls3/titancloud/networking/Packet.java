package dev.f4ls3.titancloud.networking;

import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;

import java.io.IOException;

public abstract class Packet {

    private final int packetID;

    public Packet(final int packetID) {
        this.packetID = packetID;
    }

    public abstract void read(ByteBufInputStream buffer) throws IOException;
    public abstract void write(ByteBufOutputStream buffer) throws IOException;

    public int getPacketID() {
        return packetID;
    }
}
