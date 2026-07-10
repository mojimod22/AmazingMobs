package me.zygotecode.amazingmobs.trait.impl;

import me.zygotecode.amazingmobs.trait.AbstractTrait;
import me.zygotecode.amazingmobs.trait.TraitContext;
import me.zygotecode.amazingmobs.util.Fx;

/**
 * The lower its health, the angrier it gets. Sustained STRENGTH/SPEED while below {@code threshold}.
 * params: {@code threshold} (0.5), {@code strength} (1), {@code speed} (0).
 */
public final class BerserkerTrait extends AbstractTrait {

    public BerserkerTrait() { super("berserker"); }

    @Override
    public void onTick(TraitContext c) {
        if (c.healthFraction() > d(c, "threshold", 0.5)) return;
        int dur = c.periodTicks() * 4; // refresh so it persists between controller ticks
        effect(c.entity(), "strength", i(c, "strength", 1), dur);
        if (i(c, "speed", 0) >= 0 && c.params().contains("speed")) effect(c.entity(), "speed", i(c, "speed", 0), dur);
        if (c.rng().chance(0.2)) Fx.particle(c.origin().add(0, 1, 0), "angry_villager", 4, 0.3, 0.4, 0.3, 0.01);
    }
}
