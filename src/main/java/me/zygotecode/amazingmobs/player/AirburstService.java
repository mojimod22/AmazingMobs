package me.zygotecode.amazingmobs.player;

import me.zygotecode.amazingmobs.AmazingMobs;
import me.zygotecode.amazingmobs.clazz.SkillContext;
import me.zygotecode.amazingmobs.util.Text;
import org.bukkit.FluidCollisionMode;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * <b>Airburst</b> — an always-on combat behaviour (not a skill, no class needed). When a mob's hit
 * launches a player more than {@code airburst.min-height} blocks above the ground beneath them, a
 * retaliatory blast detonates at the apex. Its size is a <i>delta</i> of the player's current Weight,
 * prestige, launch speed, and height above ground; it ignites + damages surrounding mobs and calls
 * precise lightning onto nearby mobs (and an even ring around the player). It never harms the player.
 * Active for everyone, whether or not a horde is running.
 */
public final class AirburstService implements Listener {

    private final AmazingMobs plugin;
    private final Map<UUID, State> states = new HashMap<>();
    private BukkitTask task;

    public AirburstService(AmazingMobs plugin) { this.plugin = plugin; }

    public boolean enabled() { return plugin.config().airburstEnabled; }

    private static final class State {
        boolean armed;
        long armedAt;
        double maxVel;
        double peak;        // max height above ground reached while airborne
        boolean leftGround; // actually got launched off the ground (not just hit while standing)
        boolean fired;
    }

