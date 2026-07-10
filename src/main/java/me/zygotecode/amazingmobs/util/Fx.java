package me.zygotecode.amazingmobs.util;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;

/**
 * Particle / sound feedback helpers. Names are resolved by {@link Resolvers}; a null/blank/unknown
 * name is a silent no-op so misconfigured feedback never throws inside a skill.
 */
public final class Fx {

    private Fx() {}

    public static void sound(Location loc, String name, float volume, float pitch) {
        if (loc == null || name == null || name.isBlank()) return;
        World w = loc.getWorld();
        if (w == null) return;
        Sound s = Resolvers.sound(name);
        if (s != null) w.playSound(loc, s, volume, pitch);
    }

    public static void particle(Location loc, String name, int count,
                                double dx, double dy, double dz, double speed) {
        if (loc == null || name == null || name.isBlank()) return;
        World w = loc.getWorld();
        if (w == null) return;
        Particle p = Resolvers.particle(name, null);
        if (p != null) w.spawnParticle(p, loc, count, dx, dy, dz, speed);
    }

    public static void burst(Location loc, String name, int count) {
        particle(loc, name, count, 0.5, 0.5, 0.5, 0.05);
    }
}
