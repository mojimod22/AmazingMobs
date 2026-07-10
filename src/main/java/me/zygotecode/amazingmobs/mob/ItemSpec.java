package me.zygotecode.amazingmobs.mob;

import me.zygotecode.amazingmobs.util.IntRange;
import me.zygotecode.amazingmobs.util.Rng;
import me.zygotecode.amazingmobs.util.Text;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Description of an item (gear, drop, or given item). Built into a live {@link ItemStack} at
 * runtime. Holds an already-resolved {@link Material}/{@link Enchantment}s (validity is checked
 * by the parser, so building never silently produces air for a typo'd material).
 */
public final class ItemSpec {

    private final Material material;
    private final IntRange amount;
    private final String name;          // MiniMessage, nullable
    private final List<String> lore;    // MiniMessage lines
    private final Map<Enchantment, Integer> enchants;
    private final Integer customModelData;
    private final boolean unbreakable;
    private final boolean glow;
    private final boolean hideAttributes;

    private ItemSpec(Builder b) {
        this.material = b.material;
        this.amount = b.amount;
        this.name = b.name;
        this.lore = b.lore == null ? List.of() : List.copyOf(b.lore);
        this.enchants = new LinkedHashMap<>(b.enchants);
        this.customModelData = b.customModelData;
        this.unbreakable = b.unbreakable;
        this.glow = b.glow;
        this.hideAttributes = b.hideAttributes;
    }

    public Material material() { return material; }

    /** Build a fresh ItemStack, rolling the amount range. */
    public ItemStack build(Rng rng) {
        ItemStack stack = new ItemStack(material, Math.max(1, amount.pick(rng)));
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            if (name != null && !name.isEmpty()) meta.displayName(Text.item(name));
            if (!lore.isEmpty()) meta.lore(Text.itemLore(lore));
            for (Map.Entry<Enchantment, Integer> e : enchants.entrySet()) {
                meta.addEnchant(e.getKey(), Math.max(1, e.getValue()), true);
            }
            if (unbreakable) meta.setUnbreakable(true);
            if (customModelData != null) {
                try { meta.setCustomModelData(customModelData); } catch (Throwable ignored) {}
            }
            if (glow && enchants.isEmpty()) meta.setEnchantmentGlintOverride(true);
            if (hideAttributes) meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    public static Builder builder(Material material) { return new Builder(material); }

    public static final class Builder {
        private final Material material;
        private IntRange amount = IntRange.of(1);
        private String name;
        private List<String> lore;
        private final Map<Enchantment, Integer> enchants = new LinkedHashMap<>();
        private Integer customModelData;
        private boolean unbreakable;
        private boolean glow;
        private boolean hideAttributes;

        public Builder(Material material) { this.material = material; }

        public Builder amount(IntRange v) { this.amount = v == null ? IntRange.of(1) : v; return this; }
        public Builder name(String v) { this.name = v; return this; }
        public Builder lore(List<String> v) { this.lore = v; return this; }
        public Builder enchant(Enchantment e, int level) { if (e != null) enchants.put(e, level); return this; }
        public Builder customModelData(Integer v) { this.customModelData = v; return this; }
        public Builder unbreakable(boolean v) { this.unbreakable = v; return this; }
        public Builder glow(boolean v) { this.glow = v; return this; }
        public Builder hideAttributes(boolean v) { this.hideAttributes = v; return this; }
        public ItemSpec build() { return new ItemSpec(this); }
    }
}
