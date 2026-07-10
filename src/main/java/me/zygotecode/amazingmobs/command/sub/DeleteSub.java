package me.zygotecode.amazingmobs.command.sub;

import me.zygotecode.amazingmobs.AmazingMobs;
import me.zygotecode.amazingmobs.command.SubCommand;
import org.bukkit.command.CommandSender;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class DeleteSub implements SubCommand {

    @Override public String name() { return "delete"; }
    @Override public String permission() { return "amazingmobs.mob.delete"; }
    @Override public String usage() { return "delete <mob|horde> <id>"; }
    @Override public String description() { return "Delete a mob/horde definition file"; }

    @Override
    public void execute(AmazingMobs plugin, CommandSender s, String[] args) {
        if (args.length < 2) { plugin.messages().send(s, "<red>Usage: /am " + usage()); return; }
        String type = args[0].toLowerCase(Locale.ROOT);
        String id = args[1].toLowerCase(Locale.ROOT);
        boolean mob = type.startsWith("mob");
        boolean horde = type.startsWith("horde");
        if (!mob && !horde) { plugin.messages().send(s, "<red>Usage: /am " + usage()); return; }

        File dir = mob ? plugin.mobsDir() : plugin.hordesDir();
        File file = new File(dir, id + ".yml");
        boolean fileGone = file.isFile() && file.delete();
        boolean unreg = mob ? plugin.mobRegistry().remove(id) : plugin.hordeRegistry().remove(id);

        if (!fileGone && !unreg) { plugin.messages().send(s, "<red>No " + (mob ? "mob" : "horde") + " '" + id + "' found."); return; }
        plugin.messages().send(s, "<green>Deleted <white>" + id + "</white>"
                + (fileGone ? " <gray>(file removed)</gray>" : " <yellow>(was not file-backed)</yellow>") + ".");
    }

    @Override
    public List<String> tab(AmazingMobs plugin, CommandSender sender, String[] args) {
        if (args.length == 1) {
            List<String> out = new ArrayList<>();
            for (String o : List.of("mob", "horde")) if (o.startsWith(args[0].toLowerCase(Locale.ROOT))) out.add(o);
            return out;
        }
        if (args.length == 2) {
            var ids = args[0].toLowerCase(Locale.ROOT).startsWith("horde")
                    ? plugin.hordeRegistry().ids() : plugin.mobRegistry().ids();
            return InfoSub.prefix(ids, args[1]);
        }
        return List.of();
    }
}
