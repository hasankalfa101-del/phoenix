package dev.phoenix;

import dev.phoenix.config.Config;
import dev.phoenix.gui.ClickGui;
import dev.phoenix.module.Module;
import dev.phoenix.module.ModuleManager;
import dev.phoenix.util.U;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

public class PhoenixClient implements ClientModInitializer {
    public static final Logger LOG = LoggerFactory.getLogger("phoenix");
    public static ModuleManager modules;
    private static KeyBinding guiKey;

    @Override
    public void onInitializeClient() {
        modules = new ModuleManager();
        modules.init();
        Config.load();

        KeyBinding.Category category = KeyBinding.Category.create(Identifier.of("phoenix", "main"));
        guiKey = KeyBindingHelper.registerKeyBinding(
            new KeyBinding("key.phoenix.clickgui", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, category));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (guiKey.wasPressed()) {
                if (client.currentScreen instanceof ClickGui) client.setScreen(null);
                else if (client.currentScreen == null) client.setScreen(new ClickGui());
            }
            if (!Config.pending.isEmpty() && U.ready()) {
                for (String n : new ArrayList<>(Config.pending)) {
                    Module m = modules.byName(n);
                    if (m != null) m.setEnabled(true);
                }
                Config.pending.clear();
            }
            modules.tick();
        });

        HudRenderCallback.EVENT.register((ctx, tickCounter) ->
            modules.hud(ctx, tickCounter.getTickProgress(false)));

        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> Config.save());
        LOG.info("Phoenix Client loaded with {} modules", modules.all.size());
    }
}
