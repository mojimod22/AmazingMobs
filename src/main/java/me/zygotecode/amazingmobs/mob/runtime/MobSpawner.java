package me.zygotecode.amazingmobs.mob.runtime;

import me.zygotecode.amazingmobs.mob.Equipment;
import me.zygotecode.amazingmobs.mob.MobDefinition;
import me.zygotecode.amazingmobs.mob.Presentation;
import me.zygotecode.amazingmobs.mob.StatBlock;
import me.zygotecode.amazingmobs.util.Keys;
import me.zygotecode.amazingmobs.util.Rng;
import me.zygotecode.amazingmobs.util.Text;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.persistence.PersistentDataType;

/**
 * Turns a {@link MobDefinition} into a live, fully-configured {@link LivingEntity}: applies scaled
 * stats onto attributes, equips gear, sets presentation, and tags the entity via PDC so it can be
 * re-bound after reloads and cleaned up later. Spawning is the only mutation entry point, keeping
 * the spawn path single-threaded and double-spawn-safe.
 */
public final class MobSpawner {

    private final Rng rng = Rng.shared();

    /** @return the spawned entity, or null if the world/type was unusable. */
    @SuppressWarnings("unchecked")
    public LivingEntity spawn(MobDefinition def, Location loc, SpawnMeta meta) {
        World world = loc.getWorld();
        if (world == null) return null;
        Class<? extends Entity> raw = def.baseType().getEntityClass();
        if (raw == null || !LivingEntity.class.isAssignableFrom(raw)) return null;
        Class<? extends LivingEntity> cls = (Class<? extends LivingEntity>) raw;

        StatBlock stats = def.scaling().apply(def.stats(), meta.playerCount(), meta.difficulty());
        try {
            // randomizeData = false: no vanilla random gear/age — custom mobs are fully deterministic.
            // The configure consumer runs BEFORE the entity is added, so there is no 1-tick flash of
            // default stats/equipment.
            return world.spawn(loc, cls, CreatureSpawnEvent.SpawnReason.CUSTOM, false,
                    e -> configure(e, def, stats, meta));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private void configure(LivingEntity le, MobDefinition def, StatBlock stats, SpawnMeta meta) {
        // --- identity tags (PDC) ---
        var pdc = le.getPersistentDataContainer();
        pdc.set(Keys.MOB_ID, PersistentDataType.STRING, def.id());
        if (meta.hordeInstanceId() != null) pdc.set(Keys.HORDE_INSTANCE, PersistentDataType.STRING, meta.hordeInstanceId());
        if (meta.waveIndex() >= 0) pdc.set(Keys.WAVE_INDEX, PersistentDataType.INTEGER, meta.waveIndex());
        if (meta.role() != null) pdc.set(Keys.ROLE, PersistentDataType.STRING, meta.role());

        // --- name ---
        le.customName(Text.mm(def.displayName()));
        le.setCustomNameVisible(def.presentation().nameVisible());

        // --- attributes ---
        setAttr(le, Attribute.MAX_HEALTH, stats.health());
        le.setHealth(Math.min(stats.health(), attrValue(le, Attribute.MAX_HEALTH, stats.health())));
        setAttr(le, Attribute.ATTACK_DAMAGE, stats.attackDamage());
        if (stats.overridesMovementSpeed()) setAttr(le, Attribute.MOVEMENT_SPEED, stats.movementSpeed());
        setAttr(le, Attribute.KNOCKBACK_RESISTANCE, stats.knockbackImmune() ? 1.0 : stats.knockbackResistance());
        setAttr(le, Attribute.ARMOR, stats.armor());
        setAttr(le, Attribute.ARMOR_TOUGHNESS, stats.armorToughness());
        if (stats.overridesFollowRange()) setAttr(le, Attribute.FOLLOW_RANGE, stats.followRange());
        setAttr(le, Attribute.ATTACK_KNOCKBACK, stats.attackKnockback());
        setAttr(le, Attribute.SCALE, stats.scale());
        if (stats.maxAbsorption() > 0) {
            setAttr(le, Attribute.MAX_ABSORPTION, stats.maxAbsorption());
            le.setAbsorptionAmount(stats.maxAbsorption());
        }

        // --- gear ---
        if (def.equipment() != Equipment.EMPTY) def.equipment().apply(le, rng);

        // --- flags & presentation ---
        le.setRemoveWhenFarAway(false);
        le.setPersistent(true);
        le.setCanPickupItems(false);
        Presentation pres = def.presentation();
        if (pres.glow()) le.setGlowing(true);
        if (le instanceof Mob mob) mob.setAware(true);
    }

    private static void setAttr(LivingEntity le, Attribute attr, double value) {
        AttributeInstance inst = le.getAttribute(attr);
        if (inst != null) inst.setBaseValue(value);
    }

    private static double attrValue(LivingEntity le, Attribute attr, double fallback) {
        AttributeInstance inst = le.getAttribute(attr);
        return inst != null ? inst.getValue() : fallback;
    }
}
