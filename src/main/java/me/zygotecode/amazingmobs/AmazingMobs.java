package me.zygotecode.amazingmobs;

import me.zygotecode.amazingmobs.clazz.ClassListener;
import me.zygotecode.amazingmobs.clazz.ClassManager;
import me.zygotecode.amazingmobs.clazz.ClassMenuListener;
import me.zygotecode.amazingmobs.clazz.ClassRegistry;
import me.zygotecode.amazingmobs.clazz.ClassService;
import me.zygotecode.amazingmobs.clazz.Cooldowns;
import me.zygotecode.amazingmobs.clazz.MinionManager;
import me.zygotecode.amazingmobs.command.AmazingMobsCommand;
import me.zygotecode.amazingmobs.config.ConfigSection;
import me.zygotecode.amazingmobs.config.ConfigSource;
import me.zygotecode.amazingmobs.config.DefinitionLoader;
import me.zygotecode.amazingmobs.config.LoadResult;
import me.zygotecode.amazingmobs.config.Messages;
import me.zygotecode.amazingmobs.config.PluginConfig;
import me.zygotecode.amazingmobs.config.ReloadSummary;
import me.zygotecode.amazingmobs.config.validation.ValidationReport;
import me.zygotecode.amazingmobs.horde.HordeDefinition;
import me.zygotecode.amazingmobs.horde.HordeRegistry;
import me.zygotecode.amazingmobs.horde.runtime.HordeManager;
import me.zygotecode.amazingmobs.listener.CombatListener;
import me.zygotecode.amazingmobs.listener.MobLifecycleListener;
import me.zygotecode.amazingmobs.listener.WorldBindListener;
import me.zygotecode.amazingmobs.mob.MobDefinition;
import me.zygotecode.amazingmobs.mob.MobRegistry;
import me.zygotecode.amazingmobs.mob.runtime.MobManager;
import me.zygotecode.amazingmobs.player.AirburstService;
import me.zygotecode.amazingmobs.player.WeightService;
import me.zygotecode.amazingmobs.skill.SkillRegistry;
import me.zygotecode.amazingmobs.trait.TraitRegistry;
import me.zygotecode.amazingmobs.util.Keys;
import me.zygotecode.amazingmobs.weapon.WeaponListener;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Plugin entrypoint: wires the subsystems, loads definitions, registers the command + listeners,
 * and starts the runtime managers. Holds the single instances other components read from.
 */
public final class AmazingMobs extends JavaPlugin {

    private PluginConfig config;
    private Messages messages;
    private SkillRegistry skillRegistry;
    private TraitRegistry traitRegistry;
    private MobRegistry mobRegistry;
    private HordeRegistry hordeRegistry;
    private MobManager mobManager;
    private HordeManager hordeManager;
    private WeightService weightService;
    private AirburstService airburstService;
    private ClassRegistry classRegistry;
    private MinionManager minionManager;
    private Cooldowns cooldowns;
    private ClassService classService;
    private ClassManager classManager;
    private AmazingMobsApi api;

    private File mobsDir, hordesDir, arenasDir, dropsDir;
    private Location arenaSpawn; // single global arena spawn (/am setarenapos)

