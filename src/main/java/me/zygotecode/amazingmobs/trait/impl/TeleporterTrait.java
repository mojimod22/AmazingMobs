package me.zygotecode.amazingmobs.trait.impl;

import me.zygotecode.amazingmobs.skill.Targeting;
import me.zygotecode.amazingmobs.trait.AbstractTrait;
import me.zygotecode.amazingmobs.trait.TraitContext;
import me.zygotecode.amazingmobs.util.Fx;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;

import java.util.Locale;

/**
 * Periodic short-range blink. {@code phase-walker} variant adds a brief RESISTANCE window on arrival.
 * params: {@code cooldown} (5s), {@code mode} (to_target|away), {@code distance} (3), {@code resistance}.
 */
public final class TeleporterTrait extends AbstractTrait {

    public TeleporterTrait(String id) { super(id); }

    @Override
    public void onTick(TraitContext c) {
        LivingEntity prey = Targeting.nearestPlayer(c.entity(), 30);
        if (prey == null || !ready(c, "5s")) return;
        double dist = d(c, "distance", 3);
        String mode = str(c, "mode", "to_target").toLowerCase(Locale.ROOT);
        Location from = c.origin();
        Location dest;
        Vector toward = prey.getLocation().toVector().subtract(from.toVector()).setY(0);
        if (toward.lengthSquared() < 1.0E-4) return;
        toward.normalize();
        if (mode.startsWith("away")) {
            dest = from.clone().add(toward.multiply(-dist));
        } else {
            dest = prey.getLocation().clone().subtract(toward.multiply(Math.min(dist, 2)));
        }
        dest.setYaw(from.getYaw()); dest.setPitch(from.getPitch());
        if (!dest.getBlock().isPassable() || !dest.clone().add(0, 1, 0).getBlock().isPassable()) return;
        Fx.particle(from, "portal", 25, 0.4, 0.8, 0.4, 0.1);
        c.entity().teleport(dest);
        Fx.particle(dest, "portal", 25, 0.4, 0.8, 0.4, 0.1);
        Fx.sound(dest, "entity_enderman_teleport", 1f, 1f);
        if (c.params().contains("resistance")) effect(c.entity(), "resistance", i(c, "resistance", 2), 30);
    }
}
