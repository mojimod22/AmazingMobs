package me.zygotecode.amazingmobs.clazz;

import me.zygotecode.amazingmobs.AmazingMobs;
import me.zygotecode.amazingmobs.util.Keys;
import me.zygotecode.amazingmobs.util.Text;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Owns every player-summoned ally (Necromancer conversions + skill summons). Tracks them per owner,
 * enforces a prestige-scaled cap, retargets them onto the nearest enemy each tick (and nudges vanilla
 * enemies to fight back), follows the owner when idle, and expires timed summons. Orphans are swept on
 * enable so a restart never leaks minions.
 */
public final class MinionManager {

    private final AmazingMobs plugin;
    private final Map<UUID, List<UUID>> byOwner = new HashMap<>();
    private final Map<UUID, Long> expiry = new HashMap<>();   // minion uuid -> expiry millis (0 = permanent)
    private BukkitTask task;

    public MinionManager(AmazingMobs plugin) { this.plugin = plugin; }

    public void start() {
        cleanupOrphans();
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 20L, 10L);
    }

    public void stop() {
        if (task != null) { task.cancel(); task = null; }
        for (UUID owner : new ArrayList<>(byOwner.keySet())) removeAll(owner);
    }

    // ---- caps ----------------------------------------------------------------------------------

    public int cap(int prestige) {
        var c = plugin.config();
        return Math.max(1, c.minionBaseCap + (int) Math.floor(c.minionPerPrestige * (prestige - 1)));
    }

    public int count(UUID owner) {
        List<UUID> list = byOwner.get(owner);
        if (list == null) return 0;
        list.removeIf(id -> { Entity e = plugin.getServer().getEntity(id); return e == null || e.isDead(); });
        return list.size();
    }

    // ---- spawning ------------------------------------------------------------------------------

    /** Convert a freshly-killed mob into a permanent ally (Necromancer passive). Returns true if raised. */
    public boolean convertKill(Player owner, LivingEntity victim, int prestige, double chance) {
        if (count(owner.getUniqueId()) >= cap(prestige)) return false;
        if (Math.random() >= chance) return false; // simple roll; callers gate by class
        double vhp = maxHealth(victim);
        boolean elite = vhp >= 30 || victim.getPersistentDataContainer().has(Keys.MOB_ID, PersistentDataType.STRING) && vhp >= 24;
        EntityType type = elite ? EntityType.WITHER_SKELETON : EntityType.SKELETON;
        Mob m = spawn(owner, type, victim.getLocation(), elite, 1.0 + 0.12 * (prestige - 1), 0L);
        return m != null;
    }

    /** Spawn a tracked ally. {@code lifespanMs} 0 = permanent (until death/owner gone). */
    public Mob spawn(Player owner, EntityType type, Location loc, boolean melee, double healthScale, long lifespanMs) {
        if (loc.getWorld() == null) return null;
        Entity ent = loc.getWorld().spawnEntity(loc, type);
        if (!(ent instanceof Mob mob)) { ent.remove(); return null; }

        double hp = 20 * Math.max(0.5, healthScale);
        AttributeInstance maxHp = mob.getAttribute(Attribute.MAX_HEALTH);
        if (maxHp != null) { maxHp.setBaseValue(hp); mob.setHealth(hp); }
        mob.setPersistent(true);
        mob.setRemoveWhenFarAway(false);
        mob.setGlowing(true); // allies read clearly on the field
        mob.getPersistentDataContainer().set(Keys.MINION_OWNER, PersistentDataType.STRING, owner.getUniqueId().toString());
        mob.customName(Text.mm("<dark_aqua>" + owner.getName() + "'s Minion"));
        if (mob.getEquipment() != null) {
            mob.getEquipment().setItemInMainHandDropChance(0f);
            mob.getEquipment().setItemInOffHandDropChance(0f);
        }

        byOwner.computeIfAbsent(owner.getUniqueId(), k -> new ArrayList<>()).add(mob.getUniqueId());
        if (lifespanMs > 0) expiry.put(mob.getUniqueId(), System.currentTimeMillis() + lifespanMs);
        return mob;
    }

    // ---- tick ----------------------------------------------------------------------------------

    private void tick() {
        long now = System.currentTimeMillis();
        for (Map.Entry<UUID, List<UUID>> e : byOwner.entrySet()) {
            Player owner = plugin.getServer().getPlayer(e.getKey());
            Iterator<UUID> it = e.getValue().iterator();
            while (it.hasNext()) {
                UUID id = it.next();
                Entity ent = plugin.getServer().getEntity(id);
                if (!(ent instanceof Mob mob) || mob.isDead() || !mob.isValid()) { it.remove(); expiry.remove(id); continue; }
                Long exp = expiry.get(id);
                if (exp != null && now >= exp) { it.remove(); expiry.remove(id); me.zygotecode.amazingmobs.util.Fx.particle(mob.getLocation(), "smoke", 12, 0.3, 0.3, 0.3, 0.02); mob.remove(); continue; }
                steer(mob, owner);
            }
        }
    }

    private void steer(Mob mob, Player owner) {
        LivingEntity tgt = mob.getTarget();
        boolean needTarget = tgt == null || tgt.isDead() || !SkillContext.isEnemy(tgt, owner)
                || tgt.getLocation().distanceSquared(mob.getLocation()) > 30 * 30;
        if (needTarget) {
            LivingEntity enemy = nearestEnemy(mob, owner, 28);
            if (enemy != null) {
                mob.setTarget(enemy);
                // make plain vanilla enemies fight back (custom mobs are driven by our own controller)
                if (enemy instanceof Mob em && !enemy.getPersistentDataContainer().has(Keys.MOB_ID, PersistentDataType.STRING)
                        && (em.getTarget() == null || em.getTarget() instanceof Player)) {
                    em.setTarget(mob);
                }
            } else if (owner != null && owner.getWorld().equals(mob.getWorld())
                    && owner.getLocation().distanceSquared(mob.getLocation()) > 16 * 16) {
                try { mob.getPathfinder().moveTo(owner, 1.25); } catch (Throwable ignored) {}
            }
        }
    }

    private LivingEntity nearestEnemy(Mob from, Player owner, double r) {
        LivingEntity best = null;
        double bd = Double.MAX_VALUE;
        for (Entity e : from.getNearbyEntities(r, r, r)) {
            if (!SkillContext.isEnemy(e, owner) || !(e instanceof LivingEntity le)) continue;
            double d = le.getLocation().distanceSquared(from.getLocation());
            if (d < bd) { bd = d; best = le; }
        }
        return best;
    }

    // ---- cleanup -------------------------------------------------------------------------------

    public void removeAll(UUID owner) {
        List<UUID> list = byOwner.remove(owner);
        if (list == null) return;
        for (UUID id : list) {
            Entity e = plugin.getServer().getEntity(id);
            if (e != null) e.remove();
            expiry.remove(id);
        }
    }

    /** Remove any minion entities left over from a previous run (called on enable). */
    public void cleanupOrphans() {
        int removed = 0;
        for (var w : plugin.getServer().getWorlds()) {
            for (LivingEntity le : w.getLivingEntities()) {
                if (le.getPersistentDataContainer().has(Keys.MINION_OWNER, PersistentDataType.STRING)) { le.remove(); removed++; }
            }
        }
        if (removed > 0) plugin.getLogger().info("Cleared " + removed + " orphaned minion(s) from a previous run.");
    }

    private static double maxHealth(LivingEntity le) {
        AttributeInstance a = le.getAttribute(Attribute.MAX_HEALTH);
        return a != null ? a.getValue() : 20;
    }
}
