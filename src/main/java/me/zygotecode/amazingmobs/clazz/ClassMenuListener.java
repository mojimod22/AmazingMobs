package me.zygotecode.amazingmobs.clazz;

import me.zygotecode.amazingmobs.util.Text;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryHolder;

/** Handles clicks in the {@link ClassMenu} (select a class) and {@link SkillMenu} (cast a skill). */
public final class ClassMenuListener implements Listener {

    private final ClassService service;
    private final ClassManager manager;

    public ClassMenuListener(ClassService service, ClassManager manager) {
        this.service = service;
        this.manager = manager;
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        InventoryHolder h = e.getInventory().getHolder();
        boolean classMenu = h instanceof ClassMenu;
        boolean skillMenu = h instanceof SkillMenu;
        if (!classMenu && !skillMenu) return;

        e.setCancelled(true); // our menus are read/click-only — never let items move
        if (!(e.getWhoClicked() instanceof Player p)) return;
        if (e.getClickedInventory() == null || !e.getClickedInventory().equals(e.getInventory())) return; // clicked own inv

        int slot = e.getRawSlot();
        if (classMenu) {
            String id = ((ClassMenu) h).classAt(slot);
            if (id == null) return;
            p.closeInventory();
            switch (service.setClass(p, id)) {
                case LOCKED -> p.sendMessage(Text.mm("<red>Class changing is disabled on this server."));
                case SAME -> p.sendMessage(Text.mm("<gray>That's already your class."));
                default -> {}
            }
        } else {
            String id = ((SkillMenu) h).skillAt(slot);
            if (id == null) return;
            p.closeInventory();
            manager.castById(p, id);
        }
    }
}
