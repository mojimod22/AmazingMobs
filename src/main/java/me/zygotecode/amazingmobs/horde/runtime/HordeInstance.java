package me.zygotecode.amazingmobs.horde.runtime;

import me.zygotecode.amazingmobs.api.event.HordeStopEvent;
import me.zygotecode.amazingmobs.api.event.WaveCompleteEvent;
import me.zygotecode.amazingmobs.horde.HordeDefinition;
import me.zygotecode.amazingmobs.horde.Wave;
import me.zygotecode.amazingmobs.horde.WaveEntry;
import me.zygotecode.amazingmobs.mob.runtime.MobManager;
import me.zygotecode.amazingmobs.mob.runtime.SpawnMeta;
import me.zygotecode.amazingmobs.area.SpawnFinder;
import me.zygotecode.amazingmobs.util.Fx;
import me.zygotecode.amazingmobs.util.Keys;
import me.zygotecode.amazingmobs.util.Rng;
import me.zygotecode.amazingmobs.util.Text;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Runs one horde event as a tick-driven state machine:
 * {@code DELAY → SPAWN → FIGHT} per wave, then {@code DONE}. Spawns are batched per tick and capped
 * by {@code maxConcurrentMobs}; failure to place a mob never stalls the wave (it is simply dropped
 * from the count). Cleans up leftover mobs + boss bar on finish.
 */
public final class HordeInstance {

    private enum Phase { DELAY, SPAWN, FIGHT, DONE }

    private record Queued(String mobId, String role, boolean boss, boolean objective) {}

    private final Plugin plugin;
    private final HordeDefinition def;
    private final MobManager mobManager;
    private final String instanceId;
    private final World world;
    private final Location center;
    private final int playerCount;
    private final int interval;
    private final Rng rng = Rng.shared();

    private Phase phase = Phase.DELAY;
    private int waveIndex = -1;
    private long totalTicks;
    private long stateTicks;
    private final List<Queued> queue = new ArrayList<>();
    private final Set<UUID> waveMobs = Collections.synchronizedSet(new java.util.HashSet<>());
    private final Set<UUID> objectiveMobs = Collections.synchronizedSet(new java.util.HashSet<>());
    private int spawnedThisWave;
    private double spawnMultiplier = 1.0;   // adjusted by the director
    private boolean completed;
    private boolean paused;
    private BossBar bossBar;

    public HordeInstance(Plugin plugin, HordeDefinition def, MobManager mobManager, String instanceId,
                         Location center, int playerCount, int interval) {
        this.plugin = plugin;
        this.def = def;
        this.mobManager = mobManager;
        this.instanceId = instanceId;
        this.center = center;
        this.world = center.getWorld();
        this.playerCount = Math.max(1, playerCount);
        this.interval = Math.max(1, interval);
    }

    public String instanceId() { return instanceId; }
    public HordeDefinition definition() { return def; }
    public World world() { return world; }
    public boolean isDone() { return phase == Phase.DONE; }
    public boolean completed() { return completed; }
    public int waveNumber() { return waveIndex + 1; }
    public int aliveCount() { return waveMobs.size(); }

    // ---- lifecycle -----------------------------------------------------------------------------

    public void begin() {
        announce(def.startTitle(), def.startSubtitle(), def.startMessage(), def.sound());
        if (def.startMessage() != null || def.startTitle() != null) setupBossBar();
        beginWave(0);
    }

