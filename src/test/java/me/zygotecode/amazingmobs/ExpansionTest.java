package me.zygotecode.amazingmobs;

import me.zygotecode.amazingmobs.config.ConfigSection;
import me.zygotecode.amazingmobs.config.validation.ValidationReport;
import me.zygotecode.amazingmobs.mob.MobDefinition;
import me.zygotecode.amazingmobs.mob.RiderDeathBehavior;
import me.zygotecode.amazingmobs.mob.StatBlock;
import me.zygotecode.amazingmobs.mob.Variant;
import me.zygotecode.amazingmobs.trait.TraitDefinition;
import me.zygotecode.amazingmobs.trait.TraitParser;
import me.zygotecode.amazingmobs.util.Rng;
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

/** Phase-2 pure logic: trait parsing (both forms), variant overlay + weighted roll, enums. */
class ExpansionTest {

    private static final Set<String> KNOWN = Set.of("berserker", "thorns", "exploder");

    @Test
    void traitParseIdListForm() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("traits", List.of("berserker", "thorns", "wobble")); // last is unknown
        ConfigSection cs = ConfigSection.of(m);
        List<TraitDefinition> traits = TraitParser.parse(cs, KNOWN, cs.report());
        assertEquals(2, traits.size());                  // unknown dropped
        assertEquals("berserker", traits.get(0).id());
    }

    @Test
    void traitParseMapForm() {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("trait", "berserker");
        entry.put("threshold", 0.4);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("traits", List.of(entry));
        ConfigSection cs = ConfigSection.of(m);
        List<TraitDefinition> traits = TraitParser.parse(cs, KNOWN, cs.report());
        assertEquals(1, traits.size());
        assertEquals(0.4, traits.get(0).params().getDouble("threshold", 0), 1e-9);
    }

    @Test
    void traitParseAbsent() {
        assertTrue(TraitParser.parse(ConfigSection.of(new LinkedHashMap<>()), KNOWN, new ValidationReport("t")).isEmpty());
    }

    @Test
    void variantApplyOverlaysStatsNameAndTraits() {
        MobDefinition base = MobDefinition.builder("x")
                .displayName("<red>Base")
                .stats(StatBlock.builder().health(100).attackDamage(10).armor(4).build())
                .build();
        Variant v = Variant.builder("fiery")
                .namePrefix("<gold>Fiery")
                .healthMul(1.5).damageMul(2.0).armorMul(0.5)
                .addedTraits(List.of(new TraitDefinition("berserker", ConfigSection.of(new LinkedHashMap<>()))))
                .build();
        MobDefinition out = v.apply(base);
        assertEquals(150, out.stats().health(), 1e-9);
        assertEquals(20, out.stats().attackDamage(), 1e-9);
        assertEquals(2, out.stats().armor(), 1e-9);
        assertTrue(out.displayName().contains("Fiery"));
        assertEquals(1, out.traits().size());
        // base must be untouched (immutability)
        assertEquals(100, base.stats().health(), 1e-9);
        assertTrue(base.traits().isEmpty());
    }

    @Test
    void variantPickRespectsWeightAndBase() {
        Variant active = Variant.builder("a").weight(1.0).build();
        Variant zero = Variant.builder("z").weight(0.0).build();
        // baseWeight 0 + one eligible variant => always that variant
        Variant picked = Variant.pick(List.of(active), 0.0, null, 1, Rng.seeded(1));
        assertNotNull(picked);
        assertEquals("a", picked.id());
        // a zero-weight variant is never eligible => null (spawn plain base)
        assertNull(Variant.pick(List.of(zero), 0.0, null, 1, Rng.seeded(1)));
        // empty list => null
        assertNull(Variant.pick(List.of(), 0.0, null, 1, Rng.seeded(1)));
    }

    @Test
    void riderDeathBehaviorParsing() {
        assertEquals(RiderDeathBehavior.ENRAGE, RiderDeathBehavior.fromString("enrage", RiderDeathBehavior.DROP));
        assertEquals(RiderDeathBehavior.DROP, RiderDeathBehavior.fromString("nonsense", RiderDeathBehavior.DROP));
        assertFalse(RiderDeathBehavior.KILL == RiderDeathBehavior.fromString(null, RiderDeathBehavior.KEEP));
    }
}
