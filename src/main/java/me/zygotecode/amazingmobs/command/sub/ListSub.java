package me.zygotecode.amazingmobs.command.sub;

import me.zygotecode.amazingmobs.AmazingMobs;
import me.zygotecode.amazingmobs.command.SubCommand;
import me.zygotecode.amazingmobs.horde.HordeDefinition;
import me.zygotecode.amazingmobs.mob.MobDefinition;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class ListSub implements SubCommand {

    @Override public String name() { return "list"; }
    @Override public String permission() { return "amazingmobs.use"; }
    @Override public String usage() { return "list <mobs|hordes|skills>"; }
    @Override public String description() { return "List loaded mobs, hordes or skills"; }

    @Override
    public void execute(AmazingMobs plugin, CommandSender s, String[] args) {
        String what = args.length > 0 ? args[0].toLowerCase(Locale.ROOT) : "mobs";
        switch (what) {
            case "mobs" -> {
                plugin.messages().send(s, "<aqua>Mobs</aqua> <gray>(" + plugin.mobRegistry().size() + ")</gray>");
                for (MobDefinition d : plugin.mobRegistry().all()) {
                    plugin.messages().sendRaw(s, "  " + d.tier().color() + d.id() + "</" + colorTag(d) + "> "
                            + "<dark_gray>·</dark_gray> <gray>" + plainName(d) + " <dark_gray>[" + d.tier() + "]</dark_gray>");
                }
            }
            case "hordes" -> {
                plugin.messages().send(s, "<aqua>Hordes</aqua> <gray>(" + plugin.hordeRegistry().size() + ")</gray>");
                for (HordeDefinition d : plugin.hordeRegistry().all()) {
                    plugin.messages().sendRaw(s, "  <gold>" + d.id() + "</gold> <dark_gray>·</dark_gray> <gray>"
                            + d.waves().size() + " waves, diff " + d.difficulty() + "</gray>");
                }
            }
            case "skills" -> {
                plugin.messages().send(s, "<aqua>Skills</aqua> <gray>(" + plugin.skillRegistry().size() + ")</gray>");
                plugin.messages().sendRaw(s, "<gray>" + String.join("<dark_gray>,</dark_gray> <white>",
                        plugin.skillRegistry().ids()) + "</white>");
            }
            case "traits" -> {
                plugin.messages().send(s, "<aqua>Traits</aqua> <gray>(" + plugin.traitRegistry().size() + ")</gray>");
                plugin.messages().sendRaw(s, "<gray>" + String.join("<dark_gray>,</dark_gray> <white>",
                        plugin.traitRegistry().ids()) + "</white>");
            }
            default -> plugin.messages().send(s, "<red>Usage: /am " + usage());
        }
    }

    @Override
    public List<String> tab(AmazingMobs plugin, CommandSender sender, String[] args) {
        if (args.length == 1) {
            List<String> out = new ArrayList<>();
            for (String o : List.of("mobs", "hordes", "skills", "traits")) if (o.startsWith(args[0].toLowerCase(Locale.ROOT))) out.add(o);
            return out;
        }
        return List.of();
    }

    private static String colorTag(MobDefinition d) {
        // d.tier().color() is like "<gray>"; strip the angle brackets for the closing tag
        String c = d.tier().color();
        return c.replace("<", "").replace(">", "");
    }

    private static String plainName(MobDefinition d) {
        return me.zygotecode.amazingmobs.util.Text.plain(d.displayName());
    }
}
