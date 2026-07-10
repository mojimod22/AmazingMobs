package me.zygotecode.amazingmobs.clazz;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Per-player, per-skill cooldown tracking (wall-clock millis). Runtime-only: cooldowns reset on relog,
 * which is intentional (a fresh session starts ready). No leaks — entries are overwritten in place.
 */
public final class Cooldowns {

    private final Map<UUID, Map<String, Long>> readyAt = new HashMap<>();

    /** Milliseconds remaining before {@code skillId} is usable again (0 = ready). */
    public long remainingMs(UUID player, String skillId) {
        Map<String, Long> m = readyAt.get(player);
        if (m == null) return 0;
        Long at = m.get(skillId);
        if (at == null) return 0;
        return Math.max(0, at - System.currentTimeMillis());
    }

    public boolean ready(UUID player, String skillId) { return remainingMs(player, skillId) <= 0; }

    public void trigger(UUID player, String skillId, int seconds) {
        if (seconds <= 0) return;
        readyAt.computeIfAbsent(player, k -> new HashMap<>()).put(skillId, System.currentTimeMillis() + seconds * 1000L);
    }

    public void clear(UUID player) { readyAt.remove(player); }
}
