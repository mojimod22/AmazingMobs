package me.zygotecode.amazingmobs.clazz;

import org.bukkit.Material;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Holds the player-skill roster and the class definitions. Built once at enable. Adding a class or
 * skill means editing only this file (+ {@link Skills}) — nothing in the runtime needs to change.
 */
public final class ClassRegistry {

    private final Map<String, PlayerSkill> skills = new LinkedHashMap<>();
    private final Map<String, PlayerClass> classes = new LinkedHashMap<>();

    public void registerDefaults() {
        for (PlayerSkill s : Skills.all()) skills.put(s.id(), s);

        add(new PlayerClass("necromancer", "Necromancer", "Summoner / Attrition",
                "Turn the horde against itself — your kills rise as a loyal, growing army.",
                "<dark_purple>", Material.WITHER_SKELETON_SKULL,
                List.of("<dark_purple>Soul Harvest <gray>— slain foes may rise to fight for you",
                        "<dark_purple>Dominion <gray>— minion cap grows with prestige"),
                List.of(),
                "necro_soulbolt", "necro_raisedead", "necro_army"));

        add(new PlayerClass("stormcaller", "Stormcaller", "Ranged Control",
                "Command the sky. Arc lightning through packs and smite from afar.",
                "<aqua>", Material.LIGHTNING_ROD,
                List.of("<aqua>Conductor <gray>— your bolts chain between enemies"),
                List.of(),
                "storm_chain", "storm_thunderstrike", "storm_tempest"));

        add(new PlayerClass("pyromancer", "Pyromancer", "Area Damage",
                "Burn it all down. Blasts, meteors and an inferno that follows you.",
                "<red>", Material.BLAZE_POWDER,
                List.of("<red>Cinderskin <gray>— permanently immune to fire"),
                List.of(new PlayerClass.Passive(PotionEffectType.FIRE_RESISTANCE, 0)),
                "pyro_cinderblast", "pyro_meteor", "pyro_inferno"));

        add(new PlayerClass("vanguard", "Vanguard", "Tank / Frontline",
                "Hold the line. Soak hits, taunt foes, and become briefly unbreakable.",
                "<gold>", Material.NETHERITE_CHESTPLATE,
                List.of("<gold>Ironclad <gray>— +damage resistance and +4 hearts"),
                List.of(new PlayerClass.Passive(PotionEffectType.RESISTANCE, 0),
                        new PlayerClass.Passive(PotionEffectType.HEALTH_BOOST, 1)),
                "van_bash", "van_bulwark", "van_unbreakable"));

        add(new PlayerClass("assassin", "Assassin", "Mobility / Burst",
                "Strike from the shadows, blink through the fray, mark them all for death.",
                "<dark_gray>", Material.NETHERITE_SWORD,
                List.of("<dark_gray>Fleet <gray>— permanently faster on your feet"),
                List.of(new PlayerClass.Passive(PotionEffectType.SPEED, 0)),
                "assa_dash", "assa_shadowstrike", "assa_deathmark"));

        add(new PlayerClass("guardian", "Guardian", "Support / Sustain",
                "Keep the squad alive. Heal, rally, and raise a sanctuary against the tide.",
                "<yellow>", Material.BEACON,
                List.of("<yellow>Lightbearer <gray>— slow self-regen; your buffs lift the whole team"),
                List.of(new PlayerClass.Passive(PotionEffectType.REGENERATION, 0)),
                "guard_heal", "guard_rally", "guard_sanctuary"));
    }

    private void add(PlayerClass c) { classes.put(c.id(), c); }

    public PlayerSkill skill(String id) { return id == null ? null : skills.get(id); }
    public PlayerClass classDef(String id) { return id == null ? null : classes.get(id.toLowerCase()); }
    public List<PlayerClass> classes() { return new ArrayList<>(classes.values()); }
    public int size() { return classes.size(); }

    /** Every passive effect type any class uses — cleared in bulk when a player switches class. */
    public Set<PotionEffectType> allPassiveTypes() {
        Set<PotionEffectType> set = new java.util.HashSet<>();
        for (PlayerClass c : classes.values()) for (PlayerClass.Passive p : c.passives()) set.add(p.type());
        return set;
    }
}
