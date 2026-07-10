package me.zygotecode.amazingmobs.mob.runtime;

/**
 * Optional context attached to a spawn: which horde instance / wave it belongs to and a role tag
 * ({@code minion}/{@code elite}/{@code boss}). All fields may be null/blank for a plain spawn.
 *
 * @param hordeInstanceId running horde id, or null
 * @param waveIndex       wave number, or -1
 * @param role            role marker, or null
 * @param playerCount     participants used for scaling (>=1)
 * @param difficulty      difficulty multiplier (1.0 = normal)
 */
public record SpawnMeta(String hordeInstanceId, int waveIndex, String role, int playerCount, double difficulty) {

    public static final SpawnMeta SOLO = new SpawnMeta(null, -1, null, 1, 1.0);

    public static SpawnMeta solo(int playerCount, double difficulty) {
        return new SpawnMeta(null, -1, null, Math.max(1, playerCount), difficulty);
    }
}
