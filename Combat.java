package dev.phoenix.modules;

import dev.phoenix.PhoenixClient;
import dev.phoenix.mixin.MinecraftClientAccessor;
import dev.phoenix.module.Category;
import dev.phoenix.module.Module;
import dev.phoenix.module.ModuleManager;
import dev.phoenix.module.Stub;
import dev.phoenix.setting.BoolSetting;
import dev.phoenix.setting.NumSetting;
import dev.phoenix.util.Friends;
import dev.phoenix.util.U;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public final class Combat {
    private Combat() {}

    public static void register(ModuleManager m) {
        m.add(new AimAssist());
        m.add(new Stub("Anchor Macro", Category.COMBAT));
        m.add(new Stub("Anchor Aura", Category.COMBAT));
        m.add(new CrystalMacro());
        m.add(new CrystalAura());
        m.add(new Stub("Auto City", Category.COMBAT));
        m.add(new Triggerbot());
        m.add(new KillAura());
        m.add(new ShieldBreaker());
        m.add(new MaceSwap());
        m.add(new Stub("Hover Totem", Category.COMBAT));
        m.add(new FastXP());
        m.add(new AutoTotem());
        m.add(new AutoInventoryTotem());
        m.add(new WTap());
        m.add(new CrystalOptimizer());
        m.add(new FriendGuard());
        m.add(new Stub("Spear Swap", Category.COMBAT));
        m.add(new JumpReset());
        m.add(new Stub("Key Throw", Category.COMBAT));
    }

    /** Used by the attack mixin: true when Friend Guard should stop this attack. */
    public static boolean blockAttack(Entity target) {
        return PhoenixClient.modules != null && PhoenixClient.modules.on(FriendGuard.class) && Friends.is(target);
    }

    public static LivingEntity auraTarget() {
        if (PhoenixClient.modules == null) return null;
        KillAura k = PhoenixClient.modules.get(KillAura.class);
        return k != null && k.enabled ? k.target : null;
    }

    static boolean optimizerOn() {
        return PhoenixClient.modules != null && PhoenixClient.modules.on(CrystalOptimizer.class);
    }

    static void breakCrystal(EndCrystalEntity c) {
        U.attack(c);
        if (optimizerOn()) c.discard();
    }

    // ------------------------------------------------------------------

    static class AimAssist extends Module {
        final NumSetting range = add(new NumSetting("Range", 4.5, 1, 8, 0.1));
        final NumSetting fov = add(new NumSetting("FOV", 90, 10, 360, 5));
        final NumSetting speed = add(new NumSetting("Speed", 0.25, 0.05, 1, 0.05));
        final BoolSetting clicking = add(new BoolSetting("Only When Clicking", true));
        final BoolSetting players = add(new BoolSetting("Players", true));
        final BoolSetting hostiles = add(new BoolSetting("Hostiles", false));

        AimAssist() { super("Aim Assist", Category.COMBAT, "Smoothly pulls your aim toward nearby targets."); }

        @Override public void onTick() {
            if (mc.currentScreen != null) return;
            if (clicking.get() && !mc.options.attackKey.isPressed()) return;
            LivingEntity best = null;
            float bestAngle = fov.getF() / 2f;
            for (Entity e : mc.world.getEntities()) {
                if (!U.validTarget(e, players.get(), hostiles.get(), false)) continue;
                if (mc.player.distanceTo(e) > range.get() || !mc.player.canSee(e)) continue;
                float[] r = U.rotations(U.center(e));
                float a = Math.abs(MathHelper.wrapDegrees(r[0] - mc.player.getYaw()));
                if (a <= bestAngle) {
                    bestAngle = a;
                    best = (LivingEntity) e;
                }
            }
            if (best == null) return;
            float[] r = U.rotations(U.center(best));
            float sp = speed.getF();
            mc.player.setYaw(mc.player.getYaw() + MathHelper.wrapDegrees(r[0] - mc.player.getYaw()) * sp);
            mc.player.setPitch(MathHelper.clamp(mc.player.getPitch() + (r[1] - mc.player.getPitch()) * sp, -90f, 90f));
        }
    }

    static class KillAura extends Module {
        final NumSetting range = add(new NumSetting("Range", 3.2, 2, 6, 0.1));
        final BoolSetting players = add(new BoolSetting("Players", true));
        final BoolSetting hostiles = add(new BoolSetting("Hostiles", true));
        final BoolSetting passives = add(new BoolSetting("Passives", false));
        final BoolSetting walls = add(new BoolSetting("Through Walls", false));
        final BoolSetting rotate = add(new BoolSetting("Rotate", true));
        final BoolSetting cooldown = add(new BoolSetting("Wait For Cooldown", true));
        LivingEntity target;

        KillAura() { super("Kill Aura", Category.COMBAT, "Attacks nearby entities."); }

        @Override public void onTick() {
            target = null;
            if (mc.currentScreen != null) return;
            LivingEntity t = U.nearest(range.get(), players.get(), hostiles.get(), passives.get(), walls.get());
            if (t == null) return;
            target = t;
            if (rotate.get()) {
                float[] r = U.rotations(U.center(t));
                mc.player.setYaw(r[0]);
                mc.player.setPitch(MathHelper.clamp(r[1], -90f, 90f));
            }
            if (cooldown.get() && U.cooldown() < 0.95f) return;
            U.attack(t);
        }

        @Override protected void onDisable() { target = null; }
    }

    static class Triggerbot extends Module {
        final BoolSetting players = add(new BoolSetting("Players", true));
        final BoolSetting hostiles = add(new BoolSetting("Hostiles", true));
        final BoolSetting passives = add(new BoolSetting("Passives", false));

        Triggerbot() { super("Triggerbot", Category.COMBAT, "Attacks whatever you are aiming at."); }

        @Override public void onTick() {
            if (mc.currentScreen != null) return;
            if (!(mc.crosshairTarget instanceof EntityHitResult hit)) return;
            Entity e = hit.getEntity();
            if (!U.validTarget(e, players.get(), hostiles.get(), passives.get())) return;
            if (U.cooldown() < 0.95f) return;
            U.attack(e);
        }
    }

    static class CrystalMacro extends Module {
        final NumSetting delay = add(new NumSetting("Delay (ticks)", 1, 0, 10, 1));
        final BoolSetting place = add(new BoolSetting("Auto Place", true));
        int timer;

        CrystalMacro() { super("Crystal Macro", Category.COMBAT, "Hold right click to place and break crystals at your crosshair."); }

        @Override public void onTick() {
            if (mc.currentScreen != null) return;
            timer++;
            if (timer <= delay.getInt()) return;
            HitResult hr = mc.crosshairTarget;
            if (hr instanceof EntityHitResult eh && eh.getEntity() instanceof EndCrystalEntity c) {
                breakCrystal(c);
                timer = 0;
                return;
            }
            if (!place.get() || !mc.options.useKey.isPressed()) return;
            if (hr instanceof BlockHitResult bh && hr.getType() == HitResult.Type.BLOCK) {
                BlockState st = mc.world.getBlockState(bh.getBlockPos());
                if (!st.isOf(Blocks.OBSIDIAN) && !st.isOf(Blocks.BEDROCK)) return;
                Hand hand;
                if (mc.player.getMainHandStack().isOf(Items.END_CRYSTAL)) hand = Hand.MAIN_HAND;
                else if (mc.player.getOffHandStack().isOf(Items.END_CRYSTAL)) hand = Hand.OFF_HAND;
                else return;
                mc.interactionManager.interactBlock(mc.player, hand, bh);
                mc.player.swingHand(hand);
                timer = 0;
            }
        }
    }

    static class CrystalAura extends Module {
        final NumSetting targetRange = add(new NumSetting("Target Range", 10, 4, 16, 1));
        final NumSetting placeRange = add(new NumSetting("Place Range", 4.5, 2, 6, 0.1));
        final NumSetting breakRange = add(new NumSetting("Break Range", 4.5, 2, 6, 0.1));
        final NumSetting placeDelay = add(new NumSetting("Place Delay", 2, 0, 10, 1));
        final NumSetting breakDelay = add(new NumSetting("Break Delay", 1, 0, 10, 1));
        final NumSetting minHealth = add(new NumSetting("Min Health", 8, 0, 20, 1));
        final BoolSetting autoSwitch = add(new BoolSetting("Auto Switch", true));
        int placeTimer, breakTimer;

        CrystalAura() { super("Crystal Aura", Category.COMBAT, "Places and breaks end crystals near enemy players (basic)."); }

        @Override public void onTick() {
            if (mc.currentScreen != null) return;
            if (mc.player.getHealth() < minHealth.get()) return;
            placeTimer++;
            breakTimer++;
            LivingEntity target = U.nearest(targetRange.get(), true, false, false, true);
            if (target == null) return;

            if (breakTimer > breakDelay.getInt()) {
                for (Entity e : mc.world.getEntities()) {
                    if (!(e instanceof EndCrystalEntity c)) continue;
                    if (mc.player.distanceTo(c) > breakRange.get()) continue;
                    if (c.squaredDistanceTo(target) > 36) continue;
                    if (c.squaredDistanceTo(mc.player) < 6.25 && mc.player.getHealth() < 14) continue;
                    breakCrystal(c);
                    breakTimer = 0;
                    return;
                }
            }
            if (placeTimer > placeDelay.getInt()) place(target);
        }

        private void place(LivingEntity target) {
            Hand hand;
            if (mc.player.getOffHandStack().isOf(Items.END_CRYSTAL)) hand = Hand.OFF_HAND;
            else if (mc.player.getMainHandStack().isOf(Items.END_CRYSTAL)) hand = Hand.MAIN_HAND;
            else {
                int slot = autoSwitch.get() ? U.hotbar(s -> s.isOf(Items.END_CRYSTAL)) : -1;
                if (slot < 0) return;
                U.select(slot);
                hand = Hand.MAIN_HAND;
            }
            BlockPos base = target.getBlockPos();
            Vec3d eye = mc.player.getEyePos();
            Vec3d tpos = target.getEntityPos();
            BlockPos best = null;
            double bestScore = Double.MAX_VALUE;
            for (int dx = -3; dx <= 3; dx++) {
                for (int dy = -2; dy <= 0; dy++) {
                    for (int dz = -3; dz <= 3; dz++) {
                        BlockPos p = base.add(dx, dy, dz);
                        BlockState st = mc.world.getBlockState(p);
                        if (!st.isOf(Blocks.OBSIDIAN) && !st.isOf(Blocks.BEDROCK)) continue;
                        if (!mc.world.getBlockState(p.up()).isAir() || !mc.world.getBlockState(p.up(2)).isAir()) continue;
                        if (eye.distanceTo(Vec3d.ofCenter(p)) > placeRange.get()) continue;
                        Vec3d crystal = new Vec3d(p.getX() + 0.5, p.getY() + 1, p.getZ() + 0.5);
                        if (crystal.distanceTo(mc.player.getEntityPos()) < 2.5) continue;
                        Box space = new Box(p.getX(), p.getY() + 1, p.getZ(), p.getX() + 1, p.getY() + 3, p.getZ() + 1);
                        if (!mc.world.getOtherEntities(null, space).isEmpty()) continue;
                        double score = crystal.distanceTo(tpos);
                        if (score < bestScore) {
                            bestScore = score;
                            best = p;
                        }
                    }
                }
            }
            if (best == null) return;
            BlockHitResult bhr = new BlockHitResult(Vec3d.ofCenter(best), Direction.UP, best, false);
            mc.interactionManager.interactBlock(mc.player, hand, bhr);
            mc.player.swingHand(hand);
            placeTimer = 0;
        }
    }

    static class ShieldBreaker extends Module {
        ShieldBreaker() { super("Shield Breaker", Category.COMBAT, "Switches to an axe when you aim at a blocking player."); }

        @Override public void onTick() {
            if (mc.currentScreen != null) return;
            if (!(mc.crosshairTarget instanceof EntityHitResult hit)) return;
            if (!(hit.getEntity() instanceof LivingEntity le) || !le.isBlocking()) return;
            if (mc.player.getMainHandStack().isIn(ItemTags.AXES)) return;
            int slot = U.hotbar(s -> s.isIn(ItemTags.AXES));
            if (slot >= 0) U.select(slot);
        }
    }

    static class MaceSwap extends Module {
        final NumSetting minFall = add(new NumSetting("Min Fall Distance", 1.5, 0.5, 10, 0.5));

        MaceSwap() { super("Mace", Category.COMBAT, "Switches to a mace when you fall onto a target."); }

        @Override public void onTick() {
            if (mc.currentScreen != null) return;
            if (mc.player.fallDistance < minFall.get()) return;
            if (!(mc.crosshairTarget instanceof EntityHitResult)) return;
            if (mc.player.getMainHandStack().isOf(Items.MACE)) return;
            int slot = U.hotbar(s -> s.isOf(Items.MACE));
            if (slot >= 0) U.select(slot);
        }
    }

    static class FastXP extends Module {
        FastXP() { super("FastXP", Category.COMBAT, "Throw experience bottles without the use delay."); }

        @Override public void onTick() {
            if (mc.player.getMainHandStack().isOf(Items.EXPERIENCE_BOTTLE)
                || mc.player.getOffHandStack().isOf(Items.EXPERIENCE_BOTTLE)) {
                ((MinecraftClientAccessor) mc).phoenix$setItemUseCooldown(0);
            }
        }
    }

    static class AutoTotem extends Module {
        final NumSetting delay = add(new NumSetting("Delay (ticks)", 0, 0, 10, 1));
        int timer;

        AutoTotem() { super("Auto Totem", Category.COMBAT, "Keeps a totem of undying in your offhand."); }

        @Override public void onTick() {
            if (mc.currentScreen != null) return;
            if (mc.player.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING)) {
                timer = 0;
                return;
            }
            if (++timer <= delay.getInt()) return;
            int slot = U.inventory(s -> s.isOf(Items.TOTEM_OF_UNDYING));
            if (slot < 0) return;
            U.swapWithOffhand(slot);
            timer = 0;
        }
    }

    static class AutoInventoryTotem extends Module {
        AutoInventoryTotem() { super("Auto Inventory Totem", Category.COMBAT, "Moves a totem to your offhand while your inventory is open."); }

        @Override public void onTick() {
            if (!(mc.currentScreen instanceof InventoryScreen)) return;
            if (mc.player.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING)) return;
            int slot = U.inventory(s -> s.isOf(Items.TOTEM_OF_UNDYING));
            if (slot >= 0) U.swapWithOffhand(slot);
        }
    }

    static class WTap extends Module {
        WTap() { super("W Tap", Category.COMBAT, "Resets sprint when you hit something for extra knockback."); }

        @Override public void onTick() {
            if (mc.currentScreen != null) return;
            if (mc.options.attackKey.isPressed() && mc.crosshairTarget instanceof EntityHitResult
                && mc.player.isSprinting() && U.cooldown() > 0.9f) {
                mc.player.setSprinting(false);
            }
        }
    }

    static class CrystalOptimizer extends Module {
        CrystalOptimizer() { super("Crystal Optimizer", Category.COMBAT, "Removes crystals on your side the moment you hit them."); }
    }

    static class FriendGuard extends Module {
        FriendGuard() { super("Friend Guard", Category.COMBAT, "Stops you from attacking players on your friends list."); }
    }

    static class JumpReset extends Module {
        JumpReset() { super("Jump Reset", Category.COMBAT, "Jumps when you take a hit to reduce knockback."); }

        @Override public void onTick() {
            if (mc.currentScreen != null) return;
            if (mc.player.hurtTime >= 8 && mc.player.isOnGround()) mc.player.jump();
        }
    }
}
