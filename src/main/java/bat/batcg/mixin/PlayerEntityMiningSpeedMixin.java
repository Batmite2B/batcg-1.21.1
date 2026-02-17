package bat.batcg.mixin;

import bat.batcg.belt.BeltEffects;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Hidden "haste-like" mining bonus (no visible potion effect).
 */
@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMiningSpeedMixin {

    @Inject(method = "getBlockBreakingSpeed", at = @At("RETURN"), cancellable = true)
    private void batcg$miningBonus(BlockState state, CallbackInfoReturnable<Float> cir) {
        PlayerEntity self = (PlayerEntity)(Object)this;
        float base = cir.getReturnValue();
        double mult = BeltEffects.miningMultiplier(self);
        cir.setReturnValue((float) (base * mult));
    }
}
