package me.zygotecode.amazingmobs.player;

import me.zygotecode.amazingmobs.AmazingMobs;
import me.zygotecode.amazingmobs.api.event.CustomMobDeathEvent;
import me.zygotecode.amazingmobs.mob.Tier;
import me.zygotecode.amazingmobs.util.Keys;
import me.zygotecode.amazingmobs.util.Text;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The "Weight" progression RPG. A universal, persistent per-player stat (kg) that grows with every
 * kill and grants more max-health, melee strength and a slight speed boost — scaled so the goal feels
 * strong but never absurd. Reach the goal and {@code /am prestige} to reset to base for a permanent
 * tier (a heart + a little strength, stacking). Sit at the goal too long without prestiging and the
 * weight decays back down (no parking at max for free).
 *
 * <p>Bonuses are applied as keyed {@link AttributeModifier}s (idempotent: removed-by-key then re-added),
 * so they layer cleanly on top of vanilla values and are re-applied on join.</p>
 */
public final class WeightService implements Listener {

    private final AmazingMobs plugin;
    private final double base, goal, decayTo;
    private final long decayMillis;
    private final boolean enabled;

    private final Map<UUID, Entry> data = new HashMap<>();
    private final Map<UUID, Scoreboard> boards = new HashMap<>();
    private boolean sidebarVisible = false;
    private File file;
    private BukkitTask decayTask;

    public WeightService(AmazingMobs plugin) {
        this.plugin = plugin;
        var c = plugin.config();
        this.enabled = c.weightEnabled;
        this.base = c.weightBase;
        this.goal = Math.max(c.weightBase + 1, c.weightGoal);
        this.decayTo = Math.min(goal - 1, Math.max(base, c.weightDecayTo));
        this.decayMillis = (long) (Math.max(0.1, c.weightDecayMinutes) * 60_000L);
    }

    private static final class Entry {
        double weight;
        int prestige = 1;
        long reachedAt = 0L; // wall-clock millis the goal was hit (0 = not at goal)
        Entry(double w, int p, long r) { weight = w; prestige = p; reachedAt = r; }
    }

    public boolean enabled() { return enabled; }

    // ---- lifecycle -----------------------------------------------------------------------------

