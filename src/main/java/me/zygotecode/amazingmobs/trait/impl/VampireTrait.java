package me.zygotecode.amazingmobs.trait.impl;

import me.zygotecode.amazingmobs.trait.AbstractTrait;
import me.zygotecode.amazingmobs.trait.TraitContext;
import me.zygotecode.amazingmobs.util.Fx;

/**
 * Heals itself when it lands a hit ({@code vampire}); with {@code weaken:true} it also saps the
 * victim ({@code parasite}). params: {@code heal} (2), {@code weaken}, {@code weaken-duration}.
 */
public final class VampireTrait extends AbstractTrait {

    private final boolean defaultWeaken;

    public VampireTrait(String id, boolean defaultWeaken) {
        super(id);
        this.defaultWeaken = defaultWeaken;
    }

    @Override
    public void onAttack(TraitContext c) {
        heal(c.entity(), d(c, "heal", 2));
        Fx.particle(c.origin().add(0, 1, 0), "damage_indicator", 4, 0.2, 0.4, 0.2, 0.01);
        if (flag(c, "weaken", defaultWeaken) && c.eventTarget() != null) {
            int dur = (int) me.zygotecode.amazingmobs.util.Numbers.parseTicks(c.params().getString("weaken-duration"), 80);
            effect(c.eventTarget(), "weakness", i(c, "weaken-amplifier", 0), dur);
            effect(c.eventTarget(), "slowness", 0, dur);
        }
    }
}
