package me.zygotecode.amazingmobs.api.event;

import me.zygotecode.amazingmobs.mob.MobDefinition;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/** Fired when a custom mob crosses a health-threshold phase. Informational. */
public class BossPhaseChangeEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final LivingEntity entity;
    private final MobDefinition definition;
    private final String phaseId;
    private final int phaseIndex;

    public BossPhaseChangeEvent(LivingEntity entity, MobDefinition definition, String phaseId, int phaseIndex) {
        this.entity = entity;
        this.definition = definition;
        this.phaseId = phaseId;
        this.phaseIndex = phaseIndex;
    }

    public LivingEntity getEntity() { return entity; }
    public MobDefinition getDefinition() { return definition; }
    public String getPhaseId() { return phaseId; }
    public int getPhaseIndex() { return phaseIndex; }

    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
