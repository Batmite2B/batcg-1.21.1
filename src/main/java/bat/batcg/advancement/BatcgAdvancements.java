package bat.batcg.advancement;

import bat.batcg.Batcg;
import net.minecraft.advancement.AdvancementEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public final class BatcgAdvancements {
    private BatcgAdvancements() {}

    // path ejemplo: "pokedollar_full_stack"  -> ID: batcg:pokedollar_full_stack
    public static void grant(ServerPlayerEntity player, String path, String criterion) {
        Identifier id = Identifier.of(Batcg.MOD_ID, path);
        AdvancementEntry entry = player.getServer().getAdvancementLoader().get(id);
        if (entry != null) {
            player.getAdvancementTracker().grantCriterion(entry, criterion);
        }
    }
}