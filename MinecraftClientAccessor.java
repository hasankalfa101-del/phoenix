package dev.phoenix.mixin;

import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(MinecraftClient.class)
public interface MinecraftClientAccessor {
    @Accessor("itemUseCooldown")
    void phoenix$setItemUseCooldown(int value);

    @Invoker("doAttack")
    boolean phoenix$doAttack();

    @Invoker("doItemUse")
    void phoenix$doItemUse();
}
