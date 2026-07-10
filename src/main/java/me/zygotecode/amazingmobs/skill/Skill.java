package me.zygotecode.amazingmobs.skill;

/**
 * A stateless, reusable ability. One instance is shared across every mob that uses it; all per-cast
 * data arrives via {@link SkillContext} and all per-mob cooldown state lives in {@link SkillInstance}.
 *
 * <p>Add a new ability by implementing this and registering it in {@link SkillRegistry} — no core
 * edits, no event wiring (the runtime drives triggers and passes resolved targets).</p>
 */
public interface Skill {

    /** Unique lowercase id referenced from config (e.g. {@code "fireball"}). */
    String id();

    /** Classification (offense/control/movement/defense/summon/utility). */
    SkillType type();

    /** Execute the ability. Implementations must be defensive — any throw is caught upstream. */
    void cast(SkillContext ctx);
}
