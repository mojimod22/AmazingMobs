package me.zygotecode.amazingmobs.player;

import me.zygotecode.amazingmobs.AmazingMobs;
import me.zygotecode.amazingmobs.clazz.PlayerClass;
import me.zygotecode.amazingmobs.clazz.PlayerSkill;
import me.zygotecode.amazingmobs.clazz.SkillItems;
import me.zygotecode.amazingmobs.clazz.SkillType;
import me.zygotecode.amazingmobs.util.Resolvers;
import me.zygotecode.amazingmobs.util.Text;
import me.zygotecode.amazingmobs.weapon.AkRifle;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionType;

/**
 * The "battle prep" applied to every participating player when a horde starts: full heal + reset of
 * vitals, a clean inventory, and the standard kit (enchanted netherite gear + sword + AK-47 + bow,
 * enchanted golden apples, totems, and potions). If the player has a class, its <b>active / special /
 * hyper skill items</b> take priority hotbar slots (right-click to cast), trimming the potion space.
 * Server-wide for now; gated by {@code horde.equip-all-players-on-start}.
 */
public final class PlayerKit {

    private PlayerKit() {}

    public static void resetAndEquip(AmazingMobs plugin, Player p) {
        // --- reset vitals ---
        AttributeInstance max = p.getAttribute(Attribute.MAX_HEALTH);
        p.setHealth(max != null ? max.getValue() : 20.0);
        p.setFoodLevel(20);
        p.setSaturation(20f);
        p.setExhaustion(0f);
        p.setRemainingAir(p.getMaximumAir());
        p.setFireTicks(0);
        p.setFallDistance(0f);
        try { p.setFreezeTicks(0); } catch (Throwable ignored) {}
        for (PotionEffect e : p.getActivePotionEffects()) p.removePotionEffect(e.getType());

        // --- clean slate ---
        PlayerInventory inv = p.getInventory();
        inv.clear();
        inv.setArmorContents(new ItemStack[4]);
        inv.setItemInOffHand(null);

        // --- armour ---
        inv.setHelmet(armor(Material.NETHERITE_HELMET, false));
        inv.setChestplate(armor(Material.NETHERITE_CHESTPLATE, false));
        inv.setLeggings(armor(Material.NETHERITE_LEGGINGS, false));
        inv.setBoots(armor(Material.NETHERITE_BOOTS, true));   // boots also get Feather Falling IV

        // --- off-hand shield + always: sword, AK-47 (everyone), single arrow ---
        inv.setItemInOffHand(shield());
        inv.setItem(0, sword());
        inv.setItem(1, AkRifle.build());          // the "AK-47" rifle — for all players
        inv.setItem(9, new ItemStack(Material.ARROW, 1));

        // --- class skill items take the next hotbar slots (right-click to cast) ---
        PlayerClass c = (plugin.classService() != null && plugin.classService().enabled())
                ? plugin.classService().classOf(p) : null;
        if (c != null) {
            int prestige = plugin.classManager().prestige(p);
            placeSkill(plugin, inv, 2, c.skillId(SkillType.ACTIVE), prestige);
            placeSkill(plugin, inv, 3, c.skillId(SkillType.SPECIAL), prestige);
            placeSkill(plugin, inv, 4, c.skillId(SkillType.HYPER), prestige);
            inv.setItem(5, maxStack(Material.ENCHANTED_GOLDEN_APPLE));
            inv.setItem(6, maxStack(Material.ENCHANTED_GOLDEN_APPLE));
            inv.setItem(7, maxStack(Material.TOTEM_OF_UNDYING));
            inv.setItem(8, maxStack(Material.TOTEM_OF_UNDYING));
            inv.setItem(10, bow());
            inv.setItem(11, maxStack(Material.ENCHANTED_GOLDEN_APPLE));
        } else {
            // no class → the old layout (bow + 3 gapples + 2 totems on the hotbar)
            inv.setItem(2, bow());
            inv.setItem(3, maxStack(Material.ENCHANTED_GOLDEN_APPLE));
            inv.setItem(4, maxStack(Material.ENCHANTED_GOLDEN_APPLE));
            inv.setItem(5, maxStack(Material.ENCHANTED_GOLDEN_APPLE));
            inv.setItem(6, maxStack(Material.TOTEM_OF_UNDYING));
            inv.setItem(7, maxStack(Material.TOTEM_OF_UNDYING));
        }

        // --- fill EVERY remaining empty slot with potions (drinkables are max-stack 1, so we pack
        //     many slots) rotating strength / speed / regen (strong + normal) — no empty space left ---
        PotionType[] mix = {
                PotionType.STRONG_STRENGTH, PotionType.STRONG_SWIFTNESS, PotionType.STRONG_REGENERATION,
                PotionType.STRENGTH, PotionType.SWIFTNESS, PotionType.REGENERATION
        };
        int r = 0;
        for (int slot = 2; slot <= 35; slot++) {
            ItemStack cur = inv.getItem(slot);
            if (cur != null && cur.getType() != Material.AIR) continue;  // keep everything already placed
            inv.setItem(slot, potion(mix[r++ % mix.length], 1));
        }

        p.updateInventory();
    }

