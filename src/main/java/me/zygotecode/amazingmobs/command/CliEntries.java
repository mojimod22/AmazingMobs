package me.zygotecode.amazingmobs.command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Parses {@code key=value} CLI args into the YAML-shaped maps used by skill/trait entries — so
 * authoring lists in-game (and runtime injection) reuses the exact same parser as files. Known
 * trigger keys are routed into a {@code trigger} block, everything else into {@code params}.
 */
public final class CliEntries {

    private CliEntries() {}

    private static final Set<String> TRIGGER_KEYS = Set.of(
            "types", "cooldown", "warmup", "chance", "min-range", "max-range",
            "radius", "duration", "target", "phases", "min-health-pct", "max-health-pct");

    /** Build a {@code {skill, trigger{...}, params{...}}} entry map from {@code key=value} args. */
    public static Map<String, Object> skillEntry(String skillId, List<String> kv) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("skill", skillId.toLowerCase(Locale.ROOT));
        Map<String, Object> trigger = new LinkedHashMap<>();
        Map<String, Object> params = new LinkedHashMap<>();
        for (String a : kv) {
            int eq = a.indexOf('=');
            if (eq < 1) continue;
            String k = a.substring(0, eq).toLowerCase(Locale.ROOT);
            String v = a.substring(eq + 1);
            Object val = (k.equals("types") || k.equals("phases")) ? splitList(v) : MobEditSession.coerce(v);
            if (TRIGGER_KEYS.contains(k)) trigger.put(k, val);
            else params.put(k, val);
        }
        if (!trigger.isEmpty()) entry.put("trigger", trigger);
        if (!params.isEmpty()) entry.put("params", params);
        return entry;
    }

    /** Build a flat params map (e.g. trait params, drop fields) from {@code key=value} args. */
    public static Map<String, Object> flat(List<String> kv) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (String a : kv) {
            int eq = a.indexOf('=');
            if (eq < 1) continue;
            m.put(a.substring(0, eq).toLowerCase(Locale.ROOT), MobEditSession.coerce(a.substring(eq + 1)));
        }
        return m;
    }

    private static List<String> splitList(String v) {
        return new ArrayList<>(Arrays.asList(v.split(",")));
    }
}
