package me.zygotecode.amazingmobs.scaling;

import me.zygotecode.amazingmobs.util.Numbers;

/** Pure horde-level scaling helpers (player-count driven). Unit-testable. */
public final class Scaling {

    private Scaling() {}

    /** {@code base * (1 + perPlayerPct * (clamp(players,1,cap) - 1))}. */
    public static double perPlayer(double base, double perPlayerPct, int players, int cap) {
        int eff = Numbers.clamp(players, 1, Math.max(1, cap));
        return base * (1.0 + perPlayerPct * (eff - 1));
    }

    /** Linear count add: {@code base + round(perPlayer * (clamp(players,1,cap) - 1))}, min 0. */
    public static int countForPlayers(int base, double perPlayer, int players, int cap) {
        int eff = Numbers.clamp(players, 1, Math.max(1, cap));
        return Math.max(0, base + (int) Math.round(perPlayer * (eff - 1)));
    }
}
