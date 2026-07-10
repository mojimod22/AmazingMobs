package me.zygotecode.amazingmobs.mob.runtime;

import me.zygotecode.amazingmobs.api.event.CustomMobDeathEvent;
import me.zygotecode.amazingmobs.api.event.CustomMobSpawnEvent;
import me.zygotecode.amazingmobs.mob.MobDefinition;
import me.zygotecode.amazingmobs.mob.MobRegistry;
import me.zygotecode.amazingmobs.mob.MountSpec;
import me.zygotecode.amazingmobs.mob.RiderDeathBehavior;
import me.zygotecode.amazingmobs.mob.RiderSpec;
import me.zygotecode.amazingmobs.mob.Variant;
import me.zygotecode.amazingmobs.mob.DropTable;
import me.zygotecode.amazingmobs.skill.SkillRegistry;
import me.zygotecode.amazingmobs.skill.SummonFunction;
import me.zygotecode.amazingmobs.trait.TraitRegistry;
import me.zygotecode.amazingmobs.util.Keys;
import me.zygotecode.amazingmobs.util.Resolvers;
import me.zygotecode.amazingmobs.util.Rng;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Owns every live custom mob: spawning, the single throttled controller task, death/drops,
 * rebinding after chunk/restart, and cleanup. The one mutation path keeps spawning double-spawn
 * safe and the active set a {@link ConcurrentHashMap} so a summon mid-tick never corrupts iteration.
 */
public final class MobManager {

    /** A rider→carrier link for cascading death chains. */
    private record RiderLink(UUID carrier, RiderDeathBehavior onCarrierDeath, boolean killCarrierOnRiderDeath) {}

    private final Plugin plugin;
    private final MobRegistry registry;
    private final SkillRegistry skillRegistry;
    private final TraitRegistry traitRegistry;
    private final Rng rng = Rng.shared();

    private final Map<UUID, ActiveMob> active = new ConcurrentHashMap<>();
    private final Map<UUID, RiderLink> riderOf = new ConcurrentHashMap<>();         // rider -> its carrier link
    private final Map<UUID, List<UUID>> ridersOf = new ConcurrentHashMap<>();       // carrier -> riders on it
    private final int controllerPeriod;   // ticks between controller passes
    private final int maxActiveMobs;

    private BukkitTask task;
    private long tickCounter;

    public MobManager(Plugin plugin, MobRegistry registry, SkillRegistry skillRegistry,
                      TraitRegistry traitRegistry, int controllerPeriod, int maxActiveMobs) {
        this.plugin = plugin;
        this.registry = registry;
        this.skillRegistry = skillRegistry;
        this.traitRegistry = traitRegistry;
        this.controllerPeriod = Math.max(1, controllerPeriod);
        this.maxActiveMobs = Math.max(16, maxActiveMobs);
    }

    // ---- lifecycle -----------------------------------------------------------------------------

