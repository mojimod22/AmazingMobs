package me.zygotecode.amazingmobs.mob;

import java.util.Locale;

/** What happens to a rider/passenger when the thing carrying it dies (cascading-death chains). */
public enum RiderDeathBehavior {
    KEEP,     // do nothing — rider lives on, now grounded
    DROP,     // dismount and fall (default)
    KILL,     // dies with its carrier
    SCATTER,  // flung outward with momentum
    ENRAGE;   // dismounts and gains a rage buff

    public static RiderDeathBehavior fromString(String s, RiderDeathBehavior def) {
        if (s == null) return def;
        try { return valueOf(s.trim().toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException e) { return def; }
    }
}
