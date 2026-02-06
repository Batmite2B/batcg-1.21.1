package bat.batcg.network;

import bat.batcg.card.ModBoosterComponents;
import bat.batcg.item.BoosterPackItem;
import bat.batcg.item.PokemonCardItem;
import bat.batcg.network.payload.OpenBoosterS2CPayload;
import bat.batcg.network.payload.RevealCardC2SPayload;
import bat.batcg.network.payload.RevealResultS2CPayload;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;

public final class BatcgPackets {

    private BatcgPackets() {}

    /** Llamar una vez en common init (onInitialize) */
    public static void registerPayloads() {
        PayloadTypeRegistry.playS2C().register(OpenBoosterS2CPayload.ID, OpenBoosterS2CPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(RevealCardC2SPayload.ID, RevealCardC2SPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(RevealResultS2CPayload.ID, RevealResultS2CPayload.CODEC);
    }

    /** Llamar en onInitialize (server/common) */
    public static void initServerReceivers() {
        ServerPlayNetworking.registerGlobalReceiver(RevealCardC2SPayload.ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            context.server().execute(() -> handleReveal(player, payload.handOrdinal(), payload.slot()));
        });
    }

    private static void handleReveal(ServerPlayerEntity player, int handOrdinal, int slot) {
        if (slot < 0 || slot > 2) return;

        Hand hand = Hand.values()[Math.max(0, Math.min(handOrdinal, Hand.values().length - 1))];
        var stack = player.getStackInHand(hand);

        if (!(stack.getItem() instanceof BoosterPackItem booster)) return;

        booster.ensureSeed(stack, player.getWorld().random.nextLong());

        BoosterPackItem.RevealResult result = booster.reveal(stack, slot);
        if (result == null) return;

        // Dar la carta
        player.giveItemStack(PokemonCardItem.createCard(result.pokemonId(), result.tier()));

        // Notificar al cliente
        int mask = stack.getOrDefault(ModBoosterComponents.BOOSTER_REVEALED, 0);
        ServerPlayNetworking.send(player, new RevealResultS2CPayload(
                slot,
                result.pokemonId(),
                result.tier().name(),
                mask
        ));

        // Consumir booster cuando ya se revelaron 3
        if ((mask & 0b111) == 0b111) {
            stack.decrement(1);
        }
    }
}
