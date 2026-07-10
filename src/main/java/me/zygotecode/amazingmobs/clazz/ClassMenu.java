package me.zygotecode.amazingmobs.clazz;

import me.zygotecode.amazingmobs.util.Text;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Chest GUI to preview + pick a class. Clicking an icon selects that class (handled in the listener). */
public final class ClassMenu implements InventoryHolder {

    private final Inventory inv;
    private final Map<Integer, String> slotToClass = new HashMap<>();

    public ClassMenu(ClassRegistry registry, ClassManager manager, PlayerClass current) {
        this.inv = Bukkit.createInventory(this, 27, Text.mm("<dark_purple><bold>Choose your Class</bold>"));
        int[] slots = {10, 11, 12, 13, 14, 15, 19, 20, 21, 22, 23, 24};
        List<PlayerClass> classes = registry.classes();
        for (int i = 0; i < classes.size() && i < slots.length; i++) {
            PlayerClass c = classes.get(i);
            int slot = slots[i];
            slotToClass.put(slot, c.id());
            inv.setItem(slot, icon(registry, c, current != null && current.id().equals(c.id())));
        }
    }

    public String classAt(int slot) { return slotToClass.get(slot); }

    private static ItemStack icon(ClassRegistry registry, PlayerClass c, boolean currentlyEquipped) {
        ItemStack is = new ItemStack(c.icon());
        ItemMeta m = is.getItemMeta();
        if (m != null) {
            m.displayName(Text.item(c.color() + "<bold>" + c.name() + "</bold>" + (currentlyEquipped ? " <green>(current)" : "")));
            List<Component> lore = new ArrayList<>();
            lore.add(Text.item("<gray>" + c.role()));
            lore.add(Text.item("<dark_gray>" + c.description()));
            lore.add(Text.item(" "));
            for (String pl : c.passiveLines()) lore.add(Text.item(pl));
            lore.add(Text.item(" "));
            lore.add(Text.item(skillLine(registry, c.active(), "Active")));
            lore.add(Text.item(skillLine(registry, c.special(), "Special")));
            lore.add(Text.item(skillLine(registry, c.hyper(), "Hyper")));
            lore.add(Text.item(" "));
            lore.add(Text.item(currentlyEquipped ? "<dark_gray>Already your class" : "<yellow>▶ Click to become this class"));
            m.lore(lore);
            if (currentlyEquipped) try { m.setEnchantmentGlintOverride(true); } catch (Throwable ignored) {}
            is.setItemMeta(m);
        }
        return is;
    }

    private static String skillLine(ClassRegistry registry, String skillId, String label) {
        PlayerSkill sk = registry.skill(skillId);
        String name = sk != null ? sk.name() : skillId;
        return "<gray>" + label + ": <white>" + name;
    }

    @Override public Inventory getInventory() { return inv; }
}
