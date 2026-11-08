package dev.phoenix.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public class StrSetting extends Setting<String> {
    public StrSetting(String name, String def) { super(name, def); }

    @Override public String display() { return value.isEmpty() ? "(empty)" : value; }

    @Override public JsonElement save() { return new JsonPrimitive(value); }

    @Override public void load(JsonElement e) {
        if (e != null && e.isJsonPrimitive()) value = e.getAsString();
    }
}
