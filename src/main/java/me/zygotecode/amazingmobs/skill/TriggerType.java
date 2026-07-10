package me.zygotecode.amazingmobs.skill;

import java.util.Locale;

/** What event makes a skill eligible to fire. */
public enum TriggerType {
    TICK,         // periodically, when off cooldown and conditions hold (the default)
    ON_DAMAGED,   // when the mob takes damage
    ON_ATTACK,    // when the mob lands a melee hit
    ON_SPAWN,     // once, right after spawn
    ON_DEATH,     // once, on death
    ON_LOW_HEALTH;// once, when health first drops below the trigger's maxHealthPct

    public static TriggerType fromString(String s, TriggerType def) {
        if (s == null) return def;
        try { return valueOf(s.trim().toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException e) { return def; }
    }
}
