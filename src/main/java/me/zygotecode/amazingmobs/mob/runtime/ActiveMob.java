package me.zygotecode.amazingmobs.mob.runtime;

import me.zygotecode.amazingmobs.api.event.BossPhaseChangeEvent;
import me.zygotecode.amazingmobs.api.event.SkillTriggerEvent;
import me.zygotecode.amazingmobs.mob.AiProfile;
import me.zygotecode.amazingmobs.mob.MobDefinition;
import me.zygotecode.amazingmobs.mob.Phase;
import me.zygotecode.amazingmobs.mob.StatBlock;
import me.zygotecode.amazingmobs.skill.SkillContext;
import me.zygotecode.amazingmobs.skill.SkillDefinition;
import me.zygotecode.amazingmobs.skill.SkillInstance;
import me.zygotecode.amazingmobs.skill.SkillRegistry;
import me.zygotecode.amazingmobs.skill.SummonFunction;
import me.zygotecode.amazingmobs.skill.Skill;
import me.zygotecode.amazingmobs.skill.TargetRule;
import me.zygotecode.amazingmobs.skill.Targeting;
import me.zygotecode.amazingmobs.skill.TriggerSpec;
import me.zygotecode.amazingmobs.skill.TriggerType;
import me.zygotecode.amazingmobs.trait.Trait;
import me.zygotecode.amazingmobs.trait.TraitContext;
import me.zygotecode.amazingmobs.trait.TraitDefinition;
import me.zygotecode.amazingmobs.trait.TraitInstance;
import me.zygotecode.amazingmobs.trait.TraitRegistry;
import me.zygotecode.amazingmobs.util.Fx;
import me.zygotecode.amazingmobs.util.Rng;
import me.zygotecode.amazingmobs.util.Schedulers;
import me.zygotecode.amazingmobs.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Bat;
import org.bukkit.entity.Ghast;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Phantom;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vex;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Live state + behaviour controller for one spawned custom mob. Ticked on a shared throttled task
 * (never its own runnable). All work is bounded and wrapped so one mob can never stall the loop.
 *
 * <p>Responsibilities: configurable target selection, skill scheduling (TICK + event triggers),
 * health-phase transitions, custom regen, leash/retreat, day-burn, and an optional boss bar.</p>
 */
public final class ActiveMob {

    private final Plugin plugin;
    private final LivingEntity entity;
    private final MobDefinition def;
    private final SpawnMeta meta;
    private final SummonFunction summon;
    private final Rng rng = Rng.shared();
    private final List<SkillInstance> skills = new ArrayList<>();
    private final List<TraitInstance> traits = new ArrayList<>();
    private final TraitContext traitCtx;
    private final StatBlock base;        // post-scaling base, for phase recompute
    private final Location anchor;       // spawn point, for leashing
    private final int periodTicks;

    private int currentPhase = -1;
    private BossBar bossBar;
    private long now;                 // current controller tick (set each tick)
    private long lastPathTick;        // last time we re-issued navigation
    private Location lastPathGoal;    // last navigation goal (to throttle re-pathing)
    private Location lastPos;         // last position (anti-stuck)
    private int stuckTicks;           // consecutive controller passes with no progress

    public ActiveMob(Plugin plugin, LivingEntity entity, MobDefinition def, SpawnMeta meta,
                     SkillRegistry registry, TraitRegistry traitRegistry, SummonFunction summon, int periodTicks) {
        this.plugin = plugin;
        this.entity = entity;
        this.def = def;
        this.meta = meta;
        this.summon = summon;
        this.periodTicks = Math.max(1, periodTicks);
        this.base = def.scaling().apply(def.stats(), meta.playerCount(), meta.difficulty());
        this.anchor = entity.getLocation();
        for (SkillDefinition sd : def.skills()) {
            Skill skill = registry.get(sd.skillId());
            if (skill != null) skills.add(new SkillInstance(sd, skill));
        }
        for (TraitDefinition td : def.traits()) {
            Trait t = traitRegistry.get(td.id());
            if (t != null) traits.add(new TraitInstance(td, t));
        }
        this.traitCtx = new TraitContext(plugin, entity, def, summon, rng, this.periodTicks);
        if (entity instanceof Mob mob && shouldClearGoals()) clearVanillaGoals(mob);
        if (def.presentation().bossBar()) setupBossBar();
    }

