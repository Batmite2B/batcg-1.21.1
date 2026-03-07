package bat.batcg.belt;

import bat.batcg.card.CardTier;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;

public final class BeltCards {
    private BeltCards() {}

    public static final int SLOTS = 5;

    private static final String ROOT_KEY = "batcg_belt_cards";
    private static final String KEY_ID = "id";
    private static final String KEY_TIER = "tier";
    private static final String KEY_GRADE = "grade"; // ✅ NEW

    public record SlotData(int slot, String pokemonId, CardTier tier, int grade) {
        public boolean isEmpty() {
            return pokemonId == null || pokemonId.isBlank() || tier == null;
        }
        public static SlotData empty(int slot) {
            return new SlotData(slot, null, null, 0);
        }
    }

    public static SlotData get(ItemStack belt, int slot) {
        if (belt.isEmpty() || slot < 0 || slot >= SLOTS) return SlotData.empty(slot);

        NbtList list = getOrCreateList(belt);
        if (slot >= list.size()) return SlotData.empty(slot);

        NbtCompound entry = list.getCompound(slot);
        if (entry == null || entry.isEmpty()) return SlotData.empty(slot);

        String id = entry.getString(KEY_ID);
        String tierName = entry.getString(KEY_TIER);

        if (id == null || id.isBlank()) return SlotData.empty(slot);

        CardTier tier;
        try {
            tier = (tierName == null || tierName.isBlank()) ? null
                    : CardTier.valueOf(tierName.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (Exception e) {
            tier = null;
        }
        if (tier == null) return SlotData.empty(slot);

        int grade = entry.contains(KEY_GRADE, NbtElement.INT_TYPE) ? entry.getInt(KEY_GRADE) : 0;
        grade = Math.max(0, Math.min(4, grade));

        return new SlotData(slot, id, tier, grade);
    }

    // Backwards compatible
    public static void set(ItemStack belt, int slot, String pokemonId, CardTier tier) {
        set(belt, slot, pokemonId, tier, 0);
    }

    public static void set(ItemStack belt, int slot, String pokemonId, CardTier tier, int grade) {
        if (belt.isEmpty() || slot < 0 || slot >= SLOTS) return;

        if (pokemonId == null || pokemonId.isBlank() || tier == null) {
            clear(belt, slot);
            return;
        }

        NbtList list = getOrCreateList(belt);
        ensureSize(list, SLOTS);

        NbtCompound entry = new NbtCompound();
        entry.putString(KEY_ID, pokemonId);
        entry.putString(KEY_TIER, tier.name());
        entry.putInt(KEY_GRADE, Math.max(0, Math.min(4, grade)));
        list.set(slot, entry);

        writeList(belt, list);
    }

    public static void clear(ItemStack belt, int slot) {
        if (belt.isEmpty() || slot < 0 || slot >= SLOTS) return;
        NbtList list = getOrCreateList(belt);
        ensureSize(list, SLOTS);
        list.set(slot, new NbtCompound());
        writeList(belt, list);
    }

    public static boolean isEmpty(ItemStack belt, int slot) {
        return get(belt, slot).isEmpty();
    }

    public static SlotData getFirstFilled(ItemStack belt) {
        for (int i = 0; i < SLOTS; i++) {
            SlotData d = get(belt, i);
            if (!d.isEmpty()) return d;
        }
        return null;
    }

    // ---------- CUSTOM_DATA helpers ----------
    private static NbtCompound getCustomData(ItemStack stack) {
        NbtComponent comp = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT);
        return comp.copyNbt();
    }

    private static void setCustomData(ItemStack stack, NbtCompound nbt) {
        stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));
    }

    private static NbtList getOrCreateList(ItemStack belt) {
        NbtCompound root = getCustomData(belt);

        NbtList list;
        if (root.contains(ROOT_KEY, NbtElement.LIST_TYPE)) {
            list = root.getList(ROOT_KEY, NbtElement.COMPOUND_TYPE);
        } else {
            list = new NbtList();
        }

        ensureSize(list, SLOTS);
        return list;
    }

    private static void writeList(ItemStack belt, NbtList list) {
        NbtCompound root = getCustomData(belt);
        root.put(ROOT_KEY, list);
        setCustomData(belt, root);
    }

    private static void ensureSize(NbtList list, int size) {
        while (list.size() < size) list.add(new NbtCompound());
        while (list.size() > size) list.remove(list.size() - 1);
    }

    public static int getFilledCount(ItemStack belt) {
        if (belt == null || belt.isEmpty()) return 0;

        int count = 0;
        for (int i = 0; i < SLOTS; i++) {
            SlotData d = get(belt, i);
            if (d != null && !d.isEmpty()) count++;
        }
        return count;
    }

}