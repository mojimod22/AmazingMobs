package me.zygotecode.amazingmobs.util;

import java.util.Locale;

/** Pure parsing / clamping helpers. No Bukkit — unit-testable. */
public final class Numbers {

    public static final long TICKS_PER_SECOND = 20L;

    private Numbers() {}

    public static int clamp(int v, int lo, int hi) {
        return v < lo ? lo : Math.min(v, hi);
    }

    public static long clamp(long v, long lo, long hi) {
        return v < lo ? lo : Math.min(v, hi);
    }

    public static double clamp(double v, double lo, double hi) {
        if (Double.isNaN(v)) return lo;
        return v < lo ? lo : Math.min(v, hi);
    }

    public static int parseInt(String s, int def) {
        if (s == null) return def;
        try { return Integer.parseInt(s.trim()); } catch (NumberFormatException e) { return def; }
    }

    public static double parseDouble(String s, double def) {
        if (s == null) return def;
        try { return Double.parseDouble(s.trim()); } catch (NumberFormatException e) { return def; }
    }

    public static boolean parseBool(String s, boolean def) {
        if (s == null) return def;
        String t = s.trim().toLowerCase(Locale.ROOT);
        return switch (t) {
            case "true", "yes", "y", "on", "1" -> true;
            case "false", "no", "n", "off", "0" -> false;
            default -> def;
        };
    }

    /**
     * Parse a duration into <b>ticks</b>. Accepts suffixes:
     * {@code t}=ticks, {@code s}=seconds, {@code m}=minutes, {@code h}=hours.
     * A bare number is treated as seconds. Returns {@code def} on parse failure.
     * Examples: {@code "30s"->600}, {@code "2m"->2400}, {@code "100t"->100}, {@code "5"->100}.
     */
    public static long parseTicks(String raw, long def) {
        if (raw == null) return def;
        String s = raw.trim().toLowerCase(Locale.ROOT);
        if (s.isEmpty()) return def;
        char last = s.charAt(s.length() - 1);
        try {
            if (Character.isDigit(last) || last == '.') {
                return Math.round(Double.parseDouble(s) * TICKS_PER_SECOND); // bare = seconds
            }
            double value = Double.parseDouble(s.substring(0, s.length() - 1).trim());
            return switch (last) {
                case 't' -> Math.round(value);
                case 's' -> Math.round(value * TICKS_PER_SECOND);
                case 'm' -> Math.round(value * TICKS_PER_SECOND * 60);
                case 'h' -> Math.round(value * TICKS_PER_SECOND * 3600);
                default -> def;
            };
        } catch (NumberFormatException e) {
            return def;
        }
    }

    /** Round ticks to a human seconds value (for display). */
    public static double ticksToSeconds(long ticks) {
        return ticks / (double) TICKS_PER_SECOND;
    }
}
