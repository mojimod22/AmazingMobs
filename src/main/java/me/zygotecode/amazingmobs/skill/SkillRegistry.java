package me.zygotecode.amazingmobs.skill;

import me.zygotecode.amazingmobs.skill.impl.AreaSlamSkill;
import me.zygotecode.amazingmobs.skill.impl.DashSkill;
import me.zygotecode.amazingmobs.skill.impl.EffectSkill;
import me.zygotecode.amazingmobs.skill.impl.FlightSkill;
import me.zygotecode.amazingmobs.skill.impl.HealSkill;
import me.zygotecode.amazingmobs.skill.impl.JumpAttackSkill;
import me.zygotecode.amazingmobs.skill.impl.ProjectileSkill;
import me.zygotecode.amazingmobs.skill.impl.RepelSkill;
import me.zygotecode.amazingmobs.skill.impl.SummonSkill;
import me.zygotecode.amazingmobs.skill.impl.TeleportSkill;
import me.zygotecode.amazingmobs.skill.impl.ThunderSkill;
import me.zygotecode.amazingmobs.skill.impl.TrapSkill;
import me.zygotecode.amazingmobs.skill.impl.VanishSkill;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Holds all known {@link Skill}s by id. Extensible: third parties (or future code) call
 * {@link #register} with their own implementation and it is immediately usable from config —
 * the core never needs editing to gain a skill.
 */
public final class SkillRegistry {

    private final Map<String, Skill> skills = new TreeMap<>();

    public void register(Skill skill) {
        if (skill == null) return;
        skills.put(skill.id().toLowerCase(Locale.ROOT), skill);
    }

    public Skill get(String id) {
        return id == null ? null : skills.get(id.toLowerCase(Locale.ROOT));
    }

    public boolean contains(String id) {
        return id != null && skills.containsKey(id.toLowerCase(Locale.ROOT));
    }

    public Set<String> ids() {
        return Collections.unmodifiableSet(skills.keySet());
    }

    public int size() {
        return skills.size();
    }

    /** Register the full built-in library. Called once at enable. */
    public void registerDefaults() {
        // --- ranged (one class, several flavoured ids) ---
        register(new ProjectileSkill("fireball", "fireball"));
        register(new ProjectileSkill("wither_skull", "wither_skull"));
        register(new ProjectileSkill("snowball", "snowball"));
        register(new ProjectileSkill("arrow_volley", "arrow"));
        register(new ProjectileSkill("projectile_burst", "fireball"));
        register(new ProjectileSkill("dragon_fireball", "dragon_fireball"));

        // --- offense / movement / control / summon / utility ---
        register(new ThunderSkill());
        register(new DashSkill());
        register(new JumpAttackSkill());
        register(new TeleportSkill("teleport"));
        register(new TeleportSkill("blink"));
        register(new AreaSlamSkill());
        register(new RepelSkill("repel", false));
        register(new RepelSkill("pull", true));
        register(new SummonSkill());
        register(new me.zygotecode.amazingmobs.skill.impl.LaunchMobSkill());
        register(new me.zygotecode.amazingmobs.skill.impl.OrbitSkill());
        register(new me.zygotecode.amazingmobs.skill.impl.StrikeSkill());
        register(new me.zygotecode.amazingmobs.skill.impl.ScaleSkill());
        register(new me.zygotecode.amazingmobs.skill.impl.AuraSkill());
        register(new HealSkill());
        register(new VanishSkill());
        register(new FlightSkill("flight"));
        register(new TrapSkill());

        // --- status effects (the workhorse: EffectSkill + presets) ---
        register(new EffectSkill("effect", SkillType.CONTROL, List.of()));
        register(new EffectSkill("poison", SkillType.CONTROL, EffectSkill.of("poison:0")));
        register(new EffectSkill("weakness", SkillType.CONTROL, EffectSkill.of("weakness:0")));
        register(new EffectSkill("slow", SkillType.CONTROL, EffectSkill.of("slowness:1")));
        register(new EffectSkill("blind", SkillType.CONTROL, EffectSkill.of("blindness:0", "darkness:0")));
        register(new EffectSkill("wither", SkillType.CONTROL, EffectSkill.of("wither:1")));
        register(new EffectSkill("levitate", SkillType.CONTROL, EffectSkill.of("levitation:0")));
        register(new EffectSkill("ignite", SkillType.OFFENSE, EffectSkill.of("fire")));
        register(new EffectSkill("fear", SkillType.CONTROL, EffectSkill.of("nausea:0", "slowness:1", "blindness:0", "darkness:0")));
        register(new EffectSkill("rage", SkillType.UTILITY, EffectSkill.of("strength:1", "speed:1")));
        register(new EffectSkill("shield", SkillType.DEFENSE, EffectSkill.of("resistance:3", "absorption:1")));
        register(new EffectSkill("buff", SkillType.UTILITY, EffectSkill.of("strength:0", "resistance:0")));
        register(new EffectSkill("strengthen", SkillType.UTILITY, EffectSkill.of("strength:1")));
        register(new EffectSkill("hasten", SkillType.UTILITY, EffectSkill.of("speed:1")));
        register(new EffectSkill("regenerate", SkillType.DEFENSE, EffectSkill.of("regeneration:1")));
    }
}
