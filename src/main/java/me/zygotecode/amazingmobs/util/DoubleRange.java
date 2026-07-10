package me.zygotecode.amazingmobs.util;

/** Inclusive double range. Immutable, pure. Parsed from {@code "1.5"} or {@code "0.5-2.0"}. */
public record DoubleRange(double min, double max) {

    public DoubleRange {
        if (min > max) { double t = min; min = max; max = t; }
    }

    public static DoubleRange of(double value) {
        return new DoubleRange(value, value);
    }

    public boolean fixed() {
        return Double.compare(min, max) == 0;
    }

    public double pick(Rng rng) {
        return fixed() ? min : rng.rangeDouble(min, max);
    }

    public static DoubleRange parse(String raw, DoubleRange fallback) {
        if (raw == null) return fallback;
        String s = raw.trim();
        if (s.isEmpty()) return fallback;
        // Split on a dash that is not a leading sign and not an exponent sign.
        int dash = -1;
        for (int i = 1; i < s.length(); i++) {
            if (s.charAt(i) == '-' && s.charAt(i - 1) != 'e' && s.charAt(i - 1) != 'E') {
                dash = i; break;
            }
        }
        try {
            if (dash < 0) {
                double v = Double.parseDouble(s);
                return new DoubleRange(v, v);
            }
            double a = Double.parseDouble(s.substring(0, dash).trim());
            double b = Double.parseDouble(s.substring(dash + 1).trim());
            return new DoubleRange(a, b);
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    @Override
    public String toString() {
        return fixed() ? Double.toString(min) : (min + "-" + max);
    }
}