    public void start() {
        if (task != null) return;
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tickAll, controllerPeriod, controllerPeriod);
    }

    public void stop() {
        if (task != null) { task.cancel(); task = null; }
        for (ActiveMob am : active.values()) am.cleanup();
        active.clear();
        riderOf.clear();
        ridersOf.clear();
    }

    private void tickAll() {
        tickCounter += controllerPeriod;
        // snapshot so summons created during the pass don't disturb iteration
        for (ActiveMob am : new ArrayList<>(active.values())) {
            if (!am.isValid()) {
                am.cleanup();
                active.remove(am.entity().getUniqueId());
                purgeLinks(am.entity().getUniqueId());
                continue;
            }
            try {
                am.tick(tickCounter);
            } catch (Throwable t) {
                plugin.getLogger().warning("[mob] controller tick failed for '" + am.definition().id() + "': " + t);
            }
        }
    }

    // ---- spawning ------------------------------------------------------------------------------

    /** Spawn a custom mob by id. Returns null if unknown, capacity-capped, or unspawnable. */
    public LivingEntity spawn(String id, Location loc, SpawnMeta meta) {
        MobDefinition def = registry.get(id);
        if (def == null) return null;
        MobDefinition eff = def;
        if (def.hasVariants()) {                                  // roll a mutation/variant for this spawn
            Variant v = Variant.pick(def.variants(), def.variantBaseWeight(), loc, meta.playerCount(), rng);
            if (v != null) eff = v.apply(def);
        }
        return spawnExact(eff, loc, meta);
    }

    /** Spawn an exact definition (no variant roll). Used to force-spawn a chosen variant for testing. */
    public LivingEntity spawnExact(MobDefinition def, Location loc, SpawnMeta meta) {
        if (active.size() >= maxActiveMobs) return null;
        MobSpawner spawner = new MobSpawner();
        LivingEntity entity = spawner.spawn(def, loc, meta);
        if (entity == null) return null;
        trackNew(entity, def, meta, true);
        assembleRiders(entity, def, meta);                        // build mount + stack chain
        return entity;
    }

    /** Spawn a custom mob by id, or a plain vanilla living entity if the id is a vanilla type. */
    public LivingEntity spawnAny(String idOrType, Location loc, SpawnMeta meta) {
        return spawnByIdOrType(idOrType, loc, meta);
    }

    private void trackNew(LivingEntity entity, MobDefinition def, SpawnMeta meta, boolean fireSpawn) {
        SummonFunction summon = (mobId, at) -> spawn(mobId, at,
                new SpawnMeta(meta.hordeInstanceId(), meta.waveIndex(), "minion", meta.playerCount(), meta.difficulty()));
        ActiveMob am = new ActiveMob(plugin, entity, def, meta, skillRegistry, traitRegistry, summon, controllerPeriod);
        active.put(entity.getUniqueId(), am);
        if (fireSpawn) {
            Bukkit.getPluginManager().callEvent(new CustomMobSpawnEvent(entity, def));
            am.onSpawn(tickCounter);
        }
    }

    /** Re-attach a controller to an already-configured tagged entity (after chunk load / restart). */
    public void rebind(LivingEntity entity) {
        UUID uuid = entity.getUniqueId();
        if (active.containsKey(uuid)) return;
        String id = entity.getPersistentDataContainer().get(Keys.MOB_ID, PersistentDataType.STRING);
        if (id == null) return;
        MobDefinition def = registry.get(id);
        if (def == null) return;
        String horde = entity.getPersistentDataContainer().get(Keys.HORDE_INSTANCE, PersistentDataType.STRING);
        Integer wave = entity.getPersistentDataContainer().get(Keys.WAVE_INDEX, PersistentDataType.INTEGER);
        String role = entity.getPersistentDataContainer().get(Keys.ROLE, PersistentDataType.STRING);
        SpawnMeta meta = new SpawnMeta(horde, wave == null ? -1 : wave, role, 1, 1.0);
        trackNew(entity, def, meta, false);
    }

    /** Scan all loaded worlds for tagged custom mobs and rebind any not yet tracked (on enable). */
    public int rebindLoaded() {
        int n = 0;
        for (World w : Bukkit.getWorlds()) {
            for (LivingEntity e : w.getLivingEntities()) {
                if (e.getPersistentDataContainer().has(Keys.MOB_ID, PersistentDataType.STRING)
                        && !active.containsKey(e.getUniqueId())) {
                    rebind(e);
                    n++;
                }
            }
        }
        return n;
    }

    // ---- combat / death hooks ------------------------------------------------------------------

    public void handleDeath(LivingEntity entity, EntityDeathEvent event, Player killer) {
        ActiveMob am = active.remove(entity.getUniqueId());
        if (am == null) return;
        DropTable drops = am.definition().drops();
        if (!drops.isEmpty()) {
            if (drops.clearVanillaDrops()) event.getDrops().clear();
            for (ItemStack is : drops.roll(rng)) event.getDrops().add(is);
            int xp = drops.rollXp(rng);
            if (xp > 0) event.setDroppedExp(xp);
        }
        Bukkit.getPluginManager().callEvent(new CustomMobDeathEvent(entity, am.definition(), killer));
        am.onDeath(tickCounter);
        handleChainDeath(entity);
    }

    /** Apply cascading mount/stack death chains. Safe to call for any dying entity (custom or vanilla). */
    public void handleChainDeath(LivingEntity entity) {
        UUID id = entity.getUniqueId();
        List<UUID> riders = ridersOf.remove(id);
        if (riders != null) {
            for (UUID rid : riders) applyCarrierDeath(rid, riderOf.remove(rid));
        }
        RiderLink myLink = riderOf.remove(id);
        if (myLink != null) {
            List<UUID> siblings = ridersOf.get(myLink.carrier());
            if (siblings != null) siblings.remove(id);
            if (myLink.killCarrierOnRiderDeath()) {
                Entity carrier = Bukkit.getEntity(myLink.carrier());
                if (carrier instanceof LivingEntity cle && !cle.isDead() && cle.getHealth() > 0) cle.setHealth(0);
            }
        }
    }

    private void applyCarrierDeath(UUID riderUuid, RiderLink link) {
        if (link == null) return;
        Entity e = Bukkit.getEntity(riderUuid);
        if (!(e instanceof LivingEntity rider) || rider.isDead()) return;
        switch (link.onCarrierDeath()) {
            case KILL -> { if (rider.getHealth() > 0) rider.setHealth(0); }      // cascades via its own death
            case SCATTER -> rider.setVelocity(new org.bukkit.util.Vector(
                    (rng.nextDouble() - 0.5) * 1.2, 0.6, (rng.nextDouble() - 0.5) * 1.2));
            case ENRAGE -> { applyEffect(rider, "strength", 1, 20 * 20); applyEffect(rider, "speed", 1, 20 * 20); }
            default -> { } // KEEP / DROP — Bukkit auto-ejects passengers when the carrier dies
        }
    }

    public void handleDamaged(LivingEntity entity, LivingEntity attacker) {
        ActiveMob am = active.get(entity.getUniqueId());
        if (am != null) am.onDamaged(attacker, tickCounter);
    }

    public void handleAttack(LivingEntity attacker, LivingEntity victim) {
        ActiveMob am = active.get(attacker.getUniqueId());
        if (am != null) am.onAttack(victim, tickCounter);
    }

    // ---- rider / mount assembly ----------------------------------------------------------------

    private void assembleRiders(LivingEntity base, MobDefinition def, SpawnMeta meta) {
        MountSpec ms = def.mount();
        if (ms != null && !ms.mountId().equalsIgnoreCase(def.id())) {
            LivingEntity mount = spawnByIdOrType(ms.mountId(), base.getLocation(), meta);
            if (mount != null) {
                mount.addPassenger(base);
                applyEffectStrings(base, ms.riderBonusEffects());
                riderOf.put(base.getUniqueId(), new RiderLink(mount.getUniqueId(), ms.onMountDeath(), ms.killMountWhenRiderDies()));
                ridersOf.computeIfAbsent(mount.getUniqueId(), k -> new ArrayList<>()).add(base.getUniqueId());
            }
        }
        if (def.hasRiders()) {
            LivingEntity top = base;
            int depth = 0;
            for (RiderSpec rs : def.riders()) {
                if (depth++ >= 8 || rs.mobId().equalsIgnoreCase(def.id())) continue; // depth + self-ride guard
                LivingEntity rider = spawnByIdOrType(rs.mobId(), top.getLocation(), meta);
                if (rider == null) continue;
                top.addPassenger(rider);
                applyEffectStrings(rider, rs.bonusEffects());
                riderOf.put(rider.getUniqueId(), new RiderLink(top.getUniqueId(), rs.onBaseDeath(), false));
                ridersOf.computeIfAbsent(top.getUniqueId(), k -> new ArrayList<>()).add(rider.getUniqueId());
                top = rider;
            }
        }
    }

    /** Spawn a custom mob by id, or a plain vanilla living entity if the id is a vanilla type. */
    private LivingEntity spawnByIdOrType(String idOrType, Location loc, SpawnMeta meta) {
        if (registry.contains(idOrType)) return spawn(idOrType, loc, meta);
        EntityType type = Resolvers.entityType(idOrType, null);
        if (type == null || loc.getWorld() == null) return null;
        Class<? extends Entity> cls = type.getEntityClass();
        if (cls == null || !LivingEntity.class.isAssignableFrom(cls)) return null;
        Entity e = loc.getWorld().spawnEntity(loc, type);
        return e instanceof LivingEntity le ? le : null;
    }

    private void applyEffectStrings(LivingEntity target, List<String> specs) {
        for (String s : specs) {
            String[] p = s.split(":");
            PotionEffectType pet = Resolvers.effect(p[0]);
            int amp = 0;
            try { if (p.length > 1) amp = Integer.parseInt(p[1].trim()); } catch (NumberFormatException ignored) {}
            if (pet != null) target.addPotionEffect(new PotionEffect(pet, 20 * 60 * 30, Math.max(0, amp), false, false, false));
        }
    }

    private void applyEffect(LivingEntity t, String type, int amp, int dur) {
        PotionEffectType pet = Resolvers.effect(type);
        if (pet != null) t.addPotionEffect(new PotionEffect(pet, dur, Math.max(0, amp), false, true, true));
    }

    private void purgeLinks(UUID id) {
        ridersOf.remove(id);
        riderOf.remove(id);
    }

    // ---- queries -------------------------------------------------------------------------------

    public boolean isCustomMob(Entity e) {
        return e != null && active.containsKey(e.getUniqueId());
    }

    public ActiveMob get(Entity e) {
        return e == null ? null : active.get(e.getUniqueId());
    }

    public int activeCount() { return active.size(); }
    public int maxActiveMobs() { return maxActiveMobs; }
    public long currentTick() { return tickCounter; }

    // ---- runtime mutation (live testing) -------------------------------------------------------

    /** Inject a trait onto a live custom mob. @return false if not a tracked custom mob / unknown trait. */
    public boolean giveTrait(org.bukkit.entity.Entity e, String traitId, me.zygotecode.amazingmobs.config.ConfigSection params) {
        ActiveMob am = get(e);
        if (am == null) return false;
        var t = traitRegistry.get(traitId);
        if (t == null) return false;
        am.addRuntimeTrait(new me.zygotecode.amazingmobs.trait.TraitDefinition(traitId.toLowerCase(Locale.ROOT), params), t);
        return true;
    }

    /** Inject a parsed skill definition onto a live custom mob. */
    public boolean giveSkill(org.bukkit.entity.Entity e, me.zygotecode.amazingmobs.skill.SkillDefinition def) {
        ActiveMob am = get(e);
        if (am == null || def == null) return false;
        var sk = skillRegistry.get(def.skillId());
        if (sk == null) return false;
        am.addRuntimeSkill(def, sk);
        return true;
    }

    /** Remove every tracked custom mob from the world (used by {@code /am} cleanup & disable). */
    public int removeAll() {
        int n = 0;
        for (ActiveMob am : new ArrayList<>(active.values())) {
            am.cleanup();
            if (am.isValid()) { am.entity().remove(); n++; }
        }
        active.clear();
        riderOf.clear();
        ridersOf.clear();
        return n;
    }
}
