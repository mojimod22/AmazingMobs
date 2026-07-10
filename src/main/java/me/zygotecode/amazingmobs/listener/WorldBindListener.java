package me.zygotecode.amazingmobs.listener;

import me.zygotecode.amazingmobs.AmazingMobs;
import me.zygotecode.amazingmobs.mob.runtime.MobManager;
import me.zygotecode.amazingmobs.util.Keys;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.persistence.PersistentDataType;

/**
 * Re-binds a controller to custom mobs whose chunk just (re)loaded, so behaviour resumes after a
 * chunk unload / server restart without re-spawning. Entities are tagged via PDC at spawn.
 */
public final class WorldBindListener implements Listener {

    private final MobManager mobs;

    public WorldBindListener(AmazingMobs plugin) {
        this.mobs = plugin.mobManager();
    }

    @EventHandler
    public void onEntitiesLoad(EntitiesLoadEvent e) {
        for (Entity entity : e.getEntities()) {
            if (entity instanceof LivingEntity le
                    && le.getPersistentDataContainer().has(Keys.MOB_ID, PersistentDataType.STRING)) {
                mobs.rebind(le);
            }
        }
    }
}
