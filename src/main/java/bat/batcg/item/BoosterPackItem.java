package bat.batcg.item;

import bat.batcg.card.CardIdIndex;
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

import java.util.List;
import java.util.Locale;
import java.util.Random;

public class BoosterPackItem extends Item {

    public record RevealResult(String pokemonId, CardTier tier) {}

    public BoosterPackItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);

        if (!world.isClient && user instanceof ServerPlayerEntity sp) {
            ensureSeed(stack, world.random.nextLong());

            int mask = stack.getOrDefault(ModBoosterComponents.BOOSTER_REVEALED, 0);
            ServerPlayNetworking.send(sp, new OpenBoosterS2CPayload(hand.ordinal(), mask));
        }

        return TypedActionResult.success(stack, world.isClient);
    }

    public void ensureSeed(ItemStack stack, long seed) {
        if (stack.get(ModBoosterComponents.BOOSTER_SEED) == null) {
            stack.set(ModBoosterComponents.BOOSTER_SEED, seed);
            stack.set(ModBoosterComponents.BOOSTER_REVEALED, 0);
        }
    }

    public RevealResult reveal(ItemStack boosterStack, int slot) {
        int mask = boosterStack.getOrDefault(ModBoosterComponents.BOOSTER_REVEALED, 0);
        int bit = 1 << slot;

        if ((mask & bit) != 0) return null;

        long seed = boosterStack.getOrDefault(ModBoosterComponents.BOOSTER_SEED, 0L);
        Random rng = new Random(mixSeed(seed, slot));

        CardTier tier = rollTier(rng);

        List<String> pool = (tier == CardTier.SHINY) ? CardIdIndex.shinyIds() : CardIdIndex.normalIds();
        if (pool.isEmpty()) return null;

        String pokemonId = pool.get(rng.nextInt(pool.size())).toLowerCase(Locale.ROOT);

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
