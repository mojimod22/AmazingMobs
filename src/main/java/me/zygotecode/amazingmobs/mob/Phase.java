package me.zygotecode.amazingmobs.mob;

import java.util.List;

/**
 * A health-threshold phase. When a mob's health fraction drops to {@code <= thresholdPct}, the
 * highest-priority not-yet-entered phase activates: stats are re-multiplied, listed skills are
 * toggled, and feedback (message/sound/particle) fires once. Pure data.
 *
 * <p>Phases are evaluated from highest threshold to lowest, so define them e.g. 0.66, 0.33, 0.1.</p>
 */
public final class Phase {

    private final String id;
    private final double thresholdPct;   // 0..1 health fraction
    private final double damageMultiplier;
    private final double speedMultiplier;
    private final double defenseMultiplier;
    private final List<String> enableSkills;
    private final List<String> disableSkills;
    private final String message;        // MiniMessage broadcast on enter (nullable)
    private final String sound;          // sound name on enter (nullable)
    private final String particle;       // particle name burst on enter (nullable)

    private Phase(Builder b) {
        this.id = b.id;
        this.thresholdPct = b.thresholdPct;
        this.damageMultiplier = b.damageMultiplier;
        this.speedMultiplier = b.speedMultiplier;
        this.defenseMultiplier = b.defenseMultiplier;
        this.enableSkills = b.enableSkills == null ? List.of() : List.copyOf(b.enableSkills);
        this.disableSkills = b.disableSkills == null ? List.of() : List.copyOf(b.disableSkills);
        this.message = b.message;
        this.sound = b.sound;
        this.particle = b.particle;
    }

    public String id() { return id; }
    public double thresholdPct() { return thresholdPct; }
    public double damageMultiplier() { return damageMultiplier; }
    public double speedMultiplier() { return speedMultiplier; }
    public double defenseMultiplier() { return defenseMultiplier; }
    public List<String> enableSkills() { return enableSkills; }
    public List<String> disableSkills() { return disableSkills; }
    public String message() { return message; }
    public String sound() { return sound; }
    public String particle() { return particle; }

    public static Builder builder(String id) { return new Builder(id); }

    public static final class Builder {
        private final String id;
        private double thresholdPct = 0.5;
        private double damageMultiplier = 1.0;
        private double speedMultiplier = 1.0;
        private double defenseMultiplier = 1.0;
        private List<String> enableSkills;
        private List<String> disableSkills;
        private String message, sound, particle;

        public Builder(String id) { this.id = id; }
        public Builder thresholdPct(double v) { this.thresholdPct = v; return this; }
        public Builder damageMultiplier(double v) { this.damageMultiplier = v; return this; }
        public Builder speedMultiplier(double v) { this.speedMultiplier = v; return this; }
        public Builder defenseMultiplier(double v) { this.defenseMultiplier = v; return this; }
        public Builder enableSkills(List<String> v) { this.enableSkills = v; return this; }
        public Builder disableSkills(List<String> v) { this.disableSkills = v; return this; }
        public Builder message(String v) { this.message = v; return this; }
        public Builder sound(String v) { this.sound = v; return this; }
        public Builder particle(String v) { this.particle = v; return this; }
        public Phase build() { return new Phase(this); }
    }
}
