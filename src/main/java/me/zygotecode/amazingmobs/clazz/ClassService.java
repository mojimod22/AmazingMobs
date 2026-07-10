package me.zygotecode.amazingmobs.clazz;

import me.zygotecode.amazingmobs.AmazingMobs;
import me.zygotecode.amazingmobs.util.Keys;
import me.zygotecode.amazingmobs.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Owns each player's chosen class: persistence (classes.yml), passive (de)application, and lifecycle
 * cleanup (join re-applies passives, death/quit dismisses minions). The single source of truth for
 * "what class is this player".
 */
public final class ClassService implements Listener {

    private final AmazingMobs plugin;
    private final ClassRegistry registry;
    private final MinionManager minions;
    private final Cooldowns cooldowns;
    private final Map<UUID, String> byPlayer = new HashMap<>();
    private File file;
    private BukkitTask passiveTask;

    public ClassService(AmazingMobs plugin, ClassRegistry registry, MinionManager minions, Cooldowns cooldowns) {
        this.plugin = plugin;
        this.registry = registry;
        this.minions = minions;
        this.cooldowns = cooldowns;
    }

    public boolean enabled() { return plugin.config().classesEnabled; }

    // ---- lifecycle -----------------------------------------------------------------------------

    public void start() {
        if (!enabled()) return;
        file = new File(plugin.getDataFolder(), "classes.yml");
        load();
        for (Player p : Bukkit.getOnlinePlayers()) applyPassives(p);
        // keep infinite passives topped up (some events strip effects); cheap, every 5s
        passiveTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player p : Bukkit.getOnlinePlayers()) if (classOf(p) != null) applyPassives(p);
        }, 100L, 100L);
    }

    public void stop() {
        if (passiveTask != null) { passiveTask.cancel(); passiveTask = null; }
        if (enabled()) save();
    }

    // ---- query / mutate ------------------------------------------------------------------------

    public PlayerClass classOf(UUID id) { return registry.classDef(byPlayer.get(id)); }
    public PlayerClass classOf(Player p) { return classOf(p.getUniqueId()); }
    public boolean has(Player p) { return classOf(p) != null; }

    public enum SetResult { OK, UNKNOWN, LOCKED, SAME }

    public SetResult setClass(Player p, String classId) {
        PlayerClass target = registry.classDef(classId);
        if (target == null) return SetResult.UNKNOWN;
        PlayerClass current = classOf(p);
        if (current != null && current.id().equals(target.id())) return SetResult.SAME;
        if (current != null && !plugin.config().allowClassChange) return SetResult.LOCKED;

        byPlayer.put(p.getUniqueId(), target.id());
        clearPassives(p);
        applyPassives(p);
        cooldowns.clear(p.getUniqueId());
        minions.removeAll(p.getUniqueId()); // old army doesn't carry to a new class
        save();
        p.sendMessage(Text.mm("<green>Class set: " + target.color() + "<bold>" + target.name() + "</bold></green> <gray>(" + target.role() + ")."));
        p.playSound(p.getLocation(), org.bukkit.Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1.2f);
        return SetResult.OK;
    }

    /** Wipe a player's class entirely (used when they prestige) so they re-pick from scratch. */
    public void clearClass(Player p) {
        if (byPlayer.remove(p.getUniqueId()) == null) return;
        clearPassives(p);
        cooldowns.clear(p.getUniqueId());
        minions.removeAll(p.getUniqueId());
        save();
    }

    // ---- passives ------------------------------------------------------------------------------

    public void applyPassives(Player p) {
        PlayerClass c = classOf(p);
        if (c == null) return;
        for (PlayerClass.Passive pa : c.passives()) {
            // refresh-only if already present at the right level, so we don't spam particles/re-add
            PotionEffect existing = p.getPotionEffect(pa.type());
            if (existing != null && existing.getAmplifier() == pa.amplifier() && existing.getDuration() > 40) continue;
            p.addPotionEffect(new PotionEffect(pa.type(), Integer.MAX_VALUE, pa.amplifier(), true, false, true));
        }
    }

    public void clearPassives(Player p) {
        for (PotionEffectType t : registry.allPassiveTypes()) p.removePotionEffect(t);
    }

    // ---- events --------------------------------------------------------------------------------

    @EventHandler
    public void onJoin(PlayerJoinEvent e) { if (enabled()) applyPassives(e.getPlayer()); }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent e) {
        if (enabled()) Bukkit.getScheduler().runTask(plugin, () -> applyPassives(e.getPlayer()));
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        // your summoned army falls with you
        minions.removeAll(e.getEntity().getUniqueId());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        minions.removeAll(e.getPlayer().getUniqueId());
        cooldowns.clear(e.getPlayer().getUniqueId());
        save();
    }

    /** Necromancer "Soul Harvest" passive: a mob a Necromancer kills may rise as a permanent minion. */
    @EventHandler
    public void onMobDeath(EntityDeathEvent e) {
        if (!enabled()) return;
        Player killer = e.getEntity().getKiller();
        if (killer == null) return;
        PlayerClass c = classOf(killer);
        if (c == null || !c.id().equals("necromancer")) return;
        if (e.getEntity().getPersistentDataContainer().has(Keys.MINION_OWNER, PersistentDataType.STRING)) return; // not our own minions
        int prestige = plugin.weightService() != null ? Math.max(1, plugin.weightService().prestigeOf(killer.getUniqueId())) : 1;
        double chance = Math.min(0.75, 0.35 + 0.04 * (prestige - 1));
        if (minions.convertKill(killer, e.getEntity(), prestige, chance)) {
            killer.sendActionBar(Text.mm("<dark_purple>☠ A fallen foe rises to serve you. <gray>(" + minions.count(killer.getUniqueId()) + "/" + minions.cap(prestige) + ")"));
        }
    }

    // ---- persistence ---------------------------------------------------------------------------

    public void load() {
        byPlayer.clear();
        if (file == null || !file.exists()) return;
        YamlConfiguration y = YamlConfiguration.loadConfiguration(file);
        var sec = y.getConfigurationSection("players");
        if (sec == null) return;
        for (String key : sec.getKeys(false)) {
            try {
                UUID id = UUID.fromString(key);
                String cls = sec.getString(key);
                if (registry.classDef(cls) != null) byPlayer.put(id, cls.toLowerCase());
            } catch (IllegalArgumentException ignored) {}
        }
    }

    public void save() {
        if (file == null) return;
        YamlConfiguration y = new YamlConfiguration();
        for (var en : byPlayer.entrySet()) y.set("players." + en.getKey(), en.getValue());
        try { y.save(file); } catch (Exception ex) { plugin.getLogger().warning("Could not save classes.yml: " + ex.getMessage()); }
    }
}
