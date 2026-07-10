package me.zygotecode.amazingmobs.area;

import me.zygotecode.amazingmobs.util.Rng;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Finds a <b>safe, legal, visible</b> spawn location. The primary entry point is
 * {@link #findNearPlayers}: it spawns in a ring around the players' centroid, on roughly the same
 * surface as the players (within a small Y tolerance), inside the arena, and at a minimum distance —
 * so mobs appear right where players can see them, never under/over the arena, on cliffs, or stuck
 * on walls. Fails closed (returns {@code null}) rather than spawning somewhere illegal.
 */
public final class SpawnFinder {

    private SpawnFinder() {}

    /**
     * Ring-spawn around the players' centroid on their surface level.
     *
     * @param center     arena centre (for the arena-bounds check)
     * @param spec       arena shape
     * @param refs       reference players (their centroid + average Y); empty => use {@code center}
     * @param minDist    minimum distance from every reference player
     * @param maxDist    maximum ring distance (keep &le; arena radius minus a wall margin)
     * @param yTolerance how far above/below the players' Y a valid surface may be (gentle slopes/stairs)
     */
    public static Location findNearPlayers(Location center, AreaSpec spec, List<Player> refs,
                                           double minDist, double maxDist, int yTolerance,
                                           int attempts, Rng rng) {
        if (center == null || center.getWorld() == null) return null;
        World w = center.getWorld();

        double cx, cz, refY;
        if (refs == null || refs.isEmpty()) {
            cx = center.getX(); cz = center.getZ(); refY = center.getY();
        } else {
            double sx = 0, sz = 0, sy = 0;
            for (Player p : refs) { Location l = p.getLocation(); sx += l.getX(); sz += l.getZ(); sy += l.getY(); }
            cx = sx / refs.size(); cz = sz / refs.size(); refY = sy / refs.size();
        }
        // If the players' centroid is OUTSIDE the arena (they wandered off, or the audience fell back to
        // far-away players), a ring around it would land entirely outside the bounds and every candidate
        // would be rejected — i.e. the wave would never spawn. Clamp the ring origin back to the arena
        // centre so spawns always have somewhere legal to go. (This was the "restart = no spawns" bug.)
        if (!spec.contains(center, new Location(w, cx + 0.5, center.getY(), cz + 0.5))) {
            cx = center.getX(); cz = center.getZ(); refY = center.getY();
        }
        int baseY = (int) Math.round(refY);
        double lo = Math.max(2.0, minDist);
        double hi = Math.max(lo + 1, maxDist);

        for (int i = 0; i < attempts; i++) {
            double angle = rng.rangeDouble(0, Math.PI * 2);
            double dist = rng.rangeDouble(lo, hi);
            int bx = (int) Math.floor(cx + Math.cos(angle) * dist);
            int bz = (int) Math.floor(cz + Math.sin(angle) * dist);
            if (!w.isChunkLoaded(bx >> 4, bz >> 4)) continue;             // never force-load
            if (!spec.contains(center, new Location(w, bx + 0.5, center.getY(), bz + 0.5))) continue; // inside arena

            Location ground = groundNear(w, bx, baseY, bz, yTolerance);   // same surface as players (±tol)
            if (ground == null) continue;                                  // no flat footing here (cliff/wall) — skip
            if (tooCloseToAny(ground, refs, minDist)) continue;
            return ground;
        }
        return null;
    }

    /** Legacy whole-area finder (kept for compatibility / non-player-centric callers). */
    public static Location findSafe(Location center, AreaSpec spec, double minPlayerDistance, int attempts, Rng rng) {
        if (center == null || center.getWorld() == null) return null;
        World w = center.getWorld();
        for (int i = 0; i < attempts; i++) {
            Location raw = spec.randomRaw(center, rng);
            int bx = raw.getBlockX();
            int bz = raw.getBlockZ();
            if (!w.isChunkLoaded(bx >> 4, bz >> 4)) continue;
            Location ground = groundNear(w, bx, raw.getBlockY(), bz, 6);
            if (ground == null) continue;
            if (tooCloseToAny(ground, null, minPlayerDistance)) continue;
            return ground;
        }
        return null;
    }

    /**
     * A safe, grounded standing spot near {@code anchor} — used to place players inside the arena
     * (perfectly on the floor, never in a wall/air/underground). Tries the anchor itself first, then a
     * spread of nearby offsets, snapping each to the nearest surface within ±{@code yTol}. Preserves the
     * anchor's facing. Returns {@code null} only if nothing standable exists nearby (caller falls back).
     *
     * @param spread approximate max horizontal offset (blocks) used to fan players out
     */
    public static Location groundedSpot(Location anchor, double spread, int yTol, int attempts, Rng rng) {
        if (anchor == null || anchor.getWorld() == null) return null;
        World w = anchor.getWorld();
        int ax = anchor.getBlockX(), az = anchor.getBlockZ(), ay = anchor.getBlockY();
        // 1) the anchor column itself
        Location at = groundNear(w, ax, ay, az, yTol);
        if (at != null) return face(at, anchor);
        // 2) fan out around it
        for (int i = 0; i < Math.max(1, attempts); i++) {
            double angle = rng.rangeDouble(0, Math.PI * 2);
            double dist = rng.rangeDouble(1, Math.max(1.5, spread));
            int bx = (int) Math.floor(anchor.getX() + Math.cos(angle) * dist);
            int bz = (int) Math.floor(anchor.getZ() + Math.sin(angle) * dist);
            if (!w.isChunkLoaded(bx >> 4, bz >> 4)) continue;
            Location g = groundNear(w, bx, ay, bz, yTol);
            if (g != null) return face(g, anchor);
        }
        return null;
    }

    private static Location face(Location loc, Location facing) {
        loc.setYaw(facing.getYaw());
        loc.setPitch(facing.getPitch());
        return loc;
    }

    /**
     * The nearest standable surface to {@code baseY} (within ±{@code tol}) at (x,z): a solid block
     * with two passable, non-liquid blocks above. Searching only the band around the players' Y is
     * what keeps spawns on the same level (no deep caves, no rooftops, no wall ledges far away).
     */
    private static Location groundNear(World w, int x, int baseY, int z, int tol) {
        int maxY = Math.min(baseY + tol, w.getMaxHeight() - 3);
        int minY = Math.max(w.getMinHeight(), baseY - tol);
        Location best = null;
        int bestDelta = Integer.MAX_VALUE;
        for (int y = minY; y <= maxY; y++) {
            Block ground = w.getBlockAt(x, y, z);
            if (ground.isPassable() || ground.isLiquid()) continue;
            Block feet = w.getBlockAt(x, y + 1, z);
            Block head = w.getBlockAt(x, y + 2, z);
            if (feet.isPassable() && head.isPassable() && !feet.isLiquid() && !head.isLiquid()) {
                int delta = Math.abs((y + 1) - baseY);                     // prefer the surface closest to players' Y
                if (delta < bestDelta) { bestDelta = delta; best = new Location(w, x + 0.5, y + 1, z + 0.5); }
            }
        }
        return best;
    }

    private static boolean tooCloseToAny(Location loc, List<Player> refs, double minDistance) {
        if (minDistance <= 0) return false;
        double sq = minDistance * minDistance;
        if (refs != null && !refs.isEmpty()) {
            for (Player p : refs) if (p.getLocation().distanceSquared(loc) < sq) return true;
            return false;
        }
        for (Player p : loc.getWorld().getPlayers()) {
            if (p.getLocation().distanceSquared(loc) < sq) return true;
        }
        return false;
    }
}
