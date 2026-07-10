package me.zygotecode.amazingmobs.mob;

import me.zygotecode.amazingmobs.config.ConfigSection;
import me.zygotecode.amazingmobs.config.validation.Checks;
import me.zygotecode.amazingmobs.config.validation.ValidationReport;
import me.zygotecode.amazingmobs.scaling.ScalingRule;
import me.zygotecode.amazingmobs.skill.SkillDefinition;
import me.zygotecode.amazingmobs.skill.SkillParser;
import me.zygotecode.amazingmobs.trait.TraitDefinition;
import me.zygotecode.amazingmobs.trait.TraitParser;
import me.zygotecode.amazingmobs.util.IntRange;
import me.zygotecode.amazingmobs.util.Resolvers;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * Builds a {@link MobDefinition} from a parsed YAML section. The mob's base entity type and item
 * materials are resolved here (runtime). Only genuinely unspawnable configs (missing/invalid base
 * type, invalid id) are rejected with {@code ERROR}; everything else is corrected with a
 * {@code WARN} and a default, so one bad field never loses the whole mob.
 */
public final class MobParser {

    private MobParser() {}

    public static Optional<MobDefinition> parse(String fileId, ConfigSection root,
                                                Set<String> knownSkillIds, Set<String> knownTraitIds,
                                                ValidationReport report) {
        String rawId = root.getString("id");
        String id = sanitizeId(rawId != null && !rawId.isBlank() ? rawId : fileId);
        if (id == null) {
            report.error("id", "invalid or empty id");
            return Optional.empty();
        }

        String typeName = root.getString("type");
        if (typeName == null || typeName.isBlank()) {
            report.error("type", "missing base entity 'type'");
            return Optional.empty();
        }
        EntityType type = Resolvers.entityType(typeName, null);
        if (type == null) {
            report.error("type", "unknown entity type '" + typeName + "'");
            return Optional.empty();
        }
        Class<? extends Entity> cls = type.getEntityClass();
        if (cls == null || !LivingEntity.class.isAssignableFrom(cls)) {
            report.error("type", "'" + typeName + "' is not a living entity and cannot be a custom mob");
            return Optional.empty();
        }

        MobDefinition.Builder b = MobDefinition.builder(id);
        b.baseType(type);
        b.displayName(root.getString("name"));
        b.lore(root.getStringList("lore"));
        b.tier(Tier.fromString(root.getString("tier"), Tier.COMMON));
        b.category(root.getString("category", "general"));
        b.tags(new LinkedHashSet<>(lower(root.getStringList("tags"))));
        b.stats(parseStats(root.getSection("stats"), report));
        b.equipment(parseEquipment(root.getSection("equipment"), report));
        b.ai(parseAi(root.getSection("ai"), report));
        b.skills(parseSkills(root.getSectionList("skills"), knownSkillIds, report));
        b.phases(parsePhases(root.getSectionList("phases"), report));
        b.drops(parseDrops(root.getSection("drops"), report));
        b.presentation(parsePresentation(root.getSection("presentation"), root.getString("name"), report));
        b.scaling(parseScaling(root.getSection("scaling"), report));
        b.traits(TraitParser.parse(root, knownTraitIds, report));
        b.variants(parseVariants(root.getSectionList("variants"), knownSkillIds, knownTraitIds, report));
        b.variantBaseWeight(Math.max(0, root.getDouble("variant-base-weight", 1.0)));
        b.mount(parseMount(root.getSection("mount"), report));
        b.riders(parseRiders(root.getSectionList("riders"), report));
        return Optional.of(b.build());
    }

    // ---- stats ---------------------------------------------------------------------------------

