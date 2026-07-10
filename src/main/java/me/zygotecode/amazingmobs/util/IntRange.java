package me.zygotecode.amazingmobs.util;

/**
 * Inclusive integer range. Immutable, pure. Parsed from {@code "5"} or {@code "3-8"}.
 */
public record IntRange(int min, int max) {

    public IntRange {
        if (min > max) { int t = min; min = max; max = t; } // normalise
    }

    public static IntRange of(int value) {
        return new IntRange(value, value);
    }

    public boolean fixed() {
        return min == max;
    }

    public int pick(Rng rng) {
        return rng.rangeInt(min, max);
    }

    /**
     * Parse {@code "min-max"} or a single {@code "value"}. Negative numbers are supported on the
     * lower bound (e.g. {@code "-2-3"} is not supported — keep ranges non-negative for spawn counts).
     * @return parsed range, or {@code fallback} if the text is null/blank/invalid.
     */
    public static IntRange parse(String raw, IntRange fallback) {
        if (raw == null) return fallback;
        String s = raw.trim();
        if (s.isEmpty()) return fallback;
        int dash = s.indexOf('-', s.startsWith("-") ? 1 : 0);
        try {
            if (dash < 0) {
                int v = Integer.parseInt(s);
                return new IntRange(v, v);
            }
            int a = Integer.parseInt(s.substring(0, dash).trim());
            int b = Integer.parseInt(s.substring(dash + 1).trim());
            return new IntRange(a, b);
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    @Override
    public String toString() {
        return fixed() ? Integer.toString(min) : (min + "-" + max);
    }
}
