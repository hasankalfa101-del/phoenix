package dev.phoenix.setting;

import com.google.gson.JsonElement;

/** Base class for every adjustable option on a module. */
public abstract class Setting<T> {
    public final String name;
    public T value;
    public final T def;

    protected Setting(String name, T def) {
        this.name = name;
        this.value = def;
        this.def = def;
    }

    public T get() { return value; }

    /** Text shown on the right side of the row in the GUI. */
    public abstract String display();

    public abstract JsonElement save();

    public abstract void load(JsonElement e);
}
