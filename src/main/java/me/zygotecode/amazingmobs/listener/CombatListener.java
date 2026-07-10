package me.zygotecode.amazingmobs.listener;

import me.zygotecode.amazingmobs.AmazingMobs;
import me.zygotecode.amazingmobs.mob.StatBlock;
import me.zygotecode.amazingmobs.mob.runtime.ActiveMob;
import me.zygotecode.amazingmobs.mob.runtime.MobManager;
import me.zygotecode.amazingmobs.util.Fx;
import me.zygotecode.amazingmobs.util.Rng;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

/**
 * Applies the custom combat layer: elemental immunities + damage-taken multipliers (victim side),
 * crit rolls (attacker side), and routes ON_DAMAGED / ON_ATTACK skill triggers. Two handlers share
 * one event object (EntityDamageByEntityEvent reuses EntityDamageEvent's handler list) — the
 * superclass handler does environmental/elemental work, the subclass handler does combat work.
 */
public final class CombatListener implements Listener {

    private final MobManager mobs;
    private final Rng rng = Rng.shared();

    public CombatListener(AmazingMobs plugin) {
        this.mobs = plugin.mobManager();
    }

    /** Victim-side: immunities + elemental damage multipliers (covers ALL causes). */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof LivingEntity le)) return;
        ActiveMob am = mobs.get(le);
        if (am == null) return;
        StatBlock st = am.definition().stats();
        DamageCause cause = e.getCause();

        if (st.fireImmune() && isFire(cause)) { e.setCancelled(true); return; }
        if (st.fallImmune() && (cause == DamageCause.FALL || cause == DamageCause.FLY_INTO_WALL)) { e.setCancelled(true); return; }
        if (st.drownImmune() && cause == DamageCause.DROWNING) { e.setCancelled(true); return; }

        double mul = st.damageMultiplier(cause.name(), category(cause));
        if (mul != 1.0) {
            if (mul <= 0) e.setCancelled(true);
            else e.setDamage(Math.max(0, e.getDamage() * mul));
        }
    }

    /** Attacker-side: crit + skill triggers for both parties. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onByEntity(EntityDamageByEntityEvent e) {
        LivingEntity victim = e.getEntity() instanceof LivingEntity le ? le : null;
        LivingEntity attacker = resolveAttacker(e.getDamager());

        if (attacker != null) {
            ActiveMob am = mobs.get(attacker);
            if (am != null) {
                StatBlock st = am.definition().stats();
                if (st.critChance() > 0 && rng.chance(st.critChance())) {
                    e.setDamage(e.getDamage() * st.critMultiplier());
                    if (victim != null) Fx.particle(victim.getLocation().add(0, 1, 0), "crit", 12, 0.3, 0.3, 0.3, 0.1);
                }
                if (victim != null) mobs.handleAttack(attacker, victim);
            }
        }
        if (victim != null && mobs.isCustomMob(victim)) {
            mobs.handleDamaged(victim, attacker);
        }
    }

    private static LivingEntity resolveAttacker(Entity damager) {
        if (damager instanceof LivingEntity le) return le;
        if (damager instanceof Projectile p && p.getShooter() instanceof LivingEntity shooter) return shooter;
        return null;
    }

    private static boolean isFire(DamageCause c) {
        return c == DamageCause.FIRE || c == DamageCause.FIRE_TICK || c == DamageCause.LAVA
                || c == DamageCause.HOT_FLOOR || c == DamageCause.CAMPFIRE || c == DamageCause.MELTING;
    }

    private static String category(DamageCause c) {
        return switch (c) {
            case FIRE, FIRE_TICK, LAVA, HOT_FLOOR, CAMPFIRE, MELTING -> "fire";
            case BLOCK_EXPLOSION, ENTITY_EXPLOSION -> "explosion";
            case PROJECTILE -> "projectile";
            case MAGIC, WITHER, POISON, DRAGON_BREATH -> "magic";
            case ENTITY_ATTACK, ENTITY_SWEEP_ATTACK -> "melee";
            case FALL, FLY_INTO_WALL -> "fall";
            case FREEZE -> "freeze";
            default -> "other";
        };
    }
}
