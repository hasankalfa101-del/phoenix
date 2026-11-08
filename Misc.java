package dev.phoenix.modules;

import dev.phoenix.PhoenixClient;
import dev.phoenix.mixin.MinecraftClientAccessor;
import dev.phoenix.module.Category;
import dev.phoenix.module.Module;
import dev.phoenix.module.ModuleManager;
import dev.phoenix.module.Stub;
import dev.phoenix.setting.BoolSetting;
import dev.phoenix.setting.NumSetting;
import dev.phoenix.setting.StrSetting;
import dev.phoenix.util.U;
import net.minecraft.block.BlockState;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import org.lwjgl.glfw.GLFW;

public final class Misc {
    private Misc() {}

    public static void register(ModuleManager m) {
        m.add(new LegitAutoTool());
        m.add(new AutoClicker());
        m.add(new AutoEat());
        m.add(new AutoReplenish());
        m.add(new Stub("Auto Reconnect", Category.MISC));
        Stub freecam = new Stub("Freecam", Category.MISC);
        freecam.key = GLFW.GLFW_KEY_F4;
        m.add(freecam);
        m.add(new Spam());
        m.add(new Stub("Coord Snapper", Category.MISC));
        m.add(new Stub("Chat Macro", Category.MISC));
        m.add(new Stub("Name Protect", Category.MISC));
        m.add(new Stub("No Inv Leak", Category.MISC));
        m.add(new WeatherNotifier());
        m.add(new SlowSwing());
        m.add(new Stub("Skin Change", Category.MISC));
    }

    /** Used by the swing mixin. */
    public static int swingDuration(LivingEntity e, int original) {
        if (PhoenixClient.modules == null || e != U.mc.player) return original;
        SlowSwing s = PhoenixClient.modules.get(SlowSwing.class);
        if (s == null || !s.enabled) return original;
        return Math.max(1, Math.round(original * s.mult.getF()));
    }

    // ------------------------------------------------------------------

    static class LegitAutoTool extends Module {
        int prev = -1;

        LegitAutoTool() { super("Legit Auto Tool", Category.MISC, "Switches to the fastest tool while you mine."); }

        @Override public void onTick() {
            if (mc.currentScreen != null) return;
            boolean mining = mc.options.attackKey.isPressed();
            if (mining && mc.crosshairTarget instanceof BlockHitResult bh && mc.crosshairTarget.getType() == HitResult.Type.BLOCK) {
                BlockState st = mc.world.getBlockState(bh.getBlockPos());
                if (st.isAir()) return;
                float best = mc.player.getInventory().getStack(U.selected()).getMiningSpeedMultiplier(st);
                int bestSlot = -1;
                for (int i = 0; i < 9; i++) {
                    float sp = mc.player.getInventory().getStack(i).getMiningSpeedMultiplier(st);
                    if (sp > best) {
                        best = sp;
                        bestSlot = i;
                    }
                }
                if (bestSlot >= 0) {
                    if (prev < 0) prev = U.selected();
                    U.select(bestSlot);
                }
            } else if (!mining && prev >= 0) {
                U.select(prev);
                prev = -1;
            }
        }

        @Override protected void onDisable() { prev = -1; }
    }

    static class AutoClicker extends Module {
        final NumSetting cps = add(new NumSetting("CPS", 12, 1, 20, 1));
        long next;

        AutoClicker() { super("Auto Clicker", Category.MISC, "Clicks for you while you hold left click."); }

        @Override public void onTick() {
            if (mc.currentScreen != null || !mc.options.attackKey.isPressed()) return;
            long now = System.currentTimeMillis();
            if (now < next) return;
            ((MinecraftClientAccessor) mc).phoenix$doAttack();
            next = now + (long) (1000.0 / cps.get() * (0.85 + Math.random() * 0.3));
        }
    }

    static class AutoEat extends Module {
        final NumSetting hunger = add(new NumSetting("Eat Below", 14, 1, 19, 1));
        boolean eating;
        int prevSlot = -1;

        AutoEat() { super("Auto Eat", Category.MISC, "Eats food from your hotbar when you get hungry."); }

