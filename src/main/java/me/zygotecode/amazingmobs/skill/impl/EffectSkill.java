package me.zygotecode.amazingmobs.skill.impl;

import me.zygotecode.amazingmobs.config.ConfigSection;
import me.zygotecode.amazingmobs.skill.AbstractSkill;
import me.zygotecode.amazingmobs.skill.SkillContext;
import me.zygotecode.amazingmobs.skill.SkillType;
import me.zygotecode.amazingmobs.util.Fx;
import me.zygotecode.amazingmobs.util.Resolvers;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Applies potion effects (and/or fire) to the resolved targets. The single most reused skill: it
 * backs every status-effect id — {@code poison weakness slow blind wither levitate strength speed
 * resistance regen fear rage shield buff effect} — differing only by their default effect list,
 * which config overrides via an {@code effects:} list. Targeting (self vs players vs allies) comes
 * from the trigger's {@code target} rule, so the same class buffs allies or debuffs players.
 */
public final class EffectSkill extends AbstractSkill {

    /** A default effect: a registry name + amplifier (0 = level I). {@code "fire"} = set on fire. */
    public record Eff(String type, int amplifier) {}

    private final List<Eff> defaults;

    public EffectSkill(String id, SkillType type, List<Eff> defaults) {
        super(id, type);
        this.defaults = defaults == null ? List.of() : List.copyOf(defaults);
    }

    @Override
    public void cast(SkillContext ctx) {
        List<LivingEntity> targets = ctx.targets();
        if (targets.isEmpty()) return;

        int duration = (int) (ctx.params().contains("duration")
                ? me.zygotecode.amazingmobs.util.Numbers.parseTicks(ctx.params().getString("duration"), ctx.trigger().durationTicks())
                : ctx.trigger().durationTicks());
        int fireTicks = i(ctx, "fire-ticks", 0);

        // Build effect list: explicit config wins, else fall back to this skill's presets.
        List<PotionEffect> effects = new ArrayList<>();
        boolean igniteFromDefaults = false;
        List<ConfigSection> entries = ctx.params().getSectionList("effects");
        if (!entries.isEmpty()) {
            for (ConfigSection e : entries) {
                String t = e.getString("type", "");
                int amp = e.getInt("amplifier", 0);
                int dur = e.contains("duration")
                        ? (int) me.zygotecode.amazingmobs.util.Numbers.parseTicks(e.getString("duration"), duration)
                        : duration;
                if (t.equalsIgnoreCase("fire")) { igniteFromDefaults = true; continue; }
                PotionEffectType pet = Resolvers.effect(t);
                if (pet != null) effects.add(new PotionEffect(pet, dur, Math.max(0, amp), false, true, true));
            }
        } else {
            for (Eff e : defaults) {
                if (e.type().equalsIgnoreCase("fire")) { igniteFromDefaults = true; continue; }
                PotionEffectType pet = Resolvers.effect(e.type());
                if (pet != null) effects.add(new PotionEffect(pet, duration, Math.max(0, e.amplifier()), false, true, true));
            }
            // single-effect override convenience
            String single = ctx.params().getString("type");
            if (single != null && !single.isBlank()) {
                if (single.equalsIgnoreCase("fire")) igniteFromDefaults = true;
                else {
                    PotionEffectType pet = Resolvers.effect(single);
                    if (pet != null) effects.add(new PotionEffect(pet, duration, Math.max(0, i(ctx, "amplifier", 0)), false, true, true));
                }
            }
        }

        boolean ignite = igniteFromDefaults || fireTicks > 0;
        int finalFire = fireTicks > 0 ? fireTicks : duration;

        for (LivingEntity le : targets) {
            if (le == null || !le.isValid()) continue;
            for (PotionEffect pe : effects) le.addPotionEffect(pe);
            if (ignite) le.setFireTicks(Math.max(le.getFireTicks(), finalFire));
            Fx.particle(le.getLocation().add(0, 1, 0), str(ctx, "particle", null), i(ctx, "particle-count", 12), 0.4, 0.6, 0.4, 0.02);
        }
        Fx.sound(ctx.origin(), str(ctx, "sound", null), (float) d(ctx, "sound-volume", 1.0), (float) d(ctx, "sound-pitch", 1.0));
    }

    /** Convenience to declare presets terse-ly. */
    public static List<Eff> of(String... typeAmpPairs) {
        List<Eff> out = new ArrayList<>();
        for (String s : typeAmpPairs) {
            String[] parts = s.split(":");
            int amp = parts.length > 1 ? safeInt(parts[1]) : 0;
            out.add(new Eff(parts[0].toLowerCase(Locale.ROOT), amp));
        }
        return out;
    }

    private static int safeInt(String s) {
        try { return Integer.parseInt(s.trim()); } catch (NumberFormatException e) { return 0; }
    }
}
