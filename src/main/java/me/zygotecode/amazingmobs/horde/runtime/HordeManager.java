package me.zygotecode.amazingmobs.horde.runtime;

import me.zygotecode.amazingmobs.AmazingMobs;
import me.zygotecode.amazingmobs.api.event.HordeStartEvent;
import me.zygotecode.amazingmobs.horde.HordeDefinition;
import me.zygotecode.amazingmobs.horde.HordeRegistry;
import me.zygotecode.amazingmobs.mob.runtime.MobManager;
import me.zygotecode.amazingmobs.player.PlayerKit;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Orchestrates horde events: validates activation gating (world/time/biome/players) + a concurrency
 * cap, and drives every {@link HordeInstance} from one shared task. There is intentionally NO
 * start/stop cooldown — hordes can always be started/stopped on demand.
 */
public final class HordeManager {

    private final Plugin plugin;
    private final HordeRegistry registry;
    private final MobManager mobManager;
    private final int interval;
    private final int maxConcurrent;

    private final Map<String, HordeInstance> active = new ConcurrentHashMap<>();
    private BukkitTask task;
    private long ticks;
    private int counter;

    public HordeManager(Plugin plugin, HordeRegistry registry, MobManager mobManager,
                        int interval, int maxConcurrent) {
        this.plugin = plugin;
        this.registry = registry;
        this.mobManager = mobManager;
        this.interval = Math.max(1, interval);
        this.maxConcurrent = Math.max(1, maxConcurrent);
    }

    public void start() {
        if (task != null) return;
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tickAll, interval, interval);
    }

    public void stop() {
        if (task != null) { task.cancel(); task = null; }
        for (HordeInstance inst : new ArrayList<>(active.values())) inst.stop(false);
        active.clear();
    }

    private void tickAll() {
        ticks += interval;
        for (HordeInstance inst : new ArrayList<>(active.values())) {
            try {
                inst.tick();
            } catch (Throwable t) {
                plugin.getLogger().warning("[horde] tick failed for '" + inst.definition().id() + "': " + t);
                inst.stop(false);
            }
            if (inst.isDone()) {
                active.remove(inst.instanceId());
            }
        }
    }

    // ---- start / stop --------------------------------------------------------------------------

    public StartResult start(String hordeId, Location requestedCenter, int playerCount, boolean force) {
        HordeDefinition def = registry.get(hordeId);
        if (def == null) return StartResult.fail("unknown horde '" + hordeId + "'");
        return start(def, requestedCenter, playerCount, force);
    }

    public StartResult start(HordeDefinition def, Location requestedCenter, int playerCount, boolean force) {
        if (active.size() >= maxConcurrent) return StartResult.fail("too many hordes running (" + maxConcurrent + ")");

        // resolve center
        Location center;
        if (def.area().dynamic()) {
            if (requestedCenter == null) return StartResult.fail("this horde uses a dynamic area — run it at a location");
            center = requestedCenter;
        } else {
            World w = Bukkit.getWorld(def.area().worldName());
            if (w == null) return StartResult.fail("area world '" + def.area().worldName() + "' is not loaded");
            center = def.area().resolveCenter(w, null);
        }
        World world = center.getWorld();
        if (world == null) return StartResult.fail("could not resolve a world for the area");

        if (!def.allowsWorld(world.getName())) return StartResult.fail("horde not allowed in world '" + world.getName() + "'");

        if (!force) {
            // No start/stop cooldown by design — hordes are always startable subject only to
            // world/time/biome/player gating (and the concurrency cap above).
            if (!def.allowsTime(world.getTime())) return StartResult.fail("outside this horde's time window");
            if (!biomeOk(def, center)) return StartResult.fail("biome not allowed here");
        }

        int participants = playerCount > 0 ? playerCount : countInArea(def, center);
        if (!force && participants < def.minPlayers()) {
            return StartResult.fail("needs at least " + def.minPlayers() + " player(s) in the area");
        }

        String instanceId = def.id() + "-" + (++counter);
        HordeStartEvent event = new HordeStartEvent(def, instanceId, center);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) return StartResult.fail("start cancelled by another plugin");

        HordeInstance inst = new HordeInstance(plugin, def, mobManager, instanceId, center,
                Math.max(1, participants), interval);
        active.put(instanceId, inst);
        prepPlayers();   // heal/clear/kit + show Weight sidebar (server-wide for now)
        inst.begin();
        return StartResult.ok(inst);
    }

    /** Battle-prep every online player and reveal the Weight sidebar when a horde kicks off. */
    private void prepPlayers() {
        if (!(plugin instanceof AmazingMobs am)) return;
        if (am.config().equipAllOnStart) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                try { PlayerKit.resetAndEquip(am, p); }
                catch (Throwable t) { plugin.getLogger().warning("[horde] could not equip " + p.getName() + ": " + t); }
            }
        }
        if (am.weightService() != null) am.weightService().showAll();
    }

    public boolean stopInstance(String instanceId) {
        HordeInstance inst = active.remove(instanceId);
        if (inst == null) return false;
        inst.stop(false);
        return true;
    }

    public int stopAll() {
        int n = active.size();
        for (HordeInstance inst : new ArrayList<>(active.values())) inst.stop(false);
        active.clear();
        return n;
    }

    // ---- queries -------------------------------------------------------------------------------

    public List<HordeInstance> activeInstances() { return new ArrayList<>(active.values()); }
    public int activeCount() { return active.size(); }

    /** Pause/resume one instance ("all" handled by the caller via {@link #freezeAll}). */
    public boolean freeze(String instanceId, boolean paused) {
        HordeInstance inst = active.get(instanceId);
        if (inst == null) return false;
        inst.setPaused(paused);
        return true;
    }

    public int freezeAll(boolean paused) {
        for (HordeInstance inst : active.values()) inst.setPaused(paused);
        return active.size();
    }

    // ---- helpers -------------------------------------------------------------------------------

    private boolean biomeOk(HordeDefinition def, Location center) {
        if (def.biomeAllow().isEmpty() && def.biomeDeny().isEmpty()) return true;
        Biome biome = center.getWorld().getBiome(center);
        String name = biome.getKey().getKey().toLowerCase(Locale.ROOT);
        if (!def.biomeDeny().isEmpty() && def.biomeDeny().contains(name)) return false;
        return def.biomeAllow().isEmpty() || def.biomeAllow().contains(name);
    }

    private int countInArea(HordeDefinition def, Location center) {
        int n = 0;
        for (Player p : center.getWorld().getPlayers()) {
            if (def.area().contains(center, p.getLocation())) n++;
        }
        return n;
    }
}
