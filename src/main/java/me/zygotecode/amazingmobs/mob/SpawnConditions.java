package me.zygotecode.amazingmobs.mob;

import org.bukkit.Location;
import org.bukkit.World;

import java.util.Locale;
import java.util.Set;

/**
 * Context gating for variants (and other conditional content): world / biome / time / Y / weather /
 * player-count. The set/range fields are pure data; {@link #matches} evaluates against a live world
 * at spawn. An empty/UNSET field means "no constraint".
 */
public final class SpawnConditions {

    public enum Weather { ANY, CLEAR, RAIN, THUNDER }

    public static final SpawnConditions ANY = builder().build();

    private final Set<String> worlds;
    private final Set<String> biomeAllow;
    private final Set<String> biomeDeny;
    private final long timeMin, timeMax;   // -1 = any
    private final int yMin, yMax;
    private final Weather weather;
    private final int minPlayers;

    private SpawnConditions(Builder b) {
        this.worlds = b.worlds == null ? Set.of() : Set.copyOf(b.worlds);
        this.biomeAllow = b.biomeAllow == null ? Set.of() : Set.copyOf(b.biomeAllow);
        this.biomeDeny = b.biomeDeny == null ? Set.of() : Set.copyOf(b.biomeDeny);
        this.timeMin = b.timeMin;
        this.timeMax = b.timeMax;
        this.yMin = b.yMin;
        this.yMax = b.yMax;
        this.weather = b.weather;
        this.minPlayers = b.minPlayers;
    }

    public boolean isAny() {
        return worlds.isEmpty() && biomeAllow.isEmpty() && biomeDeny.isEmpty()
                && timeMin < 0 && timeMax < 0 && yMin == Integer.MIN_VALUE && yMax == Integer.MAX_VALUE
                && weather == Weather.ANY && minPlayers <= 0;
    }

    public boolean matches(Location loc, int playerCount) {
        if (loc == null || loc.getWorld() == null) return isAny();
        World w = loc.getWorld();
        if (!worlds.isEmpty() && !worlds.contains(w.getName().toLowerCase(Locale.ROOT))) return false;
        if (loc.getBlockY() < yMin || loc.getBlockY() > yMax) return false;
        if (playerCount < minPlayers) return false;
        if (timeMin >= 0 && timeMax >= 0) {
            long t = w.getTime();
            boolean in = timeMin <= timeMax ? (t >= timeMin && t <= timeMax) : (t >= timeMin || t <= timeMax);
            if (!in) return false;
        }
        if (weather != Weather.ANY) {
            boolean storm = w.hasStorm();
            boolean thunder = w.isThundering();
            boolean ok = switch (weather) {
                case CLEAR -> !storm;
                case RAIN -> storm && !thunder;
                case THUNDER -> thunder;
                default -> true;
            };
            if (!ok) return false;
        }
        if (!biomeAllow.isEmpty() || !biomeDeny.isEmpty()) {
            String biome = w.getBiome(loc).getKey().getKey().toLowerCase(Locale.ROOT);
            if (!biomeDeny.isEmpty() && biomeDeny.contains(biome)) return false;
            if (!biomeAllow.isEmpty() && !biomeAllow.contains(biome)) return false;
        }
        return true;
    }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private Set<String> worlds, biomeAllow, biomeDeny;
        private long timeMin = -1, timeMax = -1;
        private int yMin = Integer.MIN_VALUE, yMax = Integer.MAX_VALUE;
        private Weather weather = Weather.ANY;
        private int minPlayers = 0;

        public Builder worlds(Set<String> v) { this.worlds = v; return this; }
        public Builder biomeAllow(Set<String> v) { this.biomeAllow = v; return this; }
        public Builder biomeDeny(Set<String> v) { this.biomeDeny = v; return this; }
        public Builder timeMin(long v) { this.timeMin = v; return this; }
        public Builder timeMax(long v) { this.timeMax = v; return this; }
        public Builder yMin(int v) { this.yMin = v; return this; }
        public Builder yMax(int v) { this.yMax = v; return this; }
        public Builder weather(Weather v) { this.weather = v; return this; }
        public Builder minPlayers(int v) { this.minPlayers = v; return this; }
        public SpawnConditions build() { return new SpawnConditions(this); }
    }
}
