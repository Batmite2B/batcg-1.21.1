package bat.batcg.network;

import bat.batcg.card.CardTier;
import bat.batcg.card.ModBoosterComponents;
import bat.batcg.item.BoosterPackItem;
import bat.batcg.item.PokemonCardItem;
import bat.batcg.network.payload.OpenBeltC2SPayload;
import bat.batcg.network.payload.OpenBoosterS2CPayload;
import bat.batcg.network.payload.RevealCardC2SPayload;
import bat.batcg.network.payload.RevealResultS2CPayload;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;

public final class BatcgPackets {

    private BatcgPackets() {}

    public static void registerPayloads() {
        PayloadTypeRegistry.playC2S().register(OpenBeltC2SPayload.ID, OpenBeltC2SPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(OpenBoosterS2CPayload.ID, OpenBoosterS2CPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(RevealCardC2SPayload.ID, RevealCardC2SPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(RevealResultS2CPayload.ID, RevealResultS2CPayload.CODEC);
    }

    public static void initServerReceivers() {
        ServerPlayNetworking.registerGlobalReceiver(RevealCardC2SPayload.ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            context.server().execute(() -> handleReveal(player, payload.handOrdinal(), payload.slot()));
        });



        ServerPlayNetworking.registerGlobalReceiver(OpenBeltC2SPayload.ID, (payload, context) -> {
            context.server().execute(() -> {
                bat.batcg.belt.BatcgBeltApi.openEquippedBeltScreen(context.player());
            });
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

        // ✅ Dar la carta sin riesgo de perderla (inventario lleno -> drop)
        ItemStack card = PokemonCardItem.createCard(result.pokemonId(), result.tier());
        giveOrDrop(player, card);

        // ✅ FX al revelar
        playRevealFx(player, result.tier());

        // Notificar al cliente
        int mask = stack.getOrDefault(ModBoosterComponents.BOOSTER_REVEALED, 0);
        ServerPlayNetworking.send(player, new RevealResultS2CPayload(
                slot,
                result.pokemonId(),
                result.tier().name(),
                mask
        ));

        // ✅ FX al completar pack
        if ((mask & 0b111) == 0b111) {
            playPackCompleteFx(player);
            stack.decrement(1);
        }
    }

    /** Vanilla-safe */
    private static void giveOrDrop(ServerPlayerEntity player, ItemStack stack) {
        if (stack == null || stack.isEmpty()) return;
        player.getInventory().offerOrDrop(stack);
    }

    // -------------------- FX helpers --------------------

    private static SoundEvent snd(String path) {
        return Registries.SOUND_EVENT.get(Identifier.of("minecraft", path));
    }

    private static void playAtPlayer(ServerPlayerEntity player, SoundEvent sound, float volume, float pitch) {
        player.getWorld().playSound(
                null,
                player.getBlockPos(),
                sound,
                SoundCategory.PLAYERS,
                volume,
                pitch
        );
    }

    private static void burst(ServerPlayerEntity player, int count) {
        if (!(player.getWorld() instanceof ServerWorld sw)) return;

        sw.spawnParticles(
                ParticleTypes.CRIT,
                player.getX(),
                player.getY() + 1.1,
                player.getZ(),
                count,
                0.25, 0.25, 0.25,
                0.02
        );
    }

    private static void sparkle(ServerPlayerEntity player, int count) {
        if (!(player.getWorld() instanceof ServerWorld sw)) return;

        sw.spawnParticles(
                ParticleTypes.ENCHANT,
                player.getX(),
                player.getY() + 1.1,
                player.getZ(),
                count,
                0.35, 0.35, 0.35,
                0.06
        );
    }

    private static void holy(ServerPlayerEntity player, int count) {
        if (!(player.getWorld() instanceof ServerWorld sw)) return;

        sw.spawnParticles(
                ParticleTypes.TOTEM_OF_UNDYING,
                player.getX(),
                player.getY() + 1.0,
                player.getZ(),
                count,
                0.35, 0.35, 0.35,
                0.08
        );
    }

    private static void playRevealFx(ServerPlayerEntity player, CardTier tier) {
        // Sonido base “flip”
        playAtPlayer(player, snd("item.book.page_turn"), 0.65f, 1.15f);
        burst(player, 8);

        // Stingers por rareza
        switch (tier) {
            case COMMON -> {
                // nada extra (base ya suena)
            }
            case UNCOMMON -> {
                playAtPlayer(player, snd("entity.experience_orb.pickup"), 0.7f, 1.2f);
                sparkle(player, 10);
            }
            case RARE -> {
                playAtPlayer(player, snd("block.note_block.chime"), 0.9f, 1.35f);
                sparkle(player, 18);
            }
            case EPIC -> {
                playAtPlayer(player, snd("ui.toast.challenge_complete"), 0.9f, 1.0f);
                sparkle(player, 26);
            }
            case LEGENDARY -> {
                playAtPlayer(player, snd("entity.player.levelup"), 1.0f, 1.0f);
                sparkle(player, 34);
            }
            case SHINY -> {
                // Shiny debería sentirse MUY especial
                playAtPlayer(player, snd("ui.toast.challenge_complete"), 1.0f, 1.1f);
                playAtPlayer(player, snd("entity.player.levelup"), 1.0f, 1.25f);
                holy(player, 22);
                sparkle(player, 40);
            }
        }
    }

    private static void playPackCompleteFx(ServerPlayerEntity player) {
        playAtPlayer(player, snd("block.chest.close"), 0.8f, 1.1f);
        if (player.getWorld() instanceof ServerWorld sw) {
            sw.spawnParticles(
                    ParticleTypes.HAPPY_VILLAGER,
                    player.getX(),
                    player.getY() + 1.0,
                    player.getZ(),
                    10,
                    0.35, 0.25, 0.35,
                    0.02
            );
        }
    }
}
