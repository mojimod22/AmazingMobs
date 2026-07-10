package me.zygotecode.amazingmobs.trait;

import me.zygotecode.amazingmobs.config.ConfigSection;

/**
 * A trait bound into a mob: which trait ({@code id}) and its tunable {@code params}. Immutable and
 * shared across every spawn of the mob; per-instance state lives in {@link TraitInstance}.
 *
 * <p>Traits are runtime behaviour modules (personality/tactics) layered on top of skills — they hook
 * the mob lifecycle (spawn/tick/damaged/attack/death) rather than being explicitly cast.</p>
 */
public record TraitDefinition(String id, ConfigSection params) {}
