package me.zygotecode.amazingmobs.skill.impl;

import me.zygotecode.amazingmobs.skill.AbstractSkill;
import me.zygotecode.amazingmobs.skill.SkillContext;
import me.zygotecode.amazingmobs.skill.SkillType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;

/**
 * Knockback repulsion ({@code repel}) or gravity pull ({@code pull}) of the resolved targets,
 * relative to the caster. params: {@code power}, {@code upward}, {@code pull} (overrides default).
 */
public final class RepelSkill extends AbstractSkill {

    private final boolean defaultPull;

    public RepelSkill(String id, boolean defaultPull) {
        super(id, SkillType.CONTROL);
        this.defaultPull = defaultPull;
    }

    @Override
    public void cast(SkillContext ctx) {
        boolean pull = flag(ctx, "pull", defaultPull);
        double power = d(ctx, "power", 1.3);
        double upward = d(ctx, "upward", 0.35);
        var origin = ctx.origin().toVector();
        feedback(ctx, ctx.origin());

        for (LivingEntity le : ctx.targets()) {
            if (le == null || !le.isValid() || le.equals(ctx.caster())) continue;
            Vector dir = le.getLocation().toVector().subtract(origin);
            if (dir.lengthSquared() < 1.0E-4) dir = new Vector(0, 1, 0);
            dir.normalize().multiply(power);
            if (pull) dir.multiply(-1);
            dir.setY(upward);
            le.setVelocity(le.getVelocity().add(dir));
        }
    }
}
