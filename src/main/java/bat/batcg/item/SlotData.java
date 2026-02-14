package bat.batcg.item;

import net.minecraft.item.ItemStack;

/**
 * Representa un slot + el stack que contiene dentro del belt.
 */
public record SlotData(int slot, ItemStack stack) {

    public static final SlotData EMPTY = new SlotData(-1, ItemStack.EMPTY);

    public boolean isPresent() {
        return slot >= 0 && !stack.isEmpty();
    }
}
