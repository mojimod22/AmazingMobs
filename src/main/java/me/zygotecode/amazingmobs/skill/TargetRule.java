package me.zygotecode.amazingmobs.skill;

import java.util.Locale;

/** How a skill picks who/what it affects. */
public enum TargetRule {
    SELF,                    // the caster
    TARGET,                  // the mob's current combat target
    NEAREST_PLAYER,          // closest player in range
    RANDOM_PLAYER,           // random player in range
    LOWEST_HEALTH_PLAYER,    // weakest player in range
    ALL_PLAYERS_IN_RADIUS,   // every player within radius
    ALL_IN_RADIUS;           // every living entity within radius (incl. allies for buffs)

    public static TargetRule fromString(String s, TargetRule def) {
        if (s == null) return def;
        try { return valueOf(s.trim().toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException e) { return def; }
    }
}
