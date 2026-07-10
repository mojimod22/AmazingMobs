package me.zygotecode.amazingmobs;

import me.zygotecode.amazingmobs.area.Shape;
import me.zygotecode.amazingmobs.mob.StatBlock;
import me.zygotecode.amazingmobs.scaling.Scaling;
import me.zygotecode.amazingmobs.scaling.ScalingRule;
import me.zygotecode.amazingmobs.util.DoubleRange;
import me.zygotecode.amazingmobs.util.IntRange;
import me.zygotecode.amazingmobs.util.Numbers;
import me.zygotecode.amazingmobs.util.Rng;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Pure (no-Bukkit) logic: ranges, durations, RNG, area geometry, scaling. */
class PureLogicTest {

    @Test
    void intRangeParsing() {
        assertEquals(new IntRange(3, 8), IntRange.parse("3-8", null));
        assertEquals(IntRange.of(5), IntRange.parse("5", null));
        assertEquals(IntRange.of(2), IntRange.parse("garbage", IntRange.of(2)));
        // reversed bounds normalise
        assertEquals(new IntRange(1, 9), new IntRange(9, 1));
    }

    @Test
    void doubleRangeParsing() {
        assertEquals(2.0, DoubleRange.parse("0.5-2.0", null).max(), 1e-9);
        assertEquals(1.5, DoubleRange.parse("1.5", null).min(), 1e-9);
    }

    @Test
    void durationParsing() {
        assertEquals(600L, Numbers.parseTicks("30s", -1));
        assertEquals(2400L, Numbers.parseTicks("2m", -1));
        assertEquals(100L, Numbers.parseTicks("100t", -1));
        assertEquals(100L, Numbers.parseTicks("5", -1));   // bare = seconds
        assertEquals(72000L, Numbers.parseTicks("1h", -1));
        assertEquals(42L, Numbers.parseTicks("???", 42));
    }

    @Test
    void clamp() {
        assertEquals(5, Numbers.clamp(9, 0, 5));
        assertEquals(0, Numbers.clamp(-3, 0, 5));
        assertEquals(3, Numbers.clamp(3, 0, 5));
    }

    @Test
    void rngBoundsAndDeterminism() {
        Rng rng = Rng.seeded(42);
        assertFalse(rng.chance(0.0));
        assertTrue(rng.chance(1.0));
        for (int i = 0; i < 5000; i++) {
            int v = rng.rangeInt(3, 7);
            assertTrue(v >= 3 && v <= 7);
        }
        // same seed -> same sequence
        assertEquals(Rng.seeded(1).rangeInt(0, 1_000_000), Rng.seeded(1).rangeInt(0, 1_000_000));
    }

    @Test
    void shapeContainment() {
        assertTrue(Shape.SPHERE.contains(0, 0, 0, 10, 20));
        assertFalse(Shape.SPHERE.contains(11, 0, 0, 10, 20));
        assertTrue(Shape.CUBE.contains(9.9, 0, -9.9, 10, 8));
        assertFalse(Shape.CYLINDER.contains(0, 9, 0, 10, 8)); // outside half-height
    }

    @Test
    void shapeSamplesStayInside() {
        Rng rng = Rng.seeded(7);
        for (int i = 0; i < 5000; i++) {
            double[] p = Shape.SPHERE.randomLocal(rng, 16, 32);
            assertTrue(Shape.SPHERE.contains(p[0], p[1], p[2], 16.001, 32));
        }
        for (int i = 0; i < 5000; i++) {
            double[] p = Shape.CYLINDER.randomLocal(rng, 12, 10);
            assertTrue(Shape.CYLINDER.contains(p[0], p[1], p[2], 12.001, 10.001));
        }
    }

    @Test
    void scaling() {
        StatBlock base = StatBlock.builder().health(100).attackDamage(10).build();
        assertSame(base, ScalingRule.NONE.apply(base, 8, 1.0)); // identity returns same object
        StatBlock scaled = new ScalingRule(0.1, 0, 0, 8).apply(base, 5, 1.0);
        assertEquals(140.0, scaled.health(), 1e-9); // 100 * (1 + 0.1*4)
        assertEquals(7, Scaling.countForPlayers(5, 0.5, 5, 8));
        assertEquals(5, Scaling.countForPlayers(5, 0.5, 1, 8)); // single player: base only
    }
}
