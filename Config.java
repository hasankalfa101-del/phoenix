package dev.phoenix.config;

import com.google.gson.*;
import dev.phoenix.PhoenixClient;
import dev.phoenix.module.Module;
import dev.phoenix.setting.Setting;
import dev.phoenix.util.Friends;
import dev.phoenix.util.Theme;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class Config {
    /** Modules that were on when the config was saved; enabled once a world is loaded. */
    public static final Set<String> pending = new HashSet<>();
    /** ClickGUI panel positions, keyed by category title: {x, y}. */
    public static final Map<String, float[]> panels = new HashMap<>();

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private Config() {}

    private static Path file() { return FabricLoader.getInstance().getConfigDir().resolve("phoenix.json"); }

    public static void save() {
        if (PhoenixClient.modules == null) return;
        try {
            JsonObject root = new JsonObject();
            JsonObject mods = new JsonObject();
            for (Module m : PhoenixClient.modules.all) {
                JsonObject o = new JsonObject();
                o.addProperty("enabled", m.enabled && m.implemented());
                o.addProperty("key", m.key);
                o.addProperty("hold", m.holdMode);
                JsonObject s = new JsonObject();
                for (Setting<?> st : m.settings) s.add(st.name, st.save());
                o.add("settings", s);
                mods.add(m.name, o);
            }
            root.add("modules", mods);
            JsonArray fr = new JsonArray();
            for (String n : Friends.names) fr.add(n);
            root.add("friends", fr);
            root.addProperty("accent", Theme.accent);
            JsonObject pn = new JsonObject();
            for (Map.Entry<String, float[]> e : panels.entrySet()) {
                JsonArray a = new JsonArray();
                a.add(e.getValue()[0]);
                a.add(e.getValue()[1]);
                pn.add(e.getKey(), a);
            }
            root.add("panels", pn);
            Files.writeString(file(), GSON.toJson(root));
        } catch (Exception e) {
            PhoenixClient.LOG.warn("Could not save config", e);
        }
    }

    public static void load() {
        try {
            Path f = file();
            if (!Files.exists(f)) return;
            JsonObject root = JsonParser.parseString(Files.readString(f)).getAsJsonObject();
            JsonObject mods = root.has("modules") ? root.getAsJsonObject("modules") : new JsonObject();
            for (Module m : PhoenixClient.modules.all) {
                if (!mods.has(m.name)) continue;
                JsonObject o = mods.getAsJsonObject(m.name);
                if (o.has("key")) m.key = o.get("key").getAsInt();
                if (o.has("hold")) m.holdMode = o.get("hold").getAsBoolean();
                if (o.has("enabled") && o.get("enabled").getAsBoolean()) pending.add(m.name);
                if (o.has("settings")) {
                    JsonObject s = o.getAsJsonObject("settings");
                    for (Setting<?> st : m.settings) if (s.has(st.name)) st.load(s.get(st.name));
                }
            }
            Friends.names.clear();
            if (root.has("friends")) for (JsonElement e : root.getAsJsonArray("friends")) Friends.names.add(e.getAsString());
            if (root.has("accent")) Theme.accent = root.get("accent").getAsInt();
            panels.clear();
            if (root.has("panels")) {
                for (Map.Entry<String, JsonElement> e : root.getAsJsonObject("panels").entrySet()) {
                    JsonArray a = e.getValue().getAsJsonArray();
                    panels.put(e.getKey(), new float[]{a.get(0).getAsFloat(), a.get(1).getAsFloat()});
                }
            }
        } catch (Exception e) {
            PhoenixClient.LOG.warn("Could not load config", e);
        }
    }

    public static void reset() {
        for (Module m : PhoenixClient.modules.all) {
            m.setEnabled(false);
            m.key = -1;
            m.holdMode = false;
            for (Setting<?> st : m.settings) resetOne(st);
        }
        Friends.names.clear();
        Theme.accent = Theme.PRESETS[0];
        panels.clear();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void resetOne(Setting st) { st.value = st.def; }
}