    private static void placeSkill(AmazingMobs plugin, PlayerInventory inv, int slot, String skillId, int prestige) {
        PlayerSkill sk = plugin.classRegistry().skill(skillId);
        if (sk != null) inv.setItem(slot, SkillItems.build(sk, prestige, plugin.classManager()));
    }

    private static ItemStack bow() {
        ItemStack is = new ItemStack(Material.BOW);
        ItemMeta m = is.getItemMeta();
        if (m != null) {
            m.displayName(Text.item("<gradient:#43cea2:#185a9d>Storm Bow</gradient>"));
            enchant(m, "power", 5);
            enchant(m, "flame", 1);
            enchant(m, "infinity", 1);
            enchant(m, "mending", 1);   // Infinity+Mending conflict in vanilla — forced via addEnchant as requested
            m.setUnbreakable(true);
            is.setItemMeta(m);
        }
        return is;
    }

    private static ItemStack maxStack(Material mat) {
        return new ItemStack(mat, mat.getMaxStackSize());
    }

    private static ItemStack shield() {
        ItemStack is = new ItemStack(Material.SHIELD);
        ItemMeta m = is.getItemMeta();
        if (m != null) {
            enchant(m, "protection", 4);   // forced (ignoreLevelRestriction) — vanilla shields don't take it
            enchant(m, "unbreaking", 3);
            enchant(m, "mending", 1);
            m.setUnbreakable(true);
            is.setItemMeta(m);
        }
        return is;
    }

    private static ItemStack armor(Material mat, boolean boots) {
        ItemStack is = new ItemStack(mat);
        ItemMeta m = is.getItemMeta();
        if (m != null) {
            enchant(m, "protection", 4);
            enchant(m, "unbreaking", 3);
            enchant(m, "mending", 1);
            if (boots) enchant(m, "feather_falling", 4);
            m.setUnbreakable(true);
            is.setItemMeta(m);
        }
        return is;
    }

    private static ItemStack sword() {
        ItemStack is = new ItemStack(Material.NETHERITE_SWORD);
        ItemMeta m = is.getItemMeta();
        if (m != null) {
            m.displayName(Text.item("<gradient:#ff5e62:#ff9966>Horde Cleaver</gradient>"));
            enchant(m, "sharpness", 5);
            enchant(m, "fire_aspect", 2);
            enchant(m, "sweeping_edge", 3);
            enchant(m, "knockback", 2);
            enchant(m, "unbreaking", 3);
            enchant(m, "mending", 1);
            m.setUnbreakable(true);
            is.setItemMeta(m);
        }
        return is;
    }

    private static void enchant(ItemMeta m, String key, int level) {
        Enchantment e = Resolvers.enchant(key);
        if (e != null) m.addEnchant(e, level, true);
    }

    private static ItemStack potion(PotionType type, int amount) {
        ItemStack is = new ItemStack(Material.POTION, amount);
        if (is.getItemMeta() instanceof PotionMeta m) {
            m.setBasePotionType(type);
            is.setItemMeta(m);
        }
        return is;
    }
}