    private static StatBlock parseStats(ConfigSection s, ValidationReport r) {
        StatBlock.Builder b = StatBlock.builder();
        String p = s.path();
        double health = s.getDouble("health", 20);
        if (health <= 0) { r.warn(p + ".health", "must be > 0 — clamped to 1"); health = 1; }
        b.health(health);
        b.attackDamage(Checks.atLeast(r, p + ".damage", s.getDouble("damage", 3), 0));
        if (s.contains("speed")) b.movementSpeed(Checks.inRange(r, p + ".speed", s.getDouble("speed", 0.25), 0, 5));
        b.knockbackResistance(Checks.pct(r, p + ".knockback-resistance", s.getDouble("knockback-resistance", 0)));
        b.armor(Checks.inRange(r, p + ".armor", s.getDouble("armor", 0), 0, 30));
        b.armorToughness(Checks.inRange(r, p + ".armor-toughness", s.getDouble("armor-toughness", 0), 0, 20));
        if (s.contains("follow-range")) b.followRange(Checks.atLeast(r, p + ".follow-range", s.getDouble("follow-range", 16), 0));
        b.attackKnockback(Checks.atLeast(r, p + ".attack-knockback", s.getDouble("attack-knockback", 0), 0));
        b.scale(Checks.inRange(r, p + ".scale", s.getDouble("scale", 1.0), 0.0625, 16));
        b.regenPerSecond(Checks.atLeast(r, p + ".regen-per-second", s.getDouble("regen-per-second", 0), 0));
        b.critChance(Checks.pct(r, p + ".crit-chance", s.getDouble("crit-chance", 0)));
        b.critMultiplier(Checks.atLeast(r, p + ".crit-multiplier", s.getDouble("crit-multiplier", 1.5), 1.0));
        b.maxAbsorption(Checks.atLeast(r, p + ".max-absorption", s.getDouble("max-absorption", 0), 0));
        b.fireImmune(s.getBool("fire-immune", false));
        b.fallImmune(s.getBool("fall-immune", false));
        b.drownImmune(s.getBool("drown-immune", false));
        b.knockbackImmune(s.getBool("knockback-immune", false));
        ConfigSection mults = s.getSection("damage-multipliers");
        for (String k : mults.keys()) {
            b.damageMultiplier(k, Checks.atLeast(r, mults.path() + "." + k, mults.getDouble(k, 1.0), 0));
        }
        return b.build();
    }

    // ---- equipment -----------------------------------------------------------------------------

    private static Equipment parseEquipment(ConfigSection s, ValidationReport r) {
        if (s.isEmpty()) return Equipment.EMPTY;
        Equipment.Builder b = Equipment.builder();
        slot(s, "main-hand", r, (spec, drop) -> { b.mainHand(spec); b.mainHandDrop(drop); });
        slot(s, "off-hand", r, (spec, drop) -> { b.offHand(spec); b.offHandDrop(drop); });
        slot(s, "helmet", r, (spec, drop) -> { b.helmet(spec); b.helmetDrop(drop); });
        slot(s, "chestplate", r, (spec, drop) -> { b.chestplate(spec); b.chestDrop(drop); });
        slot(s, "leggings", r, (spec, drop) -> { b.leggings(spec); b.legsDrop(drop); });
        slot(s, "boots", r, (spec, drop) -> { b.boots(spec); b.bootsDrop(drop); });
        return b.build();
    }

    private interface SlotSetter { void set(ItemSpec spec, double drop); }

    private static void slot(ConfigSection equip, String key, ValidationReport r, SlotSetter setter) {
        if (!equip.contains(key)) return;
        ConfigSection sec = equip.getSection(key);
        ItemSpec spec = ItemSpecParser.parse(sec, r);
        if (spec == null) return;
        double drop = Checks.pct(r, sec.path() + ".drop-chance", sec.getDouble("drop-chance", 0.085));
        setter.set(spec, drop);
    }

    // ---- ai ------------------------------------------------------------------------------------