    @Override
    public void onEnable() {
        Keys.init(this);
        setupFolders();
        saveResourceIfMissing("config.yml");
        saveResourceIfMissing("messages.yml");
        extractExamplesIfFirstRun();

        loadConfigAndMessages();

        this.skillRegistry = new SkillRegistry();
        this.skillRegistry.registerDefaults();
        this.traitRegistry = new TraitRegistry();
        this.traitRegistry.registerDefaults();
        this.mobRegistry = new MobRegistry();
        this.hordeRegistry = new HordeRegistry();

        this.mobManager = new MobManager(this, mobRegistry, skillRegistry, traitRegistry,
                config.mobControllerPeriod, config.maxActiveMobs);
        this.hordeManager = new HordeManager(this, hordeRegistry, mobManager,
                config.hordeTickInterval, config.maxConcurrentHordes);

        LoadResult mobs = DefinitionLoader.loadMobs(mobsDir, mobRegistry, skillRegistry.ids(), traitRegistry.ids());
        LoadResult hordes = DefinitionLoader.loadHordes(hordesDir, hordeRegistry, mobRegistry.ids());
        logSummary("mobs", mobs);
        logSummary("hordes", hordes);

        AmazingMobsCommand command = new AmazingMobsCommand(this);
        PluginCommand pc = getCommand("amazingmobs");
        if (pc != null) { pc.setExecutor(command); pc.setTabCompleter(command); }

        this.weightService = new WeightService(this);
        this.airburstService = new AirburstService(this);

        // player classes / skills / minions
        this.classRegistry = new ClassRegistry();
        this.classRegistry.registerDefaults();
        this.cooldowns = new Cooldowns();
        this.minionManager = new MinionManager(this);
        this.classService = new ClassService(this, classRegistry, minionManager, cooldowns);
        this.classManager = new ClassManager(this, classRegistry, classService, cooldowns, minionManager);

        getServer().getPluginManager().registerEvents(new CombatListener(this), this);
        getServer().getPluginManager().registerEvents(new MobLifecycleListener(this), this);
        getServer().getPluginManager().registerEvents(new WorldBindListener(this), this);
        getServer().getPluginManager().registerEvents(new WeaponListener(this), this); // AK-47 custom weapon
        getServer().getPluginManager().registerEvents(command.sessions(), this); // clear edit sessions on quit
        if (weightService.enabled()) getServer().getPluginManager().registerEvents(weightService, this);
        if (airburstService.enabled()) getServer().getPluginManager().registerEvents(airburstService, this);
        if (classService.enabled()) {
            getServer().getPluginManager().registerEvents(classService, this);
            getServer().getPluginManager().registerEvents(new ClassListener(classService, classManager), this);
            getServer().getPluginManager().registerEvents(new ClassMenuListener(classService, classManager), this);
        }

        mobManager.start();
        hordeManager.start();
        weightService.start();
        airburstService.start();
        minionManager.start();
        classService.start();
        loadArena();
        int rebound = mobManager.rebindLoaded();

        this.api = new AmazingMobsApi(this);
        getLogger().info("Enabled - " + mobRegistry.size() + " mobs, " + hordeRegistry.size()
                + " hordes, " + skillRegistry.size() + " skills, " + classRegistry.size() + " classes"
                + (rebound > 0 ? (", rebound " + rebound + " mobs") : ""));
    }

    @Override
    public void onDisable() {
        if (hordeManager != null) hordeManager.stop();
        if (mobManager != null) mobManager.stop(); // removes boss bars; tagged mobs persist & rebind on next start
        if (weightService != null) weightService.stop();
        if (airburstService != null) airburstService.stop();
        if (classService != null) classService.stop();
        if (minionManager != null) minionManager.stop();
        getLogger().info("Disabled.");
    }

    // ---- reload --------------------------------------------------------------------------------

    /** Re-read config + messages and rebuild registries atomically. Running mobs/hordes keep going. */
    public ReloadSummary reloadDefinitions() {
        loadConfigAndMessages();

        MobRegistry tmpMobs = new MobRegistry();
        LoadResult mobs = DefinitionLoader.loadMobs(mobsDir, tmpMobs, skillRegistry.ids(), traitRegistry.ids());
        mobRegistry.clear();
        for (MobDefinition d : tmpMobs.all()) mobRegistry.register(d);

        HordeRegistry tmpHordes = new HordeRegistry();
        LoadResult hordes = DefinitionLoader.loadHordes(hordesDir, tmpHordes, mobRegistry.ids());
        hordeRegistry.clear();
        for (HordeDefinition d : tmpHordes.all()) hordeRegistry.register(d);

        return new ReloadSummary(mobs, hordes);
    }

    /** Dry validation (no registration) for {@code /am validate}. */
    public ReloadSummary validateAll() {
        LoadResult mobs = DefinitionLoader.loadMobs(mobsDir, new MobRegistry(), skillRegistry.ids(), traitRegistry.ids());
        // validate hordes against the *currently loaded* mob ids so cross-references resolve
        LoadResult hordes = DefinitionLoader.loadHordes(hordesDir, new HordeRegistry(), mobRegistry.ids());
        return new ReloadSummary(mobs, hordes);
    }

    // ---- setup helpers -------------------------------------------------------------------------

    private void setupFolders() {
        getDataFolder().mkdirs();
        mobsDir = new File(getDataFolder(), "mobs");
        hordesDir = new File(getDataFolder(), "hordes");
        arenasDir = new File(getDataFolder(), "arenas");
        dropsDir = new File(getDataFolder(), "drops");
        for (File f : new File[]{hordesDir, arenasDir, dropsDir}) f.mkdirs();
        // mobsDir intentionally NOT created here so extractExamplesIfFirstRun can detect first run
    }

