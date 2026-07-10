package me.zygotecode.amazingmobs.trait.impl;

import me.zygotecode.amazingmobs.trait.AbstractTrait;
import me.zygotecode.amazingmobs.trait.TraitContext;
import me.zygotecode.amazingmobs.util.Fx;
import me.zygotecode.amazingmobs.util.Schedulers;
import me.zygotecode.amazingmobs.util.Text;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

/**
 * Drama queen: the first time it drops below {@code threshold} it "dies" (collapses, invisible,
 * invulnerable, still) for {@code delay} ticks — then comes roaring back enraged. params:
 * {@code threshold} (0.3), {@code delay} (50), {@code heal} (0.0 of max), {@code message}.
 */
public final class FakeDeathTrait extends AbstractTrait {

    public FakeDeathTrait() { super("fake_death"); }

    @Override
    public void onTick(TraitContext c) {
        if (c.instance().flag("done")) return;
        if (c.healthFraction() > d(c, "threshold", 0.3)) return;
        c.instance().putState("done", true);

        LivingEntity e = c.entity();
        long delay = (long) i(c, "delay", 50);
        e.setAI(false);
        e.setInvulnerable(true);
        e.setVisualFire(false);
        var invis = me.zygotecode.amazingmobs.util.Resolvers.effect("invisibility");
        if (invis != null) e.addPotionEffect(new org.bukkit.potion.PotionEffect(invis, (int) delay + 10, 0, false, false, false));
        Fx.particle(c.origin(), "large_smoke", 30, 0.4, 0.3, 0.4, 0.05);
        Fx.sound(c.origin(), "entity_player_death", 1f, 0.8f);

        double healPct = d(c, "heal", 0.0);
        String msg = str(c, "message", "<dark_red>It rises again!");
        Schedulers.later(c.plugin(), delay, () -> {
            if (!e.isValid()) return;
            e.setAI(true);
            e.setInvulnerable(false);
            if (invis != null) e.removePotionEffect(invis);
            if (healPct > 0) heal(e, maxHealth(e) * healPct);
            effect(e, "strength", 1, 20 * 30);
            effect(e, "speed", 1, 20 * 30);
            Fx.particle(c.origin(), "flame", 40, 0.5, 0.6, 0.5, 0.1);
            Fx.sound(c.origin(), "entity_wither_spawn", 1f, 1.2f);
            if (msg != null && !msg.isBlank()) for (Player p : players(c, 24)) p.sendMessage(Text.mm(msg));
        });
    }

    private static double maxHealth(LivingEntity e) {
        var a = e.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH);
        return a != null ? a.getValue() : e.getHealth();
    }
}
