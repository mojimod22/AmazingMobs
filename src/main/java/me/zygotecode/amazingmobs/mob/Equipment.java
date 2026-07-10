package me.zygotecode.amazingmobs.mob;

import me.zygotecode.amazingmobs.util.Rng;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.EntityEquipment;

/**
 * A mob's worn/held gear plus per-slot drop chances (0..1). Any slot may be {@code null} (empty).
 * Applied to a {@link LivingEntity} at spawn.
 */
public final class Equipment {

    public static final Equipment EMPTY = new Equipment(new Builder());

    private final ItemSpec mainHand, offHand, helmet, chestplate, leggings, boots;
    private final float mainHandDrop, offHandDrop, helmetDrop, chestDrop, legsDrop, bootsDrop;

    private Equipment(Builder b) {
        this.mainHand = b.mainHand; this.offHand = b.offHand;
        this.helmet = b.helmet; this.chestplate = b.chestplate;
        this.leggings = b.leggings; this.boots = b.boots;
        this.mainHandDrop = b.mainHandDrop; this.offHandDrop = b.offHandDrop;
        this.helmetDrop = b.helmetDrop; this.chestDrop = b.chestDrop;
        this.legsDrop = b.legsDrop; this.bootsDrop = b.bootsDrop;
    }

    public boolean isEmpty() {
        return mainHand == null && offHand == null && helmet == null
                && chestplate == null && leggings == null && boots == null;
    }

    public ItemSpec mainHand() { return mainHand; }
    public ItemSpec offHand() { return offHand; }
    public ItemSpec helmet() { return helmet; }
    public ItemSpec chestplate() { return chestplate; }
    public ItemSpec leggings() { return leggings; }
    public ItemSpec boots() { return boots; }

    /** Resolve a slot by config key name, or null. */
    public ItemSpec bySlot(String slot) {
        return switch (slot == null ? "" : slot.toLowerCase(java.util.Locale.ROOT)) {
            case "main-hand", "mainhand", "hand" -> mainHand;
            case "off-hand", "offhand" -> offHand;
            case "helmet", "head" -> helmet;
            case "chestplate", "chest" -> chestplate;
            case "leggings", "legs" -> leggings;
            case "boots", "feet" -> boots;
            default -> null;
        };
    }

    public void apply(LivingEntity entity, Rng rng) {
        EntityEquipment eq = entity.getEquipment();
        if (eq == null) return;
        if (mainHand != null) { eq.setItemInMainHand(mainHand.build(rng)); eq.setItemInMainHandDropChance(mainHandDrop); }
        if (offHand != null)  { eq.setItemInOffHand(offHand.build(rng));   eq.setItemInOffHandDropChance(offHandDrop); }
        if (helmet != null)   { eq.setHelmet(helmet.build(rng));           eq.setHelmetDropChance(helmetDrop); }
        if (chestplate != null){ eq.setChestplate(chestplate.build(rng));  eq.setChestplateDropChance(chestDrop); }
        if (leggings != null) { eq.setLeggings(leggings.build(rng));       eq.setLeggingsDropChance(legsDrop); }
        if (boots != null)    { eq.setBoots(boots.build(rng));             eq.setBootsDropChance(bootsDrop); }
    }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private ItemSpec mainHand, offHand, helmet, chestplate, leggings, boots;
        private float mainHandDrop = 0.085f, offHandDrop = 0.085f, helmetDrop = 0.085f,
                chestDrop = 0.085f, legsDrop = 0.085f, bootsDrop = 0.085f;

        public Builder mainHand(ItemSpec v) { this.mainHand = v; return this; }
        public Builder offHand(ItemSpec v) { this.offHand = v; return this; }
        public Builder helmet(ItemSpec v) { this.helmet = v; return this; }
        public Builder chestplate(ItemSpec v) { this.chestplate = v; return this; }
        public Builder leggings(ItemSpec v) { this.leggings = v; return this; }
        public Builder boots(ItemSpec v) { this.boots = v; return this; }
        public Builder mainHandDrop(double v) { this.mainHandDrop = (float) v; return this; }
        public Builder offHandDrop(double v) { this.offHandDrop = (float) v; return this; }
        public Builder helmetDrop(double v) { this.helmetDrop = (float) v; return this; }
        public Builder chestDrop(double v) { this.chestDrop = (float) v; return this; }
        public Builder legsDrop(double v) { this.legsDrop = (float) v; return this; }
        public Builder bootsDrop(double v) { this.bootsDrop = (float) v; return this; }
        public Equipment build() { return new Equipment(this); }
    }
}
