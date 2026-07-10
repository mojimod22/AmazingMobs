package me.zygotecode.amazingmobs.trait.impl;

import me.zygotecode.amazingmobs.trait.AbstractTrait;
import me.zygotecode.amazingmobs.trait.TraitContext;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

/**
 * A living mount/platform: continually hardens whatever rides it. params: {@code resistance} (0),
 * {@code regeneration} (-1 = off), {@code speed} (-1 = off). Pairs with the rider/mount system.
 */
public final class CarrierTrait extends AbstractTrait {

    public CarrierTrait() { super("carrier"); }

    @Override
    public void onTick(TraitContext c) {
        if (c.entity().getPassengers().isEmpty()) return;
        int dur = c.periodTicks() * 4;
        for (Entity p : c.entity().getPassengers()) {
            if (!(p instanceof LivingEntity rider)) continue;
            effect(rider, "resistance", i(c, "resistance", 0), dur);
            if (c.params().contains("regeneration")) effect(rider, "regeneration", i(c, "regeneration", 0), dur);
            if (c.params().contains("speed")) effect(rider, "speed", i(c, "speed", 0), dur);
        }
    }
}
