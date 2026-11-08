package dev.phoenix.mixin;

import dev.phoenix.modules.Misc;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public class LivingEntityMixin {
    @Inject(method = "getHandSwingDuration", at = @At("RETURN"), cancellable = true)
    private void phoenix$swing(CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(Misc.swingDuration((LivingEntity) (Object) this, cir.getReturnValueI()));
    }
}
