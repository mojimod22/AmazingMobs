package me.zygotecode.amazingmobs.skill.impl;

import me.zygotecode.amazingmobs.skill.AbstractSkill;
import me.zygotecode.amazingmobs.skill.SkillContext;
import me.zygotecode.amazingmobs.skill.SkillType;
import me.zygotecode.amazingmobs.util.Fx;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.LivingEntity;

/**
 * Heals the resolved targets (typically SELF or allies). params: {@code amount} (flat hearts*2),
 * {@code percent} (fraction of max health). Both stack.
 */
public final class HealSkill extends AbstractSkill {

    public HealSkill() { super("heal", SkillType.DEFENSE); }

    @Override
    public void cast(SkillContext ctx) {
        double flat = d(ctx, "amount", 0);
        double pct = d(ctx, "percent", 0);
        for (LivingEntity le : ctx.targets()) {
            if (le == null || !le.isValid()) continue;
            AttributeInstance maxAttr = le.getAttribute(Attribute.MAX_HEALTH);
            double max = maxAttr != null ? maxAttr.getValue() : le.getHealth();
            double heal = flat + max * pct;
            if (heal <= 0) continue;
            le.setHealth(Math.min(max, le.getHealth() + heal));
            Fx.particle(le.getLocation().add(0, 1, 0), str(ctx, "particle", "heart"), 8, 0.4, 0.6, 0.4, 0.02);
        }
        Fx.sound(ctx.origin(), str(ctx, "sound", "entity_player_levelup"), 0.6f, 1.5f);
    }
}
