package me.zygotecode.amazingmobs.trait.impl;

import me.zygotecode.amazingmobs.trait.AbstractTrait;
import me.zygotecode.amazingmobs.trait.TraitContext;
import me.zygotecode.amazingmobs.util.Fx;
import me.zygotecode.amazingmobs.util.Numbers;
import org.bukkit.entity.LivingEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * Periodic potion-effect aura — the synergy workhorse. {@code mode} decides who it touches:
 * SELF, ALLIES (nearby custom mobs), PLAYERS (nearby players), or ALL. Backs trait ids like
 * {@code buffer protector guardian commander saboteur hexer frost_aura disruptor}. Config can
 * override the effect list and radius/cooldown.
 */
public final class AuraTrait extends AbstractTrait {

    public enum Mode { SELF, ALLIES, PLAYERS, ALL }

    private final Mode mode;
    private final List<String> defaults; // "strength:1"

    public AuraTrait(String id, Mode mode, List<String> defaults) {
        super(id);
        this.mode = mode;
        this.defaults = defaults == null ? List.of() : List.copyOf(defaults);
    }

    @Override
    public void onTick(TraitContext c) {
        if (!ready(c, "3s")) return;
        double radius = d(c, "radius", 10);
        int dur = (int) Numbers.parseTicks(c.params().getString("duration"), 80);
        boolean sameType = flag(c, "same-type-only", false);

        List<LivingEntity> targets = new ArrayList<>();
        switch (mode) {
            case SELF -> targets.add(c.entity());
            case ALLIES -> targets.addAll(allies(c, radius, sameType));
            case PLAYERS -> targets.addAll(players(c, radius));
            case ALL -> { targets.addAll(allies(c, radius, sameType)); targets.addAll(players(c, radius)); }
        }
        if (targets.isEmpty()) return;

        boolean configured = !c.params().getSectionList("effects").isEmpty();
        for (LivingEntity t : targets) {
            if (configured) {
                effects(c.params(), t, dur);
            } else {
                for (String s : defaults) {
                    String[] p = s.split(":");
                    effect(t, p[0], p.length > 1 ? safeInt(p[1]) : 0, dur);
                }
            }
        }
        Fx.particle(c.origin().add(0, 1, 0), str(c, "particle", mode == Mode.PLAYERS ? "witch" : "happy_villager"),
                10, radius / 3, 0.6, radius / 3, 0.01);
        Fx.sound(c.origin(), str(c, "sound", null), 0.7f, 1.0f);
    }

    private static int safeInt(String s) {
        try { return Integer.parseInt(s.trim()); } catch (NumberFormatException e) { return 0; }
    }
}
