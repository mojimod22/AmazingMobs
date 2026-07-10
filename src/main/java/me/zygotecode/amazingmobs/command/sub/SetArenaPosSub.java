package me.zygotecode.amazingmobs.command.sub;

import me.zygotecode.amazingmobs.AmazingMobs;
import me.zygotecode.amazingmobs.command.SubCommand;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

/** Sets the single global arena spawn point to where the admin is standing (persisted to arena.yml). */
public final class SetArenaPosSub implements SubCommand {

    @Override public String name() { return "setarenapos"; }
    @Override public String permission() { return "amazingmobs.horde.start"; }
    @Override public String usage() { return "setarenapos"; }
    @Override public String description() { return "Set the arena spawn point to your position (for /am tpall)"; }

    @Override
    public void execute(AmazingMobs plugin, CommandSender s, String[] args) {
        if (!(s instanceof Player p)) { plugin.messages().send(s, "<red>Players only."); return; }
        Location loc = p.getLocation();
        plugin.setArenaSpawn(loc);
        plugin.messages().send(s, "<green>Arena spawn set to <white>"
                + loc.getWorld().getName() + " " + (int) loc.getX() + ", " + (int) loc.getY() + ", " + (int) loc.getZ()
                + "</white>. <gray>Use <yellow>/am tpall</yellow> to gather everyone here.</gray>");
    }

    @Override
    public List<String> tab(AmazingMobs plugin, CommandSender sender, String[] args) { return List.of(); }
}
