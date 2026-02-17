package bat.batcg.mixin;

import bat.batcg.belt.BeltSlot;
import bat.batcg.belt.BeltSlotAccess;
import bat.batcg.item.CardBeltItem;
import bat.batcg.item.ModItems;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Adds a custom 1-slot equipment slot next to offhand (shield) on the player inventory screen handler.
 * This slot is synced like any other Slot because it's part of the PlayerScreenHandler.
 */
@Mixin(PlayerScreenHandler.class)
public abstract class PlayerScreenHandlerBeltSlotMixin extends ScreenHandler {

    protected PlayerScreenHandlerBeltSlotMixin(net.minecraft.screen.ScreenHandlerType<?> type, int syncId) {
        super(type, syncId);
    }

    @Unique
    private int batcg$beltSlotIndex = -1;

    @Inject(method = "<init>(Lnet/minecraft/entity/player/PlayerInventory;ZLnet/minecraft/entity/player/PlayerEntity;)V", at = @At("TAIL"))
    private void batcg$addBeltSlot(PlayerInventory inventory, boolean onServer, PlayerEntity owner, CallbackInfo ci) {
        var beltInv = ((BeltSlotAccess) owner).batcg$getBeltSlotInventory();

        // Offhand slot is at (77, 62). We'll add our belt slot to the right.
        this.addSlot(new BeltSlot(beltInv, 0, 95, 62));
        this.batcg$beltSlotIndex = this.slots.size() - 1;
    }

    @Inject(method = "quickMove", at = @At("HEAD"), cancellable = true)
    private void batcg$quickMove(PlayerEntity player, int index, CallbackInfoReturnable<ItemStack> cir) {
        if (batcg$beltSlotIndex < 0) return;
        if (index < 0 || index >= this.slots.size()) return;

        Slot from = this.slots.get(index);
        if (!from.hasStack()) return;

        ItemStack stack = from.getStack();
        ItemStack copy = stack.copy();

        // Shift-click from belt slot -> inventory
        if (index == batcg$beltSlotIndex) {
            if (!this.insertItem(stack, 0, batcg$beltSlotIndex, true)) {
                cir.setReturnValue(ItemStack.EMPTY);
                return;
            }
            from.markDirty();
            cir.setReturnValue(copy);
            return;
        }

        // Shift-click belt item into belt slot
        if (stack.getItem() instanceof CardBeltItem) {
            Slot beltSlot = this.slots.get(batcg$beltSlotIndex);
            if (!beltSlot.hasStack() && beltSlot.canInsert(stack)) {
                ItemStack one = stack.copyWithCount(1);
                beltSlot.setStack(one);
                stack.decrement(1);
                from.markDirty();
                beltSlot.markDirty();
                cir.setReturnValue(copy);
                return;
            }
        }
        // Otherwise vanilla behavior
    }
}
