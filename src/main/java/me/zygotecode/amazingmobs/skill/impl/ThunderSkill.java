package me.zygotecode.amazingmobs.skill.impl;

import me.zygotecode.amazingmobs.skill.AbstractSkill;
import me.zygotecode.amazingmobs.skill.SkillContext;
import me.zygotecode.amazingmobs.skill.SkillType;
import me.zygotecode.amazingmobs.util.Fx;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;

/**
 * Lightning. Two modes:
 *  - default: strikes the resolved targets (players), dealing optional direct {@code damage};
 *  - {@code around: true}: harmless cosmetic bolts around the caster ({@code count} of them within
 *    {@code around-radius}) to intimidate.
 * Either way, {@code explosion: true} adds a small (block-safe) blast at each strike point:
 * {@code explosion-radius} AoE damage + knockback (+ optional {@code explosion-fire}).
 */
public final class ThunderSkill extends AbstractSkill {

    public ThunderSkill() { super("thunder", SkillType.OFFENSE); }

    @Override
    public void cast(SkillContext ctx) {
        boolean around = flag(ctx, "around", false);
        boolean effectOnly = flag(ctx, "effect-only", false);
        double direct = d(ctx, "damage", 0);
        int count = Math.max(1, i(ctx, "count", 1));

        if (around) {
            double r = d(ctx, "around-radius", 6);
            for (int n = 0; n < count; n++) {
                Location p = ctx.origin().add((ctx.rng().nextDouble() - 0.5) * 2 * r, 0,
                        (ctx.rng().nextDouble() - 0.5) * 2 * r);
                strikeAt(ctx, p, true, 0, null); // cosmetic bolts for fear (never self-harm)
            }
            return;
        }

        if (ctx.targets().isEmpty()) {
            if (ctx.primaryTarget() != null) strikeAt(ctx, ctx.primaryTarget().getLocation(), effectOnly, direct, ctx.primaryTarget());
            return;
        }
        for (LivingEntity t : ctx.targets()) strikeAt(ctx, t.getLocation(), effectOnly, direct, t);
    }

    private void strikeAt(SkillContext ctx, Location loc, boolean effectOnly, double directDmg, LivingEntity target) {
        if (loc.getWorld() == null) return;
        if (effectOnly) loc.getWorld().strikeLightningEffect(loc);
        else loc.getWorld().strikeLightning(loc);
        if (target != null && directDmg > 0) target.damage(directDmg, ctx.caster());
        if (flag(ctx, "explosion", false)) explode(ctx, loc);
    }

    private void explode(SkillContext ctx, Location at) {
        double radius = d(ctx, "explosion-radius", 3);
        double dmg = d(ctx, "explosion-damage", 4);
        boolean fire = flag(ctx, "explosion-fire", false);
        Fx.particle(at, "explosion", 1, 0, 0, 0, 0);
        Fx.particle(at, "electric_spark", 20, radius / 2, 0.3, radius / 2, 0.1);
        Fx.sound(at, "entity_generic_explode", 0.9f, 1.2f);
        for (Entity e : at.getWorld().getNearbyEntities(at, radius, radius, radius)) {
            if (!(e instanceof LivingEntity le) || le.equals(ctx.caster()) || le.isDead()) continue;
            if (dmg > 0) le.damage(dmg, ctx.caster());
            Vector kb = le.getLocation().toVector().subtract(at.toVector());
            if (kb.lengthSquared() < 1.0E-4) kb = new Vector(0, 1, 0);
            le.setVelocity(le.getVelocity().add(kb.normalize().multiply(0.5).setY(0.35)));
            if (fire) le.setFireTicks(Math.max(le.getFireTicks(), 60));
        }
    }
}
