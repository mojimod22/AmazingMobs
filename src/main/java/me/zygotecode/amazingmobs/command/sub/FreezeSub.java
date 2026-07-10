package me.zygotecode.amazingmobs.command.sub;

import me.zygotecode.amazingmobs.AmazingMobs;
import me.zygotecode.amazingmobs.command.SubCommand;
import me.zygotecode.amazingmobs.horde.runtime.HordeInstance;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;

/** {@code freeze}/{@code unfreeze <instanceId|all>} — pause/resume horde instances. */
public final class FreezeSub implements SubCommand {

    private final boolean pause;

    public FreezeSub(boolean pause) { this.pause = pause; }

    @Override public String name() { return pause ? "freeze" : "unfreeze"; }
    @Override public String permission() { return "amazingmobs.horde.stop"; }
    @Override public String usage() { return (pause ? "freeze" : "unfreeze") + " <instanceId|all>"; }
    @Override public String description() { return (pause ? "Pause" : "Resume") + " a running horde"; }

    @Override
    public void execute(AmazingMobs plugin, CommandSender s, String[] args) {
        if (args.length < 1) { plugin.messages().send(s, "<red>Usage: /am " + usage()); return; }
        if (args[0].equalsIgnoreCase("all")) {
            int n = plugin.hordeManager().freezeAll(pause);
            plugin.messages().send(s, "<green>" + (pause ? "Paused" : "Resumed") + " <white>" + n + "</white> horde(s).");
            return;
        }
        boolean ok = plugin.hordeManager().freeze(args[0], pause);
        plugin.messages().send(s, ok ? "<green>" + (pause ? "Paused" : "Resumed") + " <white>" + args[0] + "</white>."
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