    private static AiProfile parseAi(ConfigSection s, ValidationReport r) {
        if (s.isEmpty()) return AiProfile.DEFAULT;
        AiProfile.Builder b = AiProfile.builder();
        b.aggression(s.getEnum("aggression", AiProfile.Aggression.class, AiProfile.Aggression.AGGRESSIVE));
        b.movement(s.getEnum("movement", AiProfile.MovementStyle.class, AiProfile.MovementStyle.CHASE));
        List<AiProfile.TargetMode> prio = new ArrayList<>();
        for (String t : s.getStringList("target-priority")) {
            try { prio.add(AiProfile.TargetMode.valueOf(t.trim().toUpperCase(Locale.ROOT))); }
            catch (IllegalArgumentException ex) { r.warn(s.path() + ".target-priority", "unknown mode '" + t + "' — ignored"); }
        }
        if (!prio.isEmpty()) b.targetPriority(prio);
        b.targetPlayersOnly(s.getBool("target-players-only", true));
        if (s.contains("aggro-range")) b.aggroRange(Checks.atLeast(r, s.path() + ".aggro-range", s.getDouble("aggro-range", 16), 0));
        b.leashRange(Checks.atLeast(r, s.path() + ".leash-range", s.getDouble("leash-range", 0), 0));
        b.retreatHealthPct(Checks.pct(r, s.path() + ".retreat-health-pct", s.getDouble("retreat-health-pct", 0)));
        b.kiteDistance(Checks.atLeast(r, s.path() + ".kite-distance", s.getDouble("kite-distance", 8), 1));
        b.chaseSpeed(Checks.inRange(r, s.path() + ".chase-speed", s.getDouble("chase-speed", 1.15), 0.1, 4.0));
        b.clearVanillaGoals(s.getBool("clear-vanilla-goals", false));
        b.burnsInDay(s.getBool("burns-in-day", false));
        ConfigSection re = s.getSection("reinforcements");
        if (!re.isEmpty()) {
            b.callReinforcements(re.getBool("enabled", false));
            b.reinforcementMobId(re.getString("mob"));
            b.reinforcementCount(re.getIntRange("count", new IntRange(1, 2)));
            b.reinforcementCooldownSec(me.zygotecode.amazingmobs.util.Numbers.ticksToSeconds(
                    me.zygotecode.amazingmobs.util.Numbers.parseTicks(re.getString("cooldown"), 300)));
        }
        return b.build();
    }

    // ---- skills --------------------------------------------------------------------------------

    private static List<SkillDefinition> parseSkills(List<ConfigSection> list, Set<String> known, ValidationReport r) {
        List<SkillDefinition> out = new ArrayList<>();
        for (ConfigSection sec : list) {
            SkillDefinition def = SkillParser.parse(sec, known, r);
            if (def != null) out.add(def);
        }
        return out;
    }

    // ---- phases --------------------------------------------------------------------------------

    private static List<Phase> parsePhases(List<ConfigSection> list, ValidationReport r) {
        List<Phase> out = new ArrayList<>();
        int i = 0;
        for (ConfigSection sec : list) {
            String id = sec.getString("id", "phase" + (i++));
            Phase.Builder b = Phase.builder(id);
            b.thresholdPct(Checks.pct(r, sec.path() + ".threshold", sec.getDouble("threshold", 0.5)));
            b.damageMultiplier(Checks.atLeast(r, sec.path() + ".damage-mult", sec.getDouble("damage-mult", 1.0), 0));
            b.speedMultiplier(Checks.atLeast(r, sec.path() + ".speed-mult", sec.getDouble("speed-mult", 1.0), 0));
            b.defenseMultiplier(Checks.atLeast(r, sec.path() + ".defense-mult", sec.getDouble("defense-mult", 1.0), 0));
            b.enableSkills(lower(sec.getStringList("enable-skills")));
            b.disableSkills(lower(sec.getStringList("disable-skills")));
            b.message(sec.getString("message"));
            b.sound(sec.getString("sound"));
            b.particle(sec.getString("particle"));
            out.add(b.build());
        }
        return out;
    }

    // ---- drops ---------------------------------------------------------------------------------

