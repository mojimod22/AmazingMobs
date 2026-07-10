package me.zygotecode.amazingmobs.skill.impl;

import me.zygotecode.amazingmobs.skill.AbstractSkill;
import me.zygotecode.amazingmobs.skill.SkillContext;
import me.zygotecode.amazingmobs.skill.SkillType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;

/** A quick lunge toward the current target. params: {@code power}, {@code upward}. */
public final class DashSkill extends AbstractSkill {

    public DashSkill() { super("dash", SkillType.MOVEMENT); }

    @Override
    public void cast(SkillContext ctx) {
        LivingEntity caster = ctx.caster();
        LivingEntity target = ctx.primaryTarget();
        if (target == null && !ctx.targets().isEmpty()) target = ctx.targets().get(0);
        Vector dir;
        if (target != null && target.isValid()) {
            dir = target.getLocation().toVector().subtract(caster.getLocation().toVector());
            if (dir.lengthSquared() < 1.0E-4) dir = caster.getLocation().getDirection();
        } else {
            dir = caster.getLocation().getDirection();
        }
        dir.setY(0).normalize().multiply(d(ctx, "power", 1.4));
        dir.setY(d(ctx, "upward", 0.32));
        caster.setVelocity(dir);
        feedback(ctx, caster.getLocation());
    }
}
