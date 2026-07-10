package me.zygotecode.amazingmobs.mob;

import me.zygotecode.amazingmobs.skill.SkillDefinition;
import me.zygotecode.amazingmobs.trait.TraitDefinition;
import me.zygotecode.amazingmobs.util.Rng;
import org.bukkit.Location;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * A data-driven mutation overlay rolled at spawn: tweak stats, add traits/skills/tags, recolour and
 * rename, add drops — turning one base mob into fire/ice/corrupted/legendary/etc. variants. Pure
 * transform: {@link #apply} returns a new {@link MobDefinition}; conditions gate where it may roll.
 */
public final class Variant {

    private final String id;
    private final double weight;
    private final SpawnConditions conditions;
    private final String name;          // full MiniMessage override (nullable)
    private final String namePrefix;    // prepended to base name (nullable)
    private final double healthMul, damageMul, speedMul, armorMul;
    private final List<TraitDefinition> addedTraits;
    private final List<SkillDefinition> addedSkills;
    private final Set<String> addedTags;
    private final Tier tier;            // nullable
    private final Boolean glow;         // nullable
    private final String glowColor, ambientParticle, ambientSound; // nullable overrides
    private final List<DropTable.DropEntry> extraDrops;

    private Variant(Builder b) {
        this.id = b.id;
        this.weight = b.weight;
        this.conditions = b.conditions == null ? SpawnConditions.ANY : b.conditions;
        this.name = b.name;
        this.namePrefix = b.namePrefix;
        this.healthMul = b.healthMul;
        this.damageMul = b.damageMul;
        this.speedMul = b.speedMul;
        this.armorMul = b.armorMul;
        this.addedTraits = b.addedTraits == null ? List.of() : List.copyOf(b.addedTraits);
        this.addedSkills = b.addedSkills == null ? List.of() : List.copyOf(b.addedSkills);
        this.addedTags = b.addedTags == null ? Set.of() : Set.copyOf(b.addedTags);
        this.tier = b.tier;
        this.glow = b.glow;
        this.glowColor = b.glowColor;
        this.ambientParticle = b.ambientParticle;
        this.ambientSound = b.ambientSound;
        this.extraDrops = b.extraDrops == null ? List.of() : List.copyOf(b.extraDrops);
    }

    public String id() { return id; }
    public double weight() { return weight; }
    public SpawnConditions conditions() { return conditions; }

    /** Produce a mutated copy of {@code base} with this variant applied. */
    public MobDefinition apply(MobDefinition base) {
        MobDefinition.Builder b = base.toBuilder();

        if (name != null) b.displayName(name);
        else if (namePrefix != null) b.displayName(namePrefix + " " + base.displayName());

        StatBlock.Builder sb = base.stats().toBuilder();
        sb.health(base.stats().health() * healthMul);
        sb.attackDamage(base.stats().attackDamage() * damageMul);
        if (base.stats().overridesMovementSpeed()) sb.movementSpeed(base.stats().movementSpeed() * speedMul);
        sb.armor(base.stats().armor() * armorMul);
        b.stats(sb.build());

        if (!addedTraits.isEmpty()) {
            List<TraitDefinition> t = new ArrayList<>(base.traits());
            t.addAll(addedTraits);
            b.traits(t);
        }
        if (!addedSkills.isEmpty()) {
            List<SkillDefinition> s = new ArrayList<>(base.skills());
            s.addAll(addedSkills);
            b.skills(s);
        }
        if (!addedTags.isEmpty()) {
            Set<String> tg = new LinkedHashSet<>(base.tags());
            tg.addAll(addedTags);
            b.tags(tg);
        }
        if (tier != null) b.tier(tier);

        Presentation p = base.presentation();
        b.presentation(Presentation.builder()
                .glow(glow != null ? glow : p.glow())
                .glowColor(glowColor != null ? glowColor : p.glowColor())
                .nameVisible(p.nameVisible())
                .bossBar(p.bossBar())
                .bossBarColor(p.bossBarColor())
                .bossBarTitle(p.bossBarTitle())
                .ambientParticle(ambientParticle != null ? ambientParticle : p.ambientParticle())
                .ambientSound(ambientSound != null ? ambientSound : p.ambientSound())
                .build());

        if (!extraDrops.isEmpty()) {
            List<DropTable.DropEntry> entries = new ArrayList<>(base.drops().entries());
            entries.addAll(extraDrops);
            b.drops(new DropTable(entries, base.drops().xp(), base.drops().clearVanillaDrops()));
        }
        return b.build();
    }

    /**
     * Weighted pick among condition-matching variants, with {@code baseWeight} as the "no variant"
     * weight. Returns the chosen variant, or {@code null} for "spawn the plain base mob".
     */
    public static Variant pick(List<Variant> variants, double baseWeight, Location loc, int players, Rng rng) {
        if (variants == null || variants.isEmpty()) return null;
        double total = Math.max(0, baseWeight);
        List<Variant> eligible = new ArrayList<>();
        for (Variant v : variants) {
            if (v.weight > 0 && v.conditions.matches(loc, players)) { eligible.add(v); total += v.weight; }
        }
        if (eligible.isEmpty() || total <= 0) return null;
        double roll = rng.rangeDouble(0, total);
        if (roll < baseWeight) return null;
        roll -= baseWeight;
        for (Variant v : eligible) {
            roll -= v.weight;
            if (roll < 0) return v;
        }
        return eligible.get(eligible.size() - 1);
    }

    public static Builder builder(String id) { return new Builder(id); }

    public static final class Builder {
        private final String id;
        private double weight = 1.0;
        private SpawnConditions conditions;
        private String name, namePrefix;
        private double healthMul = 1, damageMul = 1, speedMul = 1, armorMul = 1;
        private List<TraitDefinition> addedTraits;
        private List<SkillDefinition> addedSkills;
        private Set<String> addedTags;
        private Tier tier;
        private Boolean glow;
        private String glowColor, ambientParticle, ambientSound;
        private List<DropTable.DropEntry> extraDrops;

        public Builder(String id) { this.id = id; }
        public Builder weight(double v) { this.weight = v; return this; }
        public Builder conditions(SpawnConditions v) { this.conditions = v; return this; }
        public Builder name(String v) { this.name = v; return this; }
        public Builder namePrefix(String v) { this.namePrefix = v; return this; }
        public Builder healthMul(double v) { this.healthMul = v; return this; }
        public Builder damageMul(double v) { this.damageMul = v; return this; }
        public Builder speedMul(double v) { this.speedMul = v; return this; }
        public Builder armorMul(double v) { this.armorMul = v; return this; }
        public Builder addedTraits(List<TraitDefinition> v) { this.addedTraits = v; return this; }
        public Builder addedSkills(List<SkillDefinition> v) { this.addedSkills = v; return this; }
        public Builder addedTags(Set<String> v) { this.addedTags = v; return this; }
        public Builder tier(Tier v) { this.tier = v; return this; }
        public Builder glow(Boolean v) { this.glow = v; return this; }
        public Builder glowColor(String v) { this.glowColor = v; return this; }
        public Builder ambientParticle(String v) { this.ambientParticle = v; return this; }
        public Builder ambientSound(String v) { this.ambientSound = v; return this; }
        public Builder extraDrops(List<DropTable.DropEntry> v) { this.extraDrops = v; return this; }
        public Variant build() { return new Variant(this); }
    }
}