    private static DropTable parseDrops(ConfigSection s, ValidationReport r) {
        if (s.isEmpty()) return DropTable.EMPTY;
        boolean clear = s.getBool("clear-vanilla", false);
        IntRange xp = s.getIntRange("xp", IntRange.of(0));
        List<DropTable.DropEntry> entries = new ArrayList<>();
        for (ConfigSection item : s.getSectionList("items")) {
            ItemSpec spec = ItemSpecParser.parse(item, r);
            if (spec == null) continue;
            double chance = Checks.pct(r, item.path() + ".chance", item.getDouble("chance", 1.0));
            entries.add(new DropTable.DropEntry(spec, chance));
        }
        return new DropTable(entries, xp, clear);
    }

    // ---- presentation --------------------------------------------------------------------------

    private static Presentation parsePresentation(ConfigSection s, String mobName, ValidationReport r) {
        Presentation.Builder b = Presentation.builder();
        b.glow(s.getBool("glow", false));
        b.glowColor(s.getString("glow-color"));
        b.nameVisible(s.getBool("name-visible", true));
        b.bossBar(s.getBool("boss-bar", false));
        b.bossBarColor(s.getString("boss-bar-color", "RED"));
        b.bossBarTitle(s.getString("boss-bar-title", mobName));
        b.ambientParticle(s.getString("ambient-particle"));
        b.ambientSound(s.getString("ambient-sound"));
        return b.build();
    }

    // ---- scaling -------------------------------------------------------------------------------

    private static ScalingRule parseScaling(ConfigSection s, ValidationReport r) {
        if (s.isEmpty()) return ScalingRule.NONE;
        return new ScalingRule(
                Checks.atLeast(r, s.path() + ".health-per-player", s.getDouble("health-per-player", 0), 0),
                Checks.atLeast(r, s.path() + ".damage-per-player", s.getDouble("damage-per-player", 0), 0),
                Checks.atLeast(r, s.path() + ".speed-per-player", s.getDouble("speed-per-player", 0), 0),
                Math.max(1, s.getInt("max-players", 8)));
    }

    // ---- variants ------------------------------------------------------------------------------

    private static List<Variant> parseVariants(List<ConfigSection> list, Set<String> knownSkills,
                                               Set<String> knownTraits, ValidationReport r) {
        List<Variant> out = new ArrayList<>();
        int i = 0;
        for (ConfigSection v : list) {
            String id = v.getString("id", "variant" + (i++));
            Variant.Builder vb = Variant.builder(id);
            vb.weight(Math.max(0, v.getDouble("weight", 1.0)));
            vb.conditions(parseConditions(v.getSection("conditions")));
            vb.name(v.getString("name"));
            vb.namePrefix(v.getString("name-prefix"));
            vb.healthMul(Checks.atLeast(r, v.path() + ".health-mul", v.getDouble("health-mul", 1.0), 0));
            vb.damageMul(Checks.atLeast(r, v.path() + ".damage-mul", v.getDouble("damage-mul", 1.0), 0));
            vb.speedMul(Checks.atLeast(r, v.path() + ".speed-mul", v.getDouble("speed-mul", 1.0), 0));
            vb.armorMul(Checks.atLeast(r, v.path() + ".armor-mul", v.getDouble("armor-mul", 1.0), 0));
            if (v.contains("tier")) vb.tier(Tier.fromString(v.getString("tier"), null));
            if (v.contains("glow")) vb.glow(v.getBool("glow", false));
            vb.glowColor(v.getString("glow-color"));
            vb.ambientParticle(v.getString("ambient-particle"));
            vb.ambientSound(v.getString("ambient-sound"));
            if (v.contains("tags")) vb.addedTags(new LinkedHashSet<>(lower(v.getStringList("tags"))));
            vb.addedTraits(TraitParser.parse(v, knownTraits, r));
            vb.addedSkills(parseSkills(v.getSectionList("skills"), knownSkills, r));
            vb.extraDrops(parseDropEntries(v.getSectionList("drops"), r));
            out.add(vb.build());
        }
        return out;
    }

