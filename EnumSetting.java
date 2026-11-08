package dev.phoenix.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public class EnumSetting extends Setting<String> {
    public final String[] options;

    public EnumSetting(String name, String def, String... options) {
        super(name, def);
        this.options = options;
    }

    public int index() {
        for (int i = 0; i < options.length; i++) if (options[i].equals(value)) return i;
        return 0;
    }

    public void cycle(int dir) {
        int i = (index() + dir + options.length) % options.length;
        value = options[i];
    }

    public boolean is(String s) { return value.equals(s); }

    @Override public String display() { return value; }

    @Override public JsonElement save() { return new JsonPrimitive(value); }

    @Override public void load(JsonElement e) {
        if (e == null || !e.isJsonPrimitive()) return;
        String s = e.getAsString();
        for (String o : options) if (o.equals(s)) value = s;
    }
}
