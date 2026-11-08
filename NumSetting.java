package dev.phoenix.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public class NumSetting extends Setting<Double> {
    public final double min, max, step;

    public NumSetting(String name, double def, double min, double max, double step) {
        super(name, def);
        this.min = min;
        this.max = max;
        this.step = step;
    }

    public int getInt() { return (int) Math.round(value); }

    public float getF() { return value.floatValue(); }

    public void set(double v) {
        if (step > 0) v = Math.round(v / step) * step;
        value = Math.max(min, Math.min(max, v));
    }

    public double fraction() { return (value - min) / (max - min); }

    @Override public String display() {
        if (step >= 1) return String.valueOf(getInt());
        return String.format(java.util.Locale.ROOT, "%.2f", value);
    }

    @Override public JsonElement save() { return new JsonPrimitive(value); }

    @Override public void load(JsonElement e) {
        if (e != null && e.isJsonPrimitive()) set(e.getAsDouble());
    }
}
