package dev.f4ls3.titancloud.networking;

import dev.f4ls3.titancloud.event.EventManager;
import dev.f4ls3.titancloud.logger.Logger;
import dev.f4ls3.titancloud.networking.packets.AuthenticationPacket;
import dev.f4ls3.titancloud.networking.packets.ConnectionClosedPacket;
import dev.f4ls3.titancloud.networking.packets.HeartbeatPacket;
import dev.f4ls3.titancloud.networking.packets.SuccessPacket;

public class Networking {

    private static final Logger logger = Logger.get("networking");

    public static void configure() {
        MinionRegistry.initialize();

        PacketRegistry.register(new AuthenticationPacket());
        PacketRegistry.register(new HeartbeatPacket());
        PacketRegistry.register(new ConnectionClosedPacket());
        PacketRegistry.register(new SuccessPacket());

        EventManager.on("network_exception_caught", objects -> {
            Exception e = (Exception) objects[0];
            Networking.getLogger().severe("Netty Error Caught:", e);
        });
    }

    public static Logger getLogger() {
        return logger;
    }
}
