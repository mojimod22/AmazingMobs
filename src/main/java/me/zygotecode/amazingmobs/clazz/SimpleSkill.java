package me.zygotecode.amazingmobs.clazz;

import org.bukkit.Material;

import java.util.List;
import java.util.function.Function;

/**
 * Concrete {@link PlayerSkill} built from a name/type/icon/cooldown + a cast lambda and a prestige-aware
 * description function. Lets the whole roster be declared compactly in {@link Skills} without a class
 * per skill.
 */
public final class SimpleSkill implements PlayerSkill {

    @FunctionalInterface public interface Cast { void cast(SkillContext ctx); }

    private final String id, name;
    private final SkillType type;
    private final Material icon;
    private final int cooldown;
    private final Function<Integer, List<String>> describe;
    private final Cast fn;

    public SimpleSkill(String id, String name, SkillType type, Material icon, int cooldown,
                       Function<Integer, List<String>> describe, Cast fn) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.icon = icon;
        this.cooldown = cooldown;
        this.describe = describe;
        this.fn = fn;
    }

    @Override public String id() { return id; }
    @Override public String name() { return name; }
    @Override public SkillType type() { return type; }
    @Override public Material icon() { return icon; }
    @Override public int baseCooldownSeconds() { return cooldown; }
    @Override public List<String> description(int prestige) { return describe.apply(prestige); }
    @Override public void cast(SkillContext ctx) { fn.cast(ctx); }
}
