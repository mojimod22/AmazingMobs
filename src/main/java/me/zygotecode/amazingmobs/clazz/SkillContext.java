package me.zygotecode.amazingmobs.clazz;

import me.zygotecode.amazingmobs.AmazingMobs;
import me.zygotecode.amazingmobs.util.Fx;
import me.zygotecode.amazingmobs.util.Keys;
import me.zygotecode.amazingmobs.util.Rng;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

/**
 * Everything a {@link PlayerSkill} needs to act, plus convenience helpers (aiming, enemy scans, damage,
 * buffs, summons, FX) so skill bodies stay tiny. One is created per cast.
 */
public final class SkillContext {

    public final AmazingMobs plugin;
    public final Player player;
    public final int prestige;          // 1+ ; higher = stronger
    public final MinionManager minions;
    public final Rng rng = Rng.shared();

    public SkillContext(AmazingMobs plugin, Player player, int prestige, MinionManager minions) {
        this.plugin = plugin;
        this.player = player;
        this.prestige = prestige;
        this.minions = minions;
    }

    // ---- prestige scaling helpers --------------------------------------------------------------

    /** Linear scale: base × (1 + perPrestige × (prestige-1)). */
    public double scale(double base, double perPrestige) { return base * (1 + perPrestige * (prestige - 1)); }
    public int bonus(int base, double perPrestige) { return base + (int) Math.floor(perPrestige * (prestige - 1)); }

    // ---- aiming --------------------------------------------------------------------------------

    public Location eye() { return player.getEyeLocation(); }
    public Vector dir() { return player.getEyeLocation().getDirection().normalize(); }

    /** Where the player is looking, stopping at the first solid block (or {@code max} blocks out). */
    public Location aimBlock(double max) {
        World w = player.getWorld();
        RayTraceResult rt = w.rayTraceBlocks(eye(), dir(), max, FluidCollisionMode.NEVER, true);
        if (rt != null && rt.getHitPosition() != null) {
            Vector hp = rt.getHitPosition();
            return new Location(w, hp.getX(), hp.getY(), hp.getZ());
        }
        return eye().add(dir().multiply(max));
    }

    /** First enemy under the crosshair within {@code max}, or null. */
    public LivingEntity aimTarget(double max) {
        World w = player.getWorld();
        RayTraceResult rt = w.rayTrace(eye(), dir(), max, FluidCollisionMode.NEVER, true, 0.7,
                e -> isEnemy(e, player));
        return rt != null && rt.getHitEntity() instanceof LivingEntity le ? le : null;
    }

    // ---- enemies -------------------------------------------------------------------------------

    public List<LivingEntity> enemiesNear(Location c, double r) {
        List<LivingEntity> out = new ArrayList<>();
        for (Entity e : c.getWorld().getNearbyEntities(c, r, r, r)) {
            if (isEnemy(e, player) && e instanceof LivingEntity le) out.add(le);
        }
        return out;
    }

    public LivingEntity nearestEnemy(Location c, double r) {
        LivingEntity best = null;
        double bd = Double.MAX_VALUE;
        for (LivingEntity le : enemiesNear(c, r)) {
            double d = le.getLocation().distanceSquared(c);
            if (d < bd) { bd = d; best = le; }
        }
        return best;
    }

    /** An enemy = any non-player, non-minion {@link Mob} (covers vanilla monsters + our custom mobs). */
    public static boolean isEnemy(Entity e, Player owner) {
        if (!(e instanceof Mob)) return false;
        if (e instanceof Player) return false;
        if (e.getPersistentDataContainer().has(Keys.MINION_OWNER, PersistentDataType.STRING)) return false;
        return true;
    }

    // ---- combat --------------------------------------------------------------------------------

    public void hurt(LivingEntity le, double dmg) {
        if (le != null && !le.isDead()) le.damage(dmg, player);
    }

    public void knock(LivingEntity le, Location from, double power) {
        if (le == null) return;
        Vector push = le.getLocation().toVector().subtract(from.toVector());
        if (push.lengthSquared() < 1.0E-4) push = new Vector(0, 1, 0);
        le.setVelocity(le.getVelocity().add(push.normalize().multiply(power).setY(Math.min(0.4, power * 0.4))));
    }

    public void ignite(LivingEntity le, int ticks) { if (le != null) le.setFireTicks(Math.max(le.getFireTicks(), ticks)); }

    // ---- buffs ---------------------------------------------------------------------------------

    public void buffSelf(PotionEffectType type, int seconds, int amplifier) {
        player.addPotionEffect(new PotionEffect(type, seconds * 20, amplifier, true, true, true));
    }

    /** Buff the caster + nearby players (allies) within {@code r}. */
    public void buffAllies(double r, PotionEffectType type, int seconds, int amplifier) {
        buffSelf(type, seconds, amplifier);
        for (Entity e : player.getNearbyEntities(r, r, r)) {
            if (e instanceof Player ally) ally.addPotionEffect(new PotionEffect(type, seconds * 20, amplifier, true, true, true));
        }
    }

    public void debuff(LivingEntity le, PotionEffectType type, int seconds, int amplifier) {
        if (le != null) le.addPotionEffect(new PotionEffect(type, seconds * 20, amplifier, true, true, true));
    }

    // ---- world effects -------------------------------------------------------------------------

    public void explode(Location loc, float power, boolean fire) {
        if (loc.getWorld() != null) loc.getWorld().createExplosion(loc.getX(), loc.getY(), loc.getZ(), power, fire, false, player);
    }

    public void lightning(Location loc) { if (loc.getWorld() != null) loc.getWorld().strikeLightning(loc); }

    public void particle(Location loc, String name, int count, double spread) {
        Fx.particle(loc, name, count, spread, spread, spread, 0.02);
    }

    public void sound(Location loc, String name, float vol, float pitch) { Fx.sound(loc, name, vol, pitch); }

    // ---- summons -------------------------------------------------------------------------------

    /** Summon a tracked ally that fights for the caster (delegates to {@link MinionManager}). */
    public Mob summon(EntityType type, Location loc, boolean melee, double healthScale, long lifespanMs) {
        return minions.spawn(player, type, loc, melee, healthScale, lifespanMs);
    }
}
