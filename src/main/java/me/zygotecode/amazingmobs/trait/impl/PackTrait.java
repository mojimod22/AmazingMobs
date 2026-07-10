package me.zygotecode.amazingmobs.trait.impl;

import me.zygotecode.amazingmobs.trait.AbstractTrait;
import me.zygotecode.amazingmobs.trait.TraitContext;
import me.zygotecode.amazingmobs.util.Fx;

/**
 * Stronger in a crowd: scales a STRENGTH buff with the number of same-type allies nearby. Backs
 * {@code pack} / {@code swarm_leader}. params: {@code radius} (10), {@code per} (0.34 amp per ally),
 * {@code max-amplifier} (3), {@code same-type-only} (true).
 */
public final class PackTrait extends AbstractTrait {

    public PackTrait(String id) { super(id); }

    @Override
    public void onTick(TraitContext c) {
        int allies = allies(c, d(c, "radius", 10), flag(c, "same-type-only", true)).size();
        if (allies <= 0) return;
        int amp = (int) Math.min(d(c, "max-amplifier", 3), Math.floor(allies * d(c, "per", 0.34)));
        if (amp < 0) return;
        int dur = c.periodTicks() * 4;
        effect(c.entity(), "strength", amp, dur);
        if (flag(c, "speed", false)) effect(c.entity(), "speed", Math.min(2, amp), dur);
        if (c.rng().chance(0.1)) Fx.particle(c.origin().add(0, 1, 0), "enchanted_hit", 4, 0.3, 0.4, 0.3, 0.01);
    }
}
