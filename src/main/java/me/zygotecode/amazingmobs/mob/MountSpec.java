package me.zygotecode.amazingmobs.mob;

import java.util.List;

/**
 * Declares that this mob spawns riding another mob (a living mount). The mount is spawned first and
 * this mob is seated on it.
 *
 * @param mountId                 custom mob id (or vanilla type id) to spawn as the mount
 * @param onMountDeath            what happens to this rider when the mount dies
 * @param riderBonusEffects       effects applied to this rider while mounted (e.g. {@code "resistance:0"})
 * @param killMountWhenRiderDies  if true, the mount dies when this rider dies
 */
public record MountSpec(String mountId, RiderDeathBehavior onMountDeath,
                        List<String> riderBonusEffects, boolean killMountWhenRiderDies) {

    public MountSpec {
        riderBonusEffects = riderBonusEffects == null ? List.of() : List.copyOf(riderBonusEffects);
    }
}
