package me.zygotecode.amazingmobs.command.sub;

import me.zygotecode.amazingmobs.AmazingMobs;
import me.zygotecode.amazingmobs.command.SubCommand;
import me.zygotecode.amazingmobs.config.LoadResult;
import me.zygotecode.amazingmobs.config.ReloadSummary;
import me.zygotecode.amazingmobs.config.validation.Issue;
import org.bukkit.command.CommandSender;

public final class ValidateSub implements SubCommand {

    @Override public String name() { return "validate"; }
    @Override public String permission() { return "amazingmobs.validate"; }
    @Override public String usage() { return "validate"; }
    @Override public String description() { return "Dry-run validate every config file"; }

    @Override
    public void execute(AmazingMobs plugin, CommandSender s, String[] args) {
        ReloadSummary sum = plugin.validateAll();
        plugin.messages().send(s, "<aqua>Validation report</aqua>");
        report(plugin, s, "mobs", sum.mobs());
        report(plugin, s, "hordes", sum.hordes());
    }

    private void report(AmazingMobs plugin, CommandSender s, String kind, LoadResult result) {
        plugin.messages().sendRaw(s, "<gray>──</gray> <white>" + kind + "</white> <gray>·</gray> "
                + "<green>" + result.loaded() + " ok</green>, <red>" + result.rejected() + " rejected</red>, "
                + "<yellow>" + result.totalWarnings() + " warnings</yellow>");
        for (LoadResult.FileResult f : result.files()) {
            if (f.report().isClean() && f.loaded()) continue;
            String head = f.loaded() ? "<green>✔</green>" : "<red>✘</red>";
            plugin.messages().sendRaw(s, "  " + head + " <white>" + f.fileName() + "</white>");
            for (Issue i : f.report().issues()) {
                String color = switch (i.level()) {
                    case ERROR -> "red"; case WARN -> "yellow"; default -> "gray";
                };
                String loc = i.path().isEmpty() ? "" : (" <dark_gray>@ " + i.path() + "</dark_gray>");
                plugin.messages().sendRaw(s, "    <" + color + ">" + i.level() + "</" + color + "> <gray>"
                        + i.message() + "</gray>" + loc);
            }
        }
    }
}