    public void tick() {
        if (phase == Phase.DONE) return;
        if (paused) { updateBossBar(); return; }
        totalTicks += interval;
        stateTicks += interval;

        if (def.durationTicks() > 0 && totalTicks >= def.durationTicks()) { finish(false); return; }
        if (audience().isEmpty() && phase == Phase.FIGHT && aliveCount() == 0) { /* nobody around, let it idle */ }

        switch (phase) {
            case DELAY -> {
                if (stateTicks >= currentWave().startDelayTicks()) {
                    announce(currentWave().title(), currentWave().subtitle(), currentWave().message(), currentWave().sound());
                    phase = Phase.SPAWN;
                    stateTicks = 0;
                }
            }
            case SPAWN -> {
                spawnBatch();
                if (queue.isEmpty()) { phase = Phase.FIGHT; stateTicks = 0; }
                else if (stateTicks > 200) { // safety net: never hang a wave if placement keeps failing
                    plugin.getLogger().warning("[horde] '" + def.id() + "' wave " + waveNumber()
                            + " could not place " + queue.size() + " mob(s) within 10s; proceeding.");
                    queue.clear();
                    phase = Phase.FIGHT; stateTicks = 0;
                }
            }
            case FIGHT -> {
                pruneDead();
                boolean cleared = clearedEnough();
                boolean timedOut = currentWave().durationTicks() > 0 && stateTicks >= currentWave().durationTicks();
                if (cleared || timedOut) {
                    adaptDirector(stateTicks);
                    Bukkit.getPluginManager().callEvent(new WaveCompleteEvent(def, instanceId, waveIndex));
                    advance();
                }
            }
            default -> { }
        }
        updateBossBar();
    }

    /** Force-stop (admin command / disable). Cleans up. */
    public void stop(boolean asCompleted) {
        finish(asCompleted);
    }

    // ---- wave control --------------------------------------------------------------------------

    private void advance() {
        if (waveIndex >= def.waves().size() - 1) {
            if (def.infinite()) beginWave(0);
            else finish(true);
        } else {
            beginWave(waveIndex + 1);
        }
    }

    private void beginWave(int i) {
        waveIndex = i;
        Wave wave = def.waves().get(i);
        queue.clear();
        waveMobs.clear();
        objectiveMobs.clear();
        spawnedThisWave = 0;
        for (WaveEntry e : wave.entries()) {
            int c = (int) Math.round(e.resolveCount(playerCount, def.maxPlayers(), rng) * spawnMultiplier);
            for (int n = 0; n < c; n++) queue.add(new Queued(e.mobId(), e.role(), e.boss(), e.objective()));
        }
        Collections.shuffle(queue);
        phase = Phase.DELAY;
        stateTicks = 0;
    }

    /** Nudge the director's spawn multiplier based on how fast the wave was beaten. */
    private void adaptDirector(long fightTicks) {
        var dir = def.director();
        if (!dir.enabled()) return;
        if (fightTicks < dir.targetClearTicks()) spawnMultiplier = Math.min(dir.maxMultiplier(), spawnMultiplier + dir.step());
        else spawnMultiplier = Math.max(dir.minMultiplier(), spawnMultiplier - dir.step());
    }

    public double spawnMultiplier() { return spawnMultiplier; }
    public boolean paused() { return paused; }
    public void setPaused(boolean paused) { this.paused = paused; }

    private void spawnBatch() {
        int budget = def.maxSpawnsPerTick();
        // Spawn around the players actually in the arena, on their surface level, so mobs appear
        // in view (never under/over the arena, on cliffs, or stuck to walls).
        java.util.List<org.bukkit.entity.Player> refs = audience();
        double minDist = def.minPlayerDistance();
        double maxDist = Math.max(minDist + 6, Math.min(def.area().radius() - 3, minDist + 18));
        while (budget-- > 0 && !queue.isEmpty()) {
            if (aliveCount() >= def.maxConcurrentMobs()) return; // concurrency cap reached
            Queued q = queue.remove(queue.size() - 1);
            Location loc = SpawnFinder.findNearPlayers(center, def.area(), refs, minDist, maxDist, 6, def.spawnAttempts(), rng);
            if (loc == null) // players out of bounds / no footing near them — fall back to the arena centre
                loc = SpawnFinder.findNearPlayers(center, def.area(), null, minDist, maxDist, 8, def.spawnAttempts(), rng);
            if (loc == null) continue; // couldn't place this one this tick; drop (count not incremented)
            LivingEntity e = mobManager.spawn(q.mobId(), loc,
                    new SpawnMeta(instanceId, waveIndex, q.role(), playerCount, def.difficulty()));
            if (e != null) {
                waveMobs.add(e.getUniqueId());
                if (q.objective()) objectiveMobs.add(e.getUniqueId());
                spawnedThisWave++;
            }
        }
    }

