package me.zygotecode.amazingmobs.skill;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Resolves the set of entities a skill affects, given a {@link TargetRule}. Runtime helper. */
public final class Targeting {

    private Targeting() {}

    public static List<LivingEntity> resolve(LivingEntity caster, LivingEntity currentTarget,
                                             TargetRule rule, double radius, double maxRange,
                                             boolean playersOnly) {
        List<LivingEntity> out = new ArrayList<>();
        if (caster == null || !caster.isValid()) return out;

        switch (rule) {
            case SELF -> out.add(caster);
            case TARGET -> {
                if (valid(currentTarget)) out.add(currentTarget);
                else { LivingEntity p = nearestPlayer(caster, effectiveRange(maxRange, radius)); if (p != null) out.add(p); }
            }
            case NEAREST_PLAYER -> { LivingEntity p = nearestPlayer(caster, effectiveRange(maxRange, radius)); if (p != null) out.add(p); }
            case RANDOM_PLAYER -> {
                List<LivingEntity> players = playersInRange(caster, effectiveRange(maxRange, radius));
                if (!players.isEmpty()) out.add(players.get((int) (Math.floor(players.size() * pseudo(caster)))));
            }
            case LOWEST_HEALTH_PLAYER -> {
                List<LivingEntity> players = playersInRange(caster, effectiveRange(maxRange, radius));
                players.stream().min(Comparator.comparingDouble(LivingEntity::getHealth)).ifPresent(out::add);
            }
            case ALL_PLAYERS_IN_RADIUS -> out.addAll(playersInRange(caster, radius));
            case ALL_IN_RADIUS -> {
                for (Entity e : caster.getNearbyEntities(radius, radius, radius)) {
                    if (e instanceof LivingEntity le && valid(le)) {
                        if (playersOnly && !(le instanceof Player)) continue;
                        out.add(le);
                    }
                }
            }
        }
        return out;
    }

    public static LivingEntity nearestPlayer(LivingEntity caster, double range) {
        Player best = null;
        double bestSq = range * range;
        Location loc = caster.getLocation();
        for (Player p : caster.getWorld().getPlayers()) {
            if (!targetable(p)) continue;
            double d = p.getLocation().distanceSquared(loc);
            if (d <= bestSq) { bestSq = d; best = p; }
        }
        return best;
    }

    private static List<LivingEntity> playersInRange(LivingEntity caster, double range) {
        List<LivingEntity> out = new ArrayList<>();
        Location loc = caster.getLocation();
        double sq = range * range;
        for (Player p : caster.getWorld().getPlayers()) {
            if (targetable(p) && p.getLocation().distanceSquared(loc) <= sq) out.add(p);
        }
        return out;
    }

    private static boolean targetable(Player p) {
        return p.isValid() && !p.isDead()
                && p.getGameMode() != GameMode.CREATIVE
                && p.getGameMode() != GameMode.SPECTATOR;
    }

    private static boolean valid(LivingEntity e) {
        return e != null && e.isValid() && !e.isDead();
    }

    private static double effectiveRange(double maxRange, double radius) {
        return maxRange > 0 ? maxRange : Math.max(radius, 16);
    }

    /** Deterministic-ish 0..1 from entity tick/UUID — avoids needing a shared RNG here. */
    private static double pseudo(LivingEntity caster) {
        int h = caster.getUniqueId().hashCode() ^ caster.getTicksLived();
        return (Math.abs(h) % 1000) / 1000.0;
    }
}
