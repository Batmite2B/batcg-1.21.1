package bat.batcg.card;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;

public final class CardStackData {
    private CardStackData() {}

    // Keys "canónicas" (usa estas)
    public static final String KEY_POKEMON_ID = "pokemonId";
    public static final String KEY_TIER_NAME  = "tierName"; // string: COMMON/UNCOMMON/...

    // Keys legacy/compat (por si tu modelo viejo buscaba otras)
    private static final String[] POKEMON_KEYS = {"pokemonId","pokemon_id","cardId","card_id","id"};
    private static final String[] TIER_KEYS    = {"tierName","tier","rarity","rarityName"};

    private static NbtCompound getData(ItemStack stack) {
        return stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT).copyNbt();
    }

    private static void setData(ItemStack stack, NbtCompound nbt) {
        stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));
    }

    public static void write(ItemStack stack, String pokemonId, CardTier tier) {
        NbtCompound nbt = getData(stack);

        // escribe canónico
        nbt.putString(KEY_POKEMON_ID, pokemonId);
        nbt.putString(KEY_TIER_NAME, tier.name());

        // también escribe compat para no romper modelos viejos
        nbt.putString("id", pokemonId);
        nbt.putString("tier", tier.name());
        nbt.putString("rarity", tier.name());

        setData(stack, nbt);
    }

    public static String readPokemonId(ItemStack stack) {
        NbtCompound nbt = getData(stack);
        for (String k : POKEMON_KEYS) {
            if (nbt.contains(k)) {
                String v = nbt.getString(k);
                if (v != null && !v.isBlank()) return v;
            }
        }
        return null;
    }

    public static CardTier readTier(ItemStack stack) {
        NbtCompound nbt = getData(stack);
        for (String k : TIER_KEYS) {
            if (!nbt.contains(k)) continue;
            String v = nbt.getString(k);
            if (v == null || v.isBlank()) continue;
            try {
                return CardTier.valueOf(v);
            } catch (IllegalArgumentException ignored) {}
        }
        return null;
    }
}
