package bat.batcg.screen;

import bat.batcg.belt.BeltCards;
import bat.batcg.item.CardBeltItem;
import bat.batcg.item.PokemonCardItem;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;

public class BeltScreenHandler extends ScreenHandler {

    private final ItemStack beltStack;
    private final Inventory beltInv;

    public BeltScreenHandler(int syncId, PlayerInventory playerInv) {
        super(ModScreenHandlers.BELT, syncId);

        // ✅ No usamos "handId" para evitar problemas: tomamos la mano que tenga el belt
        ItemStack main = playerInv.player.getMainHandStack();
        ItemStack off = playerInv.player.getOffHandStack();
        this.beltStack = (main.getItem() instanceof CardBeltItem) ? main : off;

        // Server: inventario real (escribe al ItemStack)
        // Client: dummy (los slots se sincronizan igual)
        if (playerInv.player.getWorld().isClient) this.beltInv = new SimpleInventory(BeltCards.SLOTS);
        else this.beltInv = new BeltCardInventory(beltStack);

        // 5 slots del belt
        int startX = 44;
        int y = 20;
        for (int i = 0; i < BeltCards.SLOTS; i++) {
            this.addSlot(new Slot(beltInv, i, startX + i * 18, y) {
                @Override public boolean canInsert(ItemStack stack) {
                    return stack.getItem() instanceof PokemonCardItem;
                }
                @Override public int getMaxItemCount() { return 1; }
            });
        }

        // Inventario del jugador (3 filas)
        int invY = 54;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInv, 9 + col + row * 9, 8 + col * 18, invY + row * 18));
            }
        }

        // Hotbar
        int hotbarY = 112;
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInv, col, 8 + col * 18, hotbarY));
        }
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        ItemStack main = player.getMainHandStack();
        ItemStack off = player.getOffHandStack();
        return main.getItem() instanceof CardBeltItem || off.getItem() instanceof CardBeltItem;
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int index) {
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasStack()) return ItemStack.EMPTY;

        ItemStack original = slot.getStack();
        ItemStack copy = original.copy();

        int beltSize = BeltCards.SLOTS;

        if (index < beltSize) {
            // Belt -> player
            if (!this.insertItem(original, beltSize, this.slots.size(), true)) return ItemStack.EMPTY;
        } else {
            // Player -> belt (solo cartas)
            if (original.getItem() instanceof PokemonCardItem) {
                if (!this.insertItem(original, 0, beltSize, false)) return ItemStack.EMPTY;
            } else {
                return ItemStack.EMPTY;
            }
        }

        if (original.isEmpty()) slot.setStack(ItemStack.EMPTY);
        else slot.markDirty();

        return copy;
    }
}