        private static boolean goodFood(ItemStack s) {
            return s.getComponents().contains(DataComponentTypes.FOOD)
                && !s.isOf(Items.ROTTEN_FLESH) && !s.isOf(Items.SPIDER_EYE)
                && !s.isOf(Items.PUFFERFISH) && !s.isOf(Items.POISONOUS_POTATO);
        }

        @Override public void onTick() {
            if (mc.currentScreen != null) {
                stop();
                return;
            }
            int food = mc.player.getHungerManager().getFoodLevel();
            if (eating) {
                if (food >= 20 || !mc.player.getMainHandStack().getComponents().contains(DataComponentTypes.FOOD)) stop();
                else mc.options.useKey.setPressed(true);
                return;
            }
            if (food <= hunger.getInt()) {
                int slot = U.hotbar(AutoEat::goodFood);
                if (slot < 0) return;
                prevSlot = U.selected();
                U.select(slot);
                eating = true;
                mc.options.useKey.setPressed(true);
            }
        }

        private void stop() {
            if (!eating) return;
            eating = false;
            KeyBinding.updatePressedStates();
            if (prevSlot >= 0 && U.ready()) U.select(prevSlot);
            prevSlot = -1;
        }

        @Override protected void onDisable() { stop(); }
    }

    static class AutoReplenish extends Module {
        final NumSetting threshold = add(new NumSetting("Refill At", 8, 1, 32, 1));
        int timer;

        AutoReplenish() { super("Auto Replenish", Category.MISC, "Refills hotbar stacks from your inventory."); }

        @Override public void onTick() {
            if (mc.currentScreen != null) return;
            if (++timer < 5) return;
            timer = 0;
            for (int h = 0; h < 9; h++) {
                ItemStack st = mc.player.getInventory().getStack(h);
                if (st.isEmpty() || !st.isStackable() || st.getCount() > threshold.getInt()) continue;
                for (int i = 9; i < 36; i++) {
                    ItemStack other = mc.player.getInventory().getStack(i);
                    if (other.isEmpty() || !ItemStack.areItemsAndComponentsEqual(st, other)) continue;
                    var im = mc.interactionManager;
                    var p = mc.player;
                    im.clickSlot(0, i, 0, net.minecraft.screen.slot.SlotActionType.PICKUP, p);
                    im.clickSlot(0, h + 36, 0, net.minecraft.screen.slot.SlotActionType.PICKUP, p);
                    if (!p.currentScreenHandler.getCursorStack().isEmpty()) {
                        im.clickSlot(0, i, 0, net.minecraft.screen.slot.SlotActionType.PICKUP, p);
                    }
                    return;
                }
            }
        }
    }

    static class Spam extends Module {
        final StrSetting message = add(new StrSetting("Message", "Phoenix Client"));
        final NumSetting delay = add(new NumSetting("Delay (sec)", 5, 1, 60, 1));
        int timer;

        Spam() { super("Spam", Category.MISC, "Sends a chat message on a timer."); }

        @Override protected void onEnable() { timer = 0; }

        @Override public void onTick() {
            if (++timer < delay.getInt() * 20) return;
            timer = 0;
            String msg = message.get();
            if (msg.isEmpty()) return;
            if (msg.startsWith("/")) mc.player.networkHandler.sendChatCommand(msg.substring(1));
            else mc.player.networkHandler.sendChatMessage(msg);
        }
    }

    static class WeatherNotifier extends Module {
        boolean rain, thunder, init;

        WeatherNotifier() { super("Weather Notifier", Category.MISC, "Tells you in chat when the weather changes."); }

        @Override protected void onEnable() { init = false; }

        @Override public void onTick() {
            boolean r = mc.world.isRaining(), t = mc.world.isThundering();
            if (init) {
                if (r != rain) U.chat(r ? "It started raining." : "The rain stopped.");
                if (t != thunder) U.chat(t ? "A thunderstorm started." : "The thunderstorm stopped.");
            }
            rain = r;
            thunder = t;
            init = true;
        }
    }

    static class SlowSwing extends Module {
        final NumSetting mult = add(new NumSetting("Swing Length", 2.0, 1.0, 5.0, 0.25));

        SlowSwing() { super("Slow Swing", Category.MISC, "Makes your swing animation last longer."); }
    }
}
