package me.zygotecode.amazingmobs.trait.impl;

import me.zygotecode.amazingmobs.skill.Targeting;
import me.zygotecode.amazingmobs.trait.AbstractTrait;
import me.zygotecode.amazingmobs.trait.TraitContext;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.util.Vector;

/**
 * Breaks and runs when wounded. Below {@code flee-at} health it drops its target and sprints away
 * from the nearest player. params: {@code flee-at} (0.4), {@code speed-boost} (true).
 */
public final class CowardlyTrait extends AbstractTrait {

    public CowardlyTrait() { super("cowardly"); }

    @Override
    public void onTick(TraitContext c) {
        if (c.healthFraction() > d(c, "flee-at", 0.4)) return;
        LivingEntity near = Targeting.nearestPlayer(c.entity(), 20);
        if (near == null) return;
        if (c.entity() instanceof Mob m) m.setTarget(null);
        Vector away = c.origin().toVector().subtract(near.getLocation().toVector()).setY(0);
        if (away.lengthSquared() > 1.0E-4) c.entity().setVelocity(away.normalize().multiply(0.45));
        if (flag(c, "speed-boost", true)) effect(c.entity(), "speed", 1, c.periodTicks() * 3);
    }
}
