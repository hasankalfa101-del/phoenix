package dev.phoenix.module;

public enum Category {
    COMBAT("Combat"),
    MISC("Misc"),
    DONUT("Donut"),
    RENDER("Render"),
    CLIENT("Phoenix Client"),
    MOVEMENT("Movement"),
    BASE_RAID("Base Raid"),
    VISUALS("Visuals");

    public final String title;

    Category(String title) { this.title = title; }
}
