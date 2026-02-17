package bat.batcg.belt;

import bat.batcg.item.CardBeltItem;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;

public class BeltSlot extends Slot {

    public BeltSlot(Inventory inventory, int index, int x, int y) {
        super(inventory, index, x, y);
    }

    @Override
    public boolean canInsert(ItemStack stack) {
        return stack.getItem() instanceof CardBeltItem;
    }

    @Override
    public int getMaxItemCount() {
        return 1;
    }
}
