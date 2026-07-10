package me.zygotecode.amazingmobs.clazz;

import me.zygotecode.amazingmobs.weapon.AkRifle;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

/**
 * The whole player-skill roster, declared compactly via {@link SimpleSkill}. Six classes × (active +
 * special + hyper) plus the shared AK-47 base. Bodies lean on {@link SkillContext} helpers; numbers are
 * the base values (prestige scaling is applied inside each cast via {@code ctx.scale/bonus}).
 */
public final class Skills {

    private Skills() {}

    public static List<PlayerSkill> all() {
        List<PlayerSkill> s = new ArrayList<>();

        // ---- universal base ---------------------------------------------------------------------
        s.add(new SimpleSkill("base_ak47", "AK-47", SkillType.BASE, Material.GOLDEN_AXE, 0,
                p -> List.of("<gray>Your reliable fallback: a 32-round semi-auto rifle.",
                        "<gray>Right-click the AK-47 item to fire wither-skull rounds.",
                        "<dark_gray>Every class carries one."),
                ctx -> {
                    boolean has = false;
                    for (ItemStack it : ctx.player.getInventory().getContents()) if (AkRifle.isAk(it)) { has = true; break; }
                    if (!has) { ctx.player.getInventory().addItem(AkRifle.build()); ctx.player.sendActionBar(mm("<gold>AK-47 issued — right-click to fire.")); }
                    else ctx.player.sendActionBar(mm("<gold>AK-47 ready — right-click to fire."));
                }));

        // ---- NECROMANCER ------------------------------------------------------------------------
        s.add(new SimpleSkill("necro_soulbolt", "Soul Bolt", SkillType.ACTIVE, Material.BONE, 5,
                p -> List.of("<gray>Hurl a bolt of necrotic energy at your target.",
                        line("Damage", (int) (6 * scale(p, .12))) + " <gray>+ Wither, and heals you on a kill."),
                ctx -> {
                    LivingEntity t = ctx.aimTarget(28);
                    Location end = t != null ? t.getEyeLocation() : ctx.aimBlock(28);
                    beam(ctx, end, "soul");
                    ctx.sound(ctx.eye(), "entity_evoker_cast_spell", 1f, 0.8f);
                    if (t != null) {
                        boolean lethal = t.getHealth() <= ctx.scale(6, .12);
                        ctx.hurt(t, ctx.scale(6, .12));
                        ctx.debuff(t, PotionEffectType.WITHER, 4, 0);
                        if (lethal) ctx.player.setHealth(Math.min(maxHp(ctx.player), ctx.player.getHealth() + 4));
                    }
                }));
        s.add(new SimpleSkill("necro_raisedead", "Raise Dead", SkillType.SPECIAL, Material.SKELETON_SKULL, 30,
                p -> List.of("<gray>Tear " + (3 + half(p)) + " risen skeletons from the ground to fight for you.",
                        "<gray>They join your standing minion army (capped)."),
                ctx -> {
                    int n = ctx.bonus(3, 0.5);
                    for (int i = 0; i < n; i++) {
                        Location l = around(ctx.player.getLocation(), 2.5, ctx);
                        ctx.summon(EntityType.SKELETON, l, false, 1.0 + 0.1 * (ctx.prestige - 1), 0L);
                        ctx.particle(l, "soul", 14, 0.3);
                    }
                    ctx.sound(ctx.player.getLocation(), "entity_skeleton_ambient", 1.2f, 0.7f);
                }));
        s.add(new SimpleSkill("necro_army", "Army of the Damned", SkillType.HYPER, Material.WITHER_SKELETON_SKULL, 120,
                p -> List.of("<gray>Raise a temporary host of " + (5 + half(p)) + " wither-skeleton reavers (~25s)",
                        "<gray>and unleash a necrotic aura: nearby foes rot (Wither) and slow.",
                        "<gray>You gain Strength + Absorption."),
                ctx -> {
                    int n = ctx.bonus(5, 0.5);
                    for (int i = 0; i < n; i++) ctx.summon(EntityType.WITHER_SKELETON, around(ctx.player.getLocation(), 3, ctx), true, 1.3 + 0.12 * (ctx.prestige - 1), 25_000L);
                    for (LivingEntity e : ctx.enemiesNear(ctx.player.getLocation(), 9)) { ctx.debuff(e, PotionEffectType.WITHER, 8, 1); ctx.debuff(e, PotionEffectType.SLOWNESS, 8, 1); }
                    ctx.buffSelf(PotionEffectType.STRENGTH, 12, 1);
                    ctx.buffSelf(PotionEffectType.ABSORPTION, 12, 2);
                    nova(ctx, "soul", 60); ctx.sound(ctx.player.getLocation(), "entity_wither_spawn", 1.4f, 0.8f);
                }));

        // ---- STORMCALLER ------------------------------------------------------------------------
        s.add(new SimpleSkill("storm_chain", "Chain Lightning", SkillType.ACTIVE, Material.PRISMARINE_SHARD, 6,
                p -> List.of("<gray>Call lightning on your target that arcs to " + (1 + half(p)) + " more nearby foes.",
                        line("Damage each", (int) (5 * scale(p, .12)))),
                ctx -> {
                    LivingEntity first = ctx.aimTarget(30);
                    Location at = first != null ? first.getLocation() : ctx.aimBlock(30);
                    ctx.lightning(at);
                    if (first != null) ctx.hurt(first, ctx.scale(5, .12));
                    int jumps = ctx.bonus(1, 0.5);
                    Location from = at;
                    for (LivingEntity e : ctx.enemiesNear(at, 7)) {
                        if (jumps-- <= 0) break;
                        if (e.equals(first)) continue;
                        ctx.lightning(e.getLocation()); ctx.hurt(e, ctx.scale(5, .12)); from = e.getLocation();
                    }
                }));
        s.add(new SimpleSkill("storm_thunderstrike", "Thunderstrike", SkillType.SPECIAL, Material.TRIDENT, 25,
                p -> List.of("<gray>Smite the spot you aim at with a thunderbolt + a focused blast.",
                        line("Area damage", (int) (10 * scale(p, .12))) + " <gray>in 4 blocks."),
                ctx -> {
                    Location at = ctx.aimBlock(40);
                    ctx.lightning(at);
                    ctx.explode(at, (float) ctx.scale(3, .08), false);
                    for (LivingEntity e : ctx.enemiesNear(at, 4)) ctx.hurt(e, ctx.scale(10, .12));
                }));
        s.add(new SimpleSkill("storm_tempest", "Tempest", SkillType.HYPER, Material.LIGHTNING_ROD, 110,
                p -> List.of("<gray>A roaming storm hunts your enemies: ~" + (8 + p) + " strikes over 6s.",
                        "<gray>You ride the gale (Speed + Jump)."),
                ctx -> {
                    ctx.buffSelf(PotionEffectType.SPEED, 8, 1); ctx.buffSelf(PotionEffectType.JUMP_BOOST, 8, 2);
                    int strikes = 8 + ctx.prestige;
                    repeat(ctx, 10L, strikes, () -> {
                        LivingEntity e = ctx.nearestEnemy(ctx.player.getLocation(), 18);
                        Location at = e != null ? e.getLocation() : around(ctx.player.getLocation(), 6, ctx);
                        ctx.lightning(at);
                        for (LivingEntity v : ctx.enemiesNear(at, 3)) ctx.hurt(v, ctx.scale(7, .1));
                    });
                }));

        // ---- PYROMANCER -------------------------------------------------------------------------
        s.add(new SimpleSkill("pyro_cinderblast", "Cinder Blast", SkillType.ACTIVE, Material.FIRE_CHARGE, 5,
                p -> List.of("<gray>Detonate a gout of flame where you aim.",
                        line("Area damage", (int) (6 * scale(p, .12))) + " <gray>+ sets foes ablaze."),
                ctx -> {
                    Location at = ctx.aimBlock(22);
                    ctx.explode(at, (float) ctx.scale(2, .08), true);
                    for (LivingEntity e : ctx.enemiesNear(at, 3.5)) { ctx.hurt(e, ctx.scale(6, .12)); ctx.ignite(e, 80); }
                    ctx.particle(at, "flame", 30, 0.4); ctx.sound(at, "entity_blaze_shoot", 1f, 1f);
                }));
        s.add(new SimpleSkill("pyro_meteor", "Meteor", SkillType.SPECIAL, Material.MAGMA_BLOCK, 24,
                p -> List.of("<gray>Mark a spot, then a meteor falls (~1s) for a fiery blast.",
                        line("Area damage", (int) (12 * scale(p, .12))) + " <gray>in 4.5 blocks."),
                ctx -> {
                    Location at = ctx.aimBlock(40);
                    ring(ctx, at, 4.5, "flame");
                    ctx.sound(at, "entity_blaze_shoot", 1.4f, 0.6f);
                    later(ctx, 20L, () -> {
                        ctx.explode(at, (float) ctx.scale(3.5, .07), true);
                        for (LivingEntity e : ctx.enemiesNear(at, 4.5)) { ctx.hurt(e, ctx.scale(12, .12)); ctx.ignite(e, 120); }
                        ctx.particle(at.clone().add(0, 1, 0), "explosion_emitter", 1, 0);
                    });
                }));
        s.add(new SimpleSkill("pyro_inferno", "Inferno", SkillType.HYPER, Material.BLAZE_POWDER, 110,
                p -> List.of("<gray>Become a living pyre for 7s: a fire nova every second,",
                        "<gray>burning all foes within 5. You are fire-immune + empowered."),
                ctx -> {
                    ctx.buffSelf(PotionEffectType.FIRE_RESISTANCE, 8, 0); ctx.buffSelf(PotionEffectType.STRENGTH, 8, 1);
                    repeat(ctx, 20L, 7, () -> {
                        Location c = ctx.player.getLocation();
                        nova(ctx, "flame", 40);
                        for (LivingEntity e : ctx.enemiesNear(c, 5)) { ctx.hurt(e, ctx.scale(6, .1)); ctx.ignite(e, 100); }
                        ctx.sound(c, "entity_blaze_ambient", 1f, 0.7f);
                    });
                }));

        // ---- VANGUARD ---------------------------------------------------------------------------
        s.add(new SimpleSkill("van_bash", "Shield Bash", SkillType.ACTIVE, Material.SHIELD, 5,
                p -> List.of("<gray>Slam nearby foes: knockback + brief stun (Slowness).",
                        line("Damage", (int) (5 * scale(p, .1)))),
                ctx -> {
                    Location c = ctx.player.getLocation();
                    for (LivingEntity e : ctx.enemiesNear(c, 4)) { ctx.hurt(e, ctx.scale(5, .1)); ctx.knock(e, c, 1.0); ctx.debuff(e, PotionEffectType.SLOWNESS, 3, 2); }
                    nova(ctx, "sweep_attack", 20); ctx.sound(c, "item_shield_block", 1.2f, 0.8f);
                }));
        s.add(new SimpleSkill("van_bulwark", "Bulwark", SkillType.SPECIAL, Material.NETHERITE_CHESTPLATE, 25,
                p -> List.of("<gray>You + nearby allies gain Absorption + Resistance (" + (10 + p * 2) + "s).",
                        "<gray>Taunts nearby foes onto you."),
                ctx -> {
                    int dur = 10 + ctx.prestige * 2;
                    ctx.buffAllies(10, PotionEffectType.ABSORPTION, dur, 2);
                    ctx.buffAllies(10, PotionEffectType.RESISTANCE, dur, 1);
                    taunt(ctx, 12);
                    nova(ctx, "block_crack", 30); ctx.sound(ctx.player.getLocation(), "block_anvil_land", 0.8f, 1.4f);
                }));
        s.add(new SimpleSkill("van_unbreakable", "Unbreakable", SkillType.HYPER, Material.NETHERITE_INGOT, 100,
                p -> List.of("<gray>For 8s become a wall: Resistance IV + huge Absorption + Regen,",
                        "<gray>taunt everything in 16, and shove foes back with a shockwave."),
                ctx -> {
                    ctx.buffSelf(PotionEffectType.RESISTANCE, 8, 3);
                    ctx.buffSelf(PotionEffectType.ABSORPTION, 8, 3 + ctx.prestige / 2);
                    ctx.buffSelf(PotionEffectType.REGENERATION, 8, 1);
                    taunt(ctx, 16);
                    Location c = ctx.player.getLocation();
                    for (LivingEntity e : ctx.enemiesNear(c, 6)) { ctx.knock(e, c, 1.6); ctx.hurt(e, ctx.scale(4, .1)); }
                    nova(ctx, "explosion", 40); ctx.sound(c, "item_totem_use", 1f, 1.2f);
                }));

        // ---- ASSASSIN ---------------------------------------------------------------------------
        s.add(new SimpleSkill("assa_dash", "Shadow Dash", SkillType.ACTIVE, Material.FEATHER, 4,
                p -> List.of("<gray>Lunge the way you look + brief Speed. Mobility + repositioning.",
                        "<gray>Grazed foes take " + (int) (4 * scale(p, .1)) + "."),
                ctx -> {
                    Vector v = ctx.dir().multiply(1.5).setY(0.34);
                    ctx.player.setVelocity(v);
                    ctx.player.setFallDistance(0);
                    ctx.buffSelf(PotionEffectType.SPEED, 3, 1);
                    for (LivingEntity e : ctx.enemiesNear(ctx.player.getLocation(), 2.5)) ctx.hurt(e, ctx.scale(4, .1));
                    ctx.particle(ctx.player.getLocation(), "cloud", 20, 0.2); ctx.sound(ctx.player.getLocation(), "entity_phantom_flap", 1f, 1.4f);
                }));
        s.add(new SimpleSkill("assa_shadowstrike", "Shadowstrike", SkillType.SPECIAL, Material.NETHERITE_SWORD, 18,
                p -> List.of("<gray>Blink behind your target and run them through.",
                        line("Damage", (int) (12 * scale(p, .14))) + " <gray>+ bleed (Wither)."),
                ctx -> {
                    LivingEntity t = ctx.aimTarget(30);
                    if (t == null) { ctx.player.teleport(ctx.aimBlock(20)); ctx.sound(ctx.player.getLocation(), "entity_enderman_teleport", 1f, 1f); return; }
                    Location behind = t.getLocation().clone().subtract(t.getLocation().getDirection().setY(0).normalize().multiply(1.2));
                    behind.setDirection(t.getLocation().toVector().subtract(behind.toVector()));
                    ctx.player.teleport(behind);
                    ctx.hurt(t, ctx.scale(12, .14)); ctx.debuff(t, PotionEffectType.WITHER, 5, 0);
                    ctx.particle(t.getLocation().add(0, 1, 0), "crit", 24, 0.3); ctx.sound(t.getLocation(), "entity_player_attack_crit", 1f, 0.9f);
                }));
        s.add(new SimpleSkill("assa_deathmark", "Death Mark", SkillType.HYPER, Material.WITHER_ROSE, 90,
                p -> List.of("<gray>Brand every foe within 14 (glowing). After 3s the marks detonate",
                        "<gray>for " + (int) (8 * scale(p, .12)) + " each; you heal per victim."),
                ctx -> {
                    List<LivingEntity> marked = ctx.enemiesNear(ctx.player.getLocation(), 14);
                    for (LivingEntity e : marked) { e.addPotionEffect(new org.bukkit.potion.PotionEffect(PotionEffectType.GLOWING, 70, 0, true, false, false)); ctx.particle(e.getLocation().add(0, 1, 0), "soul_fire_flame", 16, 0.3); }
                    ctx.sound(ctx.player.getLocation(), "entity_wither_shoot", 1f, 1.3f);
                    later(ctx, 60L, () -> {
                        int healed = 0;
                        for (LivingEntity e : marked) {
                            if (e == null || e.isDead()) continue;
                            ctx.hurt(e, ctx.scale(8, .12)); ctx.particle(e.getLocation().add(0, 1, 0), "soul_fire_flame", 20, 0.3); healed++;
                        }
                        if (healed > 0) ctx.player.setHealth(Math.min(maxHp(ctx.player), ctx.player.getHealth() + Math.min(8, healed)));
                    });
                }));

        // ---- GUARDIAN ---------------------------------------------------------------------------
        s.add(new SimpleSkill("guard_heal", "Healing Pulse", SkillType.ACTIVE, Material.GOLDEN_APPLE, 6,
                p -> List.of("<gray>Pulse of light: heal yourself + nearby allies " + (int) (4 * scale(p, .1)) + " HP + Regen."),
                ctx -> {
                    int heal = (int) ctx.scale(4, .1);
                    healAllies(ctx, 6, heal);
                    ctx.buffAllies(6, PotionEffectType.REGENERATION, 4, 0);
                    nova(ctx, "heart", 16); ctx.sound(ctx.player.getLocation(), "block_beacon_activate", 0.7f, 1.6f);
                }));
        s.add(new SimpleSkill("guard_rally", "Rally", SkillType.SPECIAL, Material.BELL, 25,
                p -> List.of("<gray>Inspire allies within 10: Strength + Speed + Resistance (" + (8 + p * 2) + "s)."),
                ctx -> {
                    int dur = 8 + ctx.prestige * 2;
                    ctx.buffAllies(10, PotionEffectType.STRENGTH, dur, 0);
                    ctx.buffAllies(10, PotionEffectType.SPEED, dur, 0);
                    ctx.buffAllies(10, PotionEffectType.RESISTANCE, dur, 0);
                    nova(ctx, "totem_of_undying", 30); ctx.sound(ctx.player.getLocation(), "block_bell_use", 1.2f, 1.2f);
                }));
        s.add(new SimpleSkill("guard_sanctuary", "Sanctuary", SkillType.HYPER, Material.BEACON, 100,
                p -> List.of("<gray>Raise a sanctuary for 8s: allies inside get Regen II + Resistance II",
                        "<gray>+ Absorption; foes that enter are Weakened + Slowed."),
                ctx -> {
                    repeat(ctx, 20L, 8, () -> {
                        ctx.buffAllies(8, PotionEffectType.REGENERATION, 2, 1);
                        ctx.buffAllies(8, PotionEffectType.RESISTANCE, 2, 1);
                        ctx.buffAllies(8, PotionEffectType.ABSORPTION, 3, 1);
                        for (LivingEntity e : ctx.enemiesNear(ctx.player.getLocation(), 8)) { ctx.debuff(e, PotionEffectType.WEAKNESS, 2, 1); ctx.debuff(e, PotionEffectType.SLOWNESS, 2, 1); }
                        ring(ctx, ctx.player.getLocation(), 8, "end_rod");
                    });
                    ctx.sound(ctx.player.getLocation(), "block_beacon_power_select", 1f, 1f);
                }));

        return s;
    }