    /** Normally-passive/ambient bases whose vanilla AI (wander/panic/flee/erratic-fly) only fights our
     *  controller. When made aggressive these are auto-puppeted; hostiles/golems keep their combat AI. */
    private static final java.util.Set<org.bukkit.entity.EntityType> PASSIVE_BASES = java.util.Set.of(
            org.bukkit.entity.EntityType.VILLAGER, org.bukkit.entity.EntityType.WANDERING_TRADER,
            org.bukkit.entity.EntityType.CHICKEN, org.bukkit.entity.EntityType.COW,
            org.bukkit.entity.EntityType.MOOSHROOM, org.bukkit.entity.EntityType.SHEEP,
            org.bukkit.entity.EntityType.PIG, org.bukkit.entity.EntityType.RABBIT,
            org.bukkit.entity.EntityType.BAT, org.bukkit.entity.EntityType.CAT,
            org.bukkit.entity.EntityType.OCELOT, org.bukkit.entity.EntityType.FOX,
            org.bukkit.entity.EntityType.BEE, org.bukkit.entity.EntityType.ALLAY,
            org.bukkit.entity.EntityType.PARROT, org.bukkit.entity.EntityType.GOAT,
            org.bukkit.entity.EntityType.HORSE, org.bukkit.entity.EntityType.DONKEY,
            org.bukkit.entity.EntityType.SKELETON_HORSE);

    private boolean shouldClearGoals() {
        if (def.ai().aggression() == AiProfile.Aggression.PASSIVE) return false;
        return def.ai().clearVanillaGoals() || PASSIVE_BASES.contains(entity.getType());
    }

    /**
     * Strip vanilla MOVE/TARGET/LOOK goals so this mob becomes a pure puppet of our controller —
     * no wandering, no panic, no flee, no erratic flight. Used for normally-passive/awkward bases
     * (villagers, chickens, cows, bats); our pursuit + skills then drive everything. Version-safe.
     */
    private void clearVanillaGoals(Mob mob) {
        try {
            var goals = Bukkit.getMobGoals();
            goals.removeAllGoals(mob, com.destroystokyo.paper.entity.ai.GoalType.MOVE);
            goals.removeAllGoals(mob, com.destroystokyo.paper.entity.ai.GoalType.TARGET);
            goals.removeAllGoals(mob, com.destroystokyo.paper.entity.ai.GoalType.LOOK);
        } catch (Throwable ignored) {
            // older/edge Paper builds: fall back to normal goal-driven behaviour
        }
    }

    // Trait hook kinds (avoids per-call lambda allocation on the hot tick path).
    private static final int H_SPAWN = 0, H_TICK = 1, H_DAMAGED = 2, H_ATTACK = 3, H_DEATH = 4;

    private void fireTraits(int kind, LivingEntity eventTarget, long tick) {
        if (traits.isEmpty()) return;
        double frac = healthFraction();
        String phase = phaseId();
        for (TraitInstance ti : traits) {
            traitCtx.prime(ti, tick, frac, phase, eventTarget);
            try {
                switch (kind) {
                    case H_SPAWN -> ti.trait().onSpawn(traitCtx);
                    case H_TICK -> ti.trait().onTick(traitCtx);
                    case H_DAMAGED -> ti.trait().onDamaged(traitCtx);
                    case H_ATTACK -> ti.trait().onAttack(traitCtx);
                    case H_DEATH -> ti.trait().onDeath(traitCtx);
                }
            } catch (Throwable t) {
                plugin.getLogger().warning("[trait] '" + ti.definition().id() + "' on '" + def.id() + "' failed: " + t);
            }
        }
    }

