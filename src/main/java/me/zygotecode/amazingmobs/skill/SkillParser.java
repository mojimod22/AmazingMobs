package me.zygotecode.amazingmobs.skill;

import me.zygotecode.amazingmobs.config.ConfigSection;
import me.zygotecode.amazingmobs.config.validation.Checks;
import me.zygotecode.amazingmobs.config.validation.ValidationReport;
import me.zygotecode.amazingmobs.util.Numbers;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Parses a skill entry ({@code skill}, {@code label}, {@code trigger}, {@code params}) into a
 * {@link SkillDefinition}. Pure (no Bukkit): the skill id is just a string validated against the
 * set of known ids, params are kept as a raw section, and the trigger is plain numbers/enums.
 * Fully unit-testable.
 */
public final class SkillParser {

    private SkillParser() {}

    /** @return a definition, or {@code null} if the entry is unusable (errors recorded). */
    public static SkillDefinition parse(ConfigSection sec, Set<String> knownSkillIds, ValidationReport report) {
        String id = sec.getString("skill");
        if (id == null || id.isBlank()) {
            report.error(sec.path() + ".skill", "missing 'skill' id");
            return null;
        }
        id = id.trim().toLowerCase(Locale.ROOT);
        if (knownSkillIds != null && !knownSkillIds.isEmpty() && !knownSkillIds.contains(id)) {
            report.error(sec.path() + ".skill", "unknown skill '" + id + "'");
            return null;
        }
        String label = sec.getString("label");
        ConfigSection params = sec.getSection("params");
        TriggerSpec trigger = parseTrigger(sec.getSection("trigger"), report);
        return new SkillDefinition(id, label, params, trigger);
    }

    public static TriggerSpec parseTrigger(ConfigSection t, ValidationReport report) {
        TriggerSpec.Builder b = TriggerSpec.builder();

        List<String> types = t.getStringList("types");
        if (types.isEmpty()) {
            b.trigger(TriggerType.TICK);
        } else {
            boolean any = false;
            for (String s : types) {
                TriggerType tt = TriggerType.fromString(s, null);
                if (tt == null) report.warn(t.path() + ".types", "unknown trigger '" + s + "' — ignored");
                else { b.trigger(tt); any = true; }
            }
            if (!any) b.trigger(TriggerType.TICK);
        }

        b.cooldownTicks(Numbers.parseTicks(t.getString("cooldown"), 100));
        b.warmupTicks(Numbers.parseTicks(t.getString("warmup"), 0));
        b.chance(Checks.pct(report, t.path() + ".chance", t.getDouble("chance", 1.0)));
        b.minRange(Checks.atLeast(report, t.path() + ".min-range", t.getDouble("min-range", 0), 0));
        b.maxRange(Math.max(0, t.getDouble("max-range", 0)));
        b.radius(Checks.atLeast(report, t.path() + ".radius", t.getDouble("radius", 4), 0));
        b.durationTicks(Numbers.parseTicks(t.getString("duration"), 60));
        b.targetRule(TargetRule.fromString(t.getString("target"), TargetRule.TARGET));

        List<String> phases = t.getStringList("phases");
        if (!phases.isEmpty()) b.phases(new LinkedHashSet<>(phases));

        b.minHealthPct(Checks.pct(report, t.path() + ".min-health-pct", t.getDouble("min-health-pct", 0)));
        b.maxHealthPct(Checks.pct(report, t.path() + ".max-health-pct", t.getDouble("max-health-pct", 1)));
        return b.build();
    }
}
