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

/**
 * Chest GUI showing the player's class skills (base / active / special / hyper) with type, trigger,
 * cooldown (base → effective at current prestige → remaining), prestige note, and description. Clicking
 * a ready active/special/hyper casts it. A live, readable "skill inventory".
 */
public final class SkillMenu implements InventoryHolder {

    private final Inventory inv;
    private final Map<Integer, String> slotToSkill = new HashMap<>();

    public SkillMenu(ClassManager manager, ClassService service, Player viewer) {
        PlayerClass c = service.classOf(viewer);
        int prestige = manager.prestige(viewer);
        this.inv = Bukkit.createInventory(this, 27, Text.mm((c != null ? c.color() : "<gray>")
                + "<bold>" + (c != null ? c.name() : "Skills") + "</bold> <gray>· Prestige " + prestige));
        if (c == null) return;

        place(manager, viewer, prestige, 10, c.skillId(SkillType.BASE));
        place(manager, viewer, prestige, 12, c.skillId(SkillType.ACTIVE));
        place(manager, viewer, prestige, 14, c.skillId(SkillType.SPECIAL));
        place(manager, viewer, prestige, 16, c.skillId(SkillType.HYPER));
    }

    public String skillAt(int slot) { return slotToSkill.get(slot); }

    private void place(ClassManager manager, Player viewer, int prestige, int slot, String skillId) {
        PlayerSkill sk = manager.registry().skill(skillId);
        if (sk == null) return;
        slotToSkill.put(slot, skillId);

        ItemStack is = new ItemStack(sk.icon());
        ItemMeta m = is.getItemMeta();
        if (m != null) {
            String col = sk.type().color();
            long remMs = manager.cooldowns().remainingMs(viewer.getUniqueId(), skillId);
            boolean ready = remMs <= 0;
            m.displayName(Text.item(col + "<bold>" + sk.name() + "</bold> <dark_gray>[" + sk.type().label() + "]"));
            List<Component> lore = new ArrayList<>();
            for (String d : sk.description(prestige)) lore.add(Text.item(d));
            lore.add(Text.item(" "));
            lore.add(Text.item("<gray>Trigger: <white>" + sk.type().trigger()));
            if (sk.type() == SkillType.BASE) {
                lore.add(Text.item("<dark_gray>Always available."));
            } else {
                int eff = manager.effectiveCooldown(sk, prestige);
                lore.add(Text.item("<gray>Cooldown: <white>" + eff + "s <dark_gray>(base " + sk.baseCooldownSeconds() + "s)"));
                lore.add(Text.item(ready ? "<green>✔ Ready — click to cast"
                        : "<red>✖ " + ((remMs + 999) / 1000) + "s remaining"));
                if (prestige > 1) lore.add(Text.item("<dark_purple>Prestige " + prestige + ": stronger + shorter cooldown"));
            }
            m.lore(lore);
            if (ready && sk.type() != SkillType.BASE) try { m.setEnchantmentGlintOverride(true); } catch (Throwable ignored) {}
            is.setItemMeta(m);
        }
        inv.setItem(slot, is);
    }

    @Override public Inventory getInventory() { return inv; }
}
