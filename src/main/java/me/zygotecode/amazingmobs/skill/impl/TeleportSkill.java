package me.zygotecode.amazingmobs.skill.impl;

import me.zygotecode.amazingmobs.skill.AbstractSkill;
import me.zygotecode.amazingmobs.skill.SkillContext;
import me.zygotecode.amazingmobs.skill.SkillType;
import me.zygotecode.amazingmobs.util.Fx;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;

import java.util.Locale;

/**
 * Short-range blink. params: {@code mode} (to_target | behind_target | away_from_target |
 * random_near), {@code distance}. Aborts if the destination is unsafe (inside solid blocks).
 */
public final class TeleportSkill extends AbstractSkill {

    public TeleportSkill(String id) { super(id, SkillType.MOVEMENT); }

    @Override
    public void cast(SkillContext ctx) {
        LivingEntity caster = ctx.caster();
        LivingEntity target = ctx.primaryTarget();
        if (target == null && !ctx.targets().isEmpty()) target = ctx.targets().get(0);

        String mode = str(ctx, "mode", "to_target").toLowerCase(Locale.ROOT);
        double dist = d(ctx, "distance", 4);
        Location from = caster.getLocation();
        Location dest = null;

        if (target != null && target.isValid()) {
            Location tl = target.getLocation();
            Vector look = tl.getDirection().setY(0).normalize();
            switch (mode) {
                case "behind_target" -> dest = tl.clone().subtract(look.clone().multiply(dist));
                case "away_from_target" -> {
                    Vector away = from.toVector().subtract(tl.toVector()).setY(0);
                    if (away.lengthSquared() < 1.0E-4) away = look.clone();
                    dest = from.clone().add(away.normalize().multiply(dist));
                }
                default -> { // to_target
                    Vector toward = tl.toVector().subtract(from.toVector()).setY(0);
                    if (toward.lengthSquared() < 1.0E-4) toward = look.clone();
                    dest = tl.clone().subtract(toward.normalize().multiply(Math.min(dist, 2)));
                }
            }
        } else {
            double a = ctx.rng().rangeDouble(0, Math.PI * 2);
            dest = from.clone().add(Math.cos(a) * dist, 0, Math.sin(a) * dist);
        }
        if (dest == null) return;

        dest.setYaw(from.getYaw());
        dest.setPitch(from.getPitch());
        if (!safe(dest)) return;

        Fx.particle(from, "portal", 30, 0.4, 0.8, 0.4, 0.1);
        Fx.sound(from, str(ctx, "sound", "entity_enderman_teleport"), 1f, 1f);
        caster.teleport(dest);
        Fx.particle(dest, "portal", 30, 0.4, 0.8, 0.4, 0.1);
    }

    private boolean safe(Location loc) {
        if (loc.getWorld() == null) return false;
        return loc.getBlock().isPassable() && loc.clone().add(0, 1, 0).getBlock().isPassable();
    }
}
