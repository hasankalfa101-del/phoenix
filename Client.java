package dev.phoenix.modules;

import dev.phoenix.PhoenixClient;
import dev.phoenix.module.Category;
import dev.phoenix.module.Module;
import dev.phoenix.module.ModuleManager;
import dev.phoenix.module.Stub;
import dev.phoenix.setting.BoolSetting;
import dev.phoenix.setting.NumSetting;
import dev.phoenix.util.Draw;
import dev.phoenix.util.Theme;
import dev.phoenix.util.U;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.player.PlayerEntity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** The "Phoenix Client" panel: overlays and client features. */
public final class Client {
    private Client() {}

    public static void register(ModuleManager m) {
        m.add(new Hud());
        m.add(new Stub("Spotify HUD", Category.CLIENT));
        m.add(new Stub("Region Map", Category.CLIENT));
        m.add(new Radar());
        m.add(new Hotkeys());
        m.add(new Performance());
        m.add(new Stub("Discord Presence", Category.CLIENT));
    }

    static class Hud extends Module {
        final BoolSetting watermark = add(new BoolSetting("Watermark", true));
        final BoolSetting fps = add(new BoolSetting("FPS", true));
        final BoolSetting coords = add(new BoolSetting("Coordinates", true));
        final BoolSetting list = add(new BoolSetting("Module List", true));

        Hud() { super("HUD", Category.CLIENT, "Watermark, FPS, coordinates and the enabled module list."); }

        @Override public void onHud(DrawContext ctx, float tp) {
            if (mc.options.hudHidden) return;
            var tr = mc.textRenderer;
            int w = mc.getWindow().getScaledWidth();
            int h = mc.getWindow().getScaledHeight();
            int y = 4;
            if (watermark.get()) {
                ctx.drawText(tr, "Phoenix Client", 4, y, Theme.accent, true);
                y += 10;
            }
            if (fps.get()) {
                ctx.drawText(tr, mc.getCurrentFps() + " FPS", 4, y, Theme.WHITE, true);
            }
            if (coords.get()) {
                String s = "XYZ " + mc.player.getBlockX() + " " + mc.player.getBlockY() + " " + mc.player.getBlockZ();
                ctx.drawText(tr, s, 4, h - 12, Theme.WHITE, true);
            }
            if (list.get()) {
                List<String> names = new ArrayList<>();
                for (Module mod : PhoenixClient.modules.all) {
                    if (mod.enabled && mod != this && mod.implemented()) names.add(mod.name);
                }
                names.sort(Comparator.comparingInt((String s) -> tr.getWidth(s)).reversed());
                int ly = 4;
                for (String n : names) {
                    ctx.drawText(tr, n, w - 4 - tr.getWidth(n), ly, Theme.accent, true);
                    ly += 10;
                }
            }
        }
    }

    static class Radar extends Module {
        final NumSetting size = add(new NumSetting("Size", 90, 50, 160, 5));
        final NumSetting range = add(new NumSetting("Range", 64, 16, 128, 8));
        final BoolSetting players = add(new BoolSetting("Players", true));
        final BoolSetting hostiles = add(new BoolSetting("Hostiles", true));
        final BoolSetting passives = add(new BoolSetting("Passives", false));

        Radar() { super("Radar", Category.CLIENT, "Top-down radar of nearby entities."); }

        @Override public void onHud(DrawContext ctx, float tp) {
            if (mc.options.hudHidden) return;
            int s = size.getInt();
            int x = 6, y = 40;
            int cx = x + s / 2, cy = y + s / 2;
            ctx.fill(x, y, x + s, y + s, 0xAA11151B);
            Draw.box(ctx, x, y, x + s, y + s, 0xFF2B3340, 1);
            ctx.fill(cx, y + 1, cx + 1, y + s - 1, 0x33FFFFFF);
            ctx.fill(x + 1, cy, x + s - 1, cy + 1, 0x33FFFFFF);
            double yaw = Math.toRadians(mc.player.getYaw());
            double fx = -Math.sin(yaw), fz = Math.cos(yaw);
            double rx = -Math.cos(yaw), rz = -Math.sin(yaw);
            double px = mc.player.getX(), pz = mc.player.getZ();
            double half = s / 2.0 - 3;
            for (Entity e : mc.world.getEntities()) {
                int col = Render.espColor(e, players.get(), hostiles.get(), passives.get());
                if (col == 0) continue;
                double dx = e.getX() - px, dz = e.getZ() - pz;
                double right = (dx * rx + dz * rz) / range.get();
                double fwd = (dx * fx + dz * fz) / range.get();
                double len = Math.sqrt(right * right + fwd * fwd);
                if (len > 1) {
                    right /= len;
                    fwd /= len;
                }
                int dotX = (int) Math.round(cx + right * half);
                int dotY = (int) Math.round(cy - fwd * half);
                ctx.fill(dotX - 1, dotY - 1, dotX + 2, dotY + 2, col);
            }
            ctx.fill(cx - 1, cy - 1, cx + 2, cy + 2, 0xFFFFFFFF);
        }
    }

    static class Hotkeys extends Module {
        Hotkeys() { super("Hotkeys", Category.CLIENT, "Shows modules that have a key bound."); }

        @Override public void onHud(DrawContext ctx, float tp) {
            if (mc.options.hudHidden) return;
            var tr = mc.textRenderer;
            int w = mc.getWindow().getScaledWidth();
            int h = mc.getWindow().getScaledHeight();
            List<Module> bound = new ArrayList<>();
            for (Module m : PhoenixClient.modules.all) if (m.key > 0) bound.add(m);
            int y = h - 12 - bound.size() * 10;
            for (Module m : bound) {
                String s = m.name + " [" + U.keyName(m.key) + "]";
                ctx.drawText(tr, s, w - 4 - tr.getWidth(s), y, m.enabled ? Theme.accent : Theme.TEXT_DIM, true);
                y += 10;
            }
        }
    }

    static class Performance extends Module {
        Performance() { super("Performance", Category.CLIENT, "Shows FPS and memory use."); }

        @Override public void onHud(DrawContext ctx, float tp) {
            if (mc.options.hudHidden) return;
            Runtime rt = Runtime.getRuntime();
            long used = (rt.totalMemory() - rt.freeMemory()) / (1024 * 1024);
            long max = rt.maxMemory() / (1024 * 1024);
            String s = mc.getCurrentFps() + " FPS  |  " + used + " / " + max + " MB";
            int w = mc.getWindow().getScaledWidth();
            ctx.drawText(mc.textRenderer, s, (w - mc.textRenderer.getWidth(s)) / 2, 4, Theme.WHITE, true);
        }
    }
}
