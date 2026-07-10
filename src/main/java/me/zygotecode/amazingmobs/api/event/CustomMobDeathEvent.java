package me.zygotecode.amazingmobs.api.event;

import me.zygotecode.amazingmobs.mob.MobDefinition;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/** Fired when a custom mob dies. Informational (drops are already handled). */
public class CustomMobDeathEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final LivingEntity entity;
    private final MobDefinition definition;
    private final Player killer;

    public CustomMobDeathEvent(LivingEntity entity, MobDefinition definition, Player killer) {
        this.entity = entity;
        this.definition = definition;
        this.killer = killer;
    }

    public LivingEntity getEntity() { return entity; }
    public MobDefinition getDefinition() { return definition; }
    public Player getKiller() { return killer; }

    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
