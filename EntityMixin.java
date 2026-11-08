package dev.phoenix.mixin;

import dev.phoenix.modules.Render;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public class EntityMixin {
    @Inject(method = "isGlowing", at = @At("HEAD"), cancellable = true)
    private void phoenix$glow(CallbackInfoReturnable<Boolean> cir) {
        if (Render.glowing((Entity) (Object) this)) cir.setReturnValue(true);
    }
}
