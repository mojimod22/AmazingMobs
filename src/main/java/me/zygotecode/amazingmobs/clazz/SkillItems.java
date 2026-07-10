package me.zygotecode.amazingmobs.clazz;

import me.zygotecode.amazingmobs.util.Keys;
import me.zygotecode.amazingmobs.util.Text;
import net.kyori.adventure.text.Component;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds the physical "skill items" handed out in the horde kit — right-clicking one casts its skill
 * (see {@link ClassListener}). The skill id is stamped in the item's PDC so the cast is unambiguous and
 * survives stacking/moving.
 */
public final class SkillItems {

    private SkillItems() {}

    public static ItemStack build(PlayerSkill sk, int prestige, ClassManager manager) {
        ItemStack is = new ItemStack(sk.icon());
        ItemMeta m = is.getItemMeta();
        if (m == null) return is;
        String col = sk.type().color();
        m.displayName(Text.item(col + "<bold>" + sk.name() + "</bold> <dark_gray>[" + sk.type().label() + "]"));
        List<Component> lore = new ArrayList<>();
        for (String d : sk.description(prestige)) lore.add(Text.item(d));
        lore.add(Text.item(" "));
        int cd = manager.effectiveCooldown(sk, prestige);
        if (cd > 0) lore.add(Text.item("<gray>Cooldown: <white>" + cd + "s"));
        lore.add(Text.item("<yellow>Right-click to cast <dark_gray>(or " + sk.type().trigger() + ")"));
        m.lore(lore);
        m.getPersistentDataContainer().set(Keys.SKILL_ITEM, PersistentDataType.STRING, sk.id());
        m.setUnbreakable(true);
        try { m.setEnchantmentGlintOverride(true); } catch (Throwable ignored) {}
        try { m.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES, org.bukkit.inventory.ItemFlag.HIDE_UNBREAKABLE); } catch (Throwable ignored) {}
        is.setItemMeta(m);
        return is;
    }

    public static boolean isSkillItem(ItemStack is) {
        return is != null && is.hasItemMeta()
                && is.getItemMeta().getPersistentDataContainer().has(Keys.SKILL_ITEM, PersistentDataType.STRING);
    }

    public static String skillId(ItemStack is) {
        if (!isSkillItem(is)) return null;
        return is.getItemMeta().getPersistentDataContainer().get(Keys.SKILL_ITEM, PersistentDataType.STRING);
    }
}
