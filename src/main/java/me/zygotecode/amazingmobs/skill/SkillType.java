package me.zygotecode.amazingmobs.skill;

/** Classification of a skill. Drives documentation grouping and sensible trigger defaults. */
public enum SkillType {
    OFFENSE,   // direct damage / projectiles
    CONTROL,   // debuffs, crowd control, fear
    MOVEMENT,  // dashes, blinks, flight, repositioning
    DEFENSE,   // shields, heals, knockback immunity windows
    SUMMON,    // spawn minions / reinforcements
    UTILITY    // buffs, vanish, misc
}
