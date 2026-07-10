package me.zygotecode.amazingmobs.clazz;

import me.zygotecode.amazingmobs.AmazingMobs;
import me.zygotecode.amazingmobs.util.Text;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

/**
 * Casting brain: resolves a player's class + the skill for a trigger, checks/sets the prestige-scaled
 * cooldown, runs the skill, and gives clean action-bar + sound feedback. The only entry point combat
 * triggers and the skill menu call into.
 */
public final class ClassManager {

    private final AmazingMobs plugin;
    private final ClassRegistry registry;
    private final ClassService service;
    private final Cooldowns cooldowns;
    private final MinionManager minions;

    public ClassManager(AmazingMobs plugin, ClassRegistry registry, ClassService service,
                        Cooldowns cooldowns, MinionManager minions) {
        this.plugin = plugin;
        this.registry = registry;
        this.service = service;
        this.cooldowns = cooldowns;
        this.minions = minions;
    }

    public ClassRegistry registry() { return registry; }
    public Cooldowns cooldowns() { return cooldowns; }

    public int prestige(Player p) {
        var w = plugin.weightService();
        return w != null ? Math.max(1, w.prestigeOf(p.getUniqueId())) : 1;
    }

    /** Prestige-reduced cooldown (down to -50%), times the configured global multiplier. */
    public int effectiveCooldown(PlayerSkill sk, int prestige) {
        int base = sk.baseCooldownSeconds();
        if (base <= 0) return 0;
        double reduce = Math.min(0.5, 0.03 * (prestige - 1));
        double cd = base * (1 - reduce) * plugin.config().classCooldownMultiplier;
        return (int) Math.max(1, Math.round(cd));
    }

    /** Cast the skill bound to {@code type} for the player's class. */
    public void cast(Player p, SkillType type) {
        if (!service.enabled()) return;
        PlayerClass c = service.classOf(p);
        if (c == null) {
            p.sendActionBar(Text.mm("<red>No class — pick one with <yellow>/am class</yellow>."));
            return;
        }
        PlayerSkill sk = registry.skill(c.skillId(type));
        if (sk == null) return;

        if (type == SkillType.BASE) { run(p, sk); return; } // base has no cooldown

        long rem = cooldowns.remainingMs(p.getUniqueId(), sk.id());
        if (rem > 0) {
            p.sendActionBar(Text.mm(sk.type().color() + sk.name() + " <gray>on cooldown <white>" + ((rem + 999) / 1000) + "s"));
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.7f, 0.6f);
            return;
        }
        cooldowns.trigger(p.getUniqueId(), sk.id(), effectiveCooldown(sk, prestige(p)));
        run(p, sk);
        p.sendActionBar(Text.mm(sk.type().color() + "✦ " + sk.name()));
        p.playSound(p.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.8f, 1.3f);
    }

    /** Cast a specific skill by id (skill menu). Validates it belongs to the player's class. */
    public void castById(Player p, String skillId) {
        PlayerClass c = service.classOf(p);
        if (c == null) return;
        PlayerSkill sk = registry.skill(skillId);
        if (sk == null) return;
        if (!skillId.equals(c.skillId(sk.type()))) return; // not this player's skill
        cast(p, sk.type());
    }

    private void run(Player p, PlayerSkill sk) {
        try {
            sk.cast(new SkillContext(plugin, p, prestige(p), minions));
        } catch (Throwable t) {
            plugin.getLogger().warning("[class] skill '" + sk.id() + "' failed for " + p.getName() + ": " + t);
        }
    }
}
