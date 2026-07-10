package me.zygotecode.amazingmobs.trait;

import me.zygotecode.amazingmobs.config.ConfigSection;
import me.zygotecode.amazingmobs.mob.MobDefinition;
import me.zygotecode.amazingmobs.skill.SummonFunction;
import me.zygotecode.amazingmobs.util.Rng;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;

/**
 * What a {@link Trait} hook needs. One instance is created per mob and re-primed before each hook
 * call (cheap, single-threaded reuse) so the hot tick path does not allocate per trait.
 */
public final class TraitContext {

    private final Plugin plugin;
    private final LivingEntity entity;
    private final MobDefinition definition;
    private final SummonFunction summon;
    private final Rng rng;
    private final int periodTicks;

    // re-primed per hook call:
    private TraitInstance instance;
    private long tick;
    private double healthFraction = 1.0;
    private String phaseId;
    private LivingEntity eventTarget;

    public TraitContext(Plugin plugin, LivingEntity entity, MobDefinition definition,
                        SummonFunction summon, Rng rng, int periodTicks) {
        this.plugin = plugin;
        this.entity = entity;
        this.definition = definition;
        this.summon = summon;
        this.rng = rng;
        this.periodTicks = periodTicks;
    }

    /** Set the per-call state. Called by the runtime before invoking a hook. */
    public TraitContext prime(TraitInstance instance, long tick, double healthFraction,
                              String phaseId, LivingEntity eventTarget) {
        this.instance = instance;
        this.tick = tick;
        this.healthFraction = healthFraction;
        this.phaseId = phaseId;
        this.eventTarget = eventTarget;
        return this;
    }

    public Plugin plugin() { return plugin; }
    public LivingEntity entity() { return entity; }
    public MobDefinition definition() { return definition; }
    public ConfigSection params() { return instance.definition().params(); }
    public TraitInstance instance() { return instance; }
    public Rng rng() { return rng; }
    public long tick() { return tick; }
    public int periodTicks() { return periodTicks; }
    public double healthFraction() { return healthFraction; }
    public String phaseId() { return phaseId; }
    public LivingEntity eventTarget() { return eventTarget; }
    public World world() { return entity.getWorld(); }
    public Location origin() { return entity.getLocation(); }

    public LivingEntity summon(String mobId, Location loc) {
        return summon == null ? null : summon.summon(mobId, loc);
    }
}
