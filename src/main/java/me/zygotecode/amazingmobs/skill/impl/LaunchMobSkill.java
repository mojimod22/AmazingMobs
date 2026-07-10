package me.zygotecode.amazingmobs.skill.impl;

import me.zygotecode.amazingmobs.skill.AbstractSkill;
import me.zygotecode.amazingmobs.skill.SkillContext;
import me.zygotecode.amazingmobs.skill.SkillType;
import me.zygotecode.amazingmobs.skill.Targeting;
import me.zygotecode.amazingmobs.util.Fx;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

/**
 * Hurls another mob as a living projectile. Summons {@code mob}, flings it at the target (straight,
 * arced or weakly homing), then on impact (ground / player contact / TTL) deals AoE damage +
 * knockback and either removes it ({@code consume-on-impact}) or "lands" it as a live attacker.
 *
 * <p>params: {@code mob} (required), {@code speed}, {@code arc}, {@code homing}, {@code gravity},
 * {@code damage}, {@code knockback}, {@code consume-on-impact}, {@code impact-summon},
 * {@code impact-summon-count}; trigger {@code radius}=impact radius, {@code duration}=TTL.</p>
 */
public final class LaunchMobSkill extends AbstractSkill {

    public LaunchMobSkill() { super("launch_mob", SkillType.SUMMON); }

    @Override
    public void cast(SkillContext ctx) {
        LivingEntity caster = ctx.caster();
        LivingEntity target = ctx.primaryTarget();
        if (target == null && !ctx.targets().isEmpty()) target = ctx.targets().get(0);

        String mobId = str(ctx, "mob", null);
        if (mobId == null || mobId.isBlank()) return;

        Location from = caster.getEyeLocation();
        LivingEntity proj = ctx.summon(mobId, from);
        if (proj == null) return;

        proj.setAI(false);
        boolean gravity = flag(ctx, "gravity", true);
        proj.setGravity(gravity);

        Vector dir = (target != null && target.isValid())
                ? target.getEyeLocation().toVector().subtract(from.toVector()).normalize()
                : from.getDirection();
        if (flag(ctx, "arc", false)) dir.setY(dir.getY() + 0.35);
        final double speed = d(ctx, "speed", 1.4);
        proj.setVelocity(dir.normalize().multiply(speed));
        Fx.sound(from, str(ctx, "sound", "entity_wither_shoot"), 1f, 1f);

        final LivingEntity launcher = caster;
        final double impact = Math.max(1.5, ctx.trigger().radius());
        final double dmg = d(ctx, "damage", 6);
        final double knockback = d(ctx, "knockback", 0.6);
        final boolean homing = flag(ctx, "homing", false);
        final boolean consume = flag(ctx, "consume-on-impact", true);
        final String trail = str(ctx, "trail-particle", "crit");
        final String impactParticle = str(ctx, "impact-particle", "explosion");
        final String impactSound = str(ctx, "impact-sound", "entity_generic_explode");
        final String impactSummon = str(ctx, "impact-summon", null);
        final int impactSummonCount = i(ctx, "impact-summon-count", 0);
        final int ttl = (int) Math.max(20, ctx.trigger().durationTicks() > 0 ? ctx.trigger().durationTicks() : 100);

        new BukkitRunnable() {
            int t = 0;
            @Override public void run() {
                if (!proj.isValid()) { cancel(); return; }
                t += 2;
                if (homing) {
                    LivingEntity tp = Targeting.nearestPlayer(proj, 24);
                    if (tp != null) {
                        Vector h = tp.getEyeLocation().toVector().subtract(proj.getLocation().toVector()).normalize().multiply(0.3);
                        proj.setVelocity(proj.getVelocity().add(h).normalize().multiply(speed));
                    }
                }
                Fx.particle(proj.getLocation(), trail, 3, 0.05, 0.05, 0.05, 0.01);

                boolean hitPlayer = false;
                for (Player p : proj.getWorld().getPlayers()) {
                    if (p.getLocation().distanceSquared(proj.getLocation()) <= impact * impact) { hitPlayer = true; break; }
                }
                if (proj.isOnGround() || hitPlayer || t >= ttl) {
                    Location at = proj.getLocation();
                    Fx.particle(at, impactParticle, 1, 0, 0, 0, 0);
                    Fx.sound(at, impactSound, 1f, 1f);
                    for (Entity e : at.getWorld().getNearbyEntities(at, impact, impact, impact)) {
                        if (e instanceof LivingEntity le && !le.equals(proj) && le.isValid()) {
                            if (dmg > 0) le.damage(dmg, launcher);
                            Vector kb = le.getLocation().toVector().subtract(at.toVector());
                            if (kb.lengthSquared() < 1.0E-4) kb = new Vector(0, 1, 0);
                            le.setVelocity(le.getVelocity().add(kb.normalize().multiply(knockback).setY(0.35)));
                        }
                    }
                    if (impactSummon != null && impactSummonCount > 0) {
                        for (int n = 0; n < impactSummonCount; n++) ctx.summon(impactSummon, at);
                    }
                    if (consume) proj.remove();
                    else { proj.setAI(true); proj.setGravity(true); }
                    cancel();
                }
            }
        }.runTaskTimer(ctx.plugin(), 2, 2);
    }
}
