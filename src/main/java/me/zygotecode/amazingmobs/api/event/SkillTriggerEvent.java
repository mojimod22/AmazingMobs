package me.zygotecode.amazingmobs.api.event;

import me.zygotecode.amazingmobs.mob.MobDefinition;
import me.zygotecode.amazingmobs.skill.SkillDefinition;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/** Fired when a custom mob successfully casts a skill. Informational. */
public class SkillTriggerEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final LivingEntity entity;
    private final MobDefinition definition;
    private final SkillDefinition skill;

    public SkillTriggerEvent(LivingEntity entity, MobDefinition definition, SkillDefinition skill) {
        // synchronous event (default) — always fired from the main thread
        this.entity = entity;
        this.definition = definition;
        this.skill = skill;
    }

    public LivingEntity getEntity() { return entity; }
    public MobDefinition getDefinition() { return definition; }
    public SkillDefinition getSkill() { return skill; }

    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
