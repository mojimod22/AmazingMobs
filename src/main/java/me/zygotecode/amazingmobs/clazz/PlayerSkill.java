package me.zygotecode.amazingmobs.clazz;

import org.bukkit.Material;

import java.util.List;

/**
 * One player skill. Definitions are stateless + shared; per-player cooldown lives in {@link Cooldowns}
 * and per-player power comes from the prestige passed into {@link #cast}. The {@code description} is
 * prestige-aware so the skill menu can show how it scales.
 */
public interface PlayerSkill {

    String id();

    String name();

    SkillType type();

    /** Icon shown in the skill menu. */
    Material icon();

    /** Base cooldown in seconds (before prestige reduction). */
    int baseCooldownSeconds();

    /** Lore lines describing what it does at the given prestige (for the menu). */
    List<String> description(int prestige);

    /** Execute the skill. Cooldown + feedback are handled by {@link ClassManager}. */
    void cast(SkillContext ctx);
}
