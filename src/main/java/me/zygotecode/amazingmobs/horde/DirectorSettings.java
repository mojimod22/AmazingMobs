package me.zygotecode.amazingmobs.horde;

/**
 * Adaptive-intensity "director" for a horde. After each wave clears, the running instance compares
 * how fast it was beaten against {@code targetClearTicks} and nudges a spawn multiplier within
 * [{@code minMultiplier}, {@code maxMultiplier}] by {@code step}: dominate → harder, struggle →
 * easier. Keeps events tense without becoming unfair, and avoids spawn spam. Opt-in per horde.
 */
public record DirectorSettings(boolean enabled, double minMultiplier, double maxMultiplier,
                               double step, long targetClearTicks) {

    public static final DirectorSettings DISABLED = new DirectorSettings(false, 1.0, 1.0, 0.0, 900);
}
