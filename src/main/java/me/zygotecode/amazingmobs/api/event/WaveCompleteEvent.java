package me.zygotecode.amazingmobs.api.event;

import me.zygotecode.amazingmobs.horde.HordeDefinition;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/** Fired when a horde wave is cleared (or times out) and the next begins. */
public class WaveCompleteEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final HordeDefinition definition;
    private final String instanceId;
    private final int waveIndex;

    public WaveCompleteEvent(HordeDefinition definition, String instanceId, int waveIndex) {
        this.definition = definition;
        this.instanceId = instanceId;
        this.waveIndex = waveIndex;
    }

    public HordeDefinition getDefinition() { return definition; }
    public String getInstanceId() { return instanceId; }
    public int getWaveIndex() { return waveIndex; }

    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
