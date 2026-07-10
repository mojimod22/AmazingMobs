package me.zygotecode.amazingmobs.command.sub;

import me.zygotecode.amazingmobs.AmazingMobs;
import me.zygotecode.amazingmobs.area.SpawnFinder;
import me.zygotecode.amazingmobs.command.SubCommand;
import me.zygotecode.amazingmobs.util.Rng;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Teleports every online player into the arena, snapping each to a safe grounded spot near the arena
 * spawn (never in a wall, in the air, or underground — see {@link SpawnFinder#groundedSpot}).
 */
public final class TpAllSub implements SubCommand {

    @Override public String name() { return "tpall"; }
    @Override public String permission() { return "amazingmobs.horde.start"; }
    @Override public String usage() { return "tpall"; }
    @Override public String description() { return "Teleport all online players into the arena (grounded)"; }

    @Override
    public void execute(AmazingMobs plugin, CommandSender s, String[] args) {
        Location arena = plugin.arenaSpawn();
        if (arena == null) {
            plugin.messages().send(s, "<red>No arena set. Stand where players should spawn and run <yellow>/am setarenapos</yellow> first.");
            return;
        }
        Rng rng = Rng.shared();
        int n = 0;
        double spread = Math.min(6.0, 1.5 + plugin.getServer().getOnlinePlayers().size() * 0.4);
        for (Player p : plugin.getServer().getOnlinePlayers()) {
            Location spot = SpawnFinder.groundedSpot(arena, spread, 5, 24, rng);
            p.teleport(spot != null ? spot : arena);
            n++;
        }
        plugin.messages().send(s, "<green>Teleported <white>" + n + "</white> player(s) into the arena.");
    }

    @Override
    public List<String> tab(AmazingMobs plugin, CommandSender sender, String[] args) { return List.of(); }
}
