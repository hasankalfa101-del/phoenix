package dev.phoenix.module;

import dev.phoenix.PhoenixClient;
import dev.phoenix.setting.Setting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.List;

public abstract class Module {
    protected static final MinecraftClient mc = MinecraftClient.getInstance();

    public final String name;
    public final String description;
    public final Category category;
    public final List<Setting<?>> settings = new ArrayList<>();

    public boolean enabled;
    /** GLFW key code, or -1 when unbound. */
    public int key = -1;
    /** When true the module is only on while its key is held. */
    public boolean holdMode;
    public boolean keyDown;
    /** GUI state: settings are expanded under the module row. */
    public boolean expanded;

    protected Module(String name, Category category, String description) {
        this.name = name;
        this.category = category;
        this.description = description;
    }

    protected <T extends Setting<?>> T add(T setting) {
        settings.add(setting);
        return setting;
    }

    /** False for modules that are listed in the GUI but have no behaviour yet. */
    public boolean implemented() { return true; }

    public void toggle() { setEnabled(!enabled); }

    public void setEnabled(boolean on) {
        if (on == enabled) return;
        enabled = on;
        try {
            if (on) onEnable(); else onDisable();
        } catch (Throwable t) {
            PhoenixClient.LOG.error("Module {} failed during {}", name, on ? "enable" : "disable", t);
        }
    }

    protected void onEnable() {}

    protected void onDisable() {}

    /** Called every client tick while enabled and in a world. */
    public void onTick() {}

    /** Called every HUD frame while enabled. */
    public void onHud(DrawContext ctx, float tickProgress) {}
}
