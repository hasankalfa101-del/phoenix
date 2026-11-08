package dev.phoenix.module;

import dev.phoenix.util.U;

/** A module that appears in the GUI but has no behaviour yet. */
public class Stub extends Module {
    public Stub(String name, Category category) {
        super(name, category, "Not implemented yet.");
    }

    @Override public boolean implemented() { return false; }

    @Override protected void onEnable() {
        U.chat(name + " is not implemented yet.");
        enabled = false;
    }
}
