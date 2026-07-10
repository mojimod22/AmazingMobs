package me.zygotecode.amazingmobs.trait;

/**
 * A modular behaviour/affix attachable to any mob. Stateless strategy (one instance shared by all
 * mobs that use it); per-mob state arrives via {@link TraitContext#instance()}. Hooks the mob
 * lifecycle — implement only the hooks you need. Every hook is wrapped upstream, so a throw never
 * breaks the controller tick.
 *
 * <p>Add a new trait by implementing this and registering it in {@link TraitRegistry} — usable from
 * any mob's {@code traits:} list immediately, no core edits.</p>
 */
public interface Trait {

    /** Unique lowercase id referenced from config (e.g. {@code "berserker"}). */
    String id();

    /** Once, right after spawn. */
    default void onSpawn(TraitContext ctx) {}

    /** Periodically, on the shared controller tick. Honour your own cooldown via {@link TraitInstance}. */
    default void onTick(TraitContext ctx) {}

    /** When the mob takes damage. {@code ctx.eventTarget()} is the attacker (may be null). */
    default void onDamaged(TraitContext ctx) {}

    /** When the mob lands a melee hit. {@code ctx.eventTarget()} is the victim. */
    default void onAttack(TraitContext ctx) {}

    /** On death (entity may already be invalid — use {@code ctx.entity().getLocation()}). */
    default void onDeath(TraitContext ctx) {}
}
