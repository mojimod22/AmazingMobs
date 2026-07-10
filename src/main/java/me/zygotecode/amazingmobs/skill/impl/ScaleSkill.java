package me.zygotecode.amazingmobs.skill.impl;

import me.zygotecode.amazingmobs.skill.AbstractSkill;
import me.zygotecode.amazingmobs.skill.SkillContext;
import me.zygotecode.amazingmobs.skill.SkillType;
import me.zygotecode.amazingmobs.util.Fx;
import me.zygotecode.amazingmobs.util.Schedulers;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.LivingEntity;

/**
 * Dynamically resizes the caster via the SCALE attribute — grow huge (intimidate, hit harder) or
 * shrink tiny (dodge, scuttle). Optionally pairs the size change with a speed buff (small = fast)
 * and reverts after the trigger duration. params: {@code size} (target scale), {@code revert}
 * (default true if duration>0), {@code speed} (SPEED amplifier while resized, -1 = off).
 */
public final class ScaleSkill extends AbstractSkill {

    public ScaleSkill() { super("scale", SkillType.UTILITY); }

    @Override
    public void cast(SkillContext ctx) {
        LivingEntity caster = ctx.caster();
        AttributeInstance scale = caster.getAttribute(Attribute.SCALE);
        if (scale == null) return;

        double target = Math.max(0.0625, Math.min(16, d(ctx, "size", 1.5)));
        double original = scale.getBaseValue();
        scale.setBaseValue(target);

        int speedAmp = i(ctx, "speed", -1);
        long dur = ctx.trigger().durationTicks();
        if (speedAmp >= 0) effectSpeed(caster, speedAmp, (int) Math.max(20, dur));

        Fx.particle(caster.getLocation().add(0, 1, 0), str(ctx, "particle", target >= original ? "explosion" : "cloud"),
                12, 0.4, 0.5, 0.4, 0.02);
        Fx.sound(caster.getLocation(), str(ctx, "sound", target >= original ? "entity_ravager_roar" : "entity_rabbit_jump"),
                1f, target >= original ? 0.8f : 1.6f);

        if (dur > 0 && flag(ctx, "revert", true)) {
            Schedulers.later(ctx.plugin(), dur, () -> {
                if (caster.isValid()) {
                    AttributeInstance s = caster.getAttribute(Attribute.SCALE);
                    if (s != null) s.setBaseValue(original);
                }
            });
        }
    }

    private void effectSpeed(LivingEntity le, int amp, int dur) {
        var pet = me.zygotecode.amazingmobs.util.Resolvers.effect("speed");
        if (pet != null) le.addPotionEffect(new org.bukkit.potion.PotionEffect(pet, dur, Math.max(0, amp), false, true, true));
    }
}
