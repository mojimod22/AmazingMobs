package me.zygotecode.amazingmobs.listener;

import me.zygotecode.amazingmobs.AmazingMobs;
import me.zygotecode.amazingmobs.mob.runtime.MobManager;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;

/** Hands custom-mob deaths to the manager so drops/XP/death-skills/events are applied. */
public final class MobLifecycleListener implements Listener {

    private final MobManager mobs;

    public MobLifecycleListener(AmazingMobs plugin) {
        this.mobs = plugin.mobManager();
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onDeath(EntityDeathEvent e) {
        LivingEntity entity = e.getEntity();
        if (mobs.isCustomMob(entity)) mobs.handleDeath(entity, e, entity.getKiller());
        else mobs.handleChainDeath(entity); // vanilla mount/rider death still cascades the chain
    }

    /** Block right-click interaction with custom mobs (e.g. villager trading, leashing, naming). */
    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEntityEvent e) {
        if (mobs.isCustomMob(e.getRightClicked())) e.setCancelled(true);
    }
}
