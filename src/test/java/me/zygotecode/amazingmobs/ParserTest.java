package me.zygotecode.amazingmobs;

import me.zygotecode.amazingmobs.command.MobEditSession;
import me.zygotecode.amazingmobs.config.ConfigSection;
import me.zygotecode.amazingmobs.config.validation.ValidationReport;
import me.zygotecode.amazingmobs.mob.MobParser;
import me.zygotecode.amazingmobs.skill.SkillDefinition;
import me.zygotecode.amazingmobs.skill.SkillParser;
import me.zygotecode.amazingmobs.skill.TargetRule;
import me.zygotecode.amazingmobs.skill.TriggerSpec;
import me.zygotecode.amazingmobs.skill.TriggerType;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Pure parser logic: trigger parsing, skill validation, id sanitisation, session coercion. */
class ParserTest {

    @Test
    void parseTriggerReadsAllFields() {
        Map<String, Object> t = new LinkedHashMap<>();
        t.put("types", List.of("TICK", "ON_DAMAGED"));
        t.put("cooldown", "6s");
        t.put("warmup", "1s");
        t.put("chance", 0.8);
        t.put("max-range", 30);
        t.put("radius", 4);
        t.put("target", "NEAREST_PLAYER");
        ConfigSection cs = ConfigSection.of(t);
        TriggerSpec spec = SkillParser.parseTrigger(cs, cs.report());
        assertEquals(120L, spec.cooldownTicks());
        assertEquals(20L, spec.warmupTicks());
        assertEquals(0.8, spec.chance(), 1e-9);
        assertEquals(30.0, spec.maxRange(), 1e-9);
        assertEquals(TargetRule.NEAREST_PLAYER, spec.targetRule());
        assertTrue(spec.hasTrigger(TriggerType.TICK));
        assertTrue(spec.hasTrigger(TriggerType.ON_DAMAGED));
    }

    @Test
    void parseSkillKnownVsUnknown() {
        Set<String> known = Set.of("fireball", "dash");
        ValidationReport ok = new ValidationReport("s");
        Map<String, Object> good = new LinkedHashMap<>();
        good.put("skill", "fireball");
        SkillDefinition def = SkillParser.parse(wrap(good, ok), known, ok);
        assertNotNull(def);
        assertEquals("fireball", def.skillId());
        assertFalse(ok.hasErrors());

        ValidationReport bad = new ValidationReport("s");
        Map<String, Object> unknown = new LinkedHashMap<>();
        unknown.put("skill", "wobble");
        assertNull(SkillParser.parse(wrap(unknown, bad), known, bad));
        assertTrue(bad.hasErrors());
    }

    @Test
    void conditionsMetRespectsHealthPhaseRange() {
        TriggerSpec spec = TriggerSpec.builder()
                .minHealthPct(0.0).maxHealthPct(0.5)
                .phases(Set.of("enraged"))
                .minRange(4).maxRange(20)
                .build();
        assertTrue(spec.conditionsMet(0.4, "enraged", 10));
        assertFalse(spec.conditionsMet(0.8, "enraged", 10)); // health too high
        assertFalse(spec.conditionsMet(0.4, "calm", 10));    // wrong phase
        assertFalse(spec.conditionsMet(0.4, "enraged", 2));  // too close
        assertFalse(spec.conditionsMet(0.4, "enraged", 40)); // too far
    }

    @Test
    void sanitizeId() {
        assertEquals("dread_overlord", MobParser.sanitizeId("Dread Overlord"));
        assertEquals("a_b", MobParser.sanitizeId("a--b"));
        assertEquals("boss1", MobParser.sanitizeId("Boss#1"));
        assertNull(MobParser.sanitizeId("???"));
    }

    @Test
    void sessionCoercion() {
        assertEquals(Boolean.TRUE, MobEditSession.coerce("true"));
        assertEquals(5, MobEditSession.coerce("5"));
        assertEquals(1.5, MobEditSession.coerce("1.5"));
        assertTrue(MobEditSession.coerce("3-8") instanceof String);    // ranges stay strings
        assertTrue(MobEditSession.coerce("<red>Boss") instanceof String);
        MobEditSession s = MobEditSession.fresh("x", "zombie");
        s.set("stats.health", "250");
        assertEquals(250, deep(s));
    }

    @SuppressWarnings("unchecked")
    private static Object deep(MobEditSession s) {
        Object stats = s.tree().get("stats");
        return stats instanceof Map ? ((Map<String, Object>) stats).get("health") : null;
    }

    private static ConfigSection wrap(Map<String, Object> m, ValidationReport r) {
        return new ConfigSection(m, "", r);
    }
}