    // ---- shared skill helpers ------------------------------------------------------------------

    private static double scale(int prestige, double per) { return 1 + per * (prestige - 1); }
    private static int half(int prestige) { return (int) Math.floor(0.5 * (prestige - 1)); }
    private static String line(String label, int val) { return "<gray>" + label + ": <yellow>" + val; }

    private static double maxHp(Player p) {
        var a = p.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH);
        return a != null ? a.getValue() : 20;
    }

    private static Location around(Location c, double r, SkillContext ctx) {
        double a = ctx.rng.rangeDouble(0, Math.PI * 2), d = ctx.rng.rangeDouble(1, r);
        return c.clone().add(Math.cos(a) * d, 0, Math.sin(a) * d);
    }

    private static void beam(SkillContext ctx, Location to, String particle) {
        Location from = ctx.eye();
        Vector step = to.toVector().subtract(from.toVector());
        double len = step.length();
        if (len < 0.1) return;
        step.normalize().multiply(0.6);
        Location cur = from.clone();
        for (double i = 0; i < len; i += 0.6) { ctx.particle(cur, particle, 2, 0.02); cur.add(step); }
    }

    private static void nova(SkillContext ctx, String particle, int count) {
        ctx.particle(ctx.player.getLocation().add(0, 1, 0), particle, count, 1.2);
    }

    private static void ring(SkillContext ctx, Location c, double r, String particle) {
        for (int i = 0; i < 24; i++) {
            double a = i / 24.0 * Math.PI * 2;
            ctx.particle(c.clone().add(Math.cos(a) * r, 0.2, Math.sin(a) * r), particle, 2, 0.02);
        }
    }

    private static void healAllies(SkillContext ctx, double r, double amount) {
        ctx.player.setHealth(Math.min(maxHp(ctx.player), ctx.player.getHealth() + amount));
        for (Entity e : ctx.player.getNearbyEntities(r, r, r)) {
            if (e instanceof Player ally) ally.setHealth(Math.min(maxHp(ally), ally.getHealth() + amount));
        }
    }

    /** Force nearby vanilla enemies to target the caster (custom mobs are driven by our controller). */
    private static void taunt(SkillContext ctx, double r) {
        for (Entity e : ctx.player.getNearbyEntities(r, r, r)) {
            if (e instanceof Mob m && SkillContext.isEnemy(e, ctx.player)) m.setTarget(ctx.player);
        }
    }

    private static void later(SkillContext ctx, long delay, Runnable r) {
        ctx.plugin.getServer().getScheduler().runTaskLater(ctx.plugin, r, delay);
    }

    /** Run {@code r} {@code times}, every {@code period} ticks. */
    private static void repeat(SkillContext ctx, long period, int times, Runnable r) {
        ctx.plugin.getServer().getScheduler().runTaskTimer(ctx.plugin, new org.bukkit.scheduler.BukkitRunnable() {
            int left = times;
            @Override public void run() {
                if (left-- <= 0 || !ctx.player.isOnline()) { cancel(); return; }
                r.run();
            }
        }, 0L, period);
    }

    private static net.kyori.adventure.text.Component mm(String s) { return me.zygotecode.amazingmobs.util.Text.mm(s); }
}
