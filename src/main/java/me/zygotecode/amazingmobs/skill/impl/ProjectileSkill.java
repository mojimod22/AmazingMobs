package me.zygotecode.amazingmobs.skill.impl;

import me.zygotecode.amazingmobs.skill.AbstractSkill;
import me.zygotecode.amazingmobs.skill.SkillContext;
import me.zygotecode.amazingmobs.skill.SkillType;
import me.zygotecode.amazingmobs.util.Fx;
import org.bukkit.Location;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.DragonFireball;
import org.bukkit.entity.Explosive;
import org.bukkit.entity.LargeFireball;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.SmallFireball;
import org.bukkit.entity.Snowball;
import org.bukkit.entity.WitherSkull;
import org.bukkit.util.Vector;

import java.util.Locale;

/**
 * Generic ranged attack. One class backs the {@code fireball}, {@code wither_skull},
 * {@code snowball}, {@code arrow_volley} and {@code projectile_burst} skill ids — the only
 * difference is the default projectile type, which config can still override.
 *
 * <p>params: {@code type}, {@code count}, {@code speed}, {@code spread}, {@code yield},
 * {@code incendiary}, {@code charged}, {@code damage}, {@code particle}/{@code sound}.</p>
 */
public final class ProjectileSkill extends AbstractSkill {

    private final String defaultType;

    public ProjectileSkill(String id, String defaultType) {
        super(id, SkillType.OFFENSE);
        this.defaultType = defaultType;
    }

    @Override
    public void cast(SkillContext ctx) {
        LivingEntity caster = ctx.caster();
        LivingEntity target = ctx.primaryTarget();
        if (target == null && ctx.targets().isEmpty()) return;
        if (target == null) target = ctx.targets().get(0);

        String type = str(ctx, "type", defaultType).toLowerCase(Locale.ROOT);
        int count = Math.max(1, i(ctx, "count", 1));
        double speed = d(ctx, "speed", 1.25);
        double spread = d(ctx, "spread", 0.08);
        double yield = d(ctx, "yield", 1.0);
        boolean incendiary = flag(ctx, "incendiary", false);
        boolean charged = flag(ctx, "charged", false);
        double dmg = d(ctx, "damage", 0);

        Location eye = caster.getEyeLocation();
        Vector base = target.getEyeLocation().toVector().subtract(eye.toVector()).normalize();

        for (int n = 0; n < count; n++) {
            Vector vel = base.clone();
            if (spread > 0) {
                vel.add(new Vector(
                        (ctx.rng().nextDouble() - 0.5) * spread,
                        (ctx.rng().nextDouble() - 0.5) * spread,
                        (ctx.rng().nextDouble() - 0.5) * spread));
            }
            vel.normalize().multiply(speed);
            launch(caster, type, vel, yield, incendiary, charged, dmg);
        }
        Fx.sound(eye, str(ctx, "sound", soundFor(type)), (float) d(ctx, "sound-volume", 1.0), (float) d(ctx, "sound-pitch", 1.0));
        Fx.particle(eye, str(ctx, "particle", null), i(ctx, "particle-count", 8), 0.2, 0.2, 0.2, 0.02);
    }

    private void launch(LivingEntity caster, String type, Vector vel,
                        double yield, boolean incendiary, boolean charged, double dmg) {
        Class<? extends Projectile> cls = switch (type) {
            case "small_fireball" -> SmallFireball.class;
            case "fireball", "large_fireball" -> LargeFireball.class;
            case "wither_skull" -> WitherSkull.class;
            case "dragon_fireball" -> DragonFireball.class;
            case "snowball" -> Snowball.class;
            case "arrow" -> Arrow.class;
            default -> LargeFireball.class;
        };
        Projectile proj = caster.launchProjectile(cls, vel);
        if (proj instanceof Explosive ex) {
            ex.setYield((float) yield);
            ex.setIsIncendiary(incendiary);
        }
        if (proj instanceof WitherSkull ws) ws.setCharged(charged);
        if (proj instanceof Arrow a && dmg > 0) a.setDamage(dmg);
        proj.setVelocity(vel); // ensure speed honoured regardless of default launch speed
    }

    private static String soundFor(String type) {
        return switch (type) {
            case "wither_skull" -> "entity_wither_shoot";
            case "snowball" -> "entity_snowball_throw";
            case "arrow" -> "entity_arrow_shoot";
            default -> "entity_blaze_shoot";
        };
    }
}
