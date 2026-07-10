package me.zygotecode.amazingmobs.skill.impl;

import me.zygotecode.amazingmobs.skill.AbstractSkill;
import me.zygotecode.amazingmobs.skill.SkillContext;
import me.zygotecode.amazingmobs.skill.SkillType;
import me.zygotecode.amazingmobs.util.Fx;
import me.zygotecode.amazingmobs.util.Resolvers;
import me.zygotecode.amazingmobs.util.Schedulers;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.LivingEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * Places temporary trap blocks (default cobweb) around the targets, then reverts them after the
 * trigger duration. Only replaces passable, non-liquid blocks and restores the exact original
 * block data — non-destructive by construction. params: {@code material}, {@code radius}.
 */
public final class TrapSkill extends AbstractSkill {

    public TrapSkill() { super("trap", SkillType.CONTROL); }

    @Override
    public void cast(SkillContext ctx) {
        Material mat = Resolvers.material(str(ctx, "material", "COBWEB"), Material.COBWEB);
        if (mat == null || !mat.isBlock()) return;
        int radius = Math.max(0, i(ctx, "radius", (int) Math.min(2, ctx.trigger().radius())));
        long duration = Math.max(20, ctx.trigger().durationTicks());

        List<Block> changed = new ArrayList<>();
        List<BlockData> originals = new ArrayList<>();

        for (LivingEntity t : ctx.targets()) {
            if (t == null || !t.isValid()) continue;
            Block center = t.getLocation().getBlock();
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    Block b = center.getRelative(dx, 0, dz);
                    if (!b.isPassable() || b.isLiquid() || b.getType() == mat) continue;
                    originals.add(b.getBlockData());
                    changed.add(b);
                    b.setType(mat, false);
                }
            }
            Fx.particle(t.getLocation(), str(ctx, "particle", "crit"), 12, 0.4, 0.4, 0.4, 0.02);
        }
        if (changed.isEmpty()) return;
        Fx.sound(ctx.origin(), str(ctx, "sound", "block_wool_place"), 1f, 0.8f);

        Schedulers.later(ctx.plugin(), duration, () -> {
            for (int k = 0; k < changed.size(); k++) {
                Block b = changed.get(k);
                if (b.getType() == mat) b.setBlockData(originals.get(k), false);
            }
        });
    }
}
