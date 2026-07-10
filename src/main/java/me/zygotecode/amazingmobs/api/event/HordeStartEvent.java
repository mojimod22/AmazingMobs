package me.zygotecode.amazingmobs.api.event;

import me.zygotecode.amazingmobs.horde.HordeDefinition;
import org.bukkit.Location;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/** Fired before a horde starts. Cancellable — other plugins may veto the event. */
public class HordeStartEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final HordeDefinition definition;
    private final String instanceId;
    private final Location center;
    private boolean cancelled;

    public HordeStartEvent(HordeDefinition definition, String instanceId, Location center) {
        this.definition = definition;
        this.instanceId = instanceId;
        this.center = center;
    }

    public HordeDefinition getDefinition() { return definition; }
    public String getInstanceId() { return instanceId; }
    public Location getCenter() { return center; }

    @Override public boolean isCancelled() { return cancelled; }
    @Override public void setCancelled(boolean cancel) { this.cancelled = cancel; }

    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