    public LivingEntity entity() { return entity; }
    public MobDefinition definition() { return def; }
    public SpawnMeta meta() { return meta; }
    public boolean isValid() { return entity != null && entity.isValid() && !entity.isDead(); }

    // ---- lifecycle hooks -----------------------------------------------------------------------

    public void onSpawn(long tick) {
        for (SkillInstance si : skills) {
            if (si.trigger().hasTrigger(TriggerType.ON_SPAWN)) tryCast(si, TriggerType.ON_SPAWN, null, tick);
        }
        fireTraits(H_SPAWN, null, tick);
    }

    public void onDamaged(LivingEntity attacker, long tick) {
        for (SkillInstance si : skills) {
            if (!si.disabledByPhase() && si.trigger().hasTrigger(TriggerType.ON_DAMAGED)) {
                tryCast(si, TriggerType.ON_DAMAGED, attacker, tick);
            }
        }
        fireTraits(H_DAMAGED, attacker, tick);
    }

    public void onAttack(LivingEntity victim, long tick) {
        for (SkillInstance si : skills) {
            if (!si.disabledByPhase() && si.trigger().hasTrigger(TriggerType.ON_ATTACK)) {
                tryCast(si, TriggerType.ON_ATTACK, victim, tick);
            }
        }
        fireTraits(H_ATTACK, victim, tick);
    }

    public void onDeath(long tick) {
        for (SkillInstance si : skills) {
            if (si.trigger().hasTrigger(TriggerType.ON_DEATH)) tryCast(si, TriggerType.ON_DEATH, null, tick);
        }
        fireTraits(H_DEATH, null, tick);
        cleanup();
    }

    public void cleanup() {
        if (bossBar != null) { bossBar.removeAll(); bossBar = null; }
    }

    /** Runtime trait/skill injection (for /am give-trait, /am give-skill live testing). */
    public void addRuntimeTrait(me.zygotecode.amazingmobs.trait.TraitDefinition td, me.zygotecode.amazingmobs.trait.Trait t) {
        traits.add(new TraitInstance(td, t));
    }

    public void addRuntimeSkill(SkillDefinition sd, Skill sk) {
        skills.add(new SkillInstance(sd, sk));
    }

    public int skillCount() { return skills.size(); }
    public int traitCount() { return traits.size(); }

    // ---- main tick -----------------------------------------------------------------------------

    public void tick(long tick) {
        if (!isValid()) return;
        this.now = tick;
        AiProfile ai = def.ai();
        double frac = healthFraction();

        regen();
        if (ai.burnsInDay()) dayBurn();
        pursue(ai, frac);                 // real navigation: acquire target, face it, chase/kite/strafe/flee/leash
        if (def.hasPhases()) checkPhase(frac);
        ambient();
        updateBossBar(frac);

        // Continuous combat pressure: every ready TICK skill may fire (each gated by its own
        // cooldown/chance), capped per pass so a many-skill mob can't burst everything at once.
        LivingEntity current = currentTarget();
        fireTraits(H_TICK, current, tick);
        int casts = 0;
        for (SkillInstance si : skills) {
            if (si.disabledByPhase()) continue;
            boolean tickTrig = si.trigger().hasTrigger(TriggerType.TICK);
            boolean lowTrig = si.trigger().hasTrigger(TriggerType.ON_LOW_HEALTH);
            if (!tickTrig && !lowTrig) continue;
            if (lowTrig) {
                if (!si.oneShotFired() && frac <= si.trigger().maxHealthPct()) {
                    tryCast(si, TriggerType.ON_LOW_HEALTH, current, tick);
                }
                continue;
            }
            if (casts >= 2) continue;     // allow e.g. a melee + one ability per pass
            if (tryCast(si, TriggerType.TICK, current, tick)) casts++;
        }
    }

    // ---- skill casting -------------------------------------------------------------------------