    private void pruneDead() {
        java.util.function.Predicate<UUID> dead = id -> {
            Entity e = Bukkit.getEntity(id);
            return e == null || e.isDead() || !e.isValid();
        };
        waveMobs.removeIf(dead);
        objectiveMobs.removeIf(dead);
    }

    private boolean clearedEnough() {
        // Objective waves (battlefield objects): cleared only when every objective is destroyed,
        // regardless of how many adds remain — but not before all queued objectives have spawned.
        if (currentWave().hasObjectives()) {
            return queue.isEmpty() && objectiveMobs.isEmpty();
        }
        if (spawnedThisWave <= 0 && queue.isEmpty()) return true;
        if (aliveCount() == 0) return true;
        double killed = (spawnedThisWave - aliveCount()) / (double) Math.max(1, spawnedThisWave);
        return killed >= currentWave().clearThreshold();
    }

    private Wave currentWave() {
        return def.waves().get(Math.max(0, waveIndex));
    }

    // ---- finish --------------------------------------------------------------------------------

    private void finish(boolean success) {
        if (phase == Phase.DONE) return;
        phase = Phase.DONE;
        completed = success;

        if (success && !def.rewards().isEmpty()) {
            for (Player p : audience()) def.rewards().grant(p, rng);
        }
        String msg = success ? def.endMessage() : def.failMessage();
        if (msg != null && !msg.isBlank()) for (Player p : audience()) p.sendMessage(Text.mm(msg));

        removeInstanceMobs();
        if (bossBar != null) { bossBar.removeAll(); bossBar = null; }
        Bukkit.getPluginManager().callEvent(new HordeStopEvent(def, instanceId, success));
    }

    private void removeInstanceMobs() {
        // current wave
        synchronized (waveMobs) {
            for (UUID id : waveMobs) {
                Entity e = Bukkit.getEntity(id);
                if (e != null) e.remove();
            }
            waveMobs.clear();
        }
        // sweep any stragglers tagged with this instance id across the world
        for (LivingEntity e : world.getLivingEntities()) {
            String tag = e.getPersistentDataContainer().get(Keys.HORDE_INSTANCE, PersistentDataType.STRING);
            if (instanceId.equals(tag)) e.remove();
        }
    }

    // ---- presentation --------------------------------------------------------------------------

    private List<Player> audience() {
        List<Player> out = new ArrayList<>();
        for (Player p : world.getPlayers()) {
            if (def.area().contains(center, p.getLocation())) out.add(p);
        }
        if (out.isEmpty()) out.addAll(world.getPlayers()); // fallback: whole world
        return out;
    }

    private void announce(String title, String subtitle, String message, String sound) {
        List<Player> audience = audience();
        for (Player p : audience) {
            if (message != null && !message.isBlank()) p.sendMessage(Text.mm(message));
            if (title != null && !title.isBlank()) {
                p.showTitle(Title.title(Text.mm(title), Text.mm(subtitle == null ? "" : subtitle),
                        Title.Times.times(Duration.ofMillis(300), Duration.ofMillis(2500), Duration.ofMillis(600))));
            }
            Fx.sound(p.getLocation(), sound, 1f, 1f);
        }
    }

    private void setupBossBar() {
        bossBar = Bukkit.createBossBar(Text.legacy(def.name()), BarColor.RED, BarStyle.SEGMENTED_10);
        bossBar.setProgress(1.0);
    }

    private void updateBossBar() {
        if (bossBar == null) return;
        double progress = spawnedThisWave <= 0 ? 1.0
                : Math.max(0, Math.min(1, aliveCount() / (double) spawnedThisWave));
        bossBar.setProgress(progress);
        bossBar.setTitle(Text.legacy(def.name()) + "  §7Wave " + waveNumber() + "/" + def.waves().size()
                + " §c(" + aliveCount() + ")");
        for (Player p : new ArrayList<>(bossBar.getPlayers())) {
            if (!p.getWorld().equals(world) || !def.area().contains(center, p.getLocation())) bossBar.removePlayer(p);
        }
        for (Player p : audience()) if (!bossBar.getPlayers().contains(p)) bossBar.addPlayer(p);
    }
}
