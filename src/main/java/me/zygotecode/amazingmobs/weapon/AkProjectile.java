package me.zygotecode.amazingmobs.weapon;

import me.zygotecode.amazingmobs.util.Rng;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.WitherSkull;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

/**
 * Manually drives an AK-47 wither skull along a <b>fixed straight ray at constant speed</b>. Vanilla
 * wither skulls self-accelerate (curve + speed up) and get shoved by nearby explosions, which warps the
 * trajectory — so instead of trusting the entity's physics, each tick we ray-trace one step ahead for a
 * block/mob and either detonate on the hit or teleport the skull exactly one step along the original
 * direction. Nothing (explosions, knockback, gravity) can alter the path.
 */
public final class AkProjectile extends BukkitRunnable {

    private static final double SPEED = 2.0;        // blocks/tick = 40 b/s — fast but the arc reads clearly
    private static final double MAX_RANGE = 90.0;   // blocks before it detonates in the air
    private static final double DIRECT_DAMAGE = 14.0;
    private static final float EXPLOSION_POWER = 3.0f;
    private static final double LIGHTNING_CHANCE = 0.20;
    private static final int FIRE_TICKS = 100;

    private final WitherSkull skull;
    private final Player shooter;
    private final Vector dir;       // unit direction, never changes
    private double traveled;
    private boolean done;

    public AkProjectile(WitherSkull skull, Player shooter, Vector dir) {
        this.skull = skull;
        this.shooter = shooter;
        this.dir = dir.clone().normalize();
    }

    @Override
    public void run() {
        if (done || skull == null || skull.isDead() || !skull.isValid()) { cancel(); return; }
        World w = skull.getWorld();
        Location from = skull.getLocation();

        // look one step ahead for a solid block or a living target (ignore the shooter + other projectiles)
        RayTraceResult rt = w.rayTrace(from, dir, SPEED, FluidCollisionMode.NEVER, true, 0.4,
                e -> !e.equals(shooter) && !(e instanceof Projectile) && e instanceof LivingEntity);
        if (rt != null && (rt.getHitBlock() != null || rt.getHitEntity() != null)) {
            Vector hp = rt.getHitPosition();
            impact(new Location(w, hp.getX(), hp.getY(), hp.getZ()), rt.getHitEntity());
            return;
        }

        // advance exactly one step along the original ray — constant speed, perfectly straight
        Location next = from.clone().add(dir.getX() * SPEED, dir.getY() * SPEED, dir.getZ() * SPEED);
        next.setDirection(dir);
        skull.teleport(next);
        skull.setVelocity(dir.clone().multiply(SPEED)); // cosmetic: keeps the trail aligned
        skull.setAcceleration(new Vector(0, 0, 0));     // kill vanilla self-acceleration each tick

        traveled += SPEED;
        if (traveled >= MAX_RANGE) impact(skull.getLocation(), null);
    }

    /** Detonate: heavy direct damage + medium block-safe explosion + fire + 20% lightning. */
    private void impact(Location loc, Entity hitEntity) {
        if (done) return;
        done = true;
        World w = loc.getWorld();
        if (w != null) {
            if (hitEntity instanceof LivingEntity le && !le.equals(shooter)) {
                le.damage(DIRECT_DAMAGE, shooter);
                le.setFireTicks(FIRE_TICKS);
            }
            w.createExplosion(loc.getX(), loc.getY(), loc.getZ(), EXPLOSION_POWER, true, false, shooter);
            if (Rng.shared().rangeDouble(0, 1) < LIGHTNING_CHANCE) w.strikeLightning(loc);
        }
        if (skull != null && skull.isValid()) skull.remove();
        cancel();
    }
}
