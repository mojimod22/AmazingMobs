package me.zygotecode.amazingmobs.util;

import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.potion.PotionEffectType;

import java.util.Locale;

/**
 * Runtime name → Bukkit-object resolvers. Resolving by name (via {@link Registry}) instead of
 * hard-coding constants keeps the plugin resilient to the constant renames Mojang/Bukkit make
 * between versions, and lets every particle/sound/effect be configurable as a plain string.
 *
 * <p>All methods accept {@code "minecraft:flame"} or bare {@code "flame"} (case-insensitive) and
 * return {@code null} (or the supplied default) when the name does not resolve — callers decide
 * whether that is an error or a silent skip.</p>
 */
public final class Resolvers {

    private Resolvers() {}

    /** Parse {@code "namespace:key"} or bare {@code "key"} (defaults to {@code minecraft:}). */
    public static NamespacedKey key(String raw) {
        if (raw == null) return null;
        String s = raw.trim().toLowerCase(Locale.ROOT);
        if (s.isEmpty()) return null;
        return NamespacedKey.fromString(s);
    }

    public static Material material(String raw, Material def) {
        if (raw == null) return def;
        Material m = Material.matchMaterial(raw.trim());
        return m != null ? m : def;
    }

    public static EntityType entityType(String raw, EntityType def) {
        NamespacedKey k = key(raw);
        if (k != null) {
            EntityType t = Registry.ENTITY_TYPE.get(k);
            if (t != null) return t;
        }
        return def;
    }

    public static PotionEffectType effect(String raw) {
        NamespacedKey k = key(raw);
        return k == null ? null : Registry.MOB_EFFECT.get(k);
    }

    public static Particle particle(String raw, Particle def) {
        NamespacedKey k = key(raw);
        if (k != null) {
            Particle p = Registry.PARTICLE_TYPE.get(k);
            if (p != null) return p;
        }
        // Fall back to the enum name (e.g. config wrote "FLAME").
        if (raw != null) {
            try { return Particle.valueOf(raw.trim().toUpperCase(Locale.ROOT)); }
            catch (IllegalArgumentException ignored) {}
        }
        return def;
    }

    public static Sound sound(String raw) {
        NamespacedKey k = key(raw);
        return k == null ? null : Registry.SOUND_EVENT.get(k);
    }

    public static Enchantment enchant(String raw) {
        NamespacedKey k = key(raw);
        return k == null ? null : Registry.ENCHANTMENT.get(k);
    }
}
