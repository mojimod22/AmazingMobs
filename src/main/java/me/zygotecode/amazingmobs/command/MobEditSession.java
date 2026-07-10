package me.zygotecode.amazingmobs.command;

import me.zygotecode.amazingmobs.config.ConfigSource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * A mutable, in-progress mob definition (the command-mode authoring buffer). Holds a YAML-shaped
 * map; {@code set} edits via dotted paths (e.g. {@code stats.health 200}) and the result is written
 * straight through the normal file pipeline on save — so command-built and file-built mobs are
 * identical artifacts.
 */
public final class MobEditSession {

    private String id;
    private final Map<String, Object> tree;

    public MobEditSession(String id, Map<String, Object> tree) {
        this.id = id;
        this.tree = tree;
    }

    public static MobEditSession fresh(String id, String type) {
        Map<String, Object> tree = new LinkedHashMap<>();
        tree.put("id", id);
        tree.put("type", type);
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("health", 40);
        stats.put("damage", 5);
        tree.put("stats", stats);
        return new MobEditSession(id, tree);
    }

    public String id() { return id; }
    public void setId(String id) { this.id = id; this.tree.put("id", id); }
    public Map<String, Object> tree() { return tree; }

    /** Set a dotted path, creating intermediate sections. Value type is coerced from the string. */
    @SuppressWarnings("unchecked")
    public void set(String path, String rawValue) {
        String[] parts = path.split("\\.");
        Map<String, Object> node = tree;
        for (int i = 0; i < parts.length - 1; i++) {
            Object child = node.get(parts[i]);
            if (!(child instanceof Map)) {
                child = new LinkedHashMap<String, Object>();
                node.put(parts[i], child);
            }
            node = (Map<String, Object>) child;
        }
        node.put(parts[parts.length - 1], coerce(rawValue));
    }

    @SuppressWarnings("unchecked")
    public boolean unset(String path) {
        String[] parts = path.split("\\.");
        Map<String, Object> node = tree;
        for (int i = 0; i < parts.length - 1; i++) {
            Object child = node.get(parts[i]);
            if (!(child instanceof Map)) return false;
            node = (Map<String, Object>) child;
        }
        return node.remove(parts[parts.length - 1]) != null;
    }

    /** Append an entry to the list at a dotted path (creating sections/list as needed). */
    @SuppressWarnings("unchecked")
    public void addToList(String path, Object entry) {
        String[] parts = path.split("\\.");
        Map<String, Object> node = tree;
        for (int i = 0; i < parts.length - 1; i++) {
            Object child = node.get(parts[i]);
            if (!(child instanceof Map)) { child = new LinkedHashMap<String, Object>(); node.put(parts[i], child); }
            node = (Map<String, Object>) child;
        }
        String leaf = parts[parts.length - 1];
        Object cur = node.get(leaf);
        List<Object> list = cur instanceof List ? (List<Object>) cur : new ArrayList<>();
        list.add(entry);
        node.put(leaf, list);
    }

    @SuppressWarnings("unchecked")
    public List<Object> getList(String path) {
        String[] parts = path.split("\\.");
        Map<String, Object> node = tree;
        for (int i = 0; i < parts.length - 1; i++) {
            Object child = node.get(parts[i]);
            if (!(child instanceof Map)) return Collections.emptyList();
            node = (Map<String, Object>) child;
        }
        Object cur = node.get(parts[parts.length - 1]);
        return cur instanceof List ? (List<Object>) cur : Collections.emptyList();
    }

    public boolean removeFromList(String path, int index) {
        List<Object> list = getList(path);
        if (index < 0 || index >= list.size()) return false;
        list.remove(index);
        return true;
    }

    /** Pretty YAML preview of the in-progress definition. */
    public String preview() {
        return ConfigSource.toYamlString(tree);
    }

    /** Coerce a CLI string into the most natural YAML scalar (int/double/bool/string). */
    public static Object coerce(String s) {
        if (s == null) return null;
        String t = s.trim();
        String low = t.toLowerCase(Locale.ROOT);
        if (low.equals("true") || low.equals("false")) return Boolean.parseBoolean(low);
        // keep ranges ("3-8") and decorated text as strings
        try { return Integer.parseInt(t); } catch (NumberFormatException ignored) {}
        try {
            if (t.matches("-?\\d+\\.\\d+")) return Double.parseDouble(t);
        } catch (NumberFormatException ignored) {}
        return t;
    }
}
