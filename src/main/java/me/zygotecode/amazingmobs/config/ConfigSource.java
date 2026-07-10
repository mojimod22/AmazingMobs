package me.zygotecode.amazingmobs.config;

import me.zygotecode.amazingmobs.config.validation.ValidationReport;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Bridges a {@code .yml} file to the Bukkit-free {@link ConfigSection} model. This is the ONLY place
 * that touches {@link YamlConfiguration}; everything downstream works on plain maps and is testable.
 *
 * <p>Loading never throws: a corrupt file produces an {@code ERROR} on the report and an empty
 * section, so one bad file never aborts the whole load.</p>
 */
public final class ConfigSource {

    private ConfigSource() {}

    /** Load a YAML file into a section tree. Records a single ERROR if parsing fails. */
    public static ConfigSection load(File file, ValidationReport report) {
        YamlConfiguration yaml = new YamlConfiguration();
        try {
            yaml.load(file);
        } catch (Exception ex) {
            report.error("", "could not parse YAML: " + rootMessage(ex));
            return ConfigSection.empty("", report);
        }
        return new ConfigSection(toMap(yaml), "", report);
    }

    /** Parse from a raw YAML string (used by tests and {@code /am} import paths). */
    public static ConfigSection parse(String yamlText, ValidationReport report) {
        YamlConfiguration yaml = new YamlConfiguration();
        try {
            yaml.loadFromString(yamlText);
        } catch (Exception ex) {
            report.error("", "could not parse YAML: " + rootMessage(ex));
            return ConfigSection.empty("", report);
        }
        return new ConfigSection(toMap(yaml), "", report);
    }

    /** Serialize a map tree to YAML on disk (used by {@code /am save} and {@code /am export}). */
    public static void write(File file, Map<String, Object> tree) throws IOException {
        YamlConfiguration yaml = new YamlConfiguration();
        applyTo(yaml, tree);
        File parent = file.getParentFile();
        if (parent != null) parent.mkdirs();
        yaml.save(file);
    }

    /** Serialize a map tree to a YAML string (used by {@code /am show} session preview). */
    public static String toYamlString(Map<String, Object> tree) {
        YamlConfiguration yaml = new YamlConfiguration();
        applyTo(yaml, tree);
        return yaml.saveToString();
    }

    // ---- conversion ----------------------------------------------------------------------------

    private static Map<String, Object> toMap(ConfigurationSection sec) {
        Map<String, Object> out = new LinkedHashMap<>();
        for (String key : sec.getKeys(false)) {
            Object v = sec.get(key);
            out.put(key, convert(v));
        }
        return out;
    }

    private static Object convert(Object v) {
        if (v instanceof ConfigurationSection cs) {
            return toMap(cs);
        }
        if (v instanceof Map<?, ?> m) {
            return normalizeMap(m);
        }
        if (v instanceof List<?> list) {
            List<Object> copy = new ArrayList<>(list.size());
            for (Object o : list) copy.add(convert(o));
            return copy;
        }
        return v;
    }

    private static Map<String, Object> normalizeMap(Map<?, ?> m) {
        Map<String, Object> out = new LinkedHashMap<>();
        for (Map.Entry<?, ?> e : m.entrySet()) {
            out.put(String.valueOf(e.getKey()), convert(e.getValue()));
        }
        return out;
    }

    @SuppressWarnings("unchecked")
    private static void applyTo(ConfigurationSection sec, Map<String, Object> tree) {
        for (Map.Entry<String, Object> e : tree.entrySet()) {
            Object v = e.getValue();
            if (v instanceof Map<?, ?> child) {
                ConfigurationSection cs = sec.createSection(e.getKey());
                applyTo(cs, (Map<String, Object>) child);
            } else {
                sec.set(e.getKey(), v);
            }
        }
    }

    private static String rootMessage(Throwable ex) {
        Throwable t = ex;
        while (t.getCause() != null && t.getCause() != t) t = t.getCause();
        String m = t.getMessage();
        return m == null ? t.getClass().getSimpleName() : m.split("\n")[0];
    }
}
