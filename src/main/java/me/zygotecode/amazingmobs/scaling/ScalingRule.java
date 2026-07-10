package me.zygotecode.amazingmobs.scaling;

import me.zygotecode.amazingmobs.mob.StatBlock;
import me.zygotecode.amazingmobs.util.Numbers;

/**
 * Linear per-player + difficulty scaling, with a player cap so big servers don't produce
 * unkillable mobs. Pure & unit-testable.
 *
 * <p>For {@code players} participants and a difficulty multiplier {@code diff}, a stat is scaled by
 * {@code diff * (1 + perPlayerPct * (min(players, cap) - 1))}. With all-zero per-player values and
 * {@code diff == 1} this is the identity, so scaling is opt-in.</p>
 */
public final class ScalingRule {

    public static final ScalingRule NONE = new ScalingRule(0, 0, 0, 8);

    private final double healthPerPlayerPct;
    private final double damagePerPlayerPct;
    private final double speedPerPlayerPct;
    private final int maxConsideredPlayers;

    public ScalingRule(double healthPerPlayerPct, double damagePerPlayerPct,
                       double speedPerPlayerPct, int maxConsideredPlayers) {
        this.healthPerPlayerPct = healthPerPlayerPct;
        this.damagePerPlayerPct = damagePerPlayerPct;
        this.speedPerPlayerPct = speedPerPlayerPct;
        this.maxConsideredPlayers = Math.max(1, maxConsideredPlayers);
    }

    public boolean isIdentity() {
        return healthPerPlayerPct == 0 && damagePerPlayerPct == 0 && speedPerPlayerPct == 0;
    }

    public double healthPerPlayerPct() { return healthPerPlayerPct; }
    public double damagePerPlayerPct() { return damagePerPlayerPct; }
    public double speedPerPlayerPct() { return speedPerPlayerPct; }
    public int maxConsideredPlayers() { return maxConsideredPlayers; }

    private double factor(double perPlayerPct, int players, double difficulty) {
        int effective = Numbers.clamp(players, 1, maxConsideredPlayers);
        return difficulty * (1.0 + perPlayerPct * (effective - 1));
    }

    /** @return a scaled copy of {@code base} for the given player count and difficulty multiplier. */
    public StatBlock apply(StatBlock base, int players, double difficulty) {
        if (isIdentity() && difficulty == 1.0) return base;
        return base.scaledBy(
                factor(healthPerPlayerPct, players, difficulty),
                factor(damagePerPlayerPct, players, difficulty),
                factor(speedPerPlayerPct, players, 1.0)); // speed doesn't take the difficulty mult
    }
}
