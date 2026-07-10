package me.zygotecode.amazingmobs.mob;

import me.zygotecode.amazingmobs.util.IntRange;

import java.util.List;

/**
 * Behavioural profile. Pure data consumed by the runtime mob controller + combat listener.
 *
 * <p>Implemented through Bukkit attributes + a throttled controller (target re-selection, leashing,
 * retreat, reinforcement, day-burn) rather than NMS goal injection — see DESIGN.md §8/§12.</p>
 */
public final class AiProfile {

    public enum Aggression { PASSIVE, DEFENSIVE, AGGRESSIVE }
    public enum MovementStyle { CHASE, KITE, STRAFE, AMBUSH, STATIONARY }
    /** Ordered target preferences; first satisfiable wins. */
    public enum TargetMode { NEAREST, LOWEST_HEALTH, HIGHEST_HEALTH, RANDOM, MOST_ARMORED, LEAST_ARMORED }

    public static final AiProfile DEFAULT = builder().build();

    private final Aggression aggression;
    private final MovementStyle movement;
    private final List<TargetMode> targetPriority;
    private final boolean targetPlayersOnly;
    private final double aggroRange;            // -1 => use stat follow range
    private final double leashRange;            // 0 => no leash
    private final double retreatHealthPct;      // 0 => never retreat
    private final double kiteDistance;          // for KITE/STRAFE
    private final double chaseSpeed;            // navigation speed multiplier when pursuing
    private final boolean clearVanillaGoals;    // strip vanilla MOVE/TARGET/LOOK goals → full controller puppet
    private final boolean burnsInDay;
    private final boolean callReinforcements;
    private final String reinforcementMobId;    // nullable; null => summon a copy
    private final IntRange reinforcementCount;
    private final double reinforcementCooldownSec;

    private AiProfile(Builder b) {
        this.aggression = b.aggression;
        this.movement = b.movement;
        this.targetPriority = b.targetPriority == null || b.targetPriority.isEmpty()
                ? List.of(TargetMode.NEAREST) : List.copyOf(b.targetPriority);
        this.targetPlayersOnly = b.targetPlayersOnly;
        this.aggroRange = b.aggroRange;
        this.leashRange = b.leashRange;
        this.retreatHealthPct = b.retreatHealthPct;
        this.kiteDistance = b.kiteDistance;
        this.chaseSpeed = b.chaseSpeed;
        this.clearVanillaGoals = b.clearVanillaGoals;
        this.burnsInDay = b.burnsInDay;
        this.callReinforcements = b.callReinforcements;
        this.reinforcementMobId = b.reinforcementMobId;
        this.reinforcementCount = b.reinforcementCount;
        this.reinforcementCooldownSec = b.reinforcementCooldownSec;
    }

    public Aggression aggression() { return aggression; }
    public MovementStyle movement() { return movement; }
    public List<TargetMode> targetPriority() { return targetPriority; }
    public boolean targetPlayersOnly() { return targetPlayersOnly; }
    public double aggroRange() { return aggroRange; }
    public double leashRange() { return leashRange; }
    public double retreatHealthPct() { return retreatHealthPct; }
    public double kiteDistance() { return kiteDistance; }
    public double chaseSpeed() { return chaseSpeed; }
    public boolean clearVanillaGoals() { return clearVanillaGoals; }
    public boolean burnsInDay() { return burnsInDay; }
    public boolean callReinforcements() { return callReinforcements; }
    public String reinforcementMobId() { return reinforcementMobId; }
    public IntRange reinforcementCount() { return reinforcementCount; }
    public double reinforcementCooldownSec() { return reinforcementCooldownSec; }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private Aggression aggression = Aggression.AGGRESSIVE;
        private MovementStyle movement = MovementStyle.CHASE;
        private List<TargetMode> targetPriority = List.of(TargetMode.NEAREST);
        private boolean targetPlayersOnly = true;
        private double aggroRange = -1;
        private double leashRange = 0;
        private double retreatHealthPct = 0;
        private double kiteDistance = 8.0;
        private double chaseSpeed = 1.15;
        private boolean clearVanillaGoals = false;
        private boolean burnsInDay = false;
        private boolean callReinforcements = false;
        private String reinforcementMobId = null;
        private IntRange reinforcementCount = new IntRange(1, 2);
        private double reinforcementCooldownSec = 15;

        public Builder aggression(Aggression v) { this.aggression = v; return this; }
        public Builder movement(MovementStyle v) { this.movement = v; return this; }
        public Builder targetPriority(List<TargetMode> v) { this.targetPriority = v; return this; }
        public Builder targetPlayersOnly(boolean v) { this.targetPlayersOnly = v; return this; }
        public Builder aggroRange(double v) { this.aggroRange = v; return this; }
        public Builder leashRange(double v) { this.leashRange = v; return this; }
        public Builder retreatHealthPct(double v) { this.retreatHealthPct = v; return this; }
        public Builder kiteDistance(double v) { this.kiteDistance = v; return this; }
        public Builder chaseSpeed(double v) { this.chaseSpeed = v; return this; }
        public Builder clearVanillaGoals(boolean v) { this.clearVanillaGoals = v; return this; }
        public Builder burnsInDay(boolean v) { this.burnsInDay = v; return this; }
        public Builder callReinforcements(boolean v) { this.callReinforcements = v; return this; }
        public Builder reinforcementMobId(String v) { this.reinforcementMobId = v; return this; }
        public Builder reinforcementCount(IntRange v) { this.reinforcementCount = v; return this; }
        public Builder reinforcementCooldownSec(double v) { this.reinforcementCooldownSec = v; return this; }
        public AiProfile build() { return new AiProfile(this); }
    }
}
