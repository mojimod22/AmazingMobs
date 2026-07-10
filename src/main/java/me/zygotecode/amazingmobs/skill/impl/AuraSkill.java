package me.zygotecode.amazingmobs.skill.impl;

import me.zygotecode.amazingmobs.skill.AbstractSkill;
import me.zygotecode.amazingmobs.skill.SkillContext;
import me.zygotecode.amazingmobs.skill.SkillType;
import me.zygotecode.amazingmobs.util.Fx;
import me.zygotecode.amazingmobs.util.Resolvers;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Super-Saiyan aura. Applies a sustained buff (default STRENGTH, with its ambient potion swirl) and
 * draws a rising, tapering column of bright particles around the caster — biased upward to read as a
 * power aura. params: {@code effect} (strength), {@code amplifier} (1), {@code color} (hex,
 * default gold), {@code particle} (dust|<name>), {@code count} (28), {@code radius} (0.6),
 * {@code height} (auto), {@code size} (dust scale 1.6); trigger {@code duration} = buff length.
 */
public final class AuraSkill extends AbstractSkill {

    public AuraSkill() { super("aura", SkillType.UTILITY); }

    @Override
    public void cast(SkillContext ctx) {
        LivingEntity self = ctx.caster();
        int dur = (int) Math.max(20, ctx.trigger().durationTicks());

        String eff = str(ctx, "effect", "strength");
        if (eff != null && !eff.isBlank() && !eff.equalsIgnoreCase("none")) {
            PotionEffectType pet = Resolvers.effect(eff);
            if (pet != null) self.addPotionEffect(new PotionEffect(pet, dur, Math.max(0, i(ctx, "amplifier", 1)), true, true, true));
        }
        emitAura(ctx, self);
        Fx.sound(self.getLocation(), str(ctx, "sound", "entity_illusioner_prepare_mirror"), 1f, 0.7f);
    }

    private void emitAura(SkillContext ctx, LivingEntity self) {
        Location base = self.getLocation();
        if (base.getWorld() == null) return;
        int count = Math.max(6, i(ctx, "count", 28));
        double radius = d(ctx, "radius", 0.6);
        double height = d(ctx, "height", self.getHeight() + 0.6);
        String particle = str(ctx, "particle", "dust");

        boolean dust = particle.equalsIgnoreCase("dust");
        Particle p = dust ? Particle.DUST : Resolvers.particle(particle, Particle.DUST);
        Particle.DustOptions dustOpts = dust ? new Particle.DustOptions(parseColor(str(ctx, "color", "#FFEB3B")),
                (float) d(ctx, "size", 1.6)) : null;

        for (int n = 0; n < count; n++) {
            double t = (n + ctx.rng().nextDouble()) / count;       // 0..1 up the column
            double y = t * height;
            double r = radius * (1.0 - 0.55 * t);                   // taper toward the top
            double ang = ctx.rng().rangeDouble(0, Math.PI * 2);
            Location at = base.clone().add(Math.cos(ang) * r, y, Math.sin(ang) * r);
            if (dust) base.getWorld().spawnParticle(Particle.DUST, at, 1, 0, 0.04, 0, 0, dustOpts);
            else base.getWorld().spawnParticle(p, at, 1, 0, 0.05, 0, 0.01); // slight upward drift
        }
    }

    private static Color parseColor(String hex) {
        try {
            String h = hex.startsWith("#") ? hex.substring(1) : hex;
            int rgb = Integer.parseInt(h, 16);
            return Color.fromRGB((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF);
        } catch (Exception e) {
            return Color.fromRGB(0xFF, 0xEB, 0x3B); // gold
        }
    }
}
