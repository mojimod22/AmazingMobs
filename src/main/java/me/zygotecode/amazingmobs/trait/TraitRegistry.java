package me.zygotecode.amazingmobs.trait;

import me.zygotecode.amazingmobs.trait.impl.AmbusherTrait;
import me.zygotecode.amazingmobs.trait.impl.AuraTrait;
import me.zygotecode.amazingmobs.trait.impl.BerserkerTrait;
import me.zygotecode.amazingmobs.trait.impl.CarrierTrait;
import me.zygotecode.amazingmobs.trait.impl.CowardlyTrait;
import me.zygotecode.amazingmobs.trait.impl.EnrageTrait;
import me.zygotecode.amazingmobs.trait.impl.ExploderTrait;
import me.zygotecode.amazingmobs.trait.impl.FakeDeathTrait;
import me.zygotecode.amazingmobs.trait.impl.HealAuraTrait;
import me.zygotecode.amazingmobs.trait.impl.HunterTrait;
import me.zygotecode.amazingmobs.trait.impl.LeaperTrait;
import me.zygotecode.amazingmobs.trait.impl.MimicTrait;
import me.zygotecode.amazingmobs.trait.impl.PackTrait;
import me.zygotecode.amazingmobs.trait.impl.RevengeTrait;
import me.zygotecode.amazingmobs.trait.impl.TeleporterTrait;
import me.zygotecode.amazingmobs.trait.impl.ThornsTrait;
import me.zygotecode.amazingmobs.trait.impl.VampireTrait;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/** Registry of all known {@link Trait}s. Extensible — register your own, usable from config at once. */
public final class TraitRegistry {

    private final Map<String, Trait> traits = new TreeMap<>();

    public void register(Trait t) {
        if (t != null) traits.put(t.id().toLowerCase(Locale.ROOT), t);
    }

    public Trait get(String id) { return id == null ? null : traits.get(id.toLowerCase(Locale.ROOT)); }
    public boolean contains(String id) { return id != null && traits.containsKey(id.toLowerCase(Locale.ROOT)); }
    public Set<String> ids() { return Collections.unmodifiableSet(traits.keySet()); }
    public int size() { return traits.size(); }

    public void registerDefaults() {
        // --- behaviour ---
        register(new BerserkerTrait());
        register(new CowardlyTrait());
        register(new HunterTrait());
        register(new ThornsTrait());
        register(new ExploderTrait());
        register(new LeaperTrait());
        register(new MimicTrait());
        register(new RevengeTrait());
        register(new FakeDeathTrait());
        register(new EnrageTrait());
        register(new CarrierTrait());
        register(new VampireTrait("vampire", false));
        register(new VampireTrait("parasite", true));
        register(new AmbusherTrait("ambusher"));
        register(new AmbusherTrait("burrower"));
        register(new TeleporterTrait("teleporter"));
        register(new TeleporterTrait("phase_walker"));
        register(new PackTrait("pack"));
        register(new PackTrait("swarm_leader"));

        // --- healing ---
        register(new HealAuraTrait("healer", false));
        register(new HealAuraTrait("regenerator", true));

        // --- auras (synergy / control) ---
        register(new AuraTrait("buffer", AuraTrait.Mode.ALLIES, List.of("strength:0", "speed:0")));
        register(new AuraTrait("commander", AuraTrait.Mode.ALLIES, List.of("strength:0", "speed:0", "resistance:0")));
        register(new AuraTrait("protector", AuraTrait.Mode.ALLIES, List.of("resistance:0")));
        register(new AuraTrait("guardian", AuraTrait.Mode.ALLIES, List.of("resistance:1")));
        register(new AuraTrait("ritualist", AuraTrait.Mode.ALLIES, List.of("strength:0", "regeneration:0")));
        register(new AuraTrait("saboteur", AuraTrait.Mode.PLAYERS, List.of("mining_fatigue:0", "weakness:0")));
        register(new AuraTrait("disruptor", AuraTrait.Mode.PLAYERS, List.of("slowness:0", "weakness:0")));
        register(new AuraTrait("controller", AuraTrait.Mode.PLAYERS, List.of("slowness:0", "mining_fatigue:0")));
        register(new AuraTrait("hexer", AuraTrait.Mode.PLAYERS, List.of("poison:0", "weakness:0")));
        register(new AuraTrait("frost_aura", AuraTrait.Mode.PLAYERS, List.of("slowness:1")));
        register(new AuraTrait("siege", AuraTrait.Mode.PLAYERS, List.of("slowness:0")));
        register(new AuraTrait("swift", AuraTrait.Mode.SELF, List.of("speed:1")));   // sustained movement speed
        register(new AuraTrait("frenzy", AuraTrait.Mode.SELF, List.of("speed:1", "strength:0")));
    }
}
