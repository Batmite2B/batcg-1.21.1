package bat.batcg.screen;

import bat.batcg.item.CardBeltItem;
import bat.batcg.item.ModItems;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;

public class CardBeltScreenHandler extends ScreenHandler {

    private final PlayerEntity player;
    private final ItemStack beltStack;
    private final BeltCardInventory beltInv;

    // Slot layout
    public static final int BELT_SLOTS = 5;

    public CardBeltScreenHandler(int syncId, PlayerInventory playerInv) {
        super(ModScreenHandlers.BELT, syncId);

        this.player = playerInv.player;
        this.beltStack = playerInv.player.getMainHandStack(); // ✅ abrimos desde la mano (click derecho)
        this.beltInv = new BeltCardInventory(beltStack);

        // --- 5 slots del belt (sobre la barra blanca del gamemode_switcher invertido) ---
        int startX = 39;      // centrado para textura 128 dentro de bg 176
        int y = 109;          // barra de abajo (porque invertimos la textura)
        for (int i = 0; i < BELT_SLOTS; i++) {
            this.addSlot(new BeltCardSlot(beltInv, i, startX + i * 20, y));
        }

        // --- Inventario jugador ---
        int invY = 140;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInv, col + row * 9 + 9, 8 + col * 18, invY + row * 18));
            }
        }

        // Hotbar
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInv, col, 8 + col * 18, invY + 58));
        }
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        // ✅ que se cierre si ya no lo tienes en mano
        return player.getMainHandStack().getItem() instanceof CardBeltItem
                && player.getMainHandStack().isOf(ModItems.CARD_BELT);
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int index) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (!slot.hasStack()) return ItemStack.EMPTY;

        ItemStack original = slot.getStack();
        newStack = original.copy();

        // 0..4 = slots del belt
        if (index < BELT_SLOTS) {
            // belt -> inventario
            if (!this.insertItem(original, BELT_SLOTS, this.slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else {
            // inventario -> belt
            if (!this.insertItem(original, 0, BELT_SLOTS, false)) {
                return ItemStack.EMPTY;
            }
        }

        if (original.isEmpty()) slot.setStack(ItemStack.EMPTY);
        else slot.markDirty();

        return newStack;
    }
}
