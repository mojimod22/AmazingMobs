package me.zygotecode.amazingmobs.mob;

import java.util.Locale;

/**
 * Rarity / power tier of a custom mob. Drives default presentation (colour, whether a boss bar is
 * shown) and is purely cosmetic/organisational — stats come from the definition, not the tier.
 */
public enum Tier {
    COMMON   ("<gray>",        false),
    UNCOMMON ("<green>",       false),
    RARE     ("<aqua>",        false),
    ELITE    ("<light_purple>", true),
    MINIBOSS ("<gold>",        true),
    BOSS     ("<red>",         true);

    private final String color;     // MiniMessage colour tag
    private final boolean bossBar;  // default: show a boss bar?

    Tier(String color, boolean bossBar) {
        this.color = color;
        this.bossBar = bossBar;
    }

    public String color() { return color; }
    public boolean defaultBossBar() { return bossBar; }

    public static Tier fromString(String s, Tier def) {
        if (s == null) return def;
        try { return valueOf(s.trim().toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException e) { return def; }
    }
}
