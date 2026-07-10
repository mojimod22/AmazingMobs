package me.zygotecode.amazingmobs;

import me.zygotecode.amazingmobs.config.ConfigSection;
import me.zygotecode.amazingmobs.config.validation.Checks;
import me.zygotecode.amazingmobs.config.validation.IssueLevel;
import me.zygotecode.amazingmobs.config.validation.ValidationReport;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The validation core: typed coercion with warnings, sanitisation, and report aggregation. */
class ConfigValidationTest {

    private ConfigSection section() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("health", 200);
        m.put("name", "boss");
        m.put("flag", "true");
        m.put("bad", "notnum");
        m.put("range", "3-8");
        Map<String, Object> sub = new LinkedHashMap<>();
        sub.put("x", 1);
        m.put("sub", sub);
        return ConfigSection.of(m);
    }

    @Test
    void typedGetters() {
        ConfigSection cs = section();
        assertEquals(200, cs.getInt("health", 0));
        assertEquals("boss", cs.getString("name"));
        assertTrue(cs.getBool("flag", false));
        assertEquals(8, cs.getIntRange("range", null).max());
        assertEquals(1, cs.getSection("sub").getInt("x", 0));
    }

    @Test
    void wrongTypeFallsBackAndWarns() {
        ConfigSection cs = section();
        assertEquals(9, cs.getInt("bad", 9));            // un-parseable int -> default
        assertTrue(cs.report().count(IssueLevel.WARN) >= 1); // and a WARN was recorded
    }

    @Test
    void missingKeysReturnDefaultsSilently() {
        ConfigSection cs = section();
        assertEquals(5, cs.getInt("missing", 5));
        assertEquals(0, cs.report().count(IssueLevel.WARN)); // absent != wrong-type
    }

    @Test
    void checksClampAndRecord() {
        ValidationReport r = new ValidationReport("t");
        assertEquals(1.0, Checks.pct(r, "p", 5.0), 1e-9);
        assertEquals(0.0, Checks.pct(r, "p", -1.0), 1e-9);
        assertEquals(3.0, Checks.atLeast(r, "a", 1.0, 3.0), 1e-9);
        assertEquals(3, r.count(IssueLevel.WARN));
        assertEquals(IssueLevel.WARN, r.worst());
        assertFalse(r.hasErrors());
    }

    @Test
    void reportMergePrefixesPaths() {
        ValidationReport child = new ValidationReport("c");
        child.error("health", "must be > 0");
        ValidationReport parent = new ValidationReport("p");
        parent.merge(child, "stats");
        assertTrue(parent.hasErrors());
        assertTrue(parent.issues().get(0).path().startsWith("stats."));
    }
}