    public void start() {
        if (!enabled()) return;
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 2L, 2L);
    }

    public void stop() {
        if (task != null) { task.cancel(); task = null; }
        states.clear();
    }

    /** Arm the player when a mob/projectile/explosion hits them (the thing that can launch them). */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent e) {
        if (!enabled() || !(e.getEntity() instanceof Player p)) return;
        if (p.getGameMode() == GameMode.CREATIVE || p.getGameMode() == GameMode.SPECTATOR) return;
        if (!isCombat(e.getCause())) return;
        State s = states.computeIfAbsent(p.getUniqueId(), k -> new State());
        s.armed = true;
        s.armedAt = System.currentTimeMillis();
        s.maxVel = 0;
        s.peak = 0;
        s.leftGround = false;
        s.fired = false;
    }

    private static boolean isCombat(EntityDamageEvent.DamageCause c) {
        return c == EntityDamageEvent.DamageCause.ENTITY_ATTACK
                || c == EntityDamageEvent.DamageCause.ENTITY_SWEEP_ATTACK
                || c == EntityDamageEvent.DamageCause.ENTITY_EXPLOSION
                || c == EntityDamageEvent.DamageCause.PROJECTILE
                || c == EntityDamageEvent.DamageCause.MAGIC;
    }

    private void tick() {
        long now = System.currentTimeMillis();
        double minH = plugin.config().airburstMinHeight;
        Iterator<Map.Entry<UUID, State>> it = states.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, State> en = it.next();
            Player p = plugin.getServer().getPlayer(en.getKey());
            State s = en.getValue();
            if (p == null) { it.remove(); continue; }
            if (!s.armed) continue;
            if (now - s.armedAt > 8000 || p.getGameMode() == GameMode.CREATIVE || p.getGameMode() == GameMode.SPECTATOR) {
                s.armed = false;
                continue;
            }

            if (!p.isOnGround()) {
                // airborne: remember we were launched + track peak height and launch speed
                s.leftGround = true;
                s.maxVel = Math.max(s.maxVel, p.getVelocity().length());
                s.peak = Math.max(s.peak, heightAboveGround(p));
            } else if (s.leftGround) {
                // feet back on the ground: NOW it goes off (if the launch cleared the threshold)
                if (!s.fired && s.peak > minH) detonate(p, s.peak, s.maxVel);
                s.armed = false; // one burst per launch
            }
        }
    }

    /** Distance from the player down to the first solid ground (capped at 64). */
    private double heightAboveGround(Player p) {
        Location loc = p.getLocation();
        World w = loc.getWorld();
        if (w == null) return 0;
        RayTraceResult rt = w.rayTraceBlocks(loc, new Vector(0, -1, 0), 64, FluidCollisionMode.NEVER, true);
        if (rt != null && rt.getHitPosition() != null) return Math.max(0, loc.getY() - rt.getHitPosition().getY());
        return 64;
    }

    /** The blast. Delta of weight × prestige × launch speed × height → radius / damage / strikes. */
    private void detonate(Player p, double height, double velocity) {
        WeightService ws = plugin.weightService();
        double weight = ws != null ? ws.weightOf(p.getUniqueId()) : 60;
        int prestige = ws != null ? Math.max(1, ws.prestigeOf(p.getUniqueId())) : 1;

        double mass = Math.max(0.5, weight / 60.0);          // 1.0 at base, ~2.9 at goal
        double pres = 1 + 0.12 * (prestige - 1);
        double vel = Math.min(3.0, velocity);
        double delta = (height * 0.25) * mass * pres * (1 + vel * 0.15);

        double radius = clamp(delta, 2.0, 12.0);
        float power = (float) clamp(delta, 4.0, 8.0);   // TNT (4) → massive (8): real, crater-digging blast
        int strikes = (int) clamp(Math.round(delta / 2.0), 1, 8);
        int fireTicks = (int) clamp(40 + height * 4, 40, 160);
        // lightning + fire stay OUTSIDE this ring so they never hit the player at the centre
        double safe = Math.max(6.0, radius * 0.7);
        double ringR = Math.max(radius, safe);

        Location c = p.getLocation();
        World w = c.getWorld();
        if (w == null) return;

        // gather enemy mobs nearby (never the player, never minions)
        List<LivingEntity> near = new ArrayList<>();
        for (Entity e : w.getNearbyEntities(c, radius + 6, radius + 6, radius + 6)) {
            if (SkillContext.isEnemy(e, p) && e instanceof LivingEntity le) near.add(le);
        }

        // EXPLOSION: a real TNT-grade blast that craters the terrain + damages mobs. We make every
        // nearby PLAYER briefly invulnerable across the (synchronous) explosion so it never hurts them,
        // then restore — so the area takes it, the players don't.
        boolean breakBlocks = plugin.config().airburstBreakBlocks;
        List<Player> shielded = new ArrayList<>();
        for (Entity e : w.getNearbyEntities(c, power * 2 + 4, power * 2 + 4, power * 2 + 4)) {
            if (e instanceof Player pl && !pl.isInvulnerable()) { pl.setInvulnerable(true); shielded.add(pl); }
        }
        try {
            w.createExplosion(c, power, true, breakBlocks, p); // setFire=true, breakBlocks per config
        } finally {
            for (Player pl : shielded) pl.setInvulnerable(false);
        }
        // make sure the surrounding mobs actually burn
        for (LivingEntity le : near) le.setFireTicks(fireTicks);

        // LIGHTNING: only onto mobs at a SAFE distance, then fill the count on a precise ring — never
        // within `safe` blocks of the player, so the bolts (and their fire) never strike the player.
        near.sort(Comparator.comparingDouble(le -> le.getLocation().distanceSquared(c)));
        int placed = 0;
        for (LivingEntity le : near) {
            if (placed >= strikes) break;
            if (le.getLocation().distance(c) < safe) continue; // too close to the player — skip
            w.strikeLightning(le.getLocation());
            placed++;
        }
        for (int i = placed; i < strikes; i++) {
            double a = (Math.PI * 2 * i) / Math.max(1, strikes);
            Location ring = c.clone().add(Math.cos(a) * ringR, 0, Math.sin(a) * ringR);
            w.strikeLightning(groundUnder(ring));
        }

        p.sendActionBar(Text.mm("<gold>✸ Airburst! <gray>radius <white>" + (int) radius + "</white> · <white>" + strikes + "</white>⚡"));
    }

    private Location groundUnder(Location loc) {
        World w = loc.getWorld();
        if (w == null) return loc;
        RayTraceResult rt = w.rayTraceBlocks(loc, new Vector(0, -1, 0), 24, FluidCollisionMode.NEVER, true);
        if (rt != null && rt.getHitPosition() != null) {
            Vector hp = rt.getHitPosition();
            return new Location(w, hp.getX(), hp.getY(), hp.getZ());
        }
        return loc;
    }

    private static double clamp(double v, double lo, double hi) { return v < lo ? lo : Math.min(v, hi); }
}
