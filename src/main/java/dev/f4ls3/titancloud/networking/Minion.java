package dev.f4ls3.titancloud.networking;

import io.netty.channel.ChannelHandlerContext;

import java.util.UUID;

public class Minion {

    private int minionID;
    private UUID minionUUID;
    private String minionName;
    private boolean minionConnected;
    private int minionPing;

    private ChannelHandlerContext currentContext;

    public Minion(int minionID, UUID minionUUID, String minionName, boolean minionConnected, int minionPing) {
        this.minionID = minionID;
        this.minionUUID = minionUUID;
        this.minionName = minionName;
        this.minionConnected = minionConnected;
        this.minionPing = minionPing;
    }

    public int getMinionID() {
        return minionID;
    }

    public UUID getMinionUUID() {
        return minionUUID;
    }

    public String getMinionName() {
        return minionName;
    }

    public boolean isMinionConnected() {
        return minionConnected;
    }

    public int getMinionPing() {
        return minionPing;
    }

    public ChannelHandlerContext getCurrentContext() {
        return currentContext;
    }

    public void setMinionConnected(boolean minionConnected) {
        this.minionConnected = minionConnected;
    }

    public void setMinionPing(int minionPing) {
        this.minionPing = minionPing;
    }

    public void setCurrentContext(ChannelHandlerContext currentContext) {
        this.currentContext = currentContext;
    }
}
