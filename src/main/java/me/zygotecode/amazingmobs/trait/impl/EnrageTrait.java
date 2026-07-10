package me.zygotecode.amazingmobs.trait.impl;

import me.zygotecode.amazingmobs.trait.AbstractTrait;
import me.zygotecode.amazingmobs.trait.TraitContext;
import me.zygotecode.amazingmobs.util.Fx;
import me.zygotecode.amazingmobs.util.Text;
import org.bukkit.entity.Player;

/**
 * One-shot enraged comeback: the first time it falls below {@code threshold} it gains a big, lasting
 * buff with a clear shout/telegraph. params: {@code threshold} (0.35), {@code duration} (30s),
 * {@code strength} (1), {@code speed} (1), {@code resistance} (0), {@code message}.
 */
public final class EnrageTrait extends AbstractTrait {

    public EnrageTrait() { super("enrage"); }

    @Override
    public void onTick(TraitContext c) {
        if (c.instance().flag("done")) return;
        if (c.healthFraction() > d(c, "threshold", 0.35)) return;
        c.instance().putState("done", true);

        int dur = (int) me.zygotecode.amazingmobs.util.Numbers.parseTicks(c.params().getString("duration"), 600);
        effect(c.entity(), "strength", i(c, "strength", 1), dur);
        effect(c.entity(), "speed", i(c, "speed", 1), dur);
        if (c.params().contains("resistance")) effect(c.entity(), "resistance", i(c, "resistance", 0), dur);
        Fx.particle(c.origin().add(0, 1, 0), "flame", 40, 0.5, 0.7, 0.5, 0.1);
        Fx.sound(c.origin(), str(c, "sound", "entity_ravager_roar"), 1.2f, 0.9f);
        String msg = str(c, "message", null);
        if (msg != null && !msg.isBlank()) for (Player p : players(c, 24)) p.sendMessage(Text.mm(msg));
    }
}
