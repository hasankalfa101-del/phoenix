package dev.phoenix.module;

import dev.phoenix.PhoenixClient;
import dev.phoenix.modules.*;
import dev.phoenix.util.U;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.InputUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ModuleManager {
    public final List<Module> all = new ArrayList<>();
    private final Map<Class<?>, Module> byClass = new HashMap<>();

    public void add(Module m) {
        all.add(m);
        byClass.put(m.getClass(), m);
    }

    public <T extends Module> T get(Class<T> c) { return c.cast(byClass.get(c)); }

    public boolean on(Class<? extends Module> c) {
        Module m = byClass.get(c);
        return m != null && m.enabled;
    }

    public List<Module> in(Category c) {
        List<Module> out = new ArrayList<>();
        for (Module m : all) if (m.category == c) out.add(m);
        return out;
    }

    public Module byName(String name) {
        for (Module m : all) if (m.name.equalsIgnoreCase(name)) return m;
        return null;
    }

    public void init() {
        Combat.register(this);
        Misc.register(this);
        Donut.register(this);
        Render.register(this);
        Client.register(this);
        Movement.register(this);
        BaseRaid.register(this);
        Visuals.register(this);
    }

    public void tick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        pollKeys(mc);
        if (!U.ready()) return;
        for (Module m : all) {
            if (!m.enabled) continue;
            try {
                m.onTick();
            } catch (Throwable t) {
                PhoenixClient.LOG.error("Module {} threw during tick; disabling it", m.name, t);
                m.enabled = false;
            }
        }
    }

    public void hud(DrawContext ctx, float tickProgress) {
        if (!U.ready()) return;
        for (Module m : all) {
            if (!m.enabled) continue;
            try {
                m.onHud(ctx, tickProgress);
            } catch (Throwable t) {
                PhoenixClient.LOG.error("Module {} threw during HUD render; disabling it", m.name, t);
                m.enabled = false;
            }
        }
    }

    private void pollKeys(MinecraftClient mc) {
        if (mc.currentScreen != null || mc.getWindow() == null) {
            for (Module m : all) {
                m.keyDown = false;
                if (m.holdMode && m.enabled) m.setEnabled(false);
            }
            return;
        }
        for (Module m : all) {
            if (m.key <= 0) continue;
            boolean down = InputUtil.isKeyPressed(mc.getWindow(), m.key);
            boolean was = m.keyDown;
            m.keyDown = down;
            if (m.holdMode) m.setEnabled(down);
            else if (down && !was) m.toggle();
        }
    }
}
