package me.zygotecode.amazingmobs.command.sub;

import me.zygotecode.amazingmobs.AmazingMobs;
import me.zygotecode.amazingmobs.command.SubCommand;
import me.zygotecode.amazingmobs.config.ReloadSummary;
import org.bukkit.command.CommandSender;

public final class ReloadSub implements SubCommand {

    @Override public String name() { return "reload"; }
    @Override public String permission() { return "amazingmobs.reload"; }
    @Override public String usage() { return "reload"; }
    @Override public String description() { return "Reload config and all definitions"; }

    @Override
    public void execute(AmazingMobs plugin, CommandSender s, String[] args) {
        long t0 = System.nanoTime();
        ReloadSummary sum = plugin.reloadDefinitions();
        double ms = (System.nanoTime() - t0) / 1_000_000.0;
        plugin.messages().send(s, "<green>Reloaded</green> <gray>·</gray> mobs <white>"
                + sum.mobs().loaded() + "/" + sum.mobs().total() + "</white>, hordes <white>"
                + sum.hordes().loaded() + "/" + sum.hordes().total() + "</white> <dark_gray>("
                + String.format(java.util.Locale.ROOT, "%.1f", ms) + "ms)</dark_gray>");
        if (sum.mobs().rejected() > 0 || sum.hordes().rejected() > 0
                || sum.mobs().totalWarnings() > 0 || sum.hordes().totalWarnings() > 0) {
            plugin.messages().send(s, "<yellow>Some files had issues — run <white>/am validate</white> for details.");
        }
    }
}
