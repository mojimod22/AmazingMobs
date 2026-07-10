package me.zygotecode.amazingmobs.api.event;

import me.zygotecode.amazingmobs.horde.HordeDefinition;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/** Fired when a horde ends, either completed (cleared) or not (timeout/forced stop). */
public class HordeStopEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final HordeDefinition definition;
    private final String instanceId;
    private final boolean completed;

    public HordeStopEvent(HordeDefinition definition, String instanceId, boolean completed) {
        this.definition = definition;
        this.instanceId = instanceId;
        this.completed = completed;
    }

    public HordeDefinition getDefinition() { return definition; }
    public String getInstanceId() { return instanceId; }
    public boolean isCompleted() { return completed; }

    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
