package me.zygotecode.amazingmobs.skill.impl;

import me.zygotecode.amazingmobs.skill.AbstractSkill;
import me.zygotecode.amazingmobs.skill.SkillContext;
import me.zygotecode.amazingmobs.skill.SkillType;
import me.zygotecode.amazingmobs.util.Fx;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;

/**
 * Circles the target — a tangential velocity nudge (with a slight inward pull + hover) that, fired
 * repeatedly on TICK, makes flyers like bats/phantoms swarm and orbit. params: {@code speed} (0.55),
 * {@code pull} (0.12), {@code hover} (0.0).
 */
public final class OrbitSkill extends AbstractSkill {

    public OrbitSkill() { super("orbit", SkillType.MOVEMENT); }

    @Override
    public void cast(SkillContext ctx) {
        LivingEntity caster = ctx.caster();
        LivingEntity target = ctx.primaryTarget();
        if (target == null && !ctx.targets().isEmpty()) target = ctx.targets().get(0);
        if (target == null || !target.isValid()) return;

        Vector toTarget = target.getLocation().toVector().subtract(caster.getLocation().toVector()).setY(0);
        if (toTarget.lengthSquared() < 1.0E-4) return;
        Vector tangent = new Vector(-toTarget.getZ(), 0, toTarget.getX()).normalize();
        Vector v = tangent.multiply(d(ctx, "speed", 0.55))
                .add(toTarget.normalize().multiply(d(ctx, "pull", 0.12)));
        v.setY(d(ctx, "hover", 0.0));
        caster.setVelocity(v);
        Fx.particle(caster.getLocation(), str(ctx, "particle", null), 3, 0.1, 0.1, 0.1, 0.01);
    }
}