    private boolean tryCast(SkillInstance si, TriggerType reason, LivingEntity eventTarget, long tick) {
        TriggerSpec tr = si.trigger();
        boolean oneShot = reason == TriggerType.ON_SPAWN || reason == TriggerType.ON_DEATH || reason == TriggerType.ON_LOW_HEALTH;
        if (oneShot && si.oneShotFired()) return false;
        if (!si.offCooldown(tick)) return false;

        LivingEntity current = currentTarget();
        LivingEntity ruleBase = ((reason == TriggerType.ON_DAMAGED || reason == TriggerType.ON_ATTACK) && eventTarget != null)
                ? eventTarget : current;
        List<LivingEntity> targets = Targeting.resolve(entity, ruleBase, tr.targetRule(),
                tr.radius(), tr.maxRange(), def.ai().targetPlayersOnly());
        LivingEntity primary = targets.isEmpty() ? null : targets.get(0);

        double dist = -1;
        if (tr.targetRule() != TargetRule.SELF) {
            if (primary == null) return false;          // needs a target but none in range
            dist = primary.getLocation().distance(entity.getLocation());
        }
        if (!tr.conditionsMet(healthFraction(), phaseId(), dist)) return false;
        if (!rng.chance(tr.chance())) return false;

        si.putOnCooldown(tick);
        if (oneShot) si.markOneShotFired();
        scheduleCast(si, primary, targets, reason == TriggerType.ON_DEATH);
        return true;
    }

    private void scheduleCast(SkillInstance si, LivingEntity primary, List<LivingEntity> targets, boolean force) {
        long warmup = si.trigger().warmupTicks();
        if (warmup > 0 && !force) {
            // telegraph so players can react
            Fx.particle(entity.getLocation().add(0, 1, 0), "angry_villager", 6, 0.4, 0.4, 0.4, 0.01);
            Fx.sound(entity.getLocation(), "block_note_block_pling", 0.6f, 0.6f);
            Schedulers.later(plugin, warmup, () -> doCast(si, primary, targets, false));
        } else {
            doCast(si, primary, targets, force);
        }
    }

    private void doCast(SkillInstance si, LivingEntity primary, List<LivingEntity> targets, boolean force) {
        if (entity == null) return;
        if (!force && !isValid()) return;
        SkillContext ctx = new SkillContext(plugin, entity, def, primary, targets,
                si.definition().params(), si.trigger(), rng, phaseId(), summon);
        try {
            si.skill().cast(ctx);
            Bukkit.getPluginManager().callEvent(new SkillTriggerEvent(entity, def, si.definition()));
        } catch (Throwable t) {
            plugin.getLogger().warning("[skill] '" + si.skill().id() + "' on mob '" + def.id() + "' failed: " + t);
        }
    }

    // ---- behaviour helpers ---------------------------------------------------------------------

    private void regen() {
        double perSec = base.regenPerSecond();
        if (perSec <= 0) return;
        AttributeInstance max = entity.getAttribute(Attribute.MAX_HEALTH);
        double cap = max != null ? max.getValue() : entity.getHealth();
        double add = perSec * (periodTicks / 20.0);
        if (entity.getHealth() < cap) entity.setHealth(Math.min(cap, entity.getHealth() + add));
    }

    private void dayBurn() {
        if (entity.getWorld().isDayTime() && entity.getLocation().getBlock().getLightFromSky() >= 14) {
            entity.setFireTicks(Math.max(entity.getFireTicks(), periodTicks + 20));
        }
    }

