package me.zygotecode.amazingmobs.weapon;

import me.zygotecode.amazingmobs.util.Keys;
import me.zygotecode.amazingmobs.util.Text;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.entity.WitherSkull;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * "AK-47" — a golden-axe-shaped semi-automatic rifle that deals <b>no melee damage</b> and instead
 * fires powerful homing-free wither skulls on right-click. Each skull hits hard, triggers a medium
 * explosion + fire on impact, and has a 20% chance to also call lightning. Holds 32 rounds; when empty
 * it auto-reloads after 60s. Ammo (remaining/total) is shown live in the item's name and lore.
 *
 * <p>Ammo state lives in the item's PDC ({@link Keys#AK_AMMO} + {@link Keys#AK_RELOAD_AT}), so it
 * travels with the item itself.</p>
 */
public final class AkRifle {

    public static final int AMMO_MAX = 32;
    public static final long RELOAD_MS = 60_000L;
    private static final double SKULL_SPEED = 2.4;

    private AkRifle() {}

    // ---- item -----------------------------------------------------------------------------------

    public static ItemStack build() {
        ItemStack is = new ItemStack(Material.GOLDEN_AXE);
        write(is, AMMO_MAX, 0L);
        return is;
    }

    public static boolean isAk(ItemStack is) {
        if (is == null || is.getType() != Material.GOLDEN_AXE || !is.hasItemMeta()) return false;
        return is.getItemMeta().getPersistentDataContainer().has(Keys.AK_WEAPON, PersistentDataType.BYTE);
    }

    public static int ammoOf(ItemStack is) {
        if (is == null || !is.hasItemMeta()) return 0;
        return is.getItemMeta().getPersistentDataContainer().getOrDefault(Keys.AK_AMMO, PersistentDataType.INTEGER, 0);
    }

    private static long reloadAtOf(ItemStack is) {
        if (is == null || !is.hasItemMeta()) return 0L;
        return is.getItemMeta().getPersistentDataContainer().getOrDefault(Keys.AK_RELOAD_AT, PersistentDataType.LONG, 0L);
    }

    /** Rewrite an AK item's PDC state + name/lore to reflect the given ammo/reload state. */
    private static void write(ItemStack is, int ammo, long reloadAt) {
        ItemMeta m = is.getItemMeta();
        if (m == null) return;
        PersistentDataContainer pdc = m.getPersistentDataContainer();
        pdc.set(Keys.AK_WEAPON, PersistentDataType.BYTE, (byte) 1);
        pdc.set(Keys.AK_AMMO, PersistentDataType.INTEGER, Math.max(0, ammo));
        pdc.set(Keys.AK_RELOAD_AT, PersistentDataType.LONG, reloadAt);

        boolean reloading = ammo <= 0 && reloadAt > 0;
        m.displayName(Text.item("<gold><bold>AK-47</bold></gold> <gray>[<yellow>" + ammo + "</yellow><gray>/" + AMMO_MAX + "]"));
        List<Component> lore = new ArrayList<>();
        lore.add(Text.item("<gray>Semi-auto <dark_red>wither-skull</dark_red> rifle."));
        lore.add(Text.item("<gray>Ammo: <yellow>" + ammo + " <gray>/ <white>" + AMMO_MAX));
        if (reloading) {
            long left = Math.max(1, (reloadAt - System.currentTimeMillis() + 999) / 1000);
            lore.add(Text.item("<red>Reloading… <white>" + left + "s"));
        } else {
            lore.add(Text.item("<dark_gray>Right-click to fire."));
        }
        lore.add(Text.item("<dark_gray>Reloads in 60s when empty."));
        m.lore(lore);

        // cosmetic glint + clean tooltip (its melee damage is suppressed in the listener)
        try { m.setEnchantmentGlintOverride(true); } catch (Throwable ignored) {}
        m.setUnbreakable(true);
        try { m.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES, org.bukkit.inventory.ItemFlag.HIDE_UNBREAKABLE); } catch (Throwable ignored) {}
        is.setItemMeta(m);
    }

    // ---- firing ---------------------------------------------------------------------------------

    /** Handle a right-click with the AK in {@code hand}. Returns the (possibly mutated) item to store. */
    public static void fire(Plugin plugin, Player p, ItemStack item, EquipmentSlot hand) {
        int ammo = ammoOf(item);
        long now = System.currentTimeMillis();
        long reloadAt = reloadAtOf(item);

        if (ammo <= 0) {
            if (reloadAt > 0 && now >= reloadAt) {       // lazy reload (in case the scheduled task missed it)
                ammo = AMMO_MAX; reloadAt = 0L;
            } else {
                long left = Math.max(1, (reloadAt - now + 999) / 1000);
                p.sendActionBar(Text.item("<red>AK-47 reloading… <white>" + left + "s"));
                p.playSound(p.getLocation(), Sound.BLOCK_DISPENSER_FAIL, 1f, 1.4f);
                return;
            }
        }

        ammo--;
        launchSkull(plugin, p);
        p.playSound(p.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.5f, 1.7f);
        p.playSound(p.getLocation(), Sound.ITEM_CROSSBOW_SHOOT, 1f, 0.8f);

        if (ammo <= 0) { reloadAt = now + RELOAD_MS; scheduleReload(plugin, p.getUniqueId()); }
        write(item, ammo, reloadAt);
        if (hand == EquipmentSlot.OFF_HAND) p.getInventory().setItemInOffHand(item);
        else p.getInventory().setItemInMainHand(item);

        if (ammo > 0) p.sendActionBar(Text.item("<gold>AK-47 <gray>[<yellow>" + ammo + "<gray>/" + AMMO_MAX + "]"));
        else p.sendActionBar(Text.item("<red>AK-47 empty — reloading (60s)…"));
    }

    private static void launchSkull(Plugin plugin, Player p) {
        Vector dir = p.getEyeLocation().getDirection().normalize();
        WitherSkull skull = p.launchProjectile(WitherSkull.class, dir.clone().multiply(SKULL_SPEED), s -> {
            s.setCharged(true);                          // blue = stronger
            s.setShooter(p);
            s.setGravity(false);                         // no drop
            s.setAcceleration(new Vector(0, 0, 0));      // no vanilla self-acceleration
            s.getPersistentDataContainer().set(Keys.AK_SKULL, PersistentDataType.BYTE, (byte) 1);
        });
        // hand control to a per-tick driver: fixed straight ray, constant speed, immune to explosions
        if (skull != null) new AkProjectile(skull, p, dir).runTaskTimer(plugin, 1L, 1L);
    }

    // ---- reload ---------------------------------------------------------------------------------

    private static void scheduleReload(Plugin plugin, UUID playerId) {
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            Player p = plugin.getServer().getPlayer(playerId);
            if (p == null) return;
            boolean any = false;
            PlayerInventory inv = p.getInventory();
            ItemStack[] all = inv.getContents();
            for (int i = 0; i < all.length; i++) {
                ItemStack it = all[i];
                if (isAk(it) && ammoOf(it) <= 0) { write(it, AMMO_MAX, 0L); inv.setItem(i, it); any = true; }
            }
            ItemStack off = inv.getItemInOffHand();
            if (isAk(off) && ammoOf(off) <= 0) { write(off, AMMO_MAX, 0L); inv.setItemInOffHand(off); any = true; }
            if (any) {
                p.sendActionBar(Text.item("<green>AK-47 reloaded — <white>" + AMMO_MAX + " rounds"));
                p.playSound(p.getLocation(), Sound.ITEM_CROSSBOW_LOADING_END, 1f, 1f);
            }
        }, RELOAD_MS / 50L); // ms → ticks
    }
}
