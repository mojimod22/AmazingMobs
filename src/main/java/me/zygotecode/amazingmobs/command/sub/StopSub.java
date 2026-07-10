package me.zygotecode.amazingmobs.command.sub;

import me.zygotecode.amazingmobs.AmazingMobs;
import me.zygotecode.amazingmobs.command.SubCommand;
import me.zygotecode.amazingmobs.horde.runtime.HordeInstance;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;

public final class StopSub implements SubCommand {

    @Override public String name() { return "stop"; }
    @Override public String permission() { return "amazingmobs.horde.stop"; }
    @Override public String usage() { return "stop <instanceId|all>"; }
    @Override public String description() { return "Stop a running horde (or all)"; }

    @Override
    public void execute(AmazingMobs plugin, CommandSender s, String[] args) {
        if (args.length < 1) { plugin.messages().send(s, "<red>Usage: /am " + usage()); return; }
        if (args[0].equalsIgnoreCase("all")) {
            int n = plugin.hordeManager().stopAll();
            plugin.messages().send(s, "<green>Stopped <white>" + n + "</white> horde(s).");
            return;
        }
        boolean ok = plugin.hordeManager().stopInstance(args[0]);
        plugin.messages().send(s, ok ? "<green>Stopped <white>" + args[0] + "</white>."
                : "<red>No active horde instance '" + args[0] + "'.");
    }

    @Override
    public List<String> tab(AmazingMobs plugin, CommandSender sender, String[] args) {
        if (args.length == 1) {
            List<String> out = new ArrayList<>();
            out.add("all");
            for (HordeInstance inst : plugin.hordeManager().activeInstances()) out.add(inst.instanceId());
            return InfoSub.prefix(out, args[0]);
        }
        return List.of();
    }
}
