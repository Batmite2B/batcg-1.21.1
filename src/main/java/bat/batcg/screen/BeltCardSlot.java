package bat.batcg.screen;

import bat.batcg.card.ModCardComponents;
import bat.batcg.item.PokemonCardItem;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;

public class BeltCardSlot extends Slot {

    public BeltCardSlot(Inventory inventory, int index, int x, int y) {
        super(inventory, index, x, y);
    }

    @Override
    public boolean canInsert(ItemStack stack) {
        return stack.getItem() instanceof PokemonCardItem
                && stack.get(ModCardComponents.POKEMON_ID) != null
                && stack.get(ModCardComponents.CARD_TIER) != null; // ✅ FIX
    }

    @Override
    public int getMaxItemCount() {
        return 1;
    }

    @Override
    public int getMaxItemCount(ItemStack stack) {
        return 1;
    }
}
