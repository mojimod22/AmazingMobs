package me.zygotecode.amazingmobs.util;

import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.ToDoubleFunction;

/**
 * Thin randomness helper. Pure (no Bukkit) so it is unit-testable.
 *
 * <p>Use {@link #shared()} for normal runtime randomness (backed by {@link ThreadLocalRandom},
 * never seeded) and {@link #seeded(long)} for deterministic tests.</p>
 */
public final class Rng {

    private final Random random; // null => ThreadLocalRandom

    private Rng(Random random) {
        this.random = random;
    }

    /** Shared, non-deterministic source for runtime use. */
    public static Rng shared() {
        return SHARED;
    }

    private static final Rng SHARED = new Rng(null);

    /** Deterministic source for tests. */
    public static Rng seeded(long seed) {
        return new Rng(new Random(seed));
    }

    private Random r() {
        return random != null ? random : ThreadLocalRandom.current();
    }

    /** @return true with probability {@code chance} (clamped to [0,1]). */
    public boolean chance(double chance) {
        if (chance <= 0.0) return false;
        if (chance >= 1.0) return true;
        return r().nextDouble() < chance;
    }

    /** Inclusive integer in [min,max]. Tolerates reversed bounds. */
    public int rangeInt(int min, int max) {
        if (min == max) return min;
        if (min > max) { int t = min; min = max; max = t; }
        return min + r().nextInt((max - min) + 1);
    }

    /** Double in [min,max). Tolerates reversed bounds. */
    public double rangeDouble(double min, double max) {
        if (min == max) return min;
        if (min > max) { double t = min; min = max; max = t; }
        return min + r().nextDouble() * (max - min);
    }

    public double nextDouble() {
        return r().nextDouble();
    }

    /** Uniform pick, or null if empty. */
    public <T> T pick(List<T> list) {
        if (list == null || list.isEmpty()) return null;
        return list.get(r().nextInt(list.size()));
    }

    /**
     * Weighted pick. Entries with weight &le; 0 are skipped. Returns null if no positive weight.
     */
    public <T> T weighted(List<T> list, ToDoubleFunction<T> weigher) {
        if (list == null || list.isEmpty()) return null;
        double total = 0.0;
        for (T t : list) {
            double w = weigher.applyAsDouble(t);
            if (w > 0) total += w;
        }
        if (total <= 0) return null;
        double roll = r().nextDouble() * total;
        for (T t : list) {
            double w = weigher.applyAsDouble(t);
            if (w <= 0) continue;
            roll -= w;
            if (roll < 0) return t;
        }
        return list.get(list.size() - 1); // floating-point safety net
    }
}