    /**
     * Drives target acquisition, facing and movement using the entity's real navigation
     * ({@link Mob#getPathfinder()}) so custom mobs — even normally-passive ones (chickens, cows,
     * villagers) — chase smoothly and look natural instead of jittering via velocity nudges.
     * Free-flyers (bats/phantoms/ghasts/vexes) use smooth velocity steering since they ignore the
     * ground navigator.
     */
    private void pursue(AiProfile ai, double frac) {
        if (!(entity instanceof Mob mob)) return;
        if (ai.aggression() == AiProfile.Aggression.PASSIVE) return;

        // Leash: drag back toward the spawn anchor if pulled too far.
        if (ai.leashRange() > 0 && anchor.getWorld() != null && anchor.getWorld().equals(entity.getWorld())
                && entity.getLocation().distance(anchor) > ai.leashRange()) {
            mob.setTarget(null);
            if (isFreeFlyer()) steerToward(anchor, ai.chaseSpeed());
            else mob.getPathfinder().moveTo(anchor, ai.chaseSpeed());
            return;
        }

        LivingEntity target = acquire(ai, mob);
        if (target == null) { if (!isFreeFlyer()) mob.getPathfinder().stopPathfinding(); return; }
        mob.lookAt(target);

        double dist = entity.getLocation().distance(target.getLocation());
        double speed = ai.chaseSpeed();
        boolean flyer = isFreeFlyer();

        if (ai.retreatHealthPct() > 0 && frac <= ai.retreatHealthPct()) { moveAwayFrom(target, speed * 1.15, flyer); return; }

        switch (ai.movement()) {
            case STATIONARY -> { if (flyer) hover(); else mob.getPathfinder().stopPathfinding(); }
            case KITE -> {
                double k = ai.kiteDistance();
                if (dist < k - 1) moveAwayFrom(target, speed, flyer);
                else if (dist > k + 3) goTo(mob, target, speed, flyer);
                else if (!flyer) mob.getPathfinder().stopPathfinding(); // hold position and attack/shoot
            }
            case STRAFE -> strafe(mob, target, ai, speed, flyer);
            default -> goTo(mob, target, speed, flyer); // CHASE, AMBUSH
        }
    }

    /** Sticky target acquisition (keeps the current target until it dies/leaves an extended range). */
    private LivingEntity acquire(AiProfile ai, Mob mob) {
        double range = ai.aggroRange() > 0 ? ai.aggroRange() : (base.overridesFollowRange() ? base.followRange() : 24);
        LivingEntity cur = mob.getTarget();
        double keepSq = (range * 1.6) * (range * 1.6);
        if (cur instanceof Player p && targetable(p) && p.getWorld().equals(entity.getWorld())
                && p.getLocation().distanceSquared(entity.getLocation()) <= keepSq) {
            return cur;
        }
        List<Player> candidates = new ArrayList<>();
        Location loc = entity.getLocation();
        double sq = range * range;
        for (Player p : entity.getWorld().getPlayers()) {
            if (targetable(p) && p.getLocation().distanceSquared(loc) <= sq) candidates.add(p);
        }
        if (candidates.isEmpty()) return null;
        Player chosen = pickByPriority(candidates, ai);
        if (chosen != null) mob.setTarget(chosen);
        return chosen;
    }

    private boolean targetable(Player p) {
        return p.isValid() && !p.isDead()
                && p.getGameMode() != GameMode.CREATIVE && p.getGameMode() != GameMode.SPECTATOR;
    }

    private boolean isFreeFlyer() {
        return entity instanceof Bat || entity instanceof Phantom || entity instanceof Ghast || entity instanceof Vex;
    }

    /** Path toward the target (ground) or steer toward it (flyer), throttling expensive re-paths. */
    private void goTo(Mob mob, LivingEntity target, double speed, boolean flyer) {
        if (flyer) { steerToward(target.getEyeLocation(), speed); return; }
        Location goal = target.getLocation();
        boolean repath = lastPathGoal == null || !goal.getWorld().equals(lastPathGoal.getWorld())
                || lastPathGoal.distanceSquared(goal) > 2.25 || !mob.getPathfinder().hasPath()
                || now - lastPathTick > 20;

        // anti-stuck: if we've made no progress while the target is far, jump a lip + force a repath
        Location cur = entity.getLocation();
        if (lastPos != null && lastPos.getWorld() != null && lastPos.getWorld().equals(cur.getWorld())
                && lastPos.distanceSquared(cur) < 0.09 && cur.distanceSquared(goal) > 9) {
            if (++stuckTicks >= 3) { mob.setJumping(true); repath = true; stuckTicks = 0; }
        } else {
            stuckTicks = 0;
        }
        lastPos = cur.clone();

        if (repath) {
            mob.getPathfinder().moveTo(goal, speed);
            lastPathGoal = goal.clone();
            lastPathTick = now;
        }
    }

