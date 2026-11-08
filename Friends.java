package dev.phoenix.util;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

public final class Friends {
    public static final Set<String> names = new LinkedHashSet<>();

    private Friends() {}

    public static boolean is(Entity e) {
        return e instanceof PlayerEntity p && names.contains(p.getName().getString().toLowerCase(Locale.ROOT));
    }

    public static void add(String name) {
        String n = name.trim().toLowerCase(Locale.ROOT);
        if (!n.isEmpty()) names.add(n);
    }

    public static void remove(String name) { names.remove(name.toLowerCase(Locale.ROOT)); }
}
