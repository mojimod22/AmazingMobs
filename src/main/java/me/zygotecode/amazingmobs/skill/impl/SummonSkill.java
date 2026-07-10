package me.zygotecode.amazingmobs.skill.impl;

import me.zygotecode.amazingmobs.skill.AbstractSkill;
import me.zygotecode.amazingmobs.skill.SkillContext;
import me.zygotecode.amazingmobs.skill.SkillType;
import me.zygotecode.amazingmobs.util.Fx;
import me.zygotecode.amazingmobs.util.IntRange;
import org.bukkit.Location;

/**
 * Summons reinforcement custom mobs around the caster. params: {@code mob} (defaults to a copy of
 * the caster), {@code count} (number or {@code a-b} range), {@code spread} (block radius).
 */
public final class SummonSkill extends AbstractSkill {

    public SummonSkill() { super("summon", SkillType.SUMMON); }

    @Override
    public void cast(SkillContext ctx) {
        String mobId = str(ctx, "mob", null);
        if (mobId == null || mobId.isBlank()) mobId = ctx.definition().id();
        int count = ctx.params().getIntRange("count", new IntRange(1, 2)).pick(ctx.rng());
        double spread = d(ctx, "spread", 3.0);

        Location base = ctx.origin();
        Fx.particle(base, str(ctx, "particle", "soul"), 30, 0.6, 0.6, 0.6, 0.05);
        Fx.sound(base, str(ctx, "sound", "entity_evoker_prepare_summon"), 1f, 1f);

        for (int n = 0; n < count; n++) {
            double dx = (ctx.rng().nextDouble() - 0.5) * 2 * spread;
            double dz = (ctx.rng().nextDouble() - 0.5) * 2 * spread;
            Location at = base.clone().add(dx, 0, dz);
            ctx.summon(mobId, at);
        }
    }
}
