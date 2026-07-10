package me.zygotecode.amazingmobs.verify;

import me.zygotecode.amazingmobs.config.ConfigSection;
import me.zygotecode.amazingmobs.config.validation.Checks;
import me.zygotecode.amazingmobs.config.validation.IssueLevel;
import me.zygotecode.amazingmobs.config.validation.ValidationReport;
import me.zygotecode.amazingmobs.command.MobEditSession;
import me.zygotecode.amazingmobs.area.Shape;
import me.zygotecode.amazingmobs.scaling.Scaling;
import me.zygotecode.amazingmobs.scaling.ScalingRule;
import me.zygotecode.amazingmobs.skill.SkillRegistry;
import me.zygotecode.amazingmobs.trait.TraitRegistry;
import me.zygotecode.amazingmobs.mob.StatBlock;
import me.zygotecode.amazingmobs.util.DoubleRange;
import me.zygotecode.amazingmobs.util.IntRange;
import me.zygotecode.amazingmobs.util.Numbers;
import me.zygotecode.amazingmobs.util.Rng;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * No-dependency in-environment test harness (no JUnit, no running server). Verifies the pure logic
 * AND that every bundled example mob/horde parses and cross-references resolve. SnakeYAML is used
 * directly for file loading so nothing touches the Bukkit runtime. Mirrors the canonical JUnit suite.
 */
public final class VerifyMain {

    private static int pass = 0;
    private static int fail = 0;

    public static void main(String[] args) throws Exception {
        String res = args.length > 0 ? args[0] : "src/main/resources";
        System.out.println("== AmazingMobs verifier ==");

        pureLogic();
        Set<String> mobIds = exampleMobs(new File(res, "mobs"));
        exampleHordes(new File(res, "hordes"), mobIds);

        System.out.println("--------------------------------------");
        System.out.println("PASS " + pass + "   FAIL " + fail);
        if (fail > 0) System.exit(1);
    }

    // ---- pure logic ----------------------------------------------------------------------------

    private static void pureLogic() {
        section("pure logic");

        // ranges
        eq("IntRange 3-8 min", 3, IntRange.parse("3-8", null).min());
        eq("IntRange 3-8 max", 8, IntRange.parse("3-8", null).max());
        eq("IntRange single", 5, IntRange.parse("5", null).min());
        check("IntRange invalid -> fallback", IntRange.parse("xx", IntRange.of(2)).min() == 2);
        check("DoubleRange 0.5-2.0", DoubleRange.parse("0.5-2.0", null).max() == 2.0);

        // durations
        eq("parseTicks 30s", 600L, Numbers.parseTicks("30s", -1));
        eq("parseTicks 2m", 2400L, Numbers.parseTicks("2m", -1));
        eq("parseTicks 100t", 100L, Numbers.parseTicks("100t", -1));
        eq("parseTicks bare seconds", 100L, Numbers.parseTicks("5", -1));
        eq("parseTicks invalid", 42L, Numbers.parseTicks("nope", 42));
        eq("clamp", 5, Numbers.clamp(9, 0, 5));

        // rng determinism / bounds
        Rng rng = Rng.seeded(123);
        check("chance(0) false", !rng.chance(0));
        check("chance(1) true", rng.chance(1));
        boolean inBounds = true;
        for (int i = 0; i < 1000; i++) { int v = rng.rangeInt(3, 7); if (v < 3 || v > 7) inBounds = false; }
        check("rangeInt within bounds", inBounds);

        // area geometry
        check("sphere contains center", Shape.SPHERE.contains(0, 0, 0, 10, 20));
        check("sphere excludes far", !Shape.SPHERE.contains(11, 0, 0, 10, 20));
        check("cube edge", Shape.CUBE.contains(9.9, 0, -9.9, 10, 8));
        boolean sampleOk = true;
        for (int i = 0; i < 2000; i++) {
            double[] p = Shape.SPHERE.randomLocal(rng, 16, 32);
            if (!Shape.SPHERE.contains(p[0], p[1], p[2], 16.0001, 32)) sampleOk = false;
        }
        check("sphere samples stay inside", sampleOk);

        // scaling
        StatBlock base = StatBlock.builder().health(100).attackDamage(10).build();
        check("ScalingRule NONE identity", ScalingRule.NONE.apply(base, 8, 1.0) == base);
        StatBlock scaled = new ScalingRule(0.1, 0, 0, 8).apply(base, 5, 1.0);
        check("ScalingRule scales health", scaled.health() > 100 && scaled.health() < 200);
        eq("Scaling.countForPlayers", 7, Scaling.countForPlayers(5, 0.5, 5, 8));

        // config section coercion + warnings
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("health", 200);
        m.put("name", "boss");
        m.put("flag", "true");
        m.put("bad", "notnum");
        m.put("range", "3-8");
        Map<String, Object> nested = new LinkedHashMap<>();
        nested.put("x", 1);
        m.put("sub", nested);
        ConfigSection cs = ConfigSection.of(m);
        eq("getInt", 200, cs.getInt("health", 0));
        eq("getString", "boss", cs.getString("name"));
        check("getBool from string", cs.getBool("flag", false));
        eq("getInt wrong-type default", 9, cs.getInt("bad", 9));
        check("wrong-type records WARN", cs.report().count(IssueLevel.WARN) >= 1);
        check("getSection nested", cs.getSection("sub").getInt("x", 0) == 1);
        eq("getIntRange", 8, cs.getIntRange("range", IntRange.of(1)).max());

        // checks
        ValidationReport rep = new ValidationReport("t");
        check("Checks.pct clamps high", Checks.pct(rep, "p", 5.0) == 1.0);
        check("Checks.pct clamps low", Checks.pct(rep, "p", -1.0) == 0.0);
        check("Checks recorded warns", rep.count(IssueLevel.WARN) == 2);
        check("report worst", rep.worst() == IssueLevel.WARN);

        // session coercion
        check("coerce bool", MobEditSession.coerce("true").equals(Boolean.TRUE));
        check("coerce int", MobEditSession.coerce("5").equals(5));
        check("coerce double", MobEditSession.coerce("1.5").equals(1.5));
        check("coerce range stays string", MobEditSession.coerce("3-8") instanceof String);
        MobEditSession sess = MobEditSession.fresh("test", "zombie");
        sess.set("stats.health", "250");
        check("session nested set", "250".equals(String.valueOf(deep(sess.tree(), "stats", "health")))
                || Integer.valueOf(250).equals(deep(sess.tree(), "stats", "health")));
    }

