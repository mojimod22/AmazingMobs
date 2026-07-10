package me.zygotecode.amazingmobs.mob;

import me.zygotecode.amazingmobs.scaling.ScalingRule;
import me.zygotecode.amazingmobs.skill.SkillDefinition;
import me.zygotecode.amazingmobs.trait.TraitDefinition;
import org.bukkit.entity.EntityType;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Complete, immutable description of a custom mob. Built once by {@code MobParser} and shared by
 * every spawn. The runtime ({@code MobSpawner}/{@code MobManager}) reads it but never mutates it.
 */
public final class MobDefinition {

    private final String id;
    private final String displayName;        // MiniMessage
    private final List<String> lore;
    private final EntityType baseType;
    private final Tier tier;
    private final String category;
    private final Set<String> tags;
    private final StatBlock stats;
    private final Equipment equipment;
    private final AiProfile ai;
    private final List<SkillDefinition> skills;
    private final List<Phase> phases;         // sorted high→low threshold
    private final DropTable drops;
    private final Presentation presentation;
    private final ScalingRule scaling;
    private final List<TraitDefinition> traits;
    private final List<Variant> variants;
    private final double variantBaseWeight;
    private final MountSpec mount;            // this mob rides a mount (nullable)
    private final List<RiderSpec> riders;     // mobs stacked on top of this mob

    private MobDefinition(Builder b) {
        this.id = b.id;
        this.displayName = b.displayName != null ? b.displayName : b.id;
        this.lore = b.lore == null ? List.of() : List.copyOf(b.lore);
        this.baseType = b.baseType;
        this.tier = b.tier;
        this.category = b.category;
        this.tags = b.tags == null ? Set.of() : Set.copyOf(b.tags);
        this.stats = b.stats;
        this.equipment = b.equipment == null ? Equipment.EMPTY : b.equipment;
        this.ai = b.ai == null ? AiProfile.DEFAULT : b.ai;
        this.skills = b.skills == null ? List.of() : List.copyOf(b.skills);
        List<Phase> ph = b.phases == null ? new ArrayList<>() : new ArrayList<>(b.phases);
        ph.sort((x, y) -> Double.compare(y.thresholdPct(), x.thresholdPct())); // high → low
        this.phases = List.copyOf(ph);
        this.drops = b.drops == null ? DropTable.EMPTY : b.drops;
        this.presentation = b.presentation == null ? Presentation.DEFAULT : b.presentation;
        this.scaling = b.scaling == null ? ScalingRule.NONE : b.scaling;
        this.traits = b.traits == null ? List.of() : List.copyOf(b.traits);
        this.variants = b.variants == null ? List.of() : List.copyOf(b.variants);
        this.variantBaseWeight = b.variantBaseWeight;
        this.mount = b.mount;
        this.riders = b.riders == null ? List.of() : List.copyOf(b.riders);
    }

    public String id() { return id; }
    public String displayName() { return displayName; }
    public List<String> lore() { return lore; }
    public EntityType baseType() { return baseType; }
    public Tier tier() { return tier; }
    public String category() { return category; }
    public Set<String> tags() { return tags; }
    public boolean hasTag(String t) { return tags.contains(t); }
    public StatBlock stats() { return stats; }
    public Equipment equipment() { return equipment; }
    public AiProfile ai() { return ai; }
    public List<SkillDefinition> skills() { return skills; }
    public List<Phase> phases() { return phases; }
    public boolean hasPhases() { return !phases.isEmpty(); }
    public DropTable drops() { return drops; }
    public Presentation presentation() { return presentation; }
    public ScalingRule scaling() { return scaling; }
    public List<TraitDefinition> traits() { return traits; }
    public boolean hasTraits() { return !traits.isEmpty(); }
    public List<Variant> variants() { return variants; }
    public boolean hasVariants() { return !variants.isEmpty(); }
    public double variantBaseWeight() { return variantBaseWeight; }
    public MountSpec mount() { return mount; }
    public List<RiderSpec> riders() { return riders; }
    public boolean hasRiders() { return !riders.isEmpty(); }

    public static Builder builder(String id) { return new Builder(id); }

    /** A builder pre-populated from this definition (used by variant overlays). */
    public Builder toBuilder() {
        Builder b = new Builder(id);
        b.displayName = displayName;
        b.lore = new ArrayList<>(lore);
        b.baseType = baseType;
        b.tier = tier;
        b.category = category;
        b.tags = new LinkedHashSet<>(tags);
        b.stats = stats;
        b.equipment = equipment;
        b.ai = ai;
        b.skills = new ArrayList<>(skills);
        b.phases = new ArrayList<>(phases);
        b.drops = drops;
        b.presentation = presentation;
        b.scaling = scaling;
        b.traits = new ArrayList<>(traits);
        b.variants = new ArrayList<>(variants);
        b.variantBaseWeight = variantBaseWeight;
        b.mount = mount;
        b.riders = new ArrayList<>(riders);
        return b;
    }

    public static final class Builder {
        private final String id;
        private String displayName;
        private List<String> lore;
        private EntityType baseType;
        private Tier tier = Tier.COMMON;
        private String category = "general";
        private Set<String> tags = new LinkedHashSet<>();
        private StatBlock stats = StatBlock.builder().build();
        private Equipment equipment;
        private AiProfile ai;
        private List<SkillDefinition> skills;
        private List<Phase> phases;
        private DropTable drops;
        private Presentation presentation;
        private ScalingRule scaling;
        private List<TraitDefinition> traits;
        private List<Variant> variants;
        private double variantBaseWeight = 1.0;
        private MountSpec mount;
        private List<RiderSpec> riders;

        public Builder(String id) { this.id = id; }

        public Builder displayName(String v) { this.displayName = v; return this; }
        public Builder lore(List<String> v) { this.lore = v; return this; }
        public Builder baseType(EntityType v) { this.baseType = v; return this; }
        public Builder tier(Tier v) { this.tier = v; return this; }
        public Builder category(String v) { this.category = v; return this; }
        public Builder tags(Set<String> v) { this.tags = v; return this; }
        public Builder stats(StatBlock v) { this.stats = v; return this; }
        public Builder equipment(Equipment v) { this.equipment = v; return this; }
        public Builder ai(AiProfile v) { this.ai = v; return this; }
        public Builder skills(List<SkillDefinition> v) { this.skills = v; return this; }
        public Builder phases(List<Phase> v) { this.phases = v; return this; }
        public Builder drops(DropTable v) { this.drops = v; return this; }
        public Builder presentation(Presentation v) { this.presentation = v; return this; }
        public Builder scaling(ScalingRule v) { this.scaling = v; return this; }
        public Builder traits(List<TraitDefinition> v) { this.traits = v; return this; }
        public Builder variants(List<Variant> v) { this.variants = v; return this; }
        public Builder variantBaseWeight(double v) { this.variantBaseWeight = v; return this; }
        public Builder mount(MountSpec v) { this.mount = v; return this; }
        public Builder riders(List<RiderSpec> v) { this.riders = v; return this; }
        public MobDefinition build() { return new MobDefinition(this); }
    }
}
