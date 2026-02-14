package bat.batcg.item;

import bat.batcg.card.CardTier;
import bat.batcg.card.ModBoosterComponents;
import bat.batcg.network.payload.OpenBoosterS2CPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import java.util.Random;
import bat.batcg.card.PokemonRarityTable;


public class BoosterPackItem extends Item {

    public record RevealResult(String pokemonId, CardTier tier) {}

    public BoosterPackItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {

        if (!world.isClient && user instanceof ServerPlayerEntity sp) {

            // ✅ CLAVE: si viene de un stack, separar 1 booster a la mano
            ItemStack stack = ensureSingleInHand(sp, hand);

            ensureSeed(stack, world.random.nextLong());

            int mask = stack.getOrDefault(ModBoosterComponents.BOOSTER_REVEALED, 0);
            ServerPlayNetworking.send(sp, new OpenBoosterS2CPayload(hand.ordinal(), mask));

            return TypedActionResult.success(stack, false);
        }

        return TypedActionResult.success(user.getStackInHand(hand), true);
    }


    public static void ensureSeed(ItemStack stack, long seed) {
        if (!stack.contains(ModBoosterComponents.BOOSTER_SEED)) {
            stack.set(ModBoosterComponents.BOOSTER_SEED, seed);
        }
        if (!stack.contains(ModBoosterComponents.BOOSTER_REVEALED)) {
            stack.set(ModBoosterComponents.BOOSTER_REVEALED, 0);
        }
    }

    public static void clearBoosterData(ItemStack stack) {
        stack.remove(ModBoosterComponents.BOOSTER_SEED);
        stack.remove(ModBoosterComponents.BOOSTER_REVEALED);

        stack.remove(ModBoosterComponents.BOOSTER_ID0);
        stack.remove(ModBoosterComponents.BOOSTER_ID1);
        stack.remove(ModBoosterComponents.BOOSTER_ID2);

        stack.remove(ModBoosterComponents.BOOSTER_TIER0);
        stack.remove(ModBoosterComponents.BOOSTER_TIER1);
        stack.remove(ModBoosterComponents.BOOSTER_TIER2);
    }


    /**
     * Si el jugador tiene un stack (count>1) en la mano,
     * separa 1 item a la mano y manda el resto al inventario limpio.
     * El booster "en mano" mantiene su data (si ya estaba abierto) para no perder progreso.
     */
    public static ItemStack ensureSingleInHand(PlayerEntity player, Hand hand) {
        ItemStack handStack = player.getStackInHand(hand);
        if (handStack.getCount() <= 1) return handStack;

        // Booster que se va a usar (conserva data si la tenía)
        ItemStack single = handStack.copy();
        single.setCount(1);

        // Resto del stack (se limpia para que sean boosters nuevos)
        ItemStack remainder = handStack.copy();
        remainder.setCount(handStack.getCount() - 1);
        clearBoosterData(remainder);

        // Reemplazar mano por el single
        player.setStackInHand(hand, single);

        // Intentar guardar el resto en inventario, si no cabe lo tiramos
        if (!player.getInventory().insertStack(remainder)) {
            player.dropItem(remainder, false);
        }

        return single;
    }



    public RevealResult reveal(ItemStack boosterStack, int slot) {
        int mask = boosterStack.getOrDefault(ModBoosterComponents.BOOSTER_REVEALED, 0);
        int bit = 1 << slot;

        if ((mask & bit) != 0) return null;

        long seed = boosterStack.getOrDefault(ModBoosterComponents.BOOSTER_SEED, 0L);
        Random rng = new Random(mixSeed(seed, slot));

        CardTier tier = rollTier(rng);

        String pokemonId = bat.batcg.card.PokemonRarityTable.pickPokemonId(rng, tier);
        if (pokemonId == null) return null;




        bat.batcg.Batcg.LOGGER.info("[BATCG] slot={} rolledTier={} picked={} assignedTier={}",
                slot, tier, pokemonId, bat.batcg.card.PokemonRarityTable.getAssignedTier(pokemonId));




        switch (slot) {
            case 0 -> {
                boosterStack.set(ModBoosterComponents.BOOSTER_ID0, pokemonId);
                boosterStack.set(ModBoosterComponents.BOOSTER_TIER0, tier.name());
            }
            case 1 -> {
                boosterStack.set(ModBoosterComponents.BOOSTER_ID1, pokemonId);
                boosterStack.set(ModBoosterComponents.BOOSTER_TIER1, tier.name());
            }
            case 2 -> {
                boosterStack.set(ModBoosterComponents.BOOSTER_ID2, pokemonId);
                boosterStack.set(ModBoosterComponents.BOOSTER_TIER2, tier.name());
            }
        }

        boosterStack.set(ModBoosterComponents.BOOSTER_REVEALED, mask | bit);
        return new RevealResult(pokemonId, tier);
    }

    private static long mixSeed(long seed, int slot) {
        long x = seed ^ (0x9E3779B97F4A7C15L * (slot + 1));
        x ^= (x >>> 30);
        x *= 0xBF58476D1CE4E5B9L;
        x ^= (x >>> 27);
        x *= 0x94D049BB133111EBL;
        x ^= (x >>> 31);
        return x;
    }

    private static CardTier rollTier(Random rng) {
        int r = rng.nextInt(1000);

        if (r < 650) return CardTier.COMMON;
        if (r < 850) return CardTier.UNCOMMON;
        if (r < 940) return CardTier.RARE;
        if (r < 980) return CardTier.EPIC;
        if (r < 995) return CardTier.LEGENDARY;
        return CardTier.SHINY;
    }
}
