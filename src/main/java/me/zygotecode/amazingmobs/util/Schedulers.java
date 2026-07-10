package me.zygotecode.amazingmobs.util;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

/**
 * Tiny wrapper over the Bukkit scheduler so call sites read clearly and we have one place to
 * adjust scheduling strategy later. (Folia is not targeted; {@code folia-supported: false}.)
 */
public final class Schedulers {

    private Schedulers() {}

    public static BukkitTask sync(Plugin plugin, Runnable r) {
        return Bukkit.getScheduler().runTask(plugin, r);
    }

    public static BukkitTask later(Plugin plugin, long delayTicks, Runnable r) {
        return Bukkit.getScheduler().runTaskLater(plugin, r, Math.max(1, delayTicks));
    }

    public static BukkitTask timer(Plugin plugin, long delayTicks, long periodTicks, Runnable r) {
        return Bukkit.getScheduler().runTaskTimer(plugin, r, Math.max(0, delayTicks), Math.max(1, periodTicks));
    }
}
