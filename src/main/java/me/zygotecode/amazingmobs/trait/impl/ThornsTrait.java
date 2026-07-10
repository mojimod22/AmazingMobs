package me.zygotecode.amazingmobs.trait.impl;

import me.zygotecode.amazingmobs.trait.AbstractTrait;
import me.zygotecode.amazingmobs.trait.TraitContext;
import me.zygotecode.amazingmobs.util.Fx;
import org.bukkit.entity.LivingEntity;

/**
 * Punishes attackers: reflects a flat amount of damage back on melee. params: {@code amount} (3).
 */
public final class ThornsTrait extends AbstractTrait {

    public ThornsTrait() { super("thorns"); }

    @Override
    public void onDamaged(TraitContext c) {
        LivingEntity attacker = c.eventTarget();
        double amount = d(c, "amount", 3);
        if (attacker == null || !attacker.isValid() || amount <= 0) return;
        attacker.damage(amount, c.entity());
        Fx.particle(attacker.getLocation().add(0, 1, 0), "crit", 6, 0.3, 0.3, 0.3, 0.05);
    }
}
