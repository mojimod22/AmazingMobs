package me.zygotecode.amazingmobs.horde;

import me.zygotecode.amazingmobs.area.AreaSpec;

import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Complete, immutable definition of a horde event: identity, difficulty, activation gating, the
 * spawn area + spawn rules, the ordered waves, rewards, and presentation. Built once by
 * {@code HordeParser}. The runtime ({@code HordeInstance}) reads it but never mutates it.
 */
public final class HordeDefinition {

    private final String id;
    private final String name;
    private final String description;
    private final double difficulty;          // global multiplier applied to spawned mobs
    private final long durationTicks;          // overall time cap (0 = until cleared)
    private final long cooldownTicks;          // global cooldown after it ends
    private final AreaSpec area;

    // spawn rules
    private final int maxSpawnsPerTick;
    private final int maxConcurrentMobs;
    private final double minPlayerDistance;
    private final int spawnAttempts;

    // activation gating
    private final Set<String> worlds;          // allow-list; empty = any
    private final Set<String> biomeAllow;      // empty = any
    private final Set<String> biomeDeny;
    private final long timeMin, timeMax;       // world time window; -1 = any
    private final int minPlayers, maxPlayers;

    private final boolean infinite;            // loop waves until time cap / fail
    private final DirectorSettings director;
    private final List<Wave> waves;
    private final HordeRewards rewards;

    // presentation
    private final String startMessage, startTitle, startSubtitle, endMessage, failMessage, sound;

    private HordeDefinition(Builder b) {
        this.id = b.id;
        this.name = b.name != null ? b.name : b.id;
        this.description = b.description;
        this.difficulty = b.difficulty;
        this.durationTicks = b.durationTicks;
        this.cooldownTicks = b.cooldownTicks;
        this.area = b.area;
        this.maxSpawnsPerTick = b.maxSpawnsPerTick;
        this.maxConcurrentMobs = b.maxConcurrentMobs;
        this.minPlayerDistance = b.minPlayerDistance;
        this.spawnAttempts = b.spawnAttempts;
        this.worlds = b.worlds == null ? Set.of() : Set.copyOf(b.worlds);
        this.biomeAllow = b.biomeAllow == null ? Set.of() : Set.copyOf(b.biomeAllow);
        this.biomeDeny = b.biomeDeny == null ? Set.of() : Set.copyOf(b.biomeDeny);
        this.timeMin = b.timeMin;
        this.timeMax = b.timeMax;
        this.minPlayers = b.minPlayers;
        this.maxPlayers = b.maxPlayers;
        this.infinite = b.infinite;
        this.director = b.director == null ? DirectorSettings.DISABLED : b.director;
        this.waves = b.waves == null ? List.of() : List.copyOf(b.waves);
        this.rewards = b.rewards == null ? HordeRewards.NONE : b.rewards;
        this.startMessage = b.startMessage;
        this.startTitle = b.startTitle;
        this.startSubtitle = b.startSubtitle;
        this.endMessage = b.endMessage;
        this.failMessage = b.failMessage;
        this.sound = b.sound;
    }

    public String id() { return id; }
    public String name() { return name; }
    public String description() { return description; }
    public double difficulty() { return difficulty; }
    public long durationTicks() { return durationTicks; }
    public long cooldownTicks() { return cooldownTicks; }
    public AreaSpec area() { return area; }
    public int maxSpawnsPerTick() { return maxSpawnsPerTick; }
    public int maxConcurrentMobs() { return maxConcurrentMobs; }
    public double minPlayerDistance() { return minPlayerDistance; }
    public int spawnAttempts() { return spawnAttempts; }
    public Set<String> worlds() { return worlds; }
    public Set<String> biomeAllow() { return biomeAllow; }
    public Set<String> biomeDeny() { return biomeDeny; }
    public long timeMin() { return timeMin; }
    public long timeMax() { return timeMax; }
    public int minPlayers() { return minPlayers; }
    public int maxPlayers() { return maxPlayers; }
    public boolean infinite() { return infinite; }
    public DirectorSettings director() { return director; }
    public List<Wave> waves() { return waves; }
    public HordeRewards rewards() { return rewards; }
    public String startMessage() { return startMessage; }
    public String startTitle() { return startTitle; }
    public String startSubtitle() { return startSubtitle; }
    public String endMessage() { return endMessage; }
    public String failMessage() { return failMessage; }
    public String sound() { return sound; }

