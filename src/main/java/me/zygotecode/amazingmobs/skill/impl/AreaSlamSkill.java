package me.zygotecode.amazingmobs.skill.impl;

import me.zygotecode.amazingmobs.skill.AbstractSkill;
import me.zygotecode.amazingmobs.skill.SkillContext;
import me.zygotecode.amazingmobs.skill.SkillType;
import me.zygotecode.amazingmobs.util.Fx;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

/**
 * Ground slam: damages + launches everything in {@code radius} around the caster. params:
 * {@code damage}, {@code knockup}, {@code knockback}.
 */
public final class AreaSlamSkill extends AbstractSkill {

    public AreaSlamSkill() { super("area_slam", SkillType.OFFENSE); }

    @Override
    public void cast(SkillContext ctx) {
        LivingEntity caster = ctx.caster();
        double radius = Math.max(2.0, ctx.trigger().radius());
        double dmg = d(ctx, "damage", 6);
        double knockup = d(ctx, "knockup", 0.55);
        double knockback = d(ctx, "knockback", 0.6);

        List<LivingEntity> victims = new ArrayList<>(ctx.targets());
        if (victims.isEmpty()) {
            for (Entity e : caster.getNearbyEntities(radius, radius, radius)) {
                if (e instanceof LivingEntity le && le.isValid() && !e.equals(caster)) victims.add(le);
            }
        }

        Location at = caster.getLocation();
        Fx.particle(at, str(ctx, "particle", "explosion_emitter"), 1, 0, 0, 0, 0);
        Fx.particle(at, "sweep_attack", 12, radius / 2, 0.2, radius / 2, 0);
        Fx.sound(at, str(ctx, "sound", "entity_generic_explode"), 1f, 0.7f);

        for (LivingEntity le : victims) {
            if (le == null || !le.isValid() || le.equals(caster)) continue;
            damage(ctx, le, dmg);
            Vector kb = le.getLocation().toVector().subtract(at.toVector());
            if (kb.lengthSquared() < 1.0E-4) kb = new Vector(0, 1, 0);
            kb.normalize().multiply(knockback).setY(knockup);
            le.setVelocity(le.getVelocity().add(kb));
        }
    }
}
