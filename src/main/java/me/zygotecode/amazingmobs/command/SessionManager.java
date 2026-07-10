package me.zygotecode.amazingmobs.command;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.Map;

/** Holds the in-progress {@link MobEditSession} per editor (player UUID, or "console"). Stale player
 *  sessions are dropped on quit. */
public final class SessionManager implements Listener {

    private final Map<String, MobEditSession> sessions = new HashMap<>();

    private static String key(CommandSender sender) {
        return sender instanceof Player p ? p.getUniqueId().toString() : "console";
    }

    public MobEditSession get(CommandSender sender) { return sessions.get(key(sender)); }
    public void put(CommandSender sender, MobEditSession session) { sessions.put(key(sender), session); }
    public void clear(CommandSender sender) { sessions.remove(key(sender)); }
    public boolean has(CommandSender sender) { return sessions.containsKey(key(sender)); }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        sessions.remove(e.getPlayer().getUniqueId().toString());
    }
}
