package me.zygotecode.amazingmobs.skill.impl;

import me.zygotecode.amazingmobs.skill.AbstractSkill;
import me.zygotecode.amazingmobs.skill.SkillContext;
import me.zygotecode.amazingmobs.skill.SkillType;
import me.zygotecode.amazingmobs.util.Fx;
import me.zygotecode.amazingmobs.util.Resolvers;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

/**
 * Lift-off / glide. Applies levitation (+ slow falling) to the caster and nudges it toward the
 * target so grounded mobs gain altitude to harass from above. params: {@code amplifier},
 * {@code glide} (adds slow falling), {@code approach} (horizontal nudge power).
 */
public final class FlightSkill extends AbstractSkill {

    public FlightSkill(String id) { super(id, SkillType.MOVEMENT); }

    @Override
    public void cast(SkillContext ctx) {
        LivingEntity caster = ctx.caster();
        int dur = (int) Math.max(20, ctx.trigger().durationTicks());

        PotionEffectType lev = Resolvers.effect("levitation");
        if (lev != null) caster.addPotionEffect(new PotionEffect(lev, dur, Math.max(0, i(ctx, "amplifier", 1)), false, true, true));
        if (flag(ctx, "glide", true)) {
            PotionEffectType slow = Resolvers.effect("slow_falling");
            if (slow != null) caster.addPotionEffect(new PotionEffect(slow, dur + 60, 0, false, true, true));
        }

        LivingEntity target = ctx.primaryTarget();
        double approach = d(ctx, "approach", 0.6);
        if (target != null && target.isValid() && approach > 0) {
            Vector dir = target.getLocation().toVector().subtract(caster.getLocation().toVector()).setY(0);
            if (dir.lengthSquared() > 1.0E-4) caster.setVelocity(caster.getVelocity().add(dir.normalize().multiply(approach)));
        }
        Fx.particle(ctx.origin(), str(ctx, "particle", "cloud"), 20, 0.4, 0.2, 0.4, 0.05);
        Fx.sound(ctx.origin(), str(ctx, "sound", "entity_phantom_flap"), 1f, 1f);
    }
}
