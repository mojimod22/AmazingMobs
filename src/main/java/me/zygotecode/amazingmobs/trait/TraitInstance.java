package me.zygotecode.amazingmobs.trait;

import java.util.HashMap;
import java.util.Map;

/**
 * Per-mob binding of a {@link TraitDefinition} to its resolved {@link Trait}, holding live state
 * (an internal cooldown clock + a small scratch map). Not shared between mobs.
 */
public final class TraitInstance {

    private final TraitDefinition definition;
    private final Trait trait;
    private long readyAtTick = 0;
    private final Map<String, Object> state = new HashMap<>();

    public TraitInstance(TraitDefinition definition, Trait trait) {
        this.definition = definition;
        this.trait = trait;
    }

    public TraitDefinition definition() { return definition; }
    public Trait trait() { return trait; }

    public boolean offCooldown(long tick) { return tick >= readyAtTick; }
    public void putCooldown(long tick, long cooldownTicks) { readyAtTick = tick + Math.max(1, cooldownTicks); }

    @SuppressWarnings("unchecked")
    public <T> T state(String key, T def) {
        return (T) state.getOrDefault(key, def);
    }
    public void putState(String key, Object value) { state.put(key, value); }
    public boolean flag(String key) { return Boolean.TRUE.equals(state.get(key)); }
}
