package me.zygotecode.amazingmobs.skill.impl;

import me.zygotecode.amazingmobs.skill.AbstractSkill;
import me.zygotecode.amazingmobs.skill.SkillContext;
import me.zygotecode.amazingmobs.skill.SkillType;
import me.zygotecode.amazingmobs.util.Fx;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;

/**
 * Controller-driven melee bite. Lets mobs that have no vanilla attack goal (villagers, chickens,
 * bats, cows...) actually hit a target in reach — the keystone that turns any entity into a real
 * combatant alongside the {@code hunter}/{@code drive} trait. params: {@code damage} (default = the
 * mob's attack damage), {@code knockback}; trigger {@code max-range} = reach.
 */
public final class StrikeSkill extends AbstractSkill {

    public StrikeSkill() { super("strike", SkillType.OFFENSE); }

    @Override
    public void cast(SkillContext ctx) {
        LivingEntity target = ctx.primaryTarget();
        if (target == null && !ctx.targets().isEmpty()) target = ctx.targets().get(0);
        if (target == null || !target.isValid()) return;
        double reach = ctx.trigger().maxRange() > 0 ? ctx.trigger().maxRange() : 2.5;
        if (target.getLocation().distance(ctx.origin()) > reach) return;

        double dmg = d(ctx, "damage", ctx.definition().stats().attackDamage());
        if (dmg > 0) target.damage(dmg, ctx.caster());
        Vector kb = target.getLocation().toVector().subtract(ctx.origin().toVector());
        if (kb.lengthSquared() > 1.0E-4) {
            target.setVelocity(target.getVelocity().add(kb.normalize().multiply(d(ctx, "knockback", 0.3)).setY(0.2)));
        }
        Fx.particle(target.getLocation().add(0, 1, 0), str(ctx, "particle", "sweep_attack"), 1, 0, 0, 0, 0);
        Fx.sound(ctx.origin(), str(ctx, "sound", "entity_player_attack_strong"), 1f, 1f);
    }
}
