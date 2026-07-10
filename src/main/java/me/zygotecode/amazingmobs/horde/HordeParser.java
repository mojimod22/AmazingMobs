package me.zygotecode.amazingmobs.horde;

import me.zygotecode.amazingmobs.area.AreaSpec;
import me.zygotecode.amazingmobs.area.Shape;
import me.zygotecode.amazingmobs.config.ConfigSection;
import me.zygotecode.amazingmobs.config.validation.Checks;
import me.zygotecode.amazingmobs.config.validation.ValidationReport;
import me.zygotecode.amazingmobs.mob.ItemSpec;
import me.zygotecode.amazingmobs.mob.ItemSpecParser;
import me.zygotecode.amazingmobs.mob.MobParser;
import me.zygotecode.amazingmobs.util.IntRange;
import me.zygotecode.amazingmobs.util.Numbers;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * Builds a {@link HordeDefinition} from a parsed YAML section. Structural parsing is pure; the only
 * runtime touch is reward items (materials). Wave entries referencing unknown mobs are dropped with
 * a {@code WARN}; a horde with no usable waves is rejected with {@code ERROR}.
 */
public final class HordeParser {

    private HordeParser() {}

    public static Optional<HordeDefinition> parse(String fileId, ConfigSection root,
                                                  Set<String> knownMobIds, ValidationReport report) {
        String id = MobParser.sanitizeId(root.getString("id", fileId));
        if (id == null) {
            report.error("id", "invalid or empty id");
            return Optional.empty();
        }

        HordeDefinition.Builder b = HordeDefinition.builder(id);
        b.name(root.getString("name"));
        b.description(root.getString("description"));
        b.difficulty(Checks.atLeast(report, "difficulty", root.getDouble("difficulty", 1.0), 0.1));
        b.durationTicks(Numbers.parseTicks(root.getString("duration"), 0));
        b.cooldownTicks(Numbers.parseTicks(root.getString("cooldown"), 6000));
        b.infinite(root.getBool("infinite", false));
        ConfigSection dir = root.getSection("director");
        if (!dir.isEmpty() && dir.getBool("enabled", false)) {
            b.director(new DirectorSettings(true,
                    Checks.atLeast(report, "director.min-multiplier", dir.getDouble("min-multiplier", 0.6), 0.1),
                    Math.max(0.1, dir.getDouble("max-multiplier", 1.8)),
                    Math.max(0, dir.getDouble("step", 0.15)),
                    Numbers.parseTicks(dir.getString("target-clear-seconds"), 900)));
        }
        b.area(parseArea(root.getSection("area"), report));

        ConfigSection spawn = root.getSection("spawn");
        b.maxSpawnsPerTick(Math.max(1, spawn.getInt("max-per-tick", 8)));
        b.maxConcurrentMobs(Math.max(1, spawn.getInt("max-mobs", 80)));
        b.minPlayerDistance(Checks.atLeast(report, "spawn.min-player-distance", spawn.getDouble("min-player-distance", 8), 0));
        b.spawnAttempts(Math.max(4, spawn.getInt("attempts", 24)));

        ConfigSection act = root.getSection("activation");
        b.worlds(lowerSet(act.getStringList("worlds")));
        b.biomeAllow(lowerSet(act.getStringList("biomes-allow")));
        b.biomeDeny(lowerSet(act.getStringList("biomes-deny")));
        b.timeMin(act.contains("time-min") ? act.getLong("time-min", -1) : -1);
        b.timeMax(act.contains("time-max") ? act.getLong("time-max", -1) : -1);
        b.minPlayers(Math.max(0, act.getInt("min-players", 1)));
        b.maxPlayers(Math.max(1, act.getInt("max-players", 8)));

        List<Wave> waves = parseWaves(root.getSectionList("waves"), knownMobIds, report);
        if (waves.isEmpty()) {
            report.error("waves", "horde has no usable waves (none defined, or all referenced unknown mobs)");
            return Optional.empty();
        }
        b.waves(waves);
        b.rewards(parseRewards(root.getSection("rewards"), report));

        b.startMessage(root.getString("start-message"));
        b.startTitle(root.getString("start-title"));
        b.startSubtitle(root.getString("start-subtitle"));
        b.endMessage(root.getString("end-message"));
        b.failMessage(root.getString("fail-message"));
        b.sound(root.getString("sound"));
        return Optional.of(b.build());
    }

