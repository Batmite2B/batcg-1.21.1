package bat.batcg.screen;

import bat.batcg.belt.BatcgBeltApi;
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

    private final PlayerEntity player;
    private final Inventory beltInv;

    public BeltScreenHandler(int syncId, PlayerInventory playerInv) {
        super(ModScreenHandlers.BELT, syncId);
        this.player = playerInv.player;

        ItemStack beltStack = BatcgBeltApi.findBeltForScreen(this.player);

        // Client: dummy
        // Server: inventario real que escribe al ItemStack correcto
        if (this.player.getWorld().isClient) {
            this.beltInv = new SimpleInventory(BeltCards.SLOTS);
        } else {
            this.beltInv = new BeltCardInventory(beltStack);
        }

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
        int invY = 51;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInv, 9 + col + row * 9, 8 + col * 18, invY + row * 18));
            }
        }

        // Hotbar
        int hotbarY = 109;
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInv, col, 8 + col * 18, hotbarY));
        }
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        // No cierres la GUI si el belt está en mano O equipado
        ItemStack b = BatcgBeltApi.findBeltForScreen(player);
        return !b.isEmpty() && (b.getItem() instanceof CardBeltItem);
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

    @Override
    public void onClosed(PlayerEntity player) {
        super.onClosed(player);
        // Solo server debe persistir
        if (!player.getWorld().isClient) {
            this.beltInv.markDirty();
        }
    }
}
