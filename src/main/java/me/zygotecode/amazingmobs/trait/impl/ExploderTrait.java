package me.zygotecode.amazingmobs.trait.impl;

import me.zygotecode.amazingmobs.trait.AbstractTrait;
import me.zygotecode.amazingmobs.trait.TraitContext;
import me.zygotecode.amazingmobs.util.Fx;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;

/**
 * Detonates on death — AoE damage + knockback, optional fire, optional real (block-safe by default)
 * explosion. params: {@code radius} (4), {@code damage} (8), {@code knockback} (1.0), {@code fire},
 * {@code fire-ticks} (60), {@code block-damage} (false).
 */
public final class ExploderTrait extends AbstractTrait {

    // Absolute caps on a victim's velocity right after a blast, so several kamikaze detonating at once
    // can't compound their pushes into an orbital launch. Tuned to "firm shove, not a flight".
    private static final double MAX_H = 1.1;   // horizontal speed cap
    private static final double MAX_Y = 0.55;  // upward speed cap

    public ExploderTrait() { super("exploder"); }

    @Override
    public void onDeath(TraitContext c) {
        Location at = c.origin();
        double radius = d(c, "radius", 4);
        double dmg = d(c, "damage", 8);
        double kb = d(c, "knockback", 1.0);
        boolean fire = flag(c, "fire", false);
        int fireTicks = i(c, "fire-ticks", 60);

        Fx.particle(at, "explosion_emitter", 1, 0, 0, 0, 0);
        Fx.sound(at, str(c, "sound", "entity_generic_explode"), 1.2f, 1.0f);

        if (flag(c, "block-damage", false) && at.getWorld() != null) {
            at.getWorld().createExplosion(at, (float) Math.min(8, radius), fire, true, c.entity());
            return; // vanilla explosion already handled damage/knockback
        }
        for (Entity e : c.entity().getNearbyEntities(radius, radius, radius)) {
            if (!(e instanceof LivingEntity le) || le.isDead()) continue;
            le.damage(dmg, c.entity());
            Vector push = le.getLocation().toVector().subtract(at.toVector());
            if (push.lengthSquared() < 1.0E-4) push = new Vector(0, 1, 0);
            // vertical lift now scales with knockback (so the param actually controls the "fly"),
            // and the *resulting* velocity is clamped so many simultaneous blasts can't compound.
            double vy = Math.min(0.4, kb * 0.45);
            Vector vel = le.getVelocity().add(push.normalize().multiply(kb).setY(vy));
            double h = Math.hypot(vel.getX(), vel.getZ());
            if (h > MAX_H) { double s = MAX_H / h; vel.setX(vel.getX() * s); vel.setZ(vel.getZ() * s); }
            if (vel.getY() > MAX_Y) vel.setY(MAX_Y);
            le.setVelocity(vel);
            if (fire) le.setFireTicks(Math.max(le.getFireTicks(), fireTicks));
        }
    }
}
