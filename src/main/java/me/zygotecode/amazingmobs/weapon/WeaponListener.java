package me.zygotecode.amazingmobs.weapon;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.Plugin;

/**
 * Drives the custom {@link AkRifle}: right-click fires and melee with it does nothing. The skull's
 * flight + detonation are handled by {@link AkProjectile} (a fixed-ray, constant-speed driver), so no
 * {@code ProjectileHitEvent} handling is needed here.
 */
public final class WeaponListener implements Listener {

    private final Plugin plugin;

    public WeaponListener(Plugin plugin) { this.plugin = plugin; }

    /** Right-click with the AK → fire one round (cancel the interact so the golden axe doesn't strip blocks). */
    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        Action a = e.getAction();
        if (a != Action.RIGHT_CLICK_AIR && a != Action.RIGHT_CLICK_BLOCK) return;
        if (!AkRifle.isAk(e.getItem())) return;
        e.setCancelled(true);
        AkRifle.fire(plugin, e.getPlayer(), e.getItem(), e.getHand());
    }

    /** The AK deals no melee damage. */
    @EventHandler(ignoreCancelled = true)
    public void onMelee(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player p)) return;
        EntityDamageEvent.DamageCause c = e.getCause();
        if (c != EntityDamageEvent.DamageCause.ENTITY_ATTACK && c != EntityDamageEvent.DamageCause.ENTITY_SWEEP_ATTACK) return;
        if (AkRifle.isAk(p.getInventory().getItemInMainHand())) e.setCancelled(true);
    }
}
