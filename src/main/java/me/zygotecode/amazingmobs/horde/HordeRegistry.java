package me.zygotecode.amazingmobs.horde;

import java.util.Collection;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/** In-memory store of loaded {@link HordeDefinition}s by id. Swapped atomically on reload. */
public final class HordeRegistry {

    private final Map<String, HordeDefinition> hordes = new TreeMap<>();

    public void register(HordeDefinition def) {
        if (def != null) hordes.put(def.id().toLowerCase(Locale.ROOT), def);
    }

    public HordeDefinition get(String id) {
        return id == null ? null : hordes.get(id.toLowerCase(Locale.ROOT));
    }

    public boolean contains(String id) {
        return id != null && hordes.containsKey(id.toLowerCase(Locale.ROOT));
    }

    public boolean remove(String id) {
        return id != null && hordes.remove(id.toLowerCase(Locale.ROOT)) != null;
    }

    public Collection<HordeDefinition> all() { return Collections.unmodifiableCollection(hordes.values()); }
    public Set<String> ids() { return Collections.unmodifiableSet(hordes.keySet()); }
    public int size() { return hordes.size(); }
    public void clear() { hordes.clear(); }
}