    private void moveAwayFrom(LivingEntity target, double speed, boolean flyer) {
        Vector away = entity.getLocation().toVector().subtract(target.getLocation().toVector());
        if (away.lengthSquared() < 1.0E-4) away = new Vector(1, 0, 1);
        Location dest = entity.getLocation().add(away.normalize().multiply(6));
        if (flyer) steerToward(dest, speed);
        else if (entity instanceof Mob m) m.getPathfinder().moveTo(dest, speed);
    }

    private void strafe(Mob mob, LivingEntity target, AiProfile ai, double speed, boolean flyer) {
        double radius = Math.max(4, ai.kiteDistance());
        Vector toMob = entity.getLocation().toVector().subtract(target.getLocation().toVector()).setY(0);
        if (toMob.lengthSquared() < 1.0E-4) toMob = new Vector(1, 0, 0);
        double ang = Math.atan2(toMob.getZ(), toMob.getX()) + 0.45; // rotate to circle the target
        Location dest = target.getLocation().clone().add(Math.cos(ang) * radius, 0, Math.sin(ang) * radius);
        if (flyer) { dest.setY(target.getEyeLocation().getY() + 1); steerToward(dest, speed); }
        else mob.getPathfinder().moveTo(dest, speed);
    }

    /** Smooth velocity steering for free-flyers (blends with current momentum to avoid jitter). */
    private void steerToward(Location dest, double speed) {
        Vector to = dest.toVector().subtract(entity.getLocation().toVector());
        if (to.lengthSquared() < 0.04) return;
        to.normalize().multiply(0.22 * speed);
        entity.setVelocity(entity.getVelocity().multiply(0.6).add(to.multiply(0.4)));
    }

    private void hover() {
        Vector v = entity.getVelocity();
        entity.setVelocity(new Vector(v.getX() * 0.5, Math.min(v.getY(), 0.02), v.getZ() * 0.5));
    }

    private Player pickByPriority(List<Player> players, AiProfile ai) {
        AiProfile.TargetMode mode = ai.targetPriority().isEmpty()
                ? AiProfile.TargetMode.NEAREST : ai.targetPriority().get(0);
        Location loc = entity.getLocation();
        return switch (mode) {
            case LOWEST_HEALTH -> players.stream().min((a, b) -> Double.compare(a.getHealth(), b.getHealth())).orElse(null);
            case HIGHEST_HEALTH -> players.stream().max((a, b) -> Double.compare(a.getHealth(), b.getHealth())).orElse(null);
            case RANDOM -> players.get(rng.rangeInt(0, players.size() - 1));
            case MOST_ARMORED -> players.stream().max((a, b) -> Double.compare(armor(a), armor(b))).orElse(null);
            case LEAST_ARMORED -> players.stream().min((a, b) -> Double.compare(armor(a), armor(b))).orElse(null);
            default -> players.stream().min((a, b) ->
                    Double.compare(a.getLocation().distanceSquared(loc), b.getLocation().distanceSquared(loc))).orElse(null);
        };
    }

    private static double armor(Player p) {
        AttributeInstance a = p.getAttribute(Attribute.ARMOR);
        return a != null ? a.getValue() : 0;
    }

