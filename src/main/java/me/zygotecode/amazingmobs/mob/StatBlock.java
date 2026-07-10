package me.zygotecode.amazingmobs.mob;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Immutable combat/attribute stats for a mob. Pure (no Bukkit) so it is unit-testable; the runtime
 * layer applies these onto entity {@code Attribute}s at spawn.
 *
 * <p>A value of {@code -1} for {@link #movementSpeed} / {@link #followRange} means "leave the
 * vanilla default" (those attributes vary per entity type, so blindly overriding them is wrong).</p>
 */
public final class StatBlock {

    private final double health;
    private final double attackDamage;
    private final double movementSpeed;       // -1 => keep vanilla
    private final double knockbackResistance; // 0..1
    private final double armor;               // 0..30
    private final double armorToughness;
    private final double followRange;         // -1 => keep vanilla
    private final double attackKnockback;
    private final double scale;               // 1.0 = normal size
    private final double regenPerSecond;      // custom regen (hearts/sec), 0 = none
    private final double critChance;          // 0..1
    private final double critMultiplier;      // damage multiplier on crit
    private final double maxAbsorption;       // bonus "yellow heart" buffer
    private final boolean fireImmune;
    private final boolean fallImmune;
    private final boolean drownImmune;
    private final boolean knockbackImmune;    // hard cancel of knockback events
    private final Map<String, Double> damageMultipliers; // cause/category -> taken multiplier

    private StatBlock(Builder b) {
        this.health = b.health;
        this.attackDamage = b.attackDamage;
        this.movementSpeed = b.movementSpeed;
        this.knockbackResistance = b.knockbackResistance;
        this.armor = b.armor;
        this.armorToughness = b.armorToughness;
        this.followRange = b.followRange;
        this.attackKnockback = b.attackKnockback;
        this.scale = b.scale;
        this.regenPerSecond = b.regenPerSecond;
        this.critChance = b.critChance;
        this.critMultiplier = b.critMultiplier;
        this.maxAbsorption = b.maxAbsorption;
        this.fireImmune = b.fireImmune;
        this.fallImmune = b.fallImmune;
        this.drownImmune = b.drownImmune;
        this.knockbackImmune = b.knockbackImmune;
        this.damageMultipliers = Collections.unmodifiableMap(new LinkedHashMap<>(b.damageMultipliers));
    }

    public double health() { return health; }
    public double attackDamage() { return attackDamage; }
    public double movementSpeed() { return movementSpeed; }
    public double knockbackResistance() { return knockbackResistance; }
    public double armor() { return armor; }
    public double armorToughness() { return armorToughness; }
    public double followRange() { return followRange; }
    public double attackKnockback() { return attackKnockback; }
    public double scale() { return scale; }
    public double regenPerSecond() { return regenPerSecond; }
    public double critChance() { return critChance; }
    public double critMultiplier() { return critMultiplier; }
    public double maxAbsorption() { return maxAbsorption; }
    public boolean fireImmune() { return fireImmune; }
    public boolean fallImmune() { return fallImmune; }
    public boolean drownImmune() { return drownImmune; }
    public boolean knockbackImmune() { return knockbackImmune; }
    public Map<String, Double> damageMultipliers() { return damageMultipliers; }

    public boolean overridesMovementSpeed() { return movementSpeed >= 0; }
    public boolean overridesFollowRange() { return followRange >= 0; }

    /**
     * Damage-taken multiplier for a damage cause. Looks up the exact cause name first, then a
     * broader {@code category} alias (e.g. "fire", "explosion", "magic", "projectile", "melee"),
     * then a global {@code "all"} key. Returns {@code 1.0} (no change) if nothing matches.
     */
    public double damageMultiplier(String causeName, String category) {
        if (!damageMultipliers.isEmpty()) {
            Double exact = causeName == null ? null : damageMultipliers.get(causeName.toLowerCase(Locale.ROOT));
            if (exact != null) return exact;
            Double cat = category == null ? null : damageMultipliers.get(category.toLowerCase(Locale.ROOT));
            if (cat != null) return cat;
            Double all = damageMultipliers.get("all");
            if (all != null) return all;
        }
        return 1.0;
    }

    /** Returns a copy with every scalable stat multiplied (used for player/difficulty scaling & phases). */
    public StatBlock scaledBy(double healthMul, double damageMul, double speedMul) {
        Builder b = toBuilder();
        b.health = this.health * healthMul;
        b.attackDamage = this.attackDamage * damageMul;
        if (overridesMovementSpeed()) b.movementSpeed = this.movementSpeed * speedMul;
        if (maxAbsorption > 0) b.maxAbsorption = this.maxAbsorption * healthMul;
        return b.build();
    }

    public Builder toBuilder() {
        Builder b = new Builder();
        b.health = health; b.attackDamage = attackDamage; b.movementSpeed = movementSpeed;
        b.knockbackResistance = knockbackResistance; b.armor = armor; b.armorToughness = armorToughness;
        b.followRange = followRange; b.attackKnockback = attackKnockback; b.scale = scale;
        b.regenPerSecond = regenPerSecond; b.critChance = critChance; b.critMultiplier = critMultiplier;
        b.maxAbsorption = maxAbsorption; b.fireImmune = fireImmune; b.fallImmune = fallImmune;
        b.drownImmune = drownImmune; b.knockbackImmune = knockbackImmune;
        b.damageMultipliers = new LinkedHashMap<>(damageMultipliers);
        return b;
    }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private double health = 20;
        private double attackDamage = 3;
        private double movementSpeed = -1;
        private double knockbackResistance = 0;
        private double armor = 0;
        private double armorToughness = 0;
        private double followRange = -1;
        private double attackKnockback = 0;
        private double scale = 1.0;
        private double regenPerSecond = 0;
        private double critChance = 0;
        private double critMultiplier = 1.5;
        private double maxAbsorption = 0;
        private boolean fireImmune = false;
        private boolean fallImmune = false;
        private boolean drownImmune = false;
        private boolean knockbackImmune = false;
        private Map<String, Double> damageMultipliers = new LinkedHashMap<>();

        public Builder health(double v) { this.health = v; return this; }
        public Builder attackDamage(double v) { this.attackDamage = v; return this; }
        public Builder movementSpeed(double v) { this.movementSpeed = v; return this; }
        public Builder knockbackResistance(double v) { this.knockbackResistance = v; return this; }
        public Builder armor(double v) { this.armor = v; return this; }
        public Builder armorToughness(double v) { this.armorToughness = v; return this; }
        public Builder followRange(double v) { this.followRange = v; return this; }
        public Builder attackKnockback(double v) { this.attackKnockback = v; return this; }
        public Builder scale(double v) { this.scale = v; return this; }
        public Builder regenPerSecond(double v) { this.regenPerSecond = v; return this; }
        public Builder critChance(double v) { this.critChance = v; return this; }
        public Builder critMultiplier(double v) { this.critMultiplier = v; return this; }
        public Builder maxAbsorption(double v) { this.maxAbsorption = v; return this; }
        public Builder fireImmune(boolean v) { this.fireImmune = v; return this; }
        public Builder fallImmune(boolean v) { this.fallImmune = v; return this; }
        public Builder drownImmune(boolean v) { this.drownImmune = v; return this; }
        public Builder knockbackImmune(boolean v) { this.knockbackImmune = v; return this; }
        public Builder damageMultiplier(String key, double mul) {
            if (key != null) this.damageMultipliers.put(key.toLowerCase(Locale.ROOT), mul);
            return this;
        }
        public StatBlock build() { return new StatBlock(this); }
    }
}
