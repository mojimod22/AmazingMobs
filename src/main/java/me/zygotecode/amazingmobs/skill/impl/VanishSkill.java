package me.zygotecode.amazingmobs.skill.impl;

import me.zygotecode.amazingmobs.skill.AbstractSkill;
import me.zygotecode.amazingmobs.skill.SkillContext;
import me.zygotecode.amazingmobs.skill.SkillType;
import me.zygotecode.amazingmobs.util.Fx;
import me.zygotecode.amazingmobs.util.Resolvers;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/** Invisibility burst on the caster for the trigger duration. params: {@code amplifier}. */
public final class VanishSkill extends AbstractSkill {

    public VanishSkill() { super("vanish", SkillType.UTILITY); }

    @Override
    public void cast(SkillContext ctx) {
        PotionEffectType invis = Resolvers.effect("invisibility");
        if (invis == null) return;
        int dur = (int) Math.max(20, ctx.trigger().durationTicks());
        ctx.caster().addPotionEffect(new PotionEffect(invis, dur, Math.max(0, i(ctx, "amplifier", 0)), false, false, true));
        Fx.particle(ctx.origin().add(0, 1, 0), str(ctx, "particle", "large_smoke"), 30, 0.4, 0.8, 0.4, 0.04);
        Fx.sound(ctx.origin(), str(ctx, "sound", "entity_illusioner_mirror_move"), 1f, 1f);
    }
}
