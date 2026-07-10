package me.zygotecode.amazingmobs.trait;

import me.zygotecode.amazingmobs.config.ConfigSection;
import me.zygotecode.amazingmobs.config.validation.ValidationReport;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Parses a {@code traits:} node into {@link TraitDefinition}s. Pure (no Bukkit). Accepts either a
 * simple id list ({@code traits: [berserker, thorns]}) or a list of maps
 * ({@code traits: [{trait: berserker, threshold: 0.4}, ...]}). Unknown trait ids are warned + dropped.
 */
public final class TraitParser {

    private TraitParser() {}

    /** @param parent the section that may contain a {@code traits} key. */
    public static List<TraitDefinition> parse(ConfigSection parent, Set<String> knownTraitIds, ValidationReport report) {
        List<TraitDefinition> out = new ArrayList<>();
        if (!parent.contains("traits")) return out;

        List<ConfigSection> sections = parent.getSectionList("traits");
        if (!sections.isEmpty()) {
            for (ConfigSection sec : sections) {
                String id = norm(sec.getString("trait"));
                if (id == null) { report.warn(sec.path() + ".trait", "missing trait id — skipped"); continue; }
                if (!known(knownTraitIds, id)) { report.warn(sec.path() + ".trait", "unknown trait '" + id + "' — skipped"); continue; }
                ConfigSection params = sec.contains("params") ? sec.getSection("params") : sec;
                out.add(new TraitDefinition(id, params));
            }
            return out;
        }

        // id-only list form
        for (String raw : parent.getStringList("traits")) {
            String id = norm(raw);
            if (id == null) continue;
            if (!known(knownTraitIds, id)) { report.warn(parent.path() + ".traits", "unknown trait '" + id + "' — skipped"); continue; }
            out.add(new TraitDefinition(id, ConfigSection.empty(parent.path() + ".traits." + id, report)));
        }
        return out;
    }

    private static String norm(String s) {
        return (s == null || s.isBlank()) ? null : s.trim().toLowerCase(Locale.ROOT);
    }

    private static boolean known(Set<String> known, String id) {
        return known == null || known.isEmpty() || known.contains(id);
    }
}
