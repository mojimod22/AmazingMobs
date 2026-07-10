package me.zygotecode.amazingmobs.clazz;

import org.bukkit.Material;
import org.bukkit.potion.PotionEffectType;

import java.util.List;

/**
 * A player class: identity (name/role/colour/icon), descriptive passive lines, any always-on passive
 * potion effects, and the ids of its active / special / hyper skills. The base skill (AK-47) is shared
 * by everyone, so it isn't stored here.
 */
public final class PlayerClass {

    /** An always-on passive buff granted while this class is equipped. */
    public record Passive(PotionEffectType type, int amplifier) {}

    private final String id, name, role, description, color;
    private final Material icon;
    private final List<String> passiveLines;
    private final List<Passive> passives;
    private final String active, special, hyper;

    public PlayerClass(String id, String name, String role, String description, String color, Material icon,
                       List<String> passiveLines, List<Passive> passives,
                       String active, String special, String hyper) {
        this.id = id;
        this.name = name;
        this.role = role;
        this.description = description;
        this.color = color;
        this.icon = icon;
        this.passiveLines = passiveLines;
        this.passives = passives;
        this.active = active;
        this.special = special;
        this.hyper = hyper;
    }

    public String id() { return id; }
    public String name() { return name; }
    public String role() { return role; }
    public String description() { return description; }
    public String color() { return color; }
    public Material icon() { return icon; }
    public List<String> passiveLines() { return passiveLines; }
    public List<Passive> passives() { return passives; }
    public String active() { return active; }
    public String special() { return special; }
    public String hyper() { return hyper; }

    /** Skill id for a given trigger type (BASE → the shared AK-47). */
    public String skillId(SkillType type) {
        return switch (type) {
            case BASE -> "base_ak47";
            case ACTIVE -> active;
            case SPECIAL -> special;
            case HYPER -> hyper;
        };
    }
}
