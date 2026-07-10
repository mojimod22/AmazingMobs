package me.zygotecode.amazingmobs.mob;

import java.util.List;

/**
 * One layer of a mob stack / column: a mob seated on top of this one. Multiple riders form a
 * vertical passenger chain (rider[0] on the base, rider[1] on rider[0], ...).
 *
 * @param mobId         custom mob id (or vanilla type id) for this layer
 * @param onBaseDeath   what happens to this rider when the layer below it dies (cascading death)
 * @param bonusEffects  effects applied to this rider while stacked (e.g. {@code "strength:0"})
 */
public record RiderSpec(String mobId, RiderDeathBehavior onBaseDeath, List<String> bonusEffects) {

    public RiderSpec {
        bonusEffects = bonusEffects == null ? List.of() : List.copyOf(bonusEffects);
    }
}
