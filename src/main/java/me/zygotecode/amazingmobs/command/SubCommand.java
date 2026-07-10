package me.zygotecode.amazingmobs.command;

import me.zygotecode.amazingmobs.AmazingMobs;
import org.bukkit.command.CommandSender;

import java.util.List;

/** One subcommand of {@code /amazingmobs}. Clean, self-describing units the dispatcher routes to. */
public interface SubCommand {

    String name();

    String permission();

    /** Argument usage, e.g. {@code "spawn <mobId> [amount]"}. */
    String usage();

    String description();

    /** @param args arguments AFTER the subcommand name. */
    void execute(AmazingMobs plugin, CommandSender sender, String[] args);

    default List<String> tab(AmazingMobs plugin, CommandSender sender, String[] args) {
        return List.of();
    }
}
