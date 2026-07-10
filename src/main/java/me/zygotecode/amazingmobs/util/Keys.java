package me.zygotecode.amazingmobs.util;

import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;

/**
 * Central registry of {@link NamespacedKey}s used for Persistent Data Container (PDC) tags.
 *
 * <p>Tagging entities lets the plugin re-bind custom mobs after chunk reloads / server restarts
 * and find orphans to clean up. Initialised once from the plugin in {@code onEnable}.</p>
 */
public final class Keys {

    private Keys() {}

    /** Custom mob definition id stored on the entity. */
    public static NamespacedKey MOB_ID;
    /** Marks the entity as belonging to a running horde instance. */
    public static NamespacedKey HORDE_INSTANCE;
    /** Wave index (Integer) the mob was spawned in. */
    public static NamespacedKey WAVE_INDEX;
    /** Role marker on horde mobs (e.g. "boss", "miniboss", "minion"). */
    public static NamespacedKey ROLE;
    /** Item-level tag identifying a configured custom item (for give/export round-trips). */
    public static NamespacedKey ITEM_TAG;
    /** Attribute-modifier keys for the Weight progression system. */
    public static NamespacedKey WEIGHT_HEALTH, WEIGHT_DAMAGE, WEIGHT_SPEED;
    /** AK-47 custom weapon: item marker, ammo (int), reload-ready timestamp (long); projectile marker. */
    public static NamespacedKey AK_WEAPON, AK_AMMO, AK_RELOAD_AT, AK_SKULL;
    /** Necromancer minion: owner player UUID (string) stored on the summoned ally entity. */
    public static NamespacedKey MINION_OWNER;
    /** Skill item: the skill id (string) it casts on right-click (given in the horde kit). */
    public static NamespacedKey SKILL_ITEM;

    public static void init(Plugin plugin) {
        MOB_ID = new NamespacedKey(plugin, "mob_id");
        HORDE_INSTANCE = new NamespacedKey(plugin, "horde_instance");
        WAVE_INDEX = new NamespacedKey(plugin, "wave_index");
        ROLE = new NamespacedKey(plugin, "role");
        ITEM_TAG = new NamespacedKey(plugin, "item_tag");
        WEIGHT_HEALTH = new NamespacedKey(plugin, "weight_health");
        WEIGHT_DAMAGE = new NamespacedKey(plugin, "weight_damage");
        WEIGHT_SPEED = new NamespacedKey(plugin, "weight_speed");
        AK_WEAPON = new NamespacedKey(plugin, "ak_weapon");
        AK_AMMO = new NamespacedKey(plugin, "ak_ammo");
        AK_RELOAD_AT = new NamespacedKey(plugin, "ak_reload_at");
        AK_SKULL = new NamespacedKey(plugin, "ak_skull");
        MINION_OWNER = new NamespacedKey(plugin, "minion_owner");
        SKILL_ITEM = new NamespacedKey(plugin, "skill_item");
    }
}
