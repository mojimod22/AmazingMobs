package me.zygotecode.amazingmobs.command.sub;

import me.zygotecode.amazingmobs.AmazingMobs;
import me.zygotecode.amazingmobs.command.SubCommand;
import me.zygotecode.amazingmobs.horde.HordeDefinition;
import me.zygotecode.amazingmobs.horde.Wave;
import me.zygotecode.amazingmobs.mob.MobDefinition;
import me.zygotecode.amazingmobs.skill.SkillDefinition;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public final class InfoSub implements SubCommand {

    @Override public String name() { return "info"; }
    @Override public String permission() { return "amazingmobs.use"; }
    @Override public String usage() { return "info <mob|horde> <id>"; }
    @Override public String description() { return "Show details of a mob or horde"; }

    @Override
    public void execute(AmazingMobs plugin, CommandSender s, String[] args) {
        if (args.length < 2) { plugin.messages().send(s, "<red>Usage: /am " + usage()); return; }
        String type = args[0].toLowerCase(Locale.ROOT);
        String id = args[1];
        if (type.startsWith("mob")) mobInfo(plugin, s, id);
        else if (type.startsWith("horde")) hordeInfo(plugin, s, id);
        else plugin.messages().send(s, "<red>Usage: /am " + usage());
    }

    private void mobInfo(AmazingMobs plugin, CommandSender s, String id) {
        MobDefinition d = plugin.mobRegistry().get(id);
        if (d == null) { plugin.messages().send(s, "<red>No mob '" + id + "'."); return; }
        plugin.messages().send(s, "<aqua>Mob</aqua> " + d.tier().color() + d.id());
        plugin.messages().sendRaw(s, "  <gray>name:</gray> " + d.displayName());
        plugin.messages().sendRaw(s, "  <gray>base:</gray> <white>" + d.baseType() + "</white>  <gray>tier:</gray> <white>"
                + d.tier() + "</white>  <gray>category:</gray> <white>" + d.category() + "</white>");
        plugin.messages().sendRaw(s, "  <gray>stats:</gray> <red>" + d.stats().health() + " HP</red>, <yellow>"
                + d.stats().attackDamage() + " dmg</yellow>, <white>armor " + d.stats().armor() + "</white>"
                + (d.stats().knockbackImmune() ? ", <aqua>kb-immune</aqua>" : ""));
        if (!d.skills().isEmpty()) {
            String skills = d.skills().stream().map(SkillDefinition::skillId).collect(Collectors.joining(", "));
            plugin.messages().sendRaw(s, "  <gray>skills:</gray> <white>" + skills + "</white>");
        }
        if (d.hasPhases()) plugin.messages().sendRaw(s, "  <gray>phases:</gray> <white>" + d.phases().size() + "</white>");
        if (!d.drops().isEmpty()) plugin.messages().sendRaw(s, "  <gray>drops:</gray> <white>" + d.drops().entries().size()
                + " items, xp " + d.drops().xp() + "</white>");
        if (!d.tags().isEmpty()) plugin.messages().sendRaw(s, "  <gray>tags:</gray> <dark_gray>" + String.join(", ", d.tags()) + "</dark_gray>");
    }

    private void hordeInfo(AmazingMobs plugin, CommandSender s, String id) {
        HordeDefinition d = plugin.hordeRegistry().get(id);
        if (d == null) { plugin.messages().send(s, "<red>No horde '" + id + "'."); return; }
        plugin.messages().send(s, "<gold>Horde</gold> <white>" + d.id() + "</white>");
        plugin.messages().sendRaw(s, "  <gray>name:</gray> " + d.name());
        plugin.messages().sendRaw(s, "  <gray>difficulty:</gray> <white>" + d.difficulty() + "</white>  <gray>players:</gray> <white>"
                + d.minPlayers() + "-" + d.maxPlayers() + "</white>  <gray>area:</gray> <white>" + d.area().shape()
                + " r" + (int) d.area().radius() + "</white>");
        plugin.messages().sendRaw(s, "  <gray>waves:</gray> <white>" + d.waves().size() + "</white>"
                + (d.infinite() ? " <dark_purple>(infinite)</dark_purple>" : ""));
        int i = 1;
        for (Wave w : d.waves()) {
            plugin.messages().sendRaw(s, "    <dark_gray>" + (i++) + ".</dark_gray> <white>" + w.label()
                    + "</white> <gray>(" + w.entries().size() + " types" + (w.hasBoss() ? ", boss" : "") + ")</gray>");
        }
    }

    @Override
    public List<String> tab(AmazingMobs plugin, CommandSender sender, String[] args) {
        if (args.length == 1) {
            List<String> out = new ArrayList<>();
            for (String o : List.of("mob", "horde")) if (o.startsWith(args[0].toLowerCase(Locale.ROOT))) out.add(o);
            return out;
        }
        if (args.length == 2) {
            String type = args[0].toLowerCase(Locale.ROOT);
            var ids = type.startsWith("horde") ? plugin.hordeRegistry().ids() : plugin.mobRegistry().ids();
            return prefix(ids, args[1]);
        }
        return List.of();
    }

    static List<String> prefix(Iterable<String> ids, String pre) {
        String p = pre.toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        for (String id : ids) if (id.startsWith(p)) out.add(id);
        return out;
    }
}
