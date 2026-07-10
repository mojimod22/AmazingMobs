package me.zygotecode.amazingmobs.mob;

import me.zygotecode.amazingmobs.config.ConfigSection;
import me.zygotecode.amazingmobs.config.validation.ValidationReport;
import me.zygotecode.amazingmobs.util.IntRange;
import me.zygotecode.amazingmobs.util.Resolvers;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;

/**
 * Parses an item section into an {@link ItemSpec}. Resolves {@link Material}/{@link Enchantment}
 * by name (runtime). A bad/missing material is a {@code WARN} and yields {@code null} so the caller
 * simply skips that slot/drop rather than rejecting the whole mob.
 */
public final class ItemSpecParser {

    private ItemSpecParser() {}

    public static ItemSpec parse(ConfigSection sec, ValidationReport report) {
        if (sec.isEmpty()) return null;
        String matName = sec.getString("material");
        if (matName == null || matName.isBlank()) {
            report.warn(sec.path() + ".material", "missing material — slot ignored");
            return null;
        }
        Material mat = Resolvers.material(matName, null);
        if (mat == null || !mat.isItem()) {
            report.warn(sec.path() + ".material", "unknown/invalid item material '" + matName + "' — ignored");
            return null;
        }

        ItemSpec.Builder b = ItemSpec.builder(mat);
        b.amount(sec.getIntRange("amount", IntRange.of(1)));
        b.name(sec.getString("name"));
        b.lore(sec.getStringList("lore"));

        ConfigSection ench = sec.getSection("enchants");
        for (String k : ench.keys()) {
            Enchantment e = Resolvers.enchant(k);
            if (e == null) {
                report.warn(ench.path() + "." + k, "unknown enchantment '" + k + "' — ignored");
                continue;
            }
            b.enchant(e, Math.max(1, ench.getInt(k, 1)));
        }

        if (sec.contains("custom-model-data")) b.customModelData(sec.getInt("custom-model-data", 0));
        b.unbreakable(sec.getBool("unbreakable", false));
        b.glow(sec.getBool("glow", false));
        b.hideAttributes(sec.getBool("hide-attributes", false));
        return b.build();
    }
}
