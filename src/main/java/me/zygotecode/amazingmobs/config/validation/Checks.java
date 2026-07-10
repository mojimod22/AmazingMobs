package me.zygotecode.amazingmobs.config.validation;

/**
 * Pure sanitization helpers: clamp an out-of-bounds value back into a sane range and record a
 * {@code WARN} explaining the correction. Used throughout parsing so impossible/typo'd numbers are
 * fixed (not fatal) with a clear audit trail. Unit-testable.
 */
public final class Checks {

    private Checks() {}

    /** Clamp to [0,1] (a probability / fraction). */
    public static double pct(ValidationReport r, String path, double v) {
        return inRange(r, path, v, 0.0, 1.0);
    }

    public static double inRange(ValidationReport r, String path, double v, double lo, double hi) {
        if (Double.isNaN(v)) { r.warn(path, "not a number — using " + lo); return lo; }
        if (v < lo) { r.warn(path, v + " below minimum " + lo + " — clamped"); return lo; }
        if (v > hi) { r.warn(path, v + " above maximum " + hi + " — clamped"); return hi; }
        return v;
    }

    public static double atLeast(ValidationReport r, String path, double v, double min) {
        if (Double.isNaN(v)) { r.warn(path, "not a number — using " + min); return min; }
        if (v < min) { r.warn(path, v + " below minimum " + min + " — clamped"); return min; }
        return v;
    }

    public static int atLeastInt(ValidationReport r, String path, int v, int min) {
        if (v < min) { r.warn(path, v + " below minimum " + min + " — clamped"); return min; }
        return v;
    }

    /** Require a positive value; if not, record ERROR and return the fallback. */
    public static double requirePositive(ValidationReport r, String path, double v, double fallback) {
        if (v <= 0 || Double.isNaN(v)) {
            r.error(path, "must be > 0 (got " + v + ")");
            return fallback;
        }
        return v;
    }
}
