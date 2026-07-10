package me.zygotecode.amazingmobs.trait.impl;

import me.zygotecode.amazingmobs.trait.AbstractTrait;
import me.zygotecode.amazingmobs.trait.TraitContext;
import me.zygotecode.amazingmobs.util.Fx;
import me.zygotecode.amazingmobs.util.Resolvers;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Lurks invisible until it strikes or is struck, then reveals with a burst (and an optional opener
 * effect on its first hit). Backs {@code ambusher} / {@code burrower}. params: {@code reveal-strength}.
 */
public final class AmbusherTrait extends AbstractTrait {

    public AmbusherTrait(String id) { super(id); }

    @Override
    public void onSpawn(TraitContext c) {
        PotionEffectType invis = Resolvers.effect("invisibility");
        if (invis != null) c.entity().addPotionEffect(new PotionEffect(invis, 20 * 600, 0, false, false, false));
        c.instance().putState("hidden", true);
    }

    @Override
    public void onAttack(TraitContext c) { reveal(c, true); }

    @Override
    public void onDamaged(TraitContext c) { reveal(c, false); }

    private void reveal(TraitContext c, boolean opener) {
        if (!c.instance().flag("hidden")) return;
        c.instance().putState("hidden", false);
        PotionEffectType invis = Resolvers.effect("invisibility");
        if (invis != null) c.entity().removePotionEffect(invis);
        Fx.particle(c.origin().add(0, 1, 0), "large_smoke", 25, 0.4, 0.6, 0.4, 0.05);
        Fx.sound(c.origin(), "entity_generic_extinguish_fire", 1f, 0.6f);
        if (opener && c.params().contains("reveal-strength")) {
            effect(c.entity(), "strength", i(c, "reveal-strength", 1), c.periodTicks() * 6);
        }
    }
}
