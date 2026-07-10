package me.zygotecode.amazingmobs.mob;

import me.zygotecode.amazingmobs.util.IntRange;
import me.zygotecode.amazingmobs.util.Rng;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Custom loot for a mob: a list of chance-gated item entries plus an XP range. Can optionally
 * replace the entity's vanilla drops entirely.
 */
public final class DropTable {

    /** @param item the item to drop; @param chance roll probability 0..1 */
    public record DropEntry(ItemSpec item, double chance) {}

    public static final DropTable EMPTY = new DropTable(List.of(), IntRange.of(0), false);

    private final List<DropEntry> entries;
    private final IntRange xp;
    private final boolean clearVanillaDrops;

    public DropTable(List<DropEntry> entries, IntRange xp, boolean clearVanillaDrops) {
        this.entries = entries == null ? List.of() : List.copyOf(entries);
        this.xp = xp == null ? IntRange.of(0) : xp;
        this.clearVanillaDrops = clearVanillaDrops;
    }

    public boolean clearVanillaDrops() { return clearVanillaDrops; }
    public boolean isEmpty() { return entries.isEmpty() && xp.max() == 0; }
    public List<DropEntry> entries() { return entries; }
    public IntRange xp() { return xp; }

    /** Roll the table into concrete item stacks. */
    public List<ItemStack> roll(Rng rng) {
        List<ItemStack> out = new ArrayList<>();
        for (DropEntry e : entries) {
            if (rng.chance(e.chance())) out.add(e.item().build(rng));
        }
        return out;
    }

    public int rollXp(Rng rng) {
        return xp.pick(rng);
    }
}
