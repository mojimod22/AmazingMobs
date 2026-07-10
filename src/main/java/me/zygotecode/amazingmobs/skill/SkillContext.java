package me.zygotecode.amazingmobs.skill;

import me.zygotecode.amazingmobs.config.ConfigSection;
import me.zygotecode.amazingmobs.mob.MobDefinition;
import me.zygotecode.amazingmobs.util.Rng;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;

import java.util.List;

/** Everything a {@link Skill} needs for one cast. Built by the runtime per trigger. Immutable. */
public final class SkillContext {

    private final Plugin plugin;
    private final LivingEntity caster;
    private final MobDefinition definition;
    private final LivingEntity primaryTarget; // may be null
    private final List<LivingEntity> targets;  // resolved per the trigger's TargetRule
    private final ConfigSection params;
    private final TriggerSpec trigger;
    private final Rng rng;
    private final String phaseId;             // may be null
    private final SummonFunction summon;

    public SkillContext(Plugin plugin, LivingEntity caster, MobDefinition definition,
                        LivingEntity primaryTarget, List<LivingEntity> targets, ConfigSection params,
                        TriggerSpec trigger, Rng rng, String phaseId, SummonFunction summon) {
        this.plugin = plugin;
        this.caster = caster;
        this.definition = definition;
        this.primaryTarget = primaryTarget;
        this.targets = targets == null ? List.of() : targets;
        this.params = params;
        this.trigger = trigger;
        this.rng = rng;
        this.phaseId = phaseId;
        this.summon = summon;
    }

    public Plugin plugin() { return plugin; }
    public LivingEntity caster() { return caster; }
    public MobDefinition definition() { return definition; }
    public LivingEntity primaryTarget() { return primaryTarget; }
    public List<LivingEntity> targets() { return targets; }
    public ConfigSection params() { return params; }
    public TriggerSpec trigger() { return trigger; }
    public Rng rng() { return rng; }
    public String phaseId() { return phaseId; }

    public World world() { return caster.getWorld(); }
    public Location origin() { return caster.getLocation(); }
    public Location eye() { return caster.getEyeLocation(); }

    public LivingEntity summon(String mobId, Location loc) {
        return summon == null ? null : summon.summon(mobId, loc);
    }
}
