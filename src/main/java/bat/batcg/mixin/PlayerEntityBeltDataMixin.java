package bat.batcg.mixin;

import bat.batcg.belt.BeltSlotAccess;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtOps;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Adds a 1-slot "belt equipment slot" stored on the player (saved to NBT).
 * This replaces Trinkets/Accessories dependency.
 */
@Mixin(PlayerEntity.class)
public abstract class PlayerEntityBeltDataMixin implements BeltSlotAccess {

    @Unique
    private final SimpleInventory batcg$beltSlot = new SimpleInventory(1);

    @Override
    public SimpleInventory batcg$getBeltSlotInventory() {
        return batcg$beltSlot;
    }

    @Inject(method = "writeCustomDataToNbt", at = @At("TAIL"))
    private void batcg$writeBelt(NbtCompound nbt, CallbackInfo ci) {
        var self = (PlayerEntity)(Object)this;
        var lookup = self.getWorld().getRegistryManager();
        var element = ItemStack.CODEC.encodeStart(lookup.getOps(NbtOps.INSTANCE), batcg$beltSlot.getStack(0))
                .result()
                .orElseGet(NbtCompound::new);

        if (element instanceof NbtCompound c) {
            nbt.put("BatcgBelt", c);
        }
    }

    @Inject(method = "readCustomDataFromNbt", at = @At("TAIL"))
    private void batcg$readBelt(NbtCompound nbt, CallbackInfo ci) {
        var self = (PlayerEntity)(Object)this;
        if (!nbt.contains("BatcgBelt", NbtElement.COMPOUND_TYPE)) return;

        NbtCompound c = nbt.getCompound("BatcgBelt");
        var lookup = self.getWorld().getRegistryManager();

        ItemStack stack = ItemStack.CODEC.parse(lookup.getOps(NbtOps.INSTANCE), c)
                .result()
                .orElse(ItemStack.EMPTY);

        batcg$beltSlot.setStack(0, stack);
    }

    @Inject(method = "dropInventory", at = @At("TAIL"))
    private void batcg$dropBelt(CallbackInfo ci) {
        var self = (PlayerEntity)(Object)this;
        ItemStack belt = batcg$beltSlot.getStack(0);
        if (!belt.isEmpty()) {
            self.dropItem(belt, true);
            batcg$beltSlot.setStack(0, ItemStack.EMPTY);
        }
    }
}
