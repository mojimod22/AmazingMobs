package me.zygotecode.amazingmobs.mob;

import java.util.Collection;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/** In-memory store of loaded {@link MobDefinition}s by id. Swapped atomically on reload. */
public final class MobRegistry {

    private final Map<String, MobDefinition> mobs = new TreeMap<>();

    public void register(MobDefinition def) {
        if (def != null) mobs.put(def.id().toLowerCase(Locale.ROOT), def);
    }

    public MobDefinition get(String id) {
        return id == null ? null : mobs.get(id.toLowerCase(Locale.ROOT));
    }

    public boolean contains(String id) {
        return id != null && mobs.containsKey(id.toLowerCase(Locale.ROOT));
    }

    public boolean remove(String id) {
        return id != null && mobs.remove(id.toLowerCase(Locale.ROOT)) != null;
    }

    public Collection<MobDefinition> all() { return Collections.unmodifiableCollection(mobs.values()); }
    public Set<String> ids() { return Collections.unmodifiableSet(mobs.keySet()); }
    public int size() { return mobs.size(); }
    public void clear() { mobs.clear(); }
}
