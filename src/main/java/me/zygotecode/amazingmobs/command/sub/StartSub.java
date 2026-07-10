package me.zygotecode.amazingmobs.command.sub;

import me.zygotecode.amazingmobs.AmazingMobs;
import me.zygotecode.amazingmobs.command.SubCommand;
import me.zygotecode.amazingmobs.horde.runtime.StartResult;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;

public final class StartSub implements SubCommand {

    @Override public String name() { return "start"; }
    @Override public String permission() { return "amazingmobs.horde.start"; }
    @Override public String usage() { return "start <hordeId> [force]"; }
    @Override public String description() { return "Start a horde at your location (force skips gating)"; }

    @Override
    public void execute(AmazingMobs plugin, CommandSender s, String[] args) {
        if (args.length < 1) { plugin.messages().send(s, "<red>Usage: /am " + usage()); return; }
        boolean force = args.length > 1 && args[1].equalsIgnoreCase("force");
        Location center = s instanceof Player p ? p.getLocation() : null;
        StartResult r = plugin.hordeManager().start(args[0], center, 0, force);
        if (r.ok()) {
            plugin.messages().send(s, "<green>Started horde <white>" + r.instance().definition().id()
                    + "</white> <gray>(instance " + r.instance().instanceId() + ")</gray>.");
        } else {
            plugin.messages().send(s, "<red>Could not start: " + r.error());
        }
    }

    @Override
    public List<String> tab(AmazingMobs plugin, CommandSender sender, String[] args) {
        if (args.length == 1) return InfoSub.prefix(plugin.hordeRegistry().ids(), args[0]);
        if (args.length == 2 && "force".startsWith(args[1].toLowerCase(Locale.ROOT))) return List.of("force");
        return List.of();
    }
}