    // ---- example content -----------------------------------------------------------------------

    private static Set<String> exampleMobs(File dir) {
        section("example mobs (" + dir + ")");
        Set<String> ids = new HashSet<>();
        Set<String> skills = new SkillRegistryProbe().ids();
        Set<String> traitsKnown = new TraitRegistryProbe().ids();
        File[] files = dir.listFiles((d, n) -> n.endsWith(".yml"));
        check("mobs folder present", files != null);
        if (files == null) return ids;
        int count = 0;
        for (File f : files) {
            Map<String, Object> map = load(f);
            if (map == null) { fail("YAML parse: " + f.getName()); continue; }
            ConfigSection cs = ConfigSection.of(map);
            String id = cs.getString("id");
            if (id == null) id = f.getName().replace(".yml", "");
            ids.add(id.toLowerCase());
            check(f.getName() + ": has type", cs.getString("type") != null);
            checkSkillList(f.getName(), cs, skills);
            checkTraitList(f.getName(), cs, traitsKnown);
            for (ConfigSection vr : cs.getSectionList("variants")) {
                checkSkillList(f.getName() + " variant", vr, skills);
                checkTraitList(f.getName() + " variant", vr, traitsKnown);
            }
            count++;
        }
        check("at least 20 example mobs (found " + count + ")", count >= 20);
        return ids;
    }

    private static void exampleHordes(File dir, Set<String> mobIds) {
        section("example hordes (" + dir + ")");
        File[] files = dir.listFiles((d, n) -> n.endsWith(".yml"));
        check("hordes folder present", files != null);
        if (files == null) return;
        int count = 0;
        for (File f : files) {
            Map<String, Object> map = load(f);
            if (map == null) { fail("YAML parse: " + f.getName()); continue; }
            ConfigSection cs = ConfigSection.of(map);
            List<ConfigSection> waves = cs.getSectionList("waves");
            check(f.getName() + ": has waves", !waves.isEmpty());
            for (ConfigSection w : waves) {
                for (ConfigSection me : w.getSectionList("mobs")) {
                    String mob = me.getString("mob");
                    check(f.getName() + ": wave mob '" + mob + "' exists",
                            mob != null && mobIds.contains(mob.toLowerCase()));
                }
            }
            count++;
        }
        check("at least 30 example hordes (found " + count + ")", count >= 30);
    }

    // ---- helpers -------------------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private static Map<String, Object> load(File f) {
        try {
            Object o = new Yaml().load(Files.readString(f.toPath()));
            return o instanceof Map ? (Map<String, Object>) o : new LinkedHashMap<>();
        } catch (Exception e) {
            System.out.println("  parse error in " + f.getName() + ": " + e.getMessage());
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private static Object deep(Map<String, Object> m, String a, String b) {
        Object sub = m.get(a);
        return sub instanceof Map ? ((Map<String, Object>) sub).get(b) : null;
    }

    private static void checkSkillList(String file, ConfigSection sec, Set<String> known) {
        for (ConfigSection sk : sec.getSectionList("skills")) {
            String sid = sk.getString("skill");
            check(file + ": skill '" + sid + "' is known", sid != null && known.contains(sid.toLowerCase()));
        }
    }

    private static void checkTraitList(String file, ConfigSection sec, Set<String> known) {
        List<ConfigSection> maps = sec.getSectionList("traits");
        if (!maps.isEmpty()) {
            for (ConfigSection t : maps) {
                String id = t.getString("trait");
                check(file + ": trait '" + id + "' is known", id != null && known.contains(id.toLowerCase()));
            }
        } else {
            for (String id : sec.getStringList("traits")) {
                check(file + ": trait '" + id + "' is known", known.contains(id.toLowerCase()));
            }
        }
    }

    private static void section(String name) {
        System.out.println("-- " + name);
    }

    private static void check(String name, boolean cond) {
        if (cond) { pass++; }
        else { fail++; System.out.println("  [FAIL] " + name); }
    }

    private static void eq(String name, Object expected, Object actual) {
        check(name + " (exp " + expected + ", got " + actual + ")", expected.equals(actual));
    }

    private static void fail(String name) {
        fail++;
        System.out.println("  [FAIL] " + name);
    }

    /** Tiny shim so the harness can list skill ids without importing impl classes directly. */
    private static final class SkillRegistryProbe {
        Set<String> ids() {
            SkillRegistry r = new SkillRegistry();
            r.registerDefaults();
            return lower(r.ids());
        }
    }

    private static final class TraitRegistryProbe {
        Set<String> ids() {
            TraitRegistry r = new TraitRegistry();
            r.registerDefaults();
            return lower(r.ids());
        }
    }

    private static Set<String> lower(Set<String> in) {
        Set<String> out = new HashSet<>();
        for (String s : in) out.add(s.toLowerCase());
        return out;
    }
}
