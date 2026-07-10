package me.zygotecode.amazingmobs.trait.impl;

import me.zygotecode.amazingmobs.trait.AbstractTrait;
import me.zygotecode.amazingmobs.trait.TraitContext;
import me.zygotecode.amazingmobs.util.Fx;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;

/**
 * Holds a grudge: when hurt it locks onto the attacker, briefly empowers itself, and (with
 * {@code rally:true}) sics nearby allies on the same target. params: {@code strength} (1),
 * {@code rally} (false), {@code rally-radius} (10).
 */
public final class RevengeTrait extends AbstractTrait {

    public RevengeTrait() { super("revenge"); }

    @Override
    public void onDamaged(TraitContext c) {
        LivingEntity attacker = c.eventTarget();
        if (attacker == null) return;
        if (c.entity() instanceof Mob m) m.setTarget(attacker);
        effect(c.entity(), "strength", i(c, "strength", 1), c.periodTicks() * 6);
        Fx.particle(c.origin().add(0, 1, 0), "angry_villager", 5, 0.3, 0.4, 0.3, 0.01);
        if (flag(c, "rally", false)) {
            for (LivingEntity ally : allies(c, d(c, "rally-radius", 10), false)) {
                if (ally instanceof Mob am) am.setTarget(attacker);
            }
        }
    }
}
