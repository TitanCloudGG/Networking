package dev.f4ls3.titancloud.networking;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.internal.LinkedTreeMap;
import dev.f4ls3.titancloud.event.EventManager;
import dev.f4ls3.titancloud.file.FileManager;
import io.netty.channel.ChannelHandlerContext;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MinionRegistry {

    private static final List<Minion> minions = new ArrayList<>();

    public static void initialize() {
        Documents.minions.getMap().forEach((k, v) -> {
            JsonObject obj = FileManager.getGson().fromJson(v.toString(), JsonObject.class);

            minions.add(new Minion(
                    obj.get("minionID").getAsInt(),
                    UUID.fromString(obj.get("minionUUID").getAsString()),
                    obj.get("minionName").getAsString(),
                    false,
                    -1));
        });
    }

    public static void register(final Minion minion) {
        if(minions.contains(minion)) return;
        minion.setMinionConnected(true);
        minions.add(minion);
        EventManager.fire("minion_registered", minion);
    }

    public static void unregister(final UUID minionUUID) {
        final Minion minion = minions.stream().filter(min -> min.getMinionUUID().equals(minionUUID)).findFirst().orElse(null);
        if(minion == null) return;
        minion.setMinionConnected(false);
        minion.setCurrentContext(null);
        minions.remove(minion);
        EventManager.fire("minion_unregistered", minion);
    }

    public static void unregister(final Minion minion) {
        unregister(minion.getMinionUUID());
    }

    public static void createMinion(final Minion minion) {
        if(Documents.minions.contains(minion.getMinionUUID().toString())) return;
        JsonObject obj = FileManager.getGson().toJsonTree(minion, Minion.class).getAsJsonObject();
        obj.remove("minionConnected");
        obj.remove("minionPing");
        Documents.minions.put(minion.getMinionUUID().toString(), obj);
        Documents.minions.flush();
        EventManager.fire("minion_created", minion);
    }

    public static boolean minionExists(final UUID minionUUID) {
        return minions.stream().filter(min -> min.getMinionUUID().equals(minionUUID)).count() > 1;
    }

    public static boolean minionExists(final Minion minion) {
        return minionExists(minion.getMinionUUID());
    }

    public static boolean minionConnected(final UUID minionUUID) {
        final Minion minion = minions.stream().filter(min -> min.getMinionUUID().equals(minionUUID)).findFirst().orElse(null);
        if(minion == null) return false;
        return minion.isMinionConnected();
    }

    public static boolean minionConnected(final Minion minion) {
        return minionConnected(minion.getMinionUUID());
    }

    public static Minion get(final UUID minionUUID) {
        return minions.stream().filter(minion -> minion.getMinionUUID().equals(minionUUID)).findFirst().orElse(null);
    }

    public static Minion get(final ChannelHandlerContext ctx) {
        return minions.stream().filter(minion -> minion.getCurrentContext().channel().id().equals(ctx.channel().id())).findFirst().orElse(null);
    }
}
