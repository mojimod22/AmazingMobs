package me.zygotecode.amazingmobs.trait.impl;

import me.zygotecode.amazingmobs.skill.Targeting;
import me.zygotecode.amazingmobs.trait.AbstractTrait;
import me.zygotecode.amazingmobs.trait.TraitContext;
import me.zygotecode.amazingmobs.util.Fx;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;

/**
 * Periodically pounces toward the nearest player. params: {@code cooldown} (4s), {@code power} (1.3),
 * {@code upward} (0.45), {@code range} (16).
 */
public final class LeaperTrait extends AbstractTrait {

    public LeaperTrait() { super("leaper"); }

    @Override
    public void onTick(TraitContext c) {
        LivingEntity prey = Targeting.nearestPlayer(c.entity(), d(c, "range", 16));
        if (prey == null) return;
        double dist = prey.getLocation().distance(c.origin());
        if (dist < 3 || !ready(c, "4s")) return;
        Vector dir = prey.getLocation().toVector().subtract(c.origin().toVector()).setY(0);
        if (dir.lengthSquared() < 1.0E-4) return;
        dir.normalize().multiply(d(c, "power", 1.3)).setY(d(c, "upward", 0.45));
        c.entity().setVelocity(dir);
        Fx.sound(c.origin(), str(c, "sound", "entity_spider_step"), 1f, 1.2f);
        Fx.particle(c.origin(), "cloud", 8, 0.2, 0.1, 0.2, 0.02);
    }
}