    private static AreaSpec parseArea(ConfigSection s, ValidationReport r) {
        Shape shape = Shape.fromString(s.getString("shape"), Shape.CYLINDER);
        double radius = Checks.inRange(r, "area.radius", s.getDouble("radius", 64), 4, 1024);
        double height = Checks.inRange(r, "area.height", s.getDouble("height", 32), 4, 512);
        boolean dynamic = s.getBool("dynamic", true);
        String world = s.getString("world");
        if (!dynamic && (world == null || world.isBlank())) {
            r.warn("area.world", "fixed area needs a 'world' — falling back to dynamic (around players)");
            dynamic = true;
        }
        return new AreaSpec(shape, radius, height, dynamic, world,
                s.getDouble("x", 0), s.getDouble("y", 64), s.getDouble("z", 0));
    }

    private static List<Wave> parseWaves(List<ConfigSection> list, Set<String> knownMobs, ValidationReport r) {
        List<Wave> waves = new ArrayList<>();
        int idx = 0;
        for (ConfigSection ws : list) {
            Wave.Builder wb = Wave.builder(idx);
            wb.label(ws.getString("label"));
            wb.startDelayTicks(Numbers.parseTicks(ws.getString("start-delay"), 40));
            wb.durationTicks(Numbers.parseTicks(ws.getString("duration"), 0));
            wb.clearThreshold(Checks.pct(r, ws.path() + ".clear-threshold", ws.getDouble("clear-threshold", 1.0)));
            wb.message(ws.getString("message"));
            wb.title(ws.getString("title"));
            wb.subtitle(ws.getString("subtitle"));
            wb.sound(ws.getString("sound"));

            List<WaveEntry> entries = new ArrayList<>();
            for (ConfigSection me : ws.getSectionList("mobs")) {
                String mob = me.getString("mob");
                if (mob == null || mob.isBlank()) { r.warn(me.path() + ".mob", "missing mob id — skipped"); continue; }
                mob = mob.trim().toLowerCase(Locale.ROOT);
                if (!knownMobs.isEmpty() && !knownMobs.contains(mob)) {
                    r.warn(me.path() + ".mob", "unknown mob '" + mob + "' — entry skipped");
                    continue;
                }
                IntRange count = me.getIntRange("count", IntRange.of(1));
                double perPlayer = Checks.atLeast(r, me.path() + ".per-player", me.getDouble("per-player", 0), 0);
                double chance = Checks.pct(r, me.path() + ".chance", me.getDouble("chance", 1.0));
                boolean boss = me.getBool("boss", false);
                boolean objective = me.getBool("objective", false);
                String role = me.getString("role", objective ? "objective" : (boss ? "boss" : "minion"));
                entries.add(new WaveEntry(mob, count, perPlayer, chance, role, boss, objective));
            }
            if (entries.isEmpty()) {
                r.warn(ws.path(), "wave " + (idx + 1) + " has no usable mob entries — skipped");
            } else {
                wb.entries(entries);
                waves.add(wb.build());
            }
            idx++;
        }
        return waves;
    }

    private static HordeRewards parseRewards(ConfigSection s, ValidationReport r) {
        if (s.isEmpty()) return HordeRewards.NONE;
        List<ItemSpec> items = new ArrayList<>();
        for (ConfigSection item : s.getSectionList("items")) {
            ItemSpec spec = ItemSpecParser.parse(item, r);
            if (spec != null) items.add(spec);
        }
        IntRange xp = s.getIntRange("xp", IntRange.of(0));
        List<String> commands = s.getStringList("commands");
        return new HordeRewards(items, xp, commands, s.getString("message"));
    }

    private static Set<String> lowerSet(List<String> in) {
        Set<String> out = new LinkedHashSet<>();
        for (String s : in) out.add(s.toLowerCase(Locale.ROOT));
        return out;
    }
}
