package me.zygotecode.amazingmobs.skill;

/**
 * Per-mob binding of a {@link SkillDefinition} to its resolved {@link Skill}, holding the live
 * cooldown / one-shot state. Not shared between mobs. The mob controller orchestrates selection;
 * this class just tracks readiness.
 */
public final class SkillInstance {

    private final SkillDefinition definition;
    private final Skill skill;

    private long readyAtTick = 0;     // earliest tick this may fire again
    private boolean oneShotFired = false; // for ON_SPAWN/ON_DEATH/ON_LOW_HEALTH
    private boolean disabledByPhase = false;

    public SkillInstance(SkillDefinition definition, Skill skill) {
        this.definition = definition;
        this.skill = skill;
    }

    public SkillDefinition definition() { return definition; }
    public Skill skill() { return skill; }
    public TriggerSpec trigger() { return definition.trigger(); }

    public boolean offCooldown(long tick) { return tick >= readyAtTick; }

    public void putOnCooldown(long tick) {
        readyAtTick = tick + Math.max(1, definition.trigger().cooldownTicks());
    }

    public boolean oneShotFired() { return oneShotFired; }
    public void markOneShotFired() { this.oneShotFired = true; }

    public boolean disabledByPhase() { return disabledByPhase; }
    public void setDisabledByPhase(boolean v) { this.disabledByPhase = v; }
}
