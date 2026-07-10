package me.zygotecode.amazingmobs.trait.impl;

import me.zygotecode.amazingmobs.trait.AbstractTrait;
import me.zygotecode.amazingmobs.trait.TraitContext;
import me.zygotecode.amazingmobs.util.Fx;
import org.bukkit.entity.LivingEntity;

/**
 * Periodic healing. {@code self=true} heals only the caster ({@code regenerator}); otherwise heals
 * nearby allies ({@code healer}). params: {@code amount}, {@code percent}, {@code radius}, {@code cooldown}.
 */
public final class HealAuraTrait extends AbstractTrait {

    private final boolean selfOnly;

    public HealAuraTrait(String id, boolean selfOnly) {
        super(id);
        this.selfOnly = selfOnly;
    }

    @Override
    public void onTick(TraitContext c) {
        if (!ready(c, "3s")) return;
        double flat = d(c, "amount", 4);
        double pct = d(c, "percent", 0);
        if (selfOnly) {
            applyHeal(c.entity(), flat, pct);
        } else {
            double radius = d(c, "radius", 10);
            for (LivingEntity ally : allies(c, radius, flag(c, "same-type-only", false))) applyHeal(ally, flat, pct);
            applyHeal(c.entity(), flat, pct);
        }
    }

    private void applyHeal(LivingEntity t, double flat, double pct) {
        var max = t.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH);
        double cap = max != null ? max.getValue() : t.getHealth();
        double amount = flat + cap * pct;
        if (amount <= 0 || t.getHealth() >= cap) return;
        heal(t, amount);
        Fx.particle(t.getLocation().add(0, 1, 0), "heart", 5, 0.3, 0.5, 0.3, 0.01);
    }
}
