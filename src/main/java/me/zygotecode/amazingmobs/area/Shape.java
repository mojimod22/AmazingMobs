package me.zygotecode.amazingmobs.area;

import me.zygotecode.amazingmobs.util.Rng;

import java.util.Locale;

/**
 * Pure geometry for spawn areas, in <b>local</b> coordinates (offsets from a center). No Bukkit, so
 * containment + sampling are unit-testable. {@code radius} is the horizontal half-extent; {@code
 * height} is the full vertical extent (ignored by {@link #SPHERE}, whose height is {@code 2*radius}).
 */
public enum Shape {
    SPHERE,
    CYLINDER,
    CUBE;

    /** Is the local offset inside this shape? */
    public boolean contains(double dx, double dy, double dz, double radius, double height) {
        double r2 = radius * radius;
        return switch (this) {
            case SPHERE -> dx * dx + dy * dy + dz * dz <= r2;
            case CYLINDER -> dx * dx + dz * dz <= r2 && Math.abs(dy) <= height / 2.0;
            case CUBE -> Math.abs(dx) <= radius && Math.abs(dz) <= radius && Math.abs(dy) <= height / 2.0;
        };
    }

    /** Random local offset {@code [dx,dy,dz]} uniformly within the shape. */
    public double[] randomLocal(Rng rng, double radius, double height) {
        switch (this) {
            case SPHERE -> {
                // uniform in ball: direction * radius*cbrt(u)
                double u = Math.cbrt(rng.nextDouble());
                double theta = rng.rangeDouble(0, Math.PI * 2);
                double cosPhi = rng.rangeDouble(-1, 1);
                double sinPhi = Math.sqrt(Math.max(0, 1 - cosPhi * cosPhi));
                double rr = radius * u;
                return new double[]{rr * sinPhi * Math.cos(theta), rr * cosPhi, rr * sinPhi * Math.sin(theta)};
            }
            case CYLINDER -> {
                double theta = rng.rangeDouble(0, Math.PI * 2);
                double rr = radius * Math.sqrt(rng.nextDouble());
                double dy = rng.rangeDouble(-height / 2.0, height / 2.0);
                return new double[]{rr * Math.cos(theta), dy, rr * Math.sin(theta)};
            }
            default -> { // CUBE
                return new double[]{
                        rng.rangeDouble(-radius, radius),
                        rng.rangeDouble(-height / 2.0, height / 2.0),
                        rng.rangeDouble(-radius, radius)};
            }
        }
    }

    public static Shape fromString(String s, Shape def) {
        if (s == null) return def;
        try { return valueOf(s.trim().toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException e) { return def; }
    }
}