    private static SpawnConditions parseConditions(ConfigSection s) {
        if (s.isEmpty()) return SpawnConditions.ANY;
        SpawnConditions.Builder b = SpawnConditions.builder();
        if (s.contains("worlds")) b.worlds(new LinkedHashSet<>(lower(s.getStringList("worlds"))));
        if (s.contains("biomes-allow")) b.biomeAllow(new LinkedHashSet<>(lower(s.getStringList("biomes-allow"))));
        if (s.contains("biomes-deny")) b.biomeDeny(new LinkedHashSet<>(lower(s.getStringList("biomes-deny"))));
        if (s.contains("time-min")) b.timeMin(s.getLong("time-min", -1));
        if (s.contains("time-max")) b.timeMax(s.getLong("time-max", -1));
        if (s.contains("y-min")) b.yMin(s.getInt("y-min", Integer.MIN_VALUE));
        if (s.contains("y-max")) b.yMax(s.getInt("y-max", Integer.MAX_VALUE));
        b.weather(s.getEnum("weather", SpawnConditions.Weather.class, SpawnConditions.Weather.ANY));
        if (s.contains("min-players")) b.minPlayers(s.getInt("min-players", 0));
        return b.build();
    }

    private static List<DropTable.DropEntry> parseDropEntries(List<ConfigSection> items, ValidationReport r) {
        List<DropTable.DropEntry> out = new ArrayList<>();
        for (ConfigSection item : items) {
            ItemSpec spec = ItemSpecParser.parse(item, r);
            if (spec == null) continue;
            out.add(new DropTable.DropEntry(spec, Checks.pct(r, item.path() + ".chance", item.getDouble("chance", 1.0))));
        }
        return out;
    }

    // ---- mount / riders ------------------------------------------------------------------------

    private static MountSpec parseMount(ConfigSection s, ValidationReport r) {
        if (s.isEmpty()) return null;
        String mob = s.getString("mob");
        if (mob == null || mob.isBlank()) { r.warn(s.path() + ".mob", "mount needs a 'mob' id — ignored"); return null; }
        return new MountSpec(mob.trim().toLowerCase(Locale.ROOT),
                RiderDeathBehavior.fromString(s.getString("on-mount-death"), RiderDeathBehavior.DROP),
                s.getStringList("rider-bonus"),
                s.getBool("kill-mount-when-rider-dies", false));
    }

    private static List<RiderSpec> parseRiders(List<ConfigSection> list, ValidationReport r) {
        List<RiderSpec> out = new ArrayList<>();
        for (ConfigSection rs : list) {
            String mob = rs.getString("mob");
            if (mob == null || mob.isBlank()) { r.warn(rs.path() + ".mob", "rider needs a 'mob' id — skipped"); continue; }
            out.add(new RiderSpec(mob.trim().toLowerCase(Locale.ROOT),
                    RiderDeathBehavior.fromString(rs.getString("on-base-death"), RiderDeathBehavior.DROP),
                    rs.getStringList("bonus")));
        }
        return out;
    }

    // ---- helpers -------------------------------------------------------------------------------

    /** Lowercase, keep [a-z0-9_], turn spaces/dashes into underscores. Null if empty. */
    public static String sanitizeId(String raw) {
        if (raw == null) return null;
        StringBuilder sb = new StringBuilder();
        for (char c : raw.trim().toLowerCase(Locale.ROOT).toCharArray()) {
            if (c >= 'a' && c <= 'z' || c >= '0' && c <= '9' || c == '_') sb.append(c);
            else if (c == ' ' || c == '-' || c == '.') sb.append('_');
        }
        String id = sb.toString().replaceAll("_+", "_");
        if (id.startsWith("_")) id = id.substring(1);
        if (id.endsWith("_")) id = id.substring(0, id.length() - 1);
        return id.isEmpty() ? null : id;
    }

    private static List<String> lower(List<String> in) {
        List<String> out = new ArrayList<>(in.size());
        for (String s : in) out.add(s.toLowerCase(Locale.ROOT));
        return out;
    }
}
