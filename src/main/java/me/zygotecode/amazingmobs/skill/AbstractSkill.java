package me.zygotecode.amazingmobs.skill;

import me.zygotecode.amazingmobs.util.Fx;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

import java.util.Locale;

/** Base class wiring id/type and providing terse param + feedback helpers for implementations. */
public abstract class AbstractSkill implements Skill {

    private final String id;
    private final SkillType type;

    protected AbstractSkill(String id, SkillType type) {
        this.id = id.toLowerCase(Locale.ROOT);
        this.type = type;
    }

    @Override public String id() { return id; }
    @Override public SkillType type() { return type; }

    // --- param helpers --------------------------------------------------------------------------
    protected double d(SkillContext c, String key, double def) { return c.params().getDouble(key, def); }
    protected int i(SkillContext c, String key, int def) { return c.params().getInt(key, def); }
    protected boolean flag(SkillContext c, String key, boolean def) { return c.params().getBool(key, def); }
    protected String str(SkillContext c, String key, String def) { return c.params().getString(key, def); }

    // --- feedback helpers -----------------------------------------------------------------------
    /** Play the skill's configured {@code sound}/{@code particle} params at a location. */
    protected void feedback(SkillContext c, Location at) {
        Fx.sound(at, str(c, "sound", null), (float) d(c, "sound-volume", 1.0), (float) d(c, "sound-pitch", 1.0));
        Fx.particle(at, str(c, "particle", null), i(c, "particle-count", 20), 0.5, 0.5, 0.5, 0.05);
    }

    /** Deal damage to {@code target} from the caster, respecting Bukkit damage events. */
    protected void damage(SkillContext c, LivingEntity target, double amount) {
        if (target == null || !target.isValid() || amount <= 0) return;
        target.damage(amount, c.caster());
    }
}
