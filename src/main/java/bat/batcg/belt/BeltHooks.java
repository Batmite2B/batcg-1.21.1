package bat.batcg.belt;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;

public final class BeltHooks {

    private BeltHooks() {}

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(BeltHooks::onTick);
    }

    private static void onTick(MinecraftServer server) {
        for (var player : server.getPlayerManager().getPlayerList()) {
            BeltEffects.apply(player);
            LeaderActivePowers.tick(player); // ✅ era "sp"
        }
    }
}