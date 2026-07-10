package me.zygotecode.amazingmobs.command.sub;

import me.zygotecode.amazingmobs.AmazingMobs;
import me.zygotecode.amazingmobs.command.SubCommand;
import me.zygotecode.amazingmobs.mob.runtime.SpawnMeta;
import me.zygotecode.amazingmobs.util.Numbers;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public final class SpawnSub implements SubCommand {

    @Override public String name() { return "spawn"; }
    @Override public String permission() { return "amazingmobs.mob.spawn"; }
    @Override public String usage() { return "spawn <mobId> [amount]"; }
    @Override public String description() { return "Spawn a custom mob where you look"; }

    @Override
    public void execute(AmazingMobs plugin, CommandSender s, String[] args) {
        if (!(s instanceof Player p)) { plugin.messages().send(s, "<red>Players only."); return; }
        if (args.length < 1) { plugin.messages().send(s, "<red>Usage: /am " + usage()); return; }
        String id = args[0];
        if (!plugin.mobRegistry().contains(id)) { plugin.messages().send(s, "<red>No mob '" + id + "'."); return; }
        int amount = Numbers.clamp(args.length > 1 ? Numbers.parseInt(args[1], 1) : 1, 1, 50);

        Location loc = targetLocation(p);
        int spawned = 0;
        for (int i = 0; i < amount; i++) {
            if (plugin.mobManager().spawn(id, loc, SpawnMeta.SOLO) != null) spawned++;
        }
        plugin.messages().send(s, "<green>Spawned <white>" + spawned + "</white>× <white>" + id + "</white>"
                + (spawned < amount ? " <yellow>(" + (amount - spawned) + " blocked by cap)</yellow>" : "") + ".");
    }

    static Location targetLocation(Player p) {
        Block tb = p.getTargetBlockExact(64);
        return tb != null ? tb.getLocation().add(0.5, 1, 0.5) : p.getLocation();
    }

    @Override
    public List<String> tab(AmazingMobs plugin, CommandSender sender, String[] args) {
        return args.length == 1 ? InfoSub.prefix(plugin.mobRegistry().ids(), args[0]) : List.of();
    }
}