    public void start() {
        if (!enabled) return;
        file = new File(plugin.getDataFolder(), "weights.yml");
        load();
        sidebarVisible = true; // the Weight sidebar is shown permanently, not only during hordes
        // re-apply bonuses + show the sidebar to anyone already online (e.g. after /reload)
        for (Player p : Bukkit.getOnlinePlayers()) { ensure(p.getUniqueId()); applyEffects(p); updateBoard(p); }
        decayTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 200L, 200L); // every 10s
    }

    public void stop() {
        if (decayTask != null) { decayTask.cancel(); decayTask = null; }
        if (enabled) save();
    }

    // ---- public API ----------------------------------------------------------------------------

    public double weightOf(UUID id) { Entry e = data.get(id); return e != null ? e.weight : base; }
    public int prestigeOf(UUID id) { Entry e = data.get(id); return e != null ? e.prestige : 1; }
    public double goal() { return goal; }

    /** Award weight for a kill, scaled by mob difficulty. */
    public void awardKill(Player killer, Tier tier) {
        if (!enabled || killer == null) return;
        addWeight(killer, gainFor(tier));
    }

    /** Small award for a vanilla (non-custom) mob kill. */
    public void awardVanillaKill(Player killer) {
        if (!enabled || killer == null) return;
        addWeight(killer, 0.2);
    }

    /** Add (or subtract) weight, clamp to [base, goal], refresh bonuses + sidebar, notify on goal. */
    public void addWeight(Player p, double kg) {
        if (!enabled) return;
        Entry e = ensure(p.getUniqueId());
        boolean wasAtGoal = e.weight >= goal;
        e.weight = clamp(e.weight + kg, base, goal);
        if (e.weight >= goal && !wasAtGoal) {
            e.reachedAt = System.currentTimeMillis();
            p.sendMessage(Text.mm("<gradient:#f7971e:#ffd200><bold>⚖ Peak Weight!</bold></gradient> "
                    + "<gray>You hit <white>" + (int) goal + "kg</white> — run <yellow>/am prestige</yellow> "
                    + "to bank a permanent tier, or you'll slim back to <white>" + (int) decayTo + "kg</white>.</gray>"));
            p.playSound(p.getLocation(), org.bukkit.Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
        }
        applyEffects(p);
        if (sidebarVisible) updateBoard(p);
    }

    public enum PrestigeResult { OK, NOT_ENOUGH, DISABLED }

    /** Bank a prestige tier if the player is at the goal: reset to base, +permanent tier. */
    public PrestigeResult prestige(Player p) {
        if (!enabled) return PrestigeResult.DISABLED;
        Entry e = ensure(p.getUniqueId());
        if (e.weight < goal) return PrestigeResult.NOT_ENOUGH;
        e.prestige += 1;
        e.weight = base;
        e.reachedAt = 0L;
        applyEffects(p);
        if (sidebarVisible) updateBoard(p);
        save();
        p.playSound(p.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.2f);
        p.sendMessage(Text.mm("<gradient:#c471f5:#fa71cd><bold>★ WEIGHT PRESTIGE " + e.prestige + "</bold></gradient> "
                + "<gray>— permanent <white>+1 heart</white> & <white>+strength</white>. Back to <white>"
                + (int) base + "kg</white>; grow again!</gray>"));
        // prestiging resets your class — choose a fresh one
        var cs = plugin.classService();
        if (cs != null && cs.enabled() && cs.classOf(p) != null) {
            cs.clearClass(p);
            if (sidebarVisible) updateBoard(p);
            p.sendMessage(Text.mm("<gray>Your class was reset — pick a fresh one with <yellow>/am class</yellow>."));
        }
        return PrestigeResult.OK;
    }

    // ---- effects (attribute modifiers) ---------------------------------------------------------

    /** Recompute and apply the weight/prestige bonuses to a player (idempotent). */
    public void applyEffects(Player p) {
        if (!enabled) return;
        Entry e = ensure(p.getUniqueId());
        double t = clamp((e.weight - base) / (goal - base), 0, 1);
        int pr = e.prestige - 1; // prestige 1 = no permanent bonus yet

        double healthBonus = t * 8.0 + pr * 2.0;     // up to +4 hearts at goal, +1 heart / prestige
        double damageBonus = t * 3.0 + pr * 0.5;      // up to +3 dmg at goal, +0.5 / prestige
        double speedBonus  = t * 0.015 + pr * 0.003;  // slight: +~15% of base walk speed at goal

        setModifier(p, Attribute.MAX_HEALTH, Keys.WEIGHT_HEALTH, healthBonus);
        setModifier(p, Attribute.ATTACK_DAMAGE, Keys.WEIGHT_DAMAGE, damageBonus);
        setModifier(p, Attribute.MOVEMENT_SPEED, Keys.WEIGHT_SPEED, speedBonus);

        // never leave the player above their (possibly lowered) max after a decay/prestige
        AttributeInstance max = p.getAttribute(Attribute.MAX_HEALTH);
        if (max != null && p.getHealth() > max.getValue()) p.setHealth(max.getValue());
    }

    private void setModifier(Player p, Attribute attr, NamespacedKey key, double amount) {
        AttributeInstance inst = p.getAttribute(attr);
        if (inst == null) return;
        try { inst.removeModifier(key); } catch (Throwable ignored) {}
        if (amount != 0) {
            inst.addModifier(new AttributeModifier(key, amount, AttributeModifier.Operation.ADD_NUMBER));
        }
    }

    /** Strip our modifiers (used on quit so they never double-apply on a botched relog). */
    public void clearEffects(Player p) {
        for (var pair : new Attribute[]{Attribute.MAX_HEALTH, Attribute.ATTACK_DAMAGE, Attribute.MOVEMENT_SPEED}) {
            AttributeInstance inst = p.getAttribute(pair);
            if (inst == null) continue;
            try { inst.removeModifier(Keys.WEIGHT_HEALTH); } catch (Throwable ignored) {}
            try { inst.removeModifier(Keys.WEIGHT_DAMAGE); } catch (Throwable ignored) {}
            try { inst.removeModifier(Keys.WEIGHT_SPEED); } catch (Throwable ignored) {}
        }
    }

    // ---- decay ---------------------------------------------------------------------------------

    private void tick() {
        long now = System.currentTimeMillis();
        for (Player p : Bukkit.getOnlinePlayers()) {
            Entry e = data.get(p.getUniqueId());
            if (e == null) continue;
            if (e.reachedAt > 0 && e.weight >= goal && now - e.reachedAt >= decayMillis) {
                e.weight = decayTo;
                e.reachedAt = 0L;
                applyEffects(p);
                if (sidebarVisible) updateBoard(p);
                p.sendMessage(Text.mm("<gray>You lingered at peak weight — slimmed back to <white>"
                        + (int) decayTo + "kg</white>. Prestige next time to keep the gains.</gray>"));
            }
        }
    }

    // ---- scoreboard ----------------------------------------------------------------------------

    /** Refresh + show the Weight sidebar for everyone. Sidebar is permanent; this just forces a redraw. */
    public void showAll() {
        if (!enabled) return;
        sidebarVisible = true;
        for (Player p : Bukkit.getOnlinePlayers()) updateBoard(p);
    }

    /** Hide the sidebar from everyone (admin/disable use only — not called during normal play). */
    public void hideAll() {
        sidebarVisible = false;
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
        }
        boards.clear();
    }

    public boolean sidebarVisible() { return sidebarVisible; }

    private void updateBoard(Player p) {
        Entry e = ensure(p.getUniqueId());
        Scoreboard sb = boards.computeIfAbsent(p.getUniqueId(), k -> Bukkit.getScoreboardManager().getNewScoreboard());
        Objective o = sb.getObjective("amweight");
        if (o != null) o.unregister();
        Component title = Text.mm("<gradient:#f7971e:#ffd200><bold>⚖ WEIGHT</bold></gradient>");
        o = sb.registerNewObjective("amweight", "dummy", title);
        o.setDisplaySlot(DisplaySlot.SIDEBAR);

        int w = (int) Math.round(e.weight);
        boolean atGoal = e.weight >= goal;
        String s = "§";
        String className = null;
        var cs = plugin.classService();
        if (cs != null && cs.enabled()) { var pc = cs.classOf(p); if (pc != null) className = pc.name(); }
        line(o, s + "8━━━━━━━━━", 7);
        if (className != null) line(o, s + "6Class: " + s + "f" + className, 6);
        line(o, s + "7Goal: " + s + "f" + (int) goal + "kg", 5);
        line(o, (atGoal ? s + "a" : s + "e") + "Weight: " + s + "f" + w + "kg", 4);
        line(o, s + "dPrestige: " + s + "f" + e.prestige, 3);
        line(o, s + "8━━━━━━━━━━", 1);
        p.setScoreboard(sb);
    }

    private static void line(Objective o, String text, int order) {
        if (text.length() > 40) text = text.substring(0, 40);
        o.getScore(text).setScore(order);
    }

    // ---- events --------------------------------------------------------------------------------

    @EventHandler
    public void onCustomMobDeath(CustomMobDeathEvent e) {
        if (e.getKiller() != null) awardKill(e.getKiller(), e.getDefinition().tier());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent e) {
        if (!enabled) return;
        // custom mobs are handled via CustomMobDeathEvent (richer tier info); skip them here
        if (e.getEntity().getPersistentDataContainer().has(Keys.MOB_ID)) return;
        Player killer = e.getEntity().getKiller();
        if (killer != null) awardVanillaKill(killer);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        if (!enabled) return;
        Player p = e.getPlayer();
        Entry en = ensure(p.getUniqueId());
        // a player who logged off parked at the goal still decays while away
        if (en.reachedAt > 0 && en.weight >= goal && System.currentTimeMillis() - en.reachedAt >= decayMillis) {
            en.weight = decayTo; en.reachedAt = 0L;
        }
        applyEffects(p);
        updateBoard(p); // sidebar is permanent
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        boards.remove(e.getPlayer().getUniqueId());
        // bonuses are re-applied on next join; leaving them on is also fine, but clear to be safe
        save();
    }

    // ---- persistence ---------------------------------------------------------------------------

    public void load() {
        data.clear();
        if (file == null || !file.exists()) return;
        YamlConfiguration y = YamlConfiguration.loadConfiguration(file);
        var sec = y.getConfigurationSection("players");
        if (sec == null) return;
        for (String key : sec.getKeys(false)) {
            try {
                UUID id = UUID.fromString(key);
                double w = sec.getDouble(key + ".weight", base);
                int pr = Math.max(1, sec.getInt(key + ".prestige", 1));
                long ra = sec.getLong(key + ".reached-at", 0L);
                data.put(id, new Entry(clamp(w, base, goal), pr, ra));
            } catch (IllegalArgumentException ignored) { /* bad uuid */ }
        }
    }

    public void save() {
        if (file == null) return;
        YamlConfiguration y = new YamlConfiguration();
        for (var en : data.entrySet()) {
            String base = "players." + en.getKey();
            y.set(base + ".weight", round1(en.getValue().weight));
            y.set(base + ".prestige", en.getValue().prestige);
            y.set(base + ".reached-at", en.getValue().reachedAt);
        }
        try { y.save(file); } catch (Exception ex) {
            plugin.getLogger().warning("Could not save weights.yml: " + ex.getMessage());
        }
    }

    // ---- helpers -------------------------------------------------------------------------------

    private Entry ensure(UUID id) { return data.computeIfAbsent(id, k -> new Entry(base, 1, 0L)); }

    private double gainFor(Tier tier) {
        return switch (tier) {
            case COMMON -> 0.3;
            case UNCOMMON -> 0.5;
            case RARE -> 1.0;
            case ELITE -> 2.0;
            case MINIBOSS -> 3.5;
            case BOSS -> 5.0;
        };
    }

    private static double clamp(double v, double lo, double hi) { return v < lo ? lo : Math.min(v, hi); }
    private static double round1(double v) { return Math.round(v * 10.0) / 10.0; }
}
