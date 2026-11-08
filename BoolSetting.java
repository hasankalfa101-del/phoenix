package dev.phoenix.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public class BoolSetting extends Setting<Boolean> {
    public BoolSetting(String name, boolean def) { super(name, def); }

    public void toggle() { value = !value; }

    @Override public String display() { return value ? "On" : "Off"; }

    @Override public JsonElement save() { return new JsonPrimitive(value); }

    @Override public void load(JsonElement e) {
        if (e != null && e.isJsonPrimitive()) value = e.getAsBoolean();
    }
}
