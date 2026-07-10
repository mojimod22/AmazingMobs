package me.zygotecode.amazingmobs.config;

import me.zygotecode.amazingmobs.util.Numbers;

/**
 * Typed view of {@code config.yml}. Performance knobs live here so a server owner can tune the
 * runtime cost (controller cadence, hard caps) without touching code.
 */
public final class PluginConfig {

    /** Latest config schema version this build understands. Bump when adding new top-level keys. */
    public static final int CURRENT_VERSION = 2;

    public final int configVersion;
    public final int mobControllerPeriod;  // ticks between mob controller passes
    public final int maxActiveMobs;        // hard cap on tracked custom mobs
    public final int hordeTickInterval;    // ticks between horde state-machine passes
    public final int maxConcurrentHordes;  // hard cap on simultaneous hordes
    public final boolean debug;
    public final boolean equipAllOnStart;  // heal/clear/kit every online player when a horde starts
    public final boolean weightEnabled;
    public final double weightBase, weightGoal, weightDecayTo;
    public final double weightDecayMinutes;
    public final boolean classesEnabled, allowClassChange;
    public final double classCooldownMultiplier;
    public final int minionBaseCap;
    public final double minionPerPrestige;
    public final boolean airburstEnabled;
    public final double airburstMinHeight;
    public final boolean airburstBreakBlocks;

    private PluginConfig(int configVersion, int mobControllerPeriod, int maxActiveMobs,
                         int hordeTickInterval, int maxConcurrentHordes, boolean debug,
                         boolean equipAllOnStart, boolean weightEnabled, double weightBase,
                         double weightGoal, double weightDecayTo, double weightDecayMinutes,
                         boolean classesEnabled, boolean allowClassChange, double classCooldownMultiplier,
                         int minionBaseCap, double minionPerPrestige,
                         boolean airburstEnabled, double airburstMinHeight, boolean airburstBreakBlocks) {
        this.configVersion = configVersion;
        this.mobControllerPeriod = mobControllerPeriod;
        this.maxActiveMobs = maxActiveMobs;
        this.hordeTickInterval = hordeTickInterval;
        this.maxConcurrentHordes = maxConcurrentHordes;
        this.debug = debug;
        this.equipAllOnStart = equipAllOnStart;
        this.weightEnabled = weightEnabled;
        this.weightBase = weightBase;
        this.weightGoal = weightGoal;
        this.weightDecayTo = weightDecayTo;
        this.weightDecayMinutes = weightDecayMinutes;
        this.classesEnabled = classesEnabled;
        this.allowClassChange = allowClassChange;
        this.classCooldownMultiplier = classCooldownMultiplier;
        this.minionBaseCap = minionBaseCap;
        this.minionPerPrestige = minionPerPrestige;
        this.airburstEnabled = airburstEnabled;
        this.airburstMinHeight = airburstMinHeight;
        this.airburstBreakBlocks = airburstBreakBlocks;
    }

    public static PluginConfig defaults() {
        return new PluginConfig(CURRENT_VERSION, 5, 300, 10, 3, false, true, true, 60, 175, 140, 10,
                true, true, 1.0, 6, 0.5, true, 5.0, true);
    }

    public static PluginConfig from(ConfigSection root) {
        ConfigSection perf = root.getSection("performance");
        ConfigSection horde = root.getSection("horde");
        ConfigSection weight = root.getSection("weight");
        ConfigSection classes = root.getSection("classes");
        ConfigSection airburst = root.getSection("airburst");
        return new PluginConfig(
                root.getInt("config-version", 1),
                Numbers.clamp(perf.getInt("mob-controller-period", 5), 1, 200),
                Numbers.clamp(perf.getInt("max-active-mobs", 300), 16, 10000),
                Numbers.clamp(perf.getInt("horde-tick-interval", 10), 1, 200),
                Numbers.clamp(perf.getInt("max-concurrent-hordes", 3), 1, 64),
                root.getBool("debug", false),
                horde.getBool("equip-all-players-on-start", true),
                weight.getBool("enabled", true),
                weight.getDouble("base-kg", 60),
                weight.getDouble("goal-kg", 175),
                weight.getDouble("decay-to-kg", 140),
                weight.getDouble("decay-minutes", 10),
                classes.getBool("enabled", true),
                classes.getBool("allow-changing", true),
                Numbers.clamp(classes.getDouble("cooldown-multiplier", 1.0), 0.1, 5.0),
                Numbers.clamp(classes.getInt("necromancer-minion-base-cap", 6), 1, 64),
                Numbers.clamp(classes.getDouble("necromancer-minion-per-prestige", 0.5), 0.0, 8.0),
                airburst.getBool("enabled", true),
                Numbers.clamp(airburst.getDouble("min-height", 5.0), 2.0, 64.0),
                airburst.getBool("break-blocks", true));
    }
}
