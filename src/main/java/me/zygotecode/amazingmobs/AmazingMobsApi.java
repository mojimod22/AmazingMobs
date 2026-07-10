package me.zygotecode.amazingmobs;

import me.zygotecode.amazingmobs.horde.HordeRegistry;
import me.zygotecode.amazingmobs.horde.runtime.StartResult;
import me.zygotecode.amazingmobs.mob.MobRegistry;
import me.zygotecode.amazingmobs.mob.runtime.SpawnMeta;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

/**
 * Stable, minimal facade for other plugins. Obtain via
 * {@code ((AmazingMobs) Bukkit.getPluginManager().getPlugin("AmazingMobs")).api()}.
 *
 * <p>Integration points beyond this are the Bukkit events in {@code me.zygotecode.amazingmobs.api.event}.</p>
 */
public final class AmazingMobsApi {

    private final AmazingMobs plugin;

    AmazingMobsApi(AmazingMobs plugin) {
        this.plugin = plugin;
    }

    public MobRegistry mobs() { return plugin.mobRegistry(); }
    public HordeRegistry hordes() { return plugin.hordeRegistry(); }

    /** Spawn a registered custom mob (solo scaling). Returns the entity, or null if it could not spawn. */
    public LivingEntity spawnMob(String mobId, Location location) {
        return plugin.mobManager().spawn(mobId, location, SpawnMeta.SOLO);
    }

    /** Spawn a registered custom mob with explicit player-count/difficulty scaling. */
    public LivingEntity spawnMob(String mobId, Location location, int playerCount, double difficulty) {
        return plugin.mobManager().spawn(mobId, location, SpawnMeta.solo(playerCount, difficulty));
    }

    public boolean isCustomMob(Entity entity) {
        return plugin.mobManager().isCustomMob(entity);
    }

    /** Start a horde at a center (use null center for fixed-area hordes). */
    public StartResult startHorde(String hordeId, Location center, int playerCount) {
        return plugin.hordeManager().start(hordeId, center, playerCount, false);
    }

    public boolean stopHorde(String instanceId) {
        return plugin.hordeManager().stopInstance(instanceId);
    }

    public int activeMobCount() { return plugin.mobManager().activeCount(); }
    public int activeHordeCount() { return plugin.hordeManager().activeCount(); }
}