    private void loadConfigAndMessages() {
        ValidationReport cr = new ValidationReport("config.yml");
        ConfigSection cfg = ConfigSource.load(new File(getDataFolder(), "config.yml"), cr);
        this.config = PluginConfig.from(cfg);
        for (var line : cr.lines()) getLogger().warning("[config.yml] " + line);
        if (config.configVersion < PluginConfig.CURRENT_VERSION) {
            getLogger().warning("config.yml is version " + config.configVersion + " (current is "
                    + PluginConfig.CURRENT_VERSION + "). New keys use safe defaults — regenerate or see docs to opt in.");
        }

        ValidationReport mr = new ValidationReport("messages.yml");
        ConfigSection msg = ConfigSource.load(new File(getDataFolder(), "messages.yml"), mr);
        this.messages = new Messages();
        this.messages.load(msg);
    }

    private void saveResourceIfMissing(String path) {
        if (getResource(path) == null) return;
        if (!new File(getDataFolder(), path).exists()) saveResource(path, false);
    }

    private void extractExamplesIfFirstRun() {
        if (mobsDir.exists()) return; // already set up
        mobsDir.mkdirs();
        for (String path : readIndex("examples-index.txt")) {
            if (getResource(path) != null && !new File(getDataFolder(), path).exists()) {
                saveResource(path, false);
            }
        }
        getLogger().info("First run: extracted bundled example mobs & hordes.");
    }

    private List<String> readIndex(String resource) {
        List<String> out = new ArrayList<>();
        InputStream in = getResource(resource);
        if (in == null) return out;
        try (BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty() && !line.startsWith("#")) out.add(line);
            }
        } catch (Exception e) {
            getLogger().warning("Could not read " + resource + ": " + e.getMessage());
        }
        return out;
    }

    // ---- arena spawn (single global arena for /am setarenapos + /am tpall) ----------------------

    private void loadArena() {
        File f = new File(getDataFolder(), "arena.yml");
        if (!f.exists()) return;
        YamlConfiguration y = YamlConfiguration.loadConfiguration(f);
        String worldName = y.getString("world");
        if (worldName == null) return;
        World w = getServer().getWorld(worldName);
        if (w == null) {
            // try by UUID fallback
            String wid = y.getString("world-uuid");
            if (wid != null) try { w = getServer().getWorld(UUID.fromString(wid)); } catch (IllegalArgumentException ignored) {}
        }
        if (w == null) { getLogger().warning("Arena world '" + worldName + "' not loaded; /am tpall disabled until set again."); return; }
        arenaSpawn = new Location(w, y.getDouble("x"), y.getDouble("y"), y.getDouble("z"),
                (float) y.getDouble("yaw"), (float) y.getDouble("pitch"));
    }

    public void setArenaSpawn(Location loc) {
        this.arenaSpawn = loc.clone();
        YamlConfiguration y = new YamlConfiguration();
        y.set("world", loc.getWorld().getName());
        y.set("world-uuid", loc.getWorld().getUID().toString());
        y.set("x", loc.getX());
        y.set("y", loc.getY());
        y.set("z", loc.getZ());
        y.set("yaw", loc.getYaw());
        y.set("pitch", loc.getPitch());
        try { y.save(new File(getDataFolder(), "arena.yml")); }
        catch (Exception e) { getLogger().warning("Could not save arena.yml: " + e.getMessage()); }
    }

    public Location arenaSpawn() { return arenaSpawn == null ? null : arenaSpawn.clone(); }

    private void logSummary(String kind, LoadResult result) {
        getLogger().info("Loaded " + result.loaded() + "/" + result.total() + " " + kind
                + (result.rejected() > 0 ? (" (" + result.rejected() + " rejected)") : "")
                + (result.totalWarnings() > 0 ? (", " + result.totalWarnings() + " warnings") : ""));
    }

    // ---- accessors -----------------------------------------------------------------------------

    public PluginConfig config() { return config; }
    public Messages messages() { return messages; }
    public SkillRegistry skillRegistry() { return skillRegistry; }
    public TraitRegistry traitRegistry() { return traitRegistry; }
    public MobRegistry mobRegistry() { return mobRegistry; }
    public HordeRegistry hordeRegistry() { return hordeRegistry; }
    public MobManager mobManager() { return mobManager; }
    public HordeManager hordeManager() { return hordeManager; }
    public WeightService weightService() { return weightService; }
    public ClassRegistry classRegistry() { return classRegistry; }
    public ClassService classService() { return classService; }
    public ClassManager classManager() { return classManager; }
    public MinionManager minionManager() { return minionManager; }
    public AmazingMobsApi api() { return api; }
    public File mobsDir() { return mobsDir; }
    public File hordesDir() { return hordesDir; }
    public File arenasDir() { return arenasDir; }
    public File dropsDir() { return dropsDir; }
}