    private void ambient() {
        String particle = def.presentation().ambientParticle();
        String sound = def.presentation().ambientSound();
        if (particle != null && !particle.isBlank()) Fx.particle(entity.getLocation().add(0, 1, 0), particle, 4, 0.3, 0.5, 0.3, 0.01);
        if (sound != null && !sound.isBlank() && rng.chance(0.15)) Fx.sound(entity.getLocation(), sound, 0.7f, 1.0f);
    }

    // ---- phases --------------------------------------------------------------------------------

    private void checkPhase(double frac) {
        List<Phase> phases = def.phases();
        int target = currentPhase;
        for (int i = 0; i < phases.size(); i++) {
            if (frac <= phases.get(i).thresholdPct()) target = i;
        }
        if (target > currentPhase) enterPhase(target);
    }

    private void enterPhase(int index) {
        currentPhase = index;
        Phase p = def.phases().get(index);
        setAttr(Attribute.ATTACK_DAMAGE, base.attackDamage() * p.damageMultiplier());
        if (base.overridesMovementSpeed()) setAttr(Attribute.MOVEMENT_SPEED, base.movementSpeed() * p.speedMultiplier());
        setAttr(Attribute.ARMOR, base.armor() * p.defenseMultiplier());
        for (SkillInstance si : skills) {
            String id = si.definition().skillId();
            if (p.disableSkills().contains(id)) si.setDisabledByPhase(true);
            else if (p.enableSkills().contains(id)) si.setDisabledByPhase(false);
        }
        if (p.message() != null && !p.message().isBlank()) broadcastNearby(p.message());
        Fx.sound(entity.getLocation(), p.sound(), 1f, 1f);
        Fx.particle(entity.getLocation().add(0, 1, 0), p.particle(), 40, 0.6, 0.8, 0.6, 0.05);
        Bukkit.getPluginManager().callEvent(new BossPhaseChangeEvent(entity, def, p.id(), index));
    }

    private void broadcastNearby(String mini) {
        var comp = Text.mm(mini);
        for (Player p : entity.getWorld().getPlayers()) {
            if (p.getLocation().distanceSquared(entity.getLocation()) <= 64 * 64) p.sendMessage(comp);
        }
    }

    // ---- boss bar ------------------------------------------------------------------------------

    private void setupBossBar() {
        BarColor color;
        try { color = BarColor.valueOf(def.presentation().bossBarColor().toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException e) { color = BarColor.RED; }
        String title = def.presentation().bossBarTitle() != null
                ? def.presentation().bossBarTitle() : def.displayName();
        bossBar = Bukkit.createBossBar(Text.legacy(title), color, BarStyle.SOLID);
        bossBar.setProgress(1.0);
    }

    private void updateBossBar(double frac) {
        if (bossBar == null) return;
        bossBar.setProgress(Math.max(0, Math.min(1, frac)));
        Location loc = entity.getLocation();
        // add nearby players, drop far ones — bounded by players in this world
        for (Player p : new ArrayList<>(bossBar.getPlayers())) {
            if (!p.getWorld().equals(entity.getWorld()) || p.getLocation().distanceSquared(loc) > 48 * 48) bossBar.removePlayer(p);
        }
        for (Player p : entity.getWorld().getPlayers()) {
            if (p.getLocation().distanceSquared(loc) <= 48 * 48 && !bossBar.getPlayers().contains(p)) bossBar.addPlayer(p);
        }
    }

    // ---- utils ---------------------------------------------------------------------------------

    private LivingEntity currentTarget() {
        return entity instanceof Mob m ? m.getTarget() : null;
    }

    private double healthFraction() {
        AttributeInstance max = entity.getAttribute(Attribute.MAX_HEALTH);
        double m = max != null ? max.getValue() : entity.getHealth();
        return m <= 0 ? 1 : entity.getHealth() / m;
    }

    private String phaseId() {
        return currentPhase >= 0 ? def.phases().get(currentPhase).id() : null;
    }

    private void setAttr(Attribute attr, double value) {
        AttributeInstance inst = entity.getAttribute(attr);
        if (inst != null) inst.setBaseValue(value);
    }
}
