package dev.phoenix.mixin;

import net.minecraft.client.network.ClientPlayerInteractionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ClientPlayerInteractionManager.class)
public interface ClientPlayerInteractionManagerAccessor {
    @Accessor("blockBreakingCooldown")
    void phoenix$setBlockBreakingCooldown(int value);

    @Accessor("currentBreakingProgress")
    float phoenix$getBreakingProgress();

    @Accessor("currentBreakingProgress")
    void phoenix$setBreakingProgress(float value);
}
