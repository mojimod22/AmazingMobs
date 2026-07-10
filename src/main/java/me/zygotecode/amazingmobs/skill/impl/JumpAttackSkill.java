package me.zygotecode.amazingmobs.skill.impl;

import me.zygotecode.amazingmobs.skill.AbstractSkill;
import me.zygotecode.amazingmobs.skill.SkillContext;
import me.zygotecode.amazingmobs.skill.SkillType;
import me.zygotecode.amazingmobs.util.Fx;
import me.zygotecode.amazingmobs.util.Schedulers;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;

/**
 * Leaps toward the target, then slams an AoE on landing. params: {@code power}, {@code upward},
 * {@code delay} (ticks until slam), {@code damage}, {@code knockup}, plus trigger {@code radius}.
 */
public final class JumpAttackSkill extends AbstractSkill {

    public JumpAttackSkill() { super("jump_attack", SkillType.OFFENSE); }

    @Override
    public void cast(SkillContext ctx) {
        LivingEntity caster = ctx.caster();
        LivingEntity target = ctx.primaryTarget();
        if (target == null && !ctx.targets().isEmpty()) target = ctx.targets().get(0);

        Vector dir = (target != null && target.isValid())
                ? target.getLocation().toVector().subtract(caster.getLocation().toVector()).setY(0)
                : caster.getLocation().getDirection().setY(0);
        if (dir.lengthSquared() > 1.0E-4) dir.normalize().multiply(d(ctx, "power", 0.9));
        dir.setY(d(ctx, "upward", 0.7));
        caster.setVelocity(dir);
        Fx.sound(caster.getLocation(), str(ctx, "sound", "entity_ravager_roar"), 1f, 1.2f);

        double radius = Math.max(2.0, ctx.trigger().radius());
        double dmg = d(ctx, "damage", 6);
        double knockup = d(ctx, "knockup", 0.6);
        long delay = (long) i(ctx, "delay", 14);

        Schedulers.later(ctx.plugin(), delay, () -> {
            if (!caster.isValid()) return;
            Location at = caster.getLocation();
            Fx.particle(at, str(ctx, "land-particle", "explosion"), 2, 0, 0, 0, 0);
            Fx.particle(at, "block_crack", 30, radius / 2, 0.2, radius / 2, 0.1);
            Fx.sound(at, "entity_generic_explode", 1f, 0.8f);
            for (Entity e : caster.getNearbyEntities(radius, radius, radius)) {
                if (e instanceof LivingEntity le && le.isValid()) {
                    damage(ctx, le, dmg);
                    Vector kb = le.getLocation().toVector().subtract(at.toVector());
                    if (kb.lengthSquared() < 1.0E-4) kb = new Vector(0, 1, 0);
                    kb.normalize().multiply(0.6).setY(knockup);
                    le.setVelocity(le.getVelocity().add(kb));
                }
            }
        });
    }
}
