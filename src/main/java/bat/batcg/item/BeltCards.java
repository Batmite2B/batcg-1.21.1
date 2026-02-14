package bat.batcg.item;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryWrapper;

import java.util.stream.Stream;

/**
 * Helper para guardar/leer "slots" de cartas dentro del ItemStack del belt.
 *
 * Guarda los slots en CUSTOM_DATA como una NbtList de NbtCompound, cada uno siendo un ItemStack.encode(...)
 */
public final class BeltCards {

    private BeltCards() {}

    // Ajusta esto si tu belt tiene otra cantidad de slots
    public static final int SLOT_COUNT = 6;

    private static final String NBT_KEY = "batcg_belt_cards";

    /**
     * WrapperLookup "estático" hecho desde el registro de registros.
     * Esto permite encode/decode de ItemStack sin necesitar World/Server directo.
     */
    private static final RegistryWrapper.WrapperLookup STATIC_LOOKUP = RegistryWrapper.WrapperLookup.of(
            ((Stream<? extends Registry<?>>) Registries.REGISTRIES.stream())
                    .map(reg -> ((Registry<?>) reg).getReadOnlyWrapper())
    );

    // ----------------------------
    // API pública
    // ----------------------------

    public static boolean isEmpty(ItemStack belt) {
        for (int i = 0; i < SLOT_COUNT; i++) {
            if (!isEmpty(belt, i)) return false;
        }
        return true;
    }

    public static boolean isEmpty(ItemStack belt, int slot) {
        return get(belt, slot).isEmpty();
    }

    public static ItemStack get(ItemStack belt, int slot) {
        validateSlot(slot);

        NbtList list = getOrCreateList(belt);
        if (slot >= list.size()) return ItemStack.EMPTY;

        NbtCompound stackNbt = list.getCompound(slot);
        if (stackNbt.isEmpty()) return ItemStack.EMPTY;

        // ItemStack.fromNbtOrEmpty(lookup, nbt)
        return ItemStack.fromNbtOrEmpty(STATIC_LOOKUP, stackNbt);
    }

    public static void set(ItemStack belt, int slot, ItemStack stack) {
        validateSlot(slot);

        NbtList list = getOrCreateList(belt);

        // Asegura size
        ensureListSize(list, SLOT_COUNT);

        NbtCompound encoded = stack.isEmpty()
                ? new NbtCompound()
                : asCompound(stack.encode(STATIC_LOOKUP));

        list.set(slot, encoded);

        writeList(belt, list);
    }

    public static void clear(ItemStack belt, int slot) {
        set(belt, slot, ItemStack.EMPTY);
    }

    public static void clearAll(ItemStack belt) {
        NbtList list = new NbtList();
        ensureListSize(list, SLOT_COUNT);
        writeList(belt, list);
    }

    public static int getFilledCount(ItemStack belt) {
        int filled = 0;
        for (int i = 0; i < SLOT_COUNT; i++) {
            if (!isEmpty(belt, i)) filled++;
        }
        return filled;
    }

    /**
     * Devuelve el primer slot que tenga algo (SlotData.EMPTY si no hay ninguno).
     */
    public static SlotData getFirstFilled(ItemStack belt) {
        for (int i = 0; i < SLOT_COUNT; i++) {
            ItemStack s = get(belt, i);
            if (!s.isEmpty()) return new SlotData(i, s);
        }
        return SlotData.EMPTY;
    }

    /**
     * Si en algún lado todavía te sirve el int, aquí lo tienes.
     */
    public static int getFirstFilledSlot(ItemStack belt) {
        SlotData data = getFirstFilled(belt);
        return data.isPresent() ? data.slot() : -1;
    }

    // ----------------------------
    // Internals (CUSTOM_DATA)
    // ----------------------------

    private static NbtCompound getCustomData(ItemStack stack) {
        NbtComponent comp = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT);
        return comp.copyNbt();
    }

    private static void setCustomData(ItemStack stack, NbtCompound nbt) {
        if (nbt == null || nbt.isEmpty()) {
            stack.remove(DataComponentTypes.CUSTOM_DATA);
        } else {
            stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));
        }
    }

    private static NbtList getOrCreateList(ItemStack belt) {
        NbtCompound root = getCustomData(belt);

        NbtList list;
        if (root.contains(NBT_KEY, NbtElement.LIST_TYPE)) {
            list = root.getList(NBT_KEY, NbtElement.COMPOUND_TYPE);
        } else {
            list = new NbtList();
        }

        ensureListSize(list, SLOT_COUNT);

        // No escribimos aquí todavía; solo garantizamos size.
        return list;
    }

    private static void writeList(ItemStack belt, NbtList list) {
        NbtCompound root = getCustomData(belt);
        root.put(NBT_KEY, list);
        setCustomData(belt, root);
    }

    private static void ensureListSize(NbtList list, int size) {
        while (list.size() < size) list.add(new NbtCompound());
        while (list.size() > size) list.remove(list.size() - 1);
    }

    private static NbtCompound asCompound(NbtElement element) {
        if (element instanceof NbtCompound c) return c;

        // fallback ultra defensivo (no debería pasar con ItemStack.encode)
        NbtCompound wrap = new NbtCompound();
        wrap.put("stack", element);
        return wrap;
    }

    private static void validateSlot(int slot) {
        if (slot < 0 || slot >= SLOT_COUNT) {
            throw new IllegalArgumentException("Invalid belt slot " + slot + " (0.." + (SLOT_COUNT - 1) + ")");
        }
    }
}
