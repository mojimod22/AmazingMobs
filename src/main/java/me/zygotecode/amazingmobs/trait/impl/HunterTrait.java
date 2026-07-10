package me.zygotecode.amazingmobs.trait.impl;

import me.zygotecode.amazingmobs.skill.Targeting;
import me.zygotecode.amazingmobs.trait.AbstractTrait;
import me.zygotecode.amazingmobs.trait.TraitContext;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;

/**
 * Relentless pursuer: forces re-acquisition of the nearest player over a long range (overriding
 * normal aggro), with an optional sustained speed edge. Actual movement is handled by the mob
 * controller's real navigation (see ActiveMob#pursue), so this trait no longer nudges velocity.
 * params: {@code range} (48), {@code speed} (SPEED amplifier, omitted = none).
 * (Legacy {@code drive}/{@code drive-power}/{@code reach} params are accepted but ignored.)
 */
public final class HunterTrait extends AbstractTrait {

    public HunterTrait() { super("hunter"); }

    @Override
    public void onTick(TraitContext c) {
        LivingEntity prey = Targeting.nearestPlayer(c.entity(), d(c, "range", 48));
        if (prey != null && c.entity() instanceof Mob mob && mob.getTarget() == null) mob.setTarget(prey);
        if (c.params().contains("speed")) effect(c.entity(), "speed", i(c, "speed", 0), c.periodTicks() * 3);
    }
}
