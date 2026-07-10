package me.zygotecode.amazingmobs.command.sub;

import me.zygotecode.amazingmobs.AmazingMobs;
import me.zygotecode.amazingmobs.command.MobEditSession;
import me.zygotecode.amazingmobs.command.SessionManager;
import me.zygotecode.amazingmobs.command.SubCommand;
import me.zygotecode.amazingmobs.util.Numbers;
import org.bukkit.command.CommandSender;

import java.util.List;

/** {@code rmentry <listPath> <index>} — remove an entry from a list (e.g. skills, traits, drops.items). */
public final class RmEntrySub implements SubCommand {

    private final SessionManager sessions;

    public RmEntrySub(SessionManager sessions) { this.sessions = sessions; }

    @Override public String name() { return "rmentry"; }
    @Override public String permission() { return "amazingmobs.mob.edit"; }
    @Override public String usage() { return "rmentry <listPath> <index>"; }
    @Override public String description() { return "Remove a list entry by index (skills/traits/drops.items)"; }

    @Override
    public void execute(AmazingMobs plugin, CommandSender s, String[] args) {
        MobEditSession session = sessions.get(s);
        if (session == null) { plugin.messages().send(s, "<red>No edit session."); return; }
        if (args.length < 2) { plugin.messages().send(s, "<red>Usage: /am " + usage()); return; }
        int index = Numbers.parseInt(args[1], -1);
        boolean ok = session.removeFromList(args[0], index);
        plugin.messages().send(s, ok ? "<green>Removed <white>" + args[0] + "[" + index + "]</white>."
                : "<red>No entry at " + args[0] + "[" + index + "].");
    }

    @Override
    public List<String> tab(AmazingMobs plugin, CommandSender sender, String[] args) {
        if (args.length == 1) {
            List<String> out = new java.util.ArrayList<>();
            for (String o : List.of("skills", "traits", "drops.items", "phases", "variants"))
                if (o.startsWith(args[0].toLowerCase(java.util.Locale.ROOT))) out.add(o);
            return out;
        }
        return List.of();
    }
}
