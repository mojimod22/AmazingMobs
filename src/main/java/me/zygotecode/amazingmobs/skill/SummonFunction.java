package me.zygotecode.amazingmobs.skill;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

/**
 * Lets summon skills spawn other custom mobs without the skill package depending on the mob-runtime
 * package (the runtime supplies the implementation). Returns the spawned entity, or {@code null}.
 */
@FunctionalInterface
public interface SummonFunction {
    LivingEntity summon(String mobId, Location location);
}
