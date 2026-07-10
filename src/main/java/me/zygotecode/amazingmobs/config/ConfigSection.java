package me.zygotecode.amazingmobs.config;

import me.zygotecode.amazingmobs.config.validation.ValidationReport;
import me.zygotecode.amazingmobs.util.DoubleRange;
import me.zygotecode.amazingmobs.util.IntRange;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Bukkit-free view over a parsed YAML node ({@code Map<String,Object>}). All typed getters take a
 * default and record a {@code WARN} on a present-but-wrong-type value, so parsing never throws and
 * always produces a usable value plus a clear audit trail.
 *
 * <p>Pure by design: construct one from a hand-made {@link Map} in tests via {@link #of(Map)} — no
 * server needed. At runtime {@link ConfigSource} builds the map tree from a {@code .yml} file.</p>
 */
public final class ConfigSection {

    private final Map<String, Object> map;
    private final String path;
    private final ValidationReport report;

    public ConfigSection(Map<String, Object> map, String path, ValidationReport report) {
        this.map = map == null ? Collections.emptyMap() : map;
        this.path = path == null ? "" : path;
        this.report = report == null ? new ValidationReport("?") : report;
    }

    /** Test helper: wrap a map with a fresh report rooted at "". */
    public static ConfigSection of(Map<String, Object> map) {
        return new ConfigSection(map, "", new ValidationReport("test"));
    }

    /** Empty section sharing a report (used when an optional section is absent). */
    public static ConfigSection empty(String path, ValidationReport report) {
        return new ConfigSection(Collections.emptyMap(), path, report);
    }

    public ValidationReport report() { return report; }
    public String path() { return path; }
    public boolean isEmpty() { return map.isEmpty(); }
    public Set<String> keys() { return map.keySet(); }
    public boolean contains(String key) { return map.containsKey(key); }
    public Object getRaw(String key) { return map.get(key); }

    private String childPath(String key) {
        return path.isEmpty() ? key : path + "." + key;
    }

    // ---- scalars -------------------------------------------------------------------------------

    public String getString(String key, String def) {
        Object v = map.get(key);
        if (v == null) return def;
        if (v instanceof String s) return s;
        if (v instanceof Number || v instanceof Boolean) return String.valueOf(v);
        report.warn(childPath(key), "expected text, got " + typeOf(v) + " — using default '" + def + "'");
        return def;
    }

    /** @return the string or {@code null} if absent (no warning). */
    public String getString(String key) {
        Object v = map.get(key);
        return v instanceof String s ? s : (v == null ? null : String.valueOf(v));
    }

    public int getInt(String key, int def) {
        Object v = map.get(key);
        if (v == null) return def;
        if (v instanceof Number n) return n.intValue();
        if (v instanceof String s) {
            try { return Integer.parseInt(s.trim()); } catch (NumberFormatException ignored) {}
        }
        report.warn(childPath(key), "expected integer, got '" + v + "' — using default " + def);
        return def;
    }

    public long getLong(String key, long def) {
        Object v = map.get(key);
        if (v == null) return def;
        if (v instanceof Number n) return n.longValue();
        if (v instanceof String s) {
            try { return Long.parseLong(s.trim()); } catch (NumberFormatException ignored) {}
        }
        report.warn(childPath(key), "expected number, got '" + v + "' — using default " + def);
        return def;
    }

    public double getDouble(String key, double def) {
        Object v = map.get(key);
        if (v == null) return def;
        if (v instanceof Number n) return n.doubleValue();
        if (v instanceof String s) {
            try { return Double.parseDouble(s.trim()); } catch (NumberFormatException ignored) {}
        }
        report.warn(childPath(key), "expected decimal, got '" + v + "' — using default " + def);
        return def;
    }

    public boolean getBool(String key, boolean def) {
        Object v = map.get(key);
        if (v == null) return def;
        if (v instanceof Boolean b) return b;
        if (v instanceof String s) {
            String t = s.trim().toLowerCase(Locale.ROOT);
            if (t.equals("true") || t.equals("yes") || t.equals("on")) return true;
            if (t.equals("false") || t.equals("no") || t.equals("off")) return false;
        }
        report.warn(childPath(key), "expected true/false, got '" + v + "' — using default " + def);
        return def;
    }

    /** Enum-by-name, case-insensitive, with a default and a clear warning listing it's invalid. */
    public <E extends Enum<E>> E getEnum(String key, Class<E> type, E def) {
        String s = getString(key);
        if (s == null) return def;
        try {
            return Enum.valueOf(type, s.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            report.warn(childPath(key), "unknown " + type.getSimpleName() + " '" + s + "' — using " + def);
            return def;
        }
    }

    public IntRange getIntRange(String key, IntRange def) {
        Object v = map.get(key);
        if (v == null) return def;
        if (v instanceof Number n) return IntRange.of(n.intValue());
        if (v instanceof String s) return IntRange.parse(s, def);
        report.warn(childPath(key), "expected number or 'a-b' range, got '" + v + "' — using " + def);
        return def;
    }

    public DoubleRange getDoubleRange(String key, DoubleRange def) {
        Object v = map.get(key);
        if (v == null) return def;
        if (v instanceof Number n) return DoubleRange.of(n.doubleValue());
        if (v instanceof String s) return DoubleRange.parse(s, def);
        report.warn(childPath(key), "expected number or 'a-b' range, got '" + v + "' — using " + def);
        return def;
    }

    // ---- lists ---------------------------------------------------------------------------------

    public List<String> getStringList(String key) {
        Object v = map.get(key);
        List<String> out = new ArrayList<>();
        if (v instanceof List<?> list) {
            for (Object o : list) if (o != null) out.add(String.valueOf(o));
        } else if (v instanceof String s) {
            out.add(s); // tolerate a single scalar where a list is expected
        } else if (v != null) {
            report.warn(childPath(key), "expected a list, got " + typeOf(v) + " — treating as empty");
        }
        return out;
    }

    // ---- nested --------------------------------------------------------------------------------

    public boolean isSection(String key) {
        return map.get(key) instanceof Map;
    }

    /** Nested section, or an empty section (never null) if absent / not a mapping. */
    @SuppressWarnings("unchecked")
    public ConfigSection getSection(String key) {
        Object v = map.get(key);
        if (v instanceof Map<?, ?> m) {
            return new ConfigSection((Map<String, Object>) m, childPath(key), report);
        }
        if (v != null) {
            report.warn(childPath(key), "expected a section, got " + typeOf(v) + " — treating as empty");
        }
        return ConfigSection.empty(childPath(key), report);
    }

    /**
     * List of sections. Accepts a YAML list of mappings. Non-mapping entries are skipped with a warning.
     */
    @SuppressWarnings("unchecked")
    public List<ConfigSection> getSectionList(String key) {
        Object v = map.get(key);
        List<ConfigSection> out = new ArrayList<>();
        if (v instanceof List<?> list) {
            int idx = 0;
            for (Object o : list) {
                if (o instanceof Map<?, ?> m) {
                    out.add(new ConfigSection((Map<String, Object>) m, childPath(key) + "[" + idx + "]", report));
                } else if (o != null) {
                    report.warn(childPath(key) + "[" + idx + "]", "expected a mapping — skipped");
                }
                idx++;
            }
        } else if (v != null) {
            report.warn(childPath(key), "expected a list of sections, got " + typeOf(v));
        }
        return out;
    }

    /** Raw nested map (for round-trip / overrides). Returns empty map if absent. */
    @SuppressWarnings("unchecked")
    public Map<String, Object> rawMap() {
        return new LinkedHashMap<>(map);
    }

    private static String typeOf(Object v) {
        return v == null ? "null" : v.getClass().getSimpleName();
    }
}
