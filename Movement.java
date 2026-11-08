package dev.phoenix.modules;

import dev.phoenix.mixin.LivingEntityAccessor;
import dev.phoenix.module.Category;
import dev.phoenix.module.Module;
import dev.phoenix.module.ModuleManager;
import dev.phoenix.module.Stub;
import dev.phoenix.setting.BoolSetting;
import dev.phoenix.setting.NumSetting;
import dev.phoenix.util.U;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.util.math.Vec3d;

public final class Movement {
    private Movement() {}

    public static void register(ModuleManager m) {
        m.add(new Fly());
        m.add(new AutoWalk());
        m.add(new AutoJump());
        m.add(new Stub("Click TP", Category.MOVEMENT));
        m.add(new Stub("Boat Fly", Category.MOVEMENT));
        m.add(new Stub("Boat Noclip", Category.MOVEMENT));
        m.add(new Stub("Elytra Fly", Category.MOVEMENT));
        m.add(new Stub("Elytra Swap", Category.MOVEMENT));
        m.add(new Stub("Entity Control", Category.MOVEMENT));
        m.add(new Stub("Entity Speed", Category.MOVEMENT));
        m.add(new Stub("Jesus", Category.MOVEMENT));
        m.add(new Sneak());
        m.add(new Speed());
        m.add(new Spider());
        m.add(new Sprint());
        m.add(new NoJumpDelay());
        m.add(new Stub("Trident Boost", Category.MOVEMENT));
    }

    /** Normalised {x, z} movement direction from the held movement keys, or null when standing still. */
    static double[] direction() {
        var o = U.mc.options;
        double f = (o.forwardKey.isPressed() ? 1 : 0) - (o.backKey.isPressed() ? 1 : 0);
        double s = (o.leftKey.isPressed() ? 1 : 0) - (o.rightKey.isPressed() ? 1 : 0);
        if (f == 0 && s == 0) return null;
        double yaw = Math.toRadians(U.mc.player.getYaw());
        double fx = -Math.sin(yaw), fz = Math.cos(yaw);
        double lx = Math.cos(yaw), lz = Math.sin(yaw);
        double x = fx * f + lx * s, z = fz * f + lz * s;
        double len = Math.sqrt(x * x + z * z);
        return new double[]{x / len, z / len};
    }

    static class Fly extends Module {
        final NumSetting speed = add(new NumSetting("Speed", 1.0, 0.1, 5.0, 0.1));
        final NumSetting vertical = add(new NumSetting("Vertical Speed", 0.6, 0.1, 3.0, 0.1));
        final BoolSetting antiKick = add(new BoolSetting("Anti Kick", true));
        int ticks;

        Fly() { super("Fly", Category.MOVEMENT, "Fly with your movement keys, jump to go up and sneak to go down."); }

        @Override public void onTick() {
            if (mc.currentScreen != null) return;
            var p = mc.player;
            double[] d = direction();
            double vx = d == null ? 0 : d[0] * speed.get();
            double vz = d == null ? 0 : d[1] * speed.get();
            double vy = 0;
            if (mc.options.jumpKey.isPressed()) vy += vertical.get();
            if (mc.options.sneakKey.isPressed()) vy -= vertical.get();
            if (vy == 0 && antiKick.get() && ++ticks % 40 == 0) vy = -0.04;
            p.setVelocity(vx, vy, vz);
            p.fallDistance = 0;
        }
    }

    static class AutoWalk extends Module {
        AutoWalk() { super("Auto Walk", Category.MOVEMENT, "Holds the forward key for you."); }

        @Override public void onTick() {
            if (mc.currentScreen == null) mc.options.forwardKey.setPressed(true);
        }

        @Override protected void onDisable() { KeyBinding.updatePressedStates(); }
    }

    static class AutoJump extends Module {
        AutoJump() { super("Auto Jump", Category.MOVEMENT, "Jumps automatically while you move."); }

        @Override public void onTick() {
            if (mc.currentScreen == null && mc.player.isOnGround() && U.movingInput()) mc.player.jump();
        }
    }

    static class Sneak extends Module {
        Sneak() { super("Sneak", Category.MOVEMENT, "Holds the sneak key for you."); }

        @Override public void onTick() {
            if (mc.currentScreen == null) mc.options.sneakKey.setPressed(true);
        }

        @Override protected void onDisable() { KeyBinding.updatePressedStates(); }
    }

    static class Speed extends Module {
        final NumSetting speed = add(new NumSetting("Speed", 0.32, 0.1, 1.0, 0.01));
        final BoolSetting bhop = add(new BoolSetting("Bunny Hop", true));

        Speed() { super("Speed", Category.MOVEMENT, "Moves faster than normal."); }

        @Override public void onTick() {
            if (mc.currentScreen != null) return;
            var p = mc.player;
            double[] d = direction();
            if (d == null) return;
            if (bhop.get() && p.isOnGround()) p.jump();
            Vec3d v = p.getVelocity();
            p.setVelocity(d[0] * speed.get(), v.y, d[1] * speed.get());
        }
    }

    static class Spider extends Module {
        Spider() { super("Spider", Category.MOVEMENT, "Climb walls."); }

        @Override public void onTick() {
            if (mc.player.horizontalCollision) {
                Vec3d v = mc.player.getVelocity();
                mc.player.setVelocity(v.x, 0.2, v.z);
            }
        }
    }

    static class Sprint extends Module {
        final BoolSetting omni = add(new BoolSetting("All Directions", false));

        Sprint() { super("Sprint", Category.MOVEMENT, "Always sprint while moving."); }

        @Override public void onTick() {
            var p = mc.player;
            if (mc.currentScreen != null || p.isSneaking() || p.horizontalCollision) return;
            if (p.getHungerManager().getFoodLevel() <= 6) return;
            boolean moving = omni.get() ? U.movingInput() : mc.options.forwardKey.isPressed();
            if (moving) p.setSprinting(true);
        }
    }

    static class NoJumpDelay extends Module {
        NoJumpDelay() { super("No Jump Delay", Category.MOVEMENT, "Removes the delay between jumps."); }

        @Override public void onTick() { ((LivingEntityAccessor) mc.player).phoenix$setJumpingCooldown(0); }
    }
}
