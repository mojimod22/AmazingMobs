package me.zygotecode.amazingmobs.trait.impl;

import me.zygotecode.amazingmobs.trait.AbstractTrait;
import me.zygotecode.amazingmobs.trait.TraitContext;
import me.zygotecode.amazingmobs.util.Fx;
import me.zygotecode.amazingmobs.util.Text;
import org.bukkit.entity.Player;

/**
 * Looks like a harmless mob until provoked — name hidden, then revealed (with an optional warning
 * shout) on its first hit taken. Great for "false innocent" ambushes. params: {@code reveal-message}.
 */
public final class MimicTrait extends AbstractTrait {

    public MimicTrait() { super("mimic"); }

    @Override
    public void onSpawn(TraitContext c) {
        c.entity().setCustomNameVisible(false);
        c.entity().setGlowing(false);
        c.instance().putState("hidden", true);
    }

    @Override
    public void onDamaged(TraitContext c) { reveal(c); }

    @Override
    public void onAttack(TraitContext c) { reveal(c); }

    private void reveal(TraitContext c) {
        if (!c.instance().flag("hidden")) return;
        c.instance().putState("hidden", false);
        c.entity().setCustomNameVisible(c.definition().presentation().nameVisible());
        Fx.particle(c.origin().add(0, 1, 0), "large_smoke", 20, 0.4, 0.6, 0.4, 0.05);
        Fx.sound(c.origin(), "entity_elder_guardian_curse", 1f, 1.2f);
        String msg = str(c, "reveal-message", null);
        if (msg != null && !msg.isBlank()) {
            for (Player p : players(c, 24)) p.sendMessage(Text.mm(msg));
        }
    }
}
