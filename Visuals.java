package dev.phoenix.modules;

import dev.phoenix.PhoenixClient;
import dev.phoenix.module.Category;
import dev.phoenix.module.Module;
import dev.phoenix.module.ModuleManager;
import dev.phoenix.module.Stub;
import dev.phoenix.setting.BoolSetting;
import dev.phoenix.setting.NumSetting;
import dev.phoenix.util.Draw;
import dev.phoenix.util.Proj;
import dev.phoenix.util.Theme;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.hit.EntityHitResult;
import org.lwjgl.glfw.GLFW;

public final class Visuals {
    private Visuals() {}

    public static void register(ModuleManager m) {
        m.add(new Stub("Swing Animation", Category.VISUALS));
        m.add(new Stub("Weapon Inspect", Category.VISUALS));
        m.add(new Stub("3D Hand", Category.VISUALS));
        m.add(new Stub("Hand Pose", Category.VISUALS));
        m.add(new Stub("Hand Aura", Category.VISUALS));
        m.add(new Stub("Block Overlay", Category.VISUALS));
        m.add(new CustomFov());
        Zoom zoom = new Zoom();
        zoom.key = GLFW.GLFW_KEY_C;
        zoom.holdMode = true;
        m.add(zoom);
        m.add(new CustomCrosshair());
        m.add(new Stub("Custom Glint", Category.VISUALS));
        m.add(new Stub("Hit Particles", Category.VISUALS));
        m.add(new TargetEsp());
        m.add(new Stub("Totem Spirit", Category.VISUALS));
        m.add(new Stub("Particles", Category.VISUALS));
        m.add(new Stub("Motion Blur", Category.VISUALS));
        m.add(new Stub("Atmosphere", Category.VISUALS));
        m.add(new Stub("Color Grading", Category.VISUALS));
        m.add(new Stub("Jump Circles", Category.VISUALS));
        m.add(new Stub("Pharaoh Headdress", Category.VISUALS));
        m.add(new Stub("3D Character", Category.VISUALS));
    }

    /** Used by the FOV mixin. */
    public static float adjustFov(float fov) {
        if (PhoenixClient.modules == null) return fov;
        CustomFov c = PhoenixClient.modules.get(CustomFov.class);
        if (c != null && c.enabled) fov = c.fov.getF();
        Zoom z = PhoenixClient.modules.get(Zoom.class);
        if (z != null && z.enabled) fov /= z.factor.getF();
        return fov;
    }

    /** Used by the crosshair mixin. */
    public static boolean hideVanillaCrosshair() {
        return PhoenixClient.modules != null && PhoenixClient.modules.on(CustomCrosshair.class);
    }

    static class CustomFov extends Module {
        final NumSetting fov = add(new NumSetting("FOV", 100, 30, 150, 1));

        CustomFov() { super("Custom Fov", Category.VISUALS, "Sets your field of view to any value."); }
    }

    static class Zoom extends Module {
        final NumSetting factor = add(new NumSetting("Zoom", 4.0, 1.5, 10.0, 0.5));

        Zoom() { super("Zoom", Category.VISUALS, "Hold the key to zoom in."); }
    }

    static class CustomCrosshair extends Module {
        final NumSetting size = add(new NumSetting("Size", 6, 1, 20, 1));
        final NumSetting gap = add(new NumSetting("Gap", 3, 0, 12, 1));
        final NumSetting thickness = add(new NumSetting("Thickness", 2, 1, 5, 1));
        final BoolSetting dot = add(new BoolSetting("Center Dot", false));
        final BoolSetting accent = add(new BoolSetting("Use Accent Colour", true));

        CustomCrosshair() { super("Custom Crosshair", Category.VISUALS, "Replaces the vanilla crosshair."); }

        @Override public void onHud(DrawContext ctx, float tp) {
            if (mc.options.hudHidden || mc.currentScreen != null) return;
            if (!mc.options.getPerspective().isFirstPerson()) return;
            int cx = mc.getWindow().getFramebufferWidth() / 2;
            int cy = mc.getWindow().getFramebufferHeight() / 2;
            int col = accent.get() ? Theme.accent : 0xFFFFFFFF;
            int s = size.getInt(), g = gap.getInt(), t = thickness.getInt();
            int h = t / 2;
            Draw.beginReal(ctx);
            try {
                ctx.fill(cx - g - s, cy - h, cx - g, cy - h + t, col);
                ctx.fill(cx + g, cy - h, cx + g + s, cy - h + t, col);
                ctx.fill(cx - h, cy - g - s, cx - h + t, cy - g, col);
                ctx.fill(cx - h, cy + g, cx - h + t, cy + g + s, col);
                if (dot.get()) ctx.fill(cx - h, cy - h, cx - h + t, cy - h + t, col);
            } finally {
                Draw.endReal(ctx);
            }
        }
    }

    static class TargetEsp extends Module {
        TargetEsp() { super("Target ESP", Category.VISUALS, "Marks your Kill Aura target (or the entity you are aiming at)."); }

        @Override public void onHud(DrawContext ctx, float tp) {
            Entity t = Combat.auraTarget();
            if (t == null && mc.crosshairTarget instanceof EntityHitResult hit && hit.getEntity() instanceof LivingEntity) {
                t = hit.getEntity();
            }
            if (t == null || !Proj.begin()) return;
            double[] r = Proj.rect(Render.lerpBox(t, tp));
            if (r == null) return;
            Draw.beginReal(ctx);
            try {
                double cx = (r[0] + r[2]) / 2;
                double top = r[1] - 14;
                for (int i = 0; i < 6; i++) {
                    ctx.fill((int) Math.round(cx) - 6 + i, (int) Math.round(top) + i, (int) Math.round(cx) + 6 - i, (int) Math.round(top) + i + 1, Theme.accent);
                }
                Draw.box(ctx, r[0], r[1], r[2], r[3], Theme.accent, 2);
            } finally {
                Draw.endReal(ctx);
            }
        }
    }
}