    /** Is the given world allowed (empty allow-list = any)? */
    public boolean allowsWorld(String worldName) {
        return worlds.isEmpty() || worlds.contains(worldName.toLowerCase(Locale.ROOT));
    }

    /** Is {@code t} (world time 0..24000) inside the configured window? -1 bounds mean "any". */
    public boolean allowsTime(long t) {
        if (timeMin < 0 || timeMax < 0) return true;
        return timeMin <= timeMax ? (t >= timeMin && t <= timeMax) : (t >= timeMin || t <= timeMax);
    }

    public static Builder builder(String id) { return new Builder(id); }

    public static final class Builder {
        private final String id;
        private String name, description;
        private double difficulty = 1.0;
        private long durationTicks = 0;
        private long cooldownTicks = 6000;
        private AreaSpec area;
        private int maxSpawnsPerTick = 8;
        private int maxConcurrentMobs = 80;
        private double minPlayerDistance = 8;
        private int spawnAttempts = 24;
        private Set<String> worlds, biomeAllow, biomeDeny;
        private long timeMin = -1, timeMax = -1;
        private int minPlayers = 1, maxPlayers = 8;
        private boolean infinite = false;
        private DirectorSettings director;
        private List<Wave> waves;
        private HordeRewards rewards;
        private String startMessage, startTitle, startSubtitle, endMessage, failMessage, sound;

        public Builder(String id) { this.id = id; }
        public Builder name(String v) { this.name = v; return this; }
        public Builder description(String v) { this.description = v; return this; }
        public Builder difficulty(double v) { this.difficulty = v; return this; }
        public Builder durationTicks(long v) { this.durationTicks = v; return this; }
        public Builder cooldownTicks(long v) { this.cooldownTicks = v; return this; }
        public Builder area(AreaSpec v) { this.area = v; return this; }
        public Builder maxSpawnsPerTick(int v) { this.maxSpawnsPerTick = v; return this; }
        public Builder maxConcurrentMobs(int v) { this.maxConcurrentMobs = v; return this; }
        public Builder minPlayerDistance(double v) { this.minPlayerDistance = v; return this; }
        public Builder spawnAttempts(int v) { this.spawnAttempts = v; return this; }
        public Builder worlds(Set<String> v) { this.worlds = v; return this; }
        public Builder biomeAllow(Set<String> v) { this.biomeAllow = v; return this; }
        public Builder biomeDeny(Set<String> v) { this.biomeDeny = v; return this; }
        public Builder timeMin(long v) { this.timeMin = v; return this; }
        public Builder timeMax(long v) { this.timeMax = v; return this; }
        public Builder minPlayers(int v) { this.minPlayers = v; return this; }
        public Builder maxPlayers(int v) { this.maxPlayers = v; return this; }
        public Builder infinite(boolean v) { this.infinite = v; return this; }
        public Builder director(DirectorSettings v) { this.director = v; return this; }
        public Builder waves(List<Wave> v) { this.waves = v; return this; }
        public Builder rewards(HordeRewards v) { this.rewards = v; return this; }
        public Builder startMessage(String v) { this.startMessage = v; return this; }
        public Builder startTitle(String v) { this.startTitle = v; return this; }
        public Builder startSubtitle(String v) { this.startSubtitle = v; return this; }
        public Builder endMessage(String v) { this.endMessage = v; return this; }
        public Builder failMessage(String v) { this.failMessage = v; return this; }
        public Builder sound(String v) { this.sound = v; return this; }
        public HordeDefinition build() { return new HordeDefinition(this); }
    }
}
