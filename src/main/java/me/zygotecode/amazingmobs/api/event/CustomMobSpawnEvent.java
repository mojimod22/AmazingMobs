package me.zygotecode.amazingmobs.api.event;

import me.zygotecode.amazingmobs.mob.MobDefinition;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/** Fired right after a custom mob is spawned and configured. Informational. */
public class CustomMobSpawnEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final LivingEntity entity;
    private final MobDefinition definition;

    public CustomMobSpawnEvent(LivingEntity entity, MobDefinition definition) {
        this.entity = entity;
        this.definition = definition;
    }

    public LivingEntity getEntity() { return entity; }
    public MobDefinition getDefinition() { return definition; }

    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
