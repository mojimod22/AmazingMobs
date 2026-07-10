package me.zygotecode.amazingmobs.area;

import me.zygotecode.amazingmobs.util.Rng;
import org.bukkit.Location;
import org.bukkit.World;

/**
 * A spawn/combat area: a {@link Shape} of a given radius/height around a center. The center is
 * either FIXED (explicit world coords) or DYNAMIC (resolved at runtime around the trigger/players).
 * Geometry math is delegated to the pure {@link Shape}; this class only binds it to a world.
 */
public final class AreaSpec {

    private final Shape shape;
    private final double radius;
    private final double height;
    private final boolean dynamic;        // true => center supplied at runtime
    private final String worldName;       // for FIXED; nullable for DYNAMIC
    private final double x, y, z;          // FIXED center

    public AreaSpec(Shape shape, double radius, double height, boolean dynamic,
                    String worldName, double x, double y, double z) {
        this.shape = shape;
        this.radius = radius;
        this.height = height;
        this.dynamic = dynamic;
        this.worldName = worldName;
        this.x = x; this.y = y; this.z = z;
    }

    public Shape shape() { return shape; }
    public double radius() { return radius; }
    public double height() { return height; }
    public boolean dynamic() { return dynamic; }
    public String worldName() { return worldName; }

    /** Resolve the center: FIXED coords (needs the world) or the supplied dynamic center. */
    public Location resolveCenter(World fixedWorld, Location dynamicCenter) {
        if (dynamic) return dynamicCenter;
        if (fixedWorld == null) return null;
        return new Location(fixedWorld, x, y, z);
    }

    public boolean contains(Location center, Location point) {
        if (center == null || point == null || center.getWorld() == null) return false;
        if (!center.getWorld().equals(point.getWorld())) return false;
        return shape.contains(point.getX() - center.getX(), point.getY() - center.getY(),
                point.getZ() - center.getZ(), radius, effectiveHeight());
    }

    /** A random raw location in the area (Y not yet ground-snapped — see SpawnFinder). */
    public Location randomRaw(Location center, Rng rng) {
        double[] off = shape.randomLocal(rng, radius, effectiveHeight());
        return center.clone().add(off[0], off[1], off[2]);
    }

    private double effectiveHeight() {
        return shape == Shape.SPHERE ? radius * 2 : height;
    }
}
