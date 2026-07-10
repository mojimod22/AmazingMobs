package me.zygotecode.amazingmobs.command.sub;

import me.zygotecode.amazingmobs.AmazingMobs;
import me.zygotecode.amazingmobs.command.SubCommand;
import org.bukkit.command.CommandSender;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;

public final class ExportSub implements SubCommand {

    @Override public String name() { return "export"; }
    @Override public String permission() { return "amazingmobs.mob.export"; }
    @Override public String usage() { return "export <mobId>"; }
    @Override public String description() { return "Copy a mob's YAML to the exports/ folder (portable)"; }

    @Override
    public void execute(AmazingMobs plugin, CommandSender s, String[] args) {
        if (args.length < 1) { plugin.messages().send(s, "<red>Usage: /am " + usage()); return; }
        String id = args[0].toLowerCase(java.util.Locale.ROOT);
        File src = new File(plugin.mobsDir(), id + ".yml");
        if (!src.isFile()) { plugin.messages().send(s, "<red>Only file-backed mobs can be exported (mobs/" + id + ".yml not found)."); return; }
        File exportsDir = new File(plugin.getDataFolder(), "exports");
        exportsDir.mkdirs();
        File dest = new File(exportsDir, id + ".yml");
        try {
            Files.copy(src.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            plugin.messages().send(s, "<red>Export failed: " + e.getMessage());
            return;
        }
        plugin.messages().send(s, "<green>Exported <white>" + id + "</white> → <gray>exports/" + id + ".yml</gray>");
    }

    @Override
    public List<String> tab(AmazingMobs plugin, CommandSender sender, String[] args) {
        return args.length == 1 ? InfoSub.prefix(plugin.mobRegistry().ids(), args[0]) : List.of();
    }
}
