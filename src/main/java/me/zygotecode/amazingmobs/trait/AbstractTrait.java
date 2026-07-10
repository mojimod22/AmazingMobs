package me.zygotecode.amazingmobs.trait;

import me.zygotecode.amazingmobs.config.ConfigSection;
import me.zygotecode.amazingmobs.util.Keys;
import me.zygotecode.amazingmobs.util.Numbers;
import me.zygotecode.amazingmobs.util.Resolvers;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;

/** Base trait with param, cooldown, ally/player scan, and effect helpers. */
public abstract class AbstractTrait implements Trait {

    private final String id;

    protected AbstractTrait(String id) { this.id = id; }

    @Override public String id() { return id; }

    // --- params -------------------------------------------------------------------------------
    protected double d(TraitContext c, String k, double def) { return c.params().getDouble(k, def); }
    protected int i(TraitContext c, String k, int def) { return c.params().getInt(k, def); }
    protected boolean flag(TraitContext c, String k, boolean def) { return c.params().getBool(k, def); }
    protected String str(TraitContext c, String k, String def) { return c.params().getString(k, def); }

    /** Gate a periodic trait by its configured {@code cooldown} param. Returns true if it may fire now. */
    protected boolean ready(TraitContext c, String defaultCooldown) {
        long cd = Numbers.parseTicks(c.params().getString("cooldown"), Numbers.parseTicks(defaultCooldown, 100));
        if (!c.instance().offCooldown(c.tick())) return false;
        c.instance().putCooldown(c.tick(), cd);
        return true;
    }

    // --- scans (PDC-based, no MobManager dependency) --------------------------------------------

    /** Nearby custom mobs (tagged with {@link Keys#MOB_ID}), excluding self. */
    protected List<LivingEntity> allies(TraitContext c, double radius, boolean sameTypeOnly) {
        List<LivingEntity> out = new ArrayList<>();
        String selfId = c.entity().getPersistentDataContainer().get(Keys.MOB_ID, PersistentDataType.STRING);
        for (Entity e : c.entity().getNearbyEntities(radius, radius, radius)) {
            if (!(e instanceof LivingEntity le) || e.equals(c.entity()) || le.isDead()) continue;
            String tag = le.getPersistentDataContainer().get(Keys.MOB_ID, PersistentDataType.STRING);
            if (tag == null) continue;
            if (sameTypeOnly && (selfId == null || !selfId.equals(tag))) continue;
            out.add(le);
        }
        return out;
    }

    protected List<Player> players(TraitContext c, double radius) {
        List<Player> out = new ArrayList<>();
        double sq = radius * radius;
        for (Player p : c.world().getPlayers()) {
            if (!p.isDead() && p.getGameMode() != org.bukkit.GameMode.CREATIVE
                    && p.getGameMode() != org.bukkit.GameMode.SPECTATOR
                    && p.getLocation().distanceSquared(c.origin()) <= sq) out.add(p);
        }
        return out;
    }

    // --- effects --------------------------------------------------------------------------------

    protected void effect(LivingEntity target, String type, int amplifier, int durationTicks) {
        PotionEffectType pet = Resolvers.effect(type);
        if (pet != null && target != null && target.isValid()) {
            target.addPotionEffect(new PotionEffect(pet, durationTicks, Math.max(0, amplifier), false, true, true));
        }
    }

    /** Apply a list of {type,amplifier} effects from a params section to a target. */
    protected void effects(ConfigSection section, LivingEntity target, int durationTicks) {
        for (ConfigSection e : section.getSectionList("effects")) {
            effect(target, e.getString("type", ""), e.getInt("amplifier", 0),
                    e.contains("duration") ? (int) Numbers.parseTicks(e.getString("duration"), durationTicks) : durationTicks);
        }
    }

    protected void heal(LivingEntity target, double amount) {
        if (target == null || !target.isValid() || amount <= 0) return;
        AttributeInstance max = target.getAttribute(Attribute.MAX_HEALTH);
        double cap = max != null ? max.getValue() : target.getHealth();
        target.setHealth(Math.min(cap, target.getHealth() + amount));
    }
}
