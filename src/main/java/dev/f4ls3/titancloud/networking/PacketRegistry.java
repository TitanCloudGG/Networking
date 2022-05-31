package dev.f4ls3.titancloud.networking;

import dev.f4ls3.titancloud.event.EventManager;

import java.util.HashMap;

public class PacketRegistry {

    private static final HashMap<Integer, Packet> packets = new HashMap<>();

    public static void register(final Packet packet) {
        if(packets.containsKey(packet.getPacketID())) return;
        packets.put(packet.getPacketID(), packet);
        EventManager.fire("packet_registered", packet);
    }

    public static void unregister(final Packet packet) {
        if(!packets.containsKey(packet.getPacketID())) return;
        packets.remove(packet.getPacketID(), packet);
        EventManager.fire("packet_unregistered", packet);
    }

    public static void unregister(final int packetID) {
        if(packets.containsKey(packetID)) return;
        unregister(packets.get(packetID));
    }

    public static Packet get(final int packetID) {
        if(!packets.containsKey(packetID)) return null;
        return packets.get(packetID);
    }
}
