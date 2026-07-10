package me.zygotecode.amazingmobs.clazz;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;

/**
 * Combat triggers for class skills (no client keybind API exists, so we hijack standard inputs):
 * <ul>
 *   <li><b>F</b> (swap hand) → ACTIVE</li>
 *   <li><b>Shift+F</b> (swap while sneaking) → SPECIAL</li>
 *   <li><b>Shift+Q</b> (drop while sneaking) → HYPER</li>
 *   <li><b>Right-click a skill item</b> (from the horde kit) → that skill</li>
 * </ul>
 * Only intercepted for players who have a class; everyone else swaps/drops/uses normally.
 */
public final class ClassListener implements Listener {

    private final ClassService service;
    private final ClassManager manager;

    public ClassListener(ClassService service, ClassManager manager) {
        this.service = service;
        this.manager = manager;
    }

    @EventHandler
    public void onSwap(PlayerSwapHandItemsEvent e) {
        if (!service.enabled() || !service.has(e.getPlayer())) return;
        e.setCancelled(true);
        manager.cast(e.getPlayer(), e.getPlayer().isSneaking() ? SkillType.SPECIAL : SkillType.ACTIVE);
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent e) {
        if (!service.enabled() || !service.has(e.getPlayer())) return;
        if (!e.getPlayer().isSneaking()) return; // plain drops still work
        e.setCancelled(true);
        manager.cast(e.getPlayer(), SkillType.HYPER);
    }

    /** Right-click a skill item → cast that skill (cancels the item's vanilla use). */
    @EventHandler
    public void onUse(PlayerInteractEvent e) {
        Action a = e.getAction();
        if (a != Action.RIGHT_CLICK_AIR && a != Action.RIGHT_CLICK_BLOCK) return;
        if (!service.enabled() || !service.has(e.getPlayer())) return;
        String id = SkillItems.skillId(e.getItem());
        if (id == null) return;
        e.setCancelled(true);
        manager.castById(e.getPlayer(), id);
    }
}
