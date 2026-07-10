package me.zygotecode.amazingmobs.skill;

import me.zygotecode.amazingmobs.config.ConfigSection;

/**
 * A skill bound into a mob: which skill ({@code skillId}), its tunable {@code params}, and its
 * {@link TriggerSpec}. Immutable; the same definition is shared by every spawned instance of the
 * mob (per-instance cooldown state lives in {@link SkillInstance}).
 */
public final class SkillDefinition {

    private final String skillId;
    private final String label;       // optional human label for messages/telegraphs
    private final ConfigSection params;
    private final TriggerSpec trigger;

    public SkillDefinition(String skillId, String label, ConfigSection params, TriggerSpec trigger) {
        this.skillId = skillId;
        this.label = label;
        this.params = params;
        this.trigger = trigger;
    }

    public String skillId() { return skillId; }
    public String label() { return label; }
    public ConfigSection params() { return params; }
    public TriggerSpec trigger() { return trigger; }
}
