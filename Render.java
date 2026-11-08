package dev.phoenix.modules;

import dev.phoenix.PhoenixClient;
import dev.phoenix.module.Category;
import dev.phoenix.module.Module;
import dev.phoenix.module.ModuleManager;
import dev.phoenix.module.Stub;
import dev.phoenix.setting.BoolSetting;
import dev.phoenix.setting.EnumSetting;
import dev.phoenix.setting.NumSetting;
import dev.phoenix.util.*;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.*;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class Render {
    private Render() {}

    public static void register(ModuleManager m) {
        m.add(new Fullbright());
        m.add(new Esp());
        m.add(new GlowEsp());
        m.add(new Stub("Nametags", Category.RENDER));
        m.add(new Stub("Spawner Nametags", Category.RENDER));
        m.add(new BlockEsp());
        m.add(new StorageEsp());
        m.add(new Stub("Freelook", Category.RENDER));
        m.add(new Stub("Hole ESP", Category.RENDER));
        m.add(new Stub("Pearl Trajectory", Category.RENDER));
        m.add(new Stub("Trajectories", Category.RENDER));
        m.add(new ItemEsp());
        m.add(new Stub("Xray", Category.RENDER));
        m.add(new Stub("No Render", Category.RENDER));
        m.add(new Stub("Time Changer", Category.RENDER));
        m.add(new LowHealthWarning());
        m.add(new Tracers());
    }

    /** Used by the entity mixin. */
    public static boolean glowing(Entity e) {
        if (PhoenixClient.modules == null || !U.ready()) return false;
        GlowEsp g = PhoenixClient.modules.get(GlowEsp.class);
        return g != null && g.enabled && g.matches(e);
    }

    /** ARGB colour to draw an entity with, or 0 when it should not be shown. */
    static int espColor(Entity e, boolean players, boolean hostiles, boolean passives) {
        if (e == U.mc.player || !(e instanceof LivingEntity le) || !le.isAlive() || e instanceof ArmorStandEntity) return 0;
        if (e instanceof PlayerEntity p) {
            if (p.isSpectator()) return 0;
            if (Friends.is(e)) return 0xFF22C55E;
            return players ? 0xFFFF4D4D : 0;
        }
        if (e instanceof Monster) return hostiles ? 0xFFFFA500 : 0;
        return passives ? 0xFF7CFC00 : 0;
    }

    static Box lerpBox(Entity e, float tp) {
        Vec3d lp = e.getLerpedPos(tp);
        Vec3d cur = e.getEntityPos();
        return e.getBoundingBox().offset(lp.x - cur.x, lp.y - cur.y, lp.z - cur.z);
    }

    // ------------------------------------------------------------------

    static class Fullbright extends Module {
        Fullbright() { super("Fullbright", Category.RENDER, "See in the dark."); }

        @Override public void onTick() {
            var p = mc.player;
            var fx = p.getStatusEffect(StatusEffects.NIGHT_VISION);
            if (fx == null || fx.getDuration() < 400) {
                p.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 1000, 0, false, false, false));
            }
        }

        @Override protected void onDisable() {
            if (mc.player != null) mc.player.removeStatusEffect(StatusEffects.NIGHT_VISION);
        }
    }

    static class Esp extends Module {
        final BoolSetting players = add(new BoolSetting("Players", true));
        final BoolSetting hostiles = add(new BoolSetting("Hostiles", true));
        final BoolSetting passives = add(new BoolSetting("Passives", false));
        final NumSetting range = add(new NumSetting("Range", 128, 16, 256, 8));
        final NumSetting thickness = add(new NumSetting("Thickness", 2, 1, 4, 1));

        Esp() { super("ESP", Category.RENDER, "Draws boxes around entities."); }

        @Override public void onHud(DrawContext ctx, float tp) {
            if (!Proj.begin()) return;
            Draw.beginReal(ctx);
            try {
                for (Entity e : mc.world.getEntities()) {
                    int col = espColor(e, players.get(), hostiles.get(), passives.get());
                    if (col == 0 || mc.player.distanceTo(e) > range.get()) continue;
                    double[] r = Proj.rect(lerpBox(e, tp));
                    if (r == null) continue;
                    Draw.box(ctx, r[0], r[1], r[2], r[3], col, thickness.getInt());
                }
            } finally {
                Draw.endReal(ctx);
            }
        }
    }

    static class GlowEsp extends Module {
        final BoolSetting players = add(new BoolSetting("Players", true));
        final BoolSetting hostiles = add(new BoolSetting("Hostiles", true));
        final BoolSetting passives = add(new BoolSetting("Passives", false));

        GlowEsp() { super("Glow ESP", Category.RENDER, "Gives entities the vanilla glowing outline."); }

        boolean matches(Entity e) {
            return espColor(e, players.get(), hostiles.get(), passives.get()) != 0;
        }
    }

    static class Tracers extends Module {
        final BoolSetting players = add(new BoolSetting("Players", true));
        final BoolSetting hostiles = add(new BoolSetting("Hostiles", false));
        final BoolSetting passives = add(new BoolSetting("Passives", false));
        final NumSetting range = add(new NumSetting("Range", 200, 16, 400, 8));
        final EnumSetting origin = add(new EnumSetting("Origin", "Crosshair", "Crosshair", "Bottom"));

        Tracers() { super("Tracers", Category.RENDER, "Draws lines to entities."); }

        @Override public void onHud(DrawContext ctx, float tp) {
            if (!Proj.begin()) return;
            Draw.beginReal(ctx);
            try {
                double ox = Proj.W / 2.0;
                double oy = origin.is("Bottom") ? Proj.H : Proj.H / 2.0;
                for (Entity e : mc.world.getEntities()) {
                    int col = espColor(e, players.get(), hostiles.get(), passives.get());
                    if (col == 0 || mc.player.distanceTo(e) > range.get()) continue;
                    Vec3d pos = e.getLerpedPos(tp).add(0, e.getHeight() / 2.0, 0);
                    double[] p = Proj.to(pos);
                    if (p == null) continue;
                    Draw.line(ctx, ox, oy, p[0], p[1], col, 2);
                }
            } finally {
                Draw.endReal(ctx);
            }
        }
    }

    static class ItemEsp extends Module {
        final NumSetting range = add(new NumSetting("Range", 64, 8, 128, 4));

        ItemEsp() { super("Item ESP", Category.RENDER, "Draws boxes around dropped items."); }

        @Override public void onHud(DrawContext ctx, float tp) {
            if (!Proj.begin()) return;
            Draw.beginReal(ctx);
            try {
                for (Entity e : mc.world.getEntities()) {
                    if (!(e instanceof ItemEntity) || mc.player.distanceTo(e) > range.get()) continue;
                    double[] r = Proj.rect(lerpBox(e, tp));
                    if (r == null) continue;
                    Draw.box(ctx, r[0], r[1], r[2], r[3], 0xFFFFD700, 2);
                }
            } finally {
                Draw.endReal(ctx);
            }
        }
    }

    static class StorageEsp extends Module {
        final BoolSetting chests = add(new BoolSetting("Chests", true));
        final BoolSetting barrels = add(new BoolSetting("Barrels", true));
        final BoolSetting shulkers = add(new BoolSetting("Shulkers", true));
        final BoolSetting ender = add(new BoolSetting("Ender Chests", true));
        final BoolSetting hoppers = add(new BoolSetting("Hoppers / Droppers", false));
        final BoolSetting furnaces = add(new BoolSetting("Furnaces", false));
        final NumSetting range = add(new NumSetting("Range", 128, 16, 256, 8));

        StorageEsp() { super("Storage ESP", Category.RENDER, "Draws boxes around containers."); }

        private boolean wanted(BlockEntity be) {
            if (be instanceof EnderChestBlockEntity) return ender.get();
            if (be instanceof ChestBlockEntity) return chests.get();
            if (be instanceof BarrelBlockEntity) return barrels.get();
            if (be instanceof ShulkerBoxBlockEntity) return shulkers.get();
            if (be instanceof HopperBlockEntity || be instanceof DispenserBlockEntity) return hoppers.get();
            if (be instanceof AbstractFurnaceBlockEntity) return furnaces.get();
            return false;
        }

        @Override public void onHud(DrawContext ctx, float tp) {
            if (!Proj.begin()) return;
            Draw.beginReal(ctx);
            try {
                double max2 = range.get() * range.get();
                for (BlockEntity be : Scan.blockEntities()) {
                    int col = Scan.storageColor(be);
                    if (col == 0 || !wanted(be)) continue;
                    var p = be.getPos();
                    if (mc.player.squaredDistanceTo(p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5) > max2) continue;
                    double[] r = Proj.rect(new Box(p));
                    if (r == null) continue;
                    Draw.box(ctx, r[0], r[1], r[2], r[3], col, 2);
                }
            } finally {
                Draw.endReal(ctx);
            }
        }
    }

    static class BlockEsp extends Module {
        final BoolSetting diamond = add(new BoolSetting("Diamond", true));
        final BoolSetting debris = add(new BoolSetting("Ancient Debris", true));
        final BoolSetting emerald = add(new BoolSetting("Emerald", false));
        final BoolSetting gold = add(new BoolSetting("Gold", false));
        final BoolSetting spawners = add(new BoolSetting("Spawners", true));
        final NumSetting radius = add(new NumSetting("Chunk Radius", 4, 1, 8, 1));
        final BlockFinder finder = new BlockFinder();

        BlockEsp() { super("Block ESP", Category.RENDER, "Draws boxes around selected blocks in nearby chunks."); }

        private Set<Block> targets() {
            Set<Block> t = new HashSet<>();
            if (diamond.get()) { t.add(Blocks.DIAMOND_ORE); t.add(Blocks.DEEPSLATE_DIAMOND_ORE); }
            if (debris.get()) t.add(Blocks.ANCIENT_DEBRIS);
            if (emerald.get()) { t.add(Blocks.EMERALD_ORE); t.add(Blocks.DEEPSLATE_EMERALD_ORE); }
            if (gold.get()) { t.add(Blocks.GOLD_ORE); t.add(Blocks.DEEPSLATE_GOLD_ORE); t.add(Blocks.NETHER_GOLD_ORE); }
            if (spawners.get()) t.add(Blocks.SPAWNER);
            return t;
        }

        @Override protected void onDisable() { finder.reset(); }

        @Override public void onTick() { finder.update(targets(), radius.getInt(), 40); }

        @Override public void onHud(DrawContext ctx, float tp) { finder.draw(ctx, 0xFF4DD2FF, 200); }
    }

    static class LowHealthWarning extends Module {
        final NumSetting health = add(new NumSetting("Warn Below (hp)", 6, 1, 19, 1));

        LowHealthWarning() { super("Low Health Warning", Category.RENDER, "Pulses a red border when your health is low."); }

        @Override public void onHud(DrawContext ctx, float tp) {
            float hp = mc.player.getHealth() + mc.player.getAbsorptionAmount();
            if (hp > health.get()) return;
            int w = mc.getWindow().getScaledWidth();
            int h = mc.getWindow().getScaledHeight();
            double pulse = 0.5 + 0.5 * Math.sin(System.currentTimeMillis() / 180.0);
            int a = (int) (70 + 90 * pulse);
            int red = (a << 24) | 0xFF2020;
            int clear = 0x00FF2020;
            ctx.fillGradient(0, 0, w, h / 6, red, clear);
            ctx.fillGradient(0, h - h / 6, w, h, clear, red);
        }
    }
}
