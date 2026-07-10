package me.zygotecode.amazingmobs.horde;

import me.zygotecode.amazingmobs.scaling.Scaling;
import me.zygotecode.amazingmobs.util.IntRange;
import me.zygotecode.amazingmobs.util.Rng;

/**
 * One mob line in a wave: which mob, how many (scaled by player count), an inclusion chance (for
 * "special" mobs), a role marker, and whether it is the wave's boss. Pure data.
 *
 * @param mobId         custom mob id to spawn
 * @param count         base count range
 * @param countPerPlayer extra mobs added per participant beyond the first
 * @param chance        probability this line spawns at all (1.0 = always)
 * @param role          PDC role tag (minion/elite/boss/objective)
 * @param boss          if true, this entry is the wave's boss and gates wave clear
 * @param objective     if true, this is a battlefield objective (e.g. spawner totem); the wave is
 *                      cleared only when all objective entities are destroyed, regardless of adds
 */
public record WaveEntry(String mobId, IntRange count, double countPerPlayer,
                        double chance, String role, boolean boss, boolean objective) {

    /** Rolled spawn count for the given player count (0 if its chance fails). */
    public int resolveCount(int players, int maxPlayers, Rng rng) {
        if (chance < 1.0 && !rng.chance(chance)) return 0;
        return Scaling.countForPlayers(count.pick(rng), countPerPlayer, players, maxPlayers);
    }
}
