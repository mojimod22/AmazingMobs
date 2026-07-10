package me.zygotecode.amazingmobs.skill;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * When/whether a skill may fire and who it targets. Pure data + the gating predicate
 * {@link #conditionsMet}. Cooldown/warmup *state* lives in {@link SkillInstance}, not here.
 */
public final class TriggerSpec {

    private final Set<TriggerType> triggers;
    private final long cooldownTicks;
    private final long warmupTicks;
    private final double chance;        // 0..1 roll each eligible attempt
    private final double minRange;      // min distance to target (blocks)
    private final double maxRange;      // max distance to target; <=0 => unbounded
    private final double radius;        // AoE radius
    private final long durationTicks;   // effect duration
    private final TargetRule targetRule;
    private final Set<String> phases;   // restrict to these phase ids; empty => any
    private final double minHealthPct;  // caster health fraction window
    private final double maxHealthPct;

    private TriggerSpec(Builder b) {
        this.triggers = b.triggers.isEmpty() ? Set.of(TriggerType.TICK) : Set.copyOf(b.triggers);
        this.cooldownTicks = b.cooldownTicks;
        this.warmupTicks = b.warmupTicks;
        this.chance = b.chance;
        this.minRange = b.minRange;
        this.maxRange = b.maxRange;
        this.radius = b.radius;
        this.durationTicks = b.durationTicks;
        this.targetRule = b.targetRule;
        this.phases = b.phases == null ? Set.of() : Set.copyOf(b.phases);
        this.minHealthPct = b.minHealthPct;
        this.maxHealthPct = b.maxHealthPct;
    }

    public Set<TriggerType> triggers() { return triggers; }
    public boolean hasTrigger(TriggerType t) { return triggers.contains(t); }
    public long cooldownTicks() { return cooldownTicks; }
    public long warmupTicks() { return warmupTicks; }
    public double chance() { return chance; }
    public double minRange() { return minRange; }
    public double maxRange() { return maxRange; }
    public double radius() { return radius; }
    public long durationTicks() { return durationTicks; }
    public TargetRule targetRule() { return targetRule; }
    public Set<String> phases() { return phases; }
    public double minHealthPct() { return minHealthPct; }
    public double maxHealthPct() { return maxHealthPct; }

    /**
     * Non-cooldown gating: health window, phase restriction, and distance-to-target. Cooldown,
     * warmup and the random chance roll are handled by {@link SkillInstance}.
     *
     * @param healthPct   caster health fraction 0..1
     * @param phaseId     current phase id, or null
     * @param distance    distance to the chosen target, or -1 if not applicable
     */
    public boolean conditionsMet(double healthPct, String phaseId, double distance) {
        if (healthPct < minHealthPct || healthPct > maxHealthPct) return false;
        if (!phases.isEmpty() && (phaseId == null || !phases.contains(phaseId))) return false;
        if (distance >= 0) {
            if (distance < minRange) return false;
            if (maxRange > 0 && distance > maxRange) return false;
        }
        return true;
    }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private final Set<TriggerType> triggers = new LinkedHashSet<>();
        private long cooldownTicks = 100;
        private long warmupTicks = 0;
        private double chance = 1.0;
        private double minRange = 0;
        private double maxRange = 0;
        private double radius = 4.0;
        private long durationTicks = 60;
        private TargetRule targetRule = TargetRule.TARGET;
        private Set<String> phases;
        private double minHealthPct = 0.0;
        private double maxHealthPct = 1.0;

        public Builder trigger(TriggerType t) { this.triggers.add(t); return this; }
        public Builder cooldownTicks(long v) { this.cooldownTicks = v; return this; }
        public Builder warmupTicks(long v) { this.warmupTicks = v; return this; }
        public Builder chance(double v) { this.chance = v; return this; }
        public Builder minRange(double v) { this.minRange = v; return this; }
        public Builder maxRange(double v) { this.maxRange = v; return this; }
        public Builder radius(double v) { this.radius = v; return this; }
        public Builder durationTicks(long v) { this.durationTicks = v; return this; }
        public Builder targetRule(TargetRule v) { this.targetRule = v; return this; }
        public Builder phases(Set<String> v) { this.phases = v; return this; }
        public Builder minHealthPct(double v) { this.minHealthPct = v; return this; }
        public Builder maxHealthPct(double v) { this.maxHealthPct = v; return this; }
        public TriggerSpec build() { return new TriggerSpec(this); }
    }
}
