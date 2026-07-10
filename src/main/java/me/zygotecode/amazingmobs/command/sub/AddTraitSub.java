package me.zygotecode.amazingmobs.command.sub;

import me.zygotecode.amazingmobs.AmazingMobs;
import me.zygotecode.amazingmobs.command.CliEntries;
import me.zygotecode.amazingmobs.command.MobEditSession;
import me.zygotecode.amazingmobs.command.SessionManager;
import me.zygotecode.amazingmobs.command.SubCommand;
import org.bukkit.command.CommandSender;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/** {@code addtrait <traitId> [key=val...]} — append a trait to the mob being edited. */
public final class AddTraitSub implements SubCommand {

    private final SessionManager sessions;

    public AddTraitSub(SessionManager sessions) { this.sessions = sessions; }

    @Override public String name() { return "addtrait"; }
    @Override public String permission() { return "amazingmobs.mob.edit"; }
    @Override public String usage() { return "addtrait <traitId> [param=val...]"; }
    @Override public String description() { return "Add a trait to the edit session (e.g. addtrait berserker threshold=0.4)"; }

    @Override
    public void execute(AmazingMobs plugin, CommandSender s, String[] args) {
        MobEditSession session = sessions.get(s);
        if (session == null) { plugin.messages().send(s, "<red>No edit session — use <white>/am create</white> or <white>/am edit</white>."); return; }
        if (args.length < 1) { plugin.messages().send(s, "<red>Usage: /am " + usage()); return; }
        if (!plugin.traitRegistry().contains(args[0])) { plugin.messages().send(s, "<red>Unknown trait '" + args[0] + "'."); return; }
        Map<String, Object> entry = CliEntries.flat(Arrays.asList(Arrays.copyOfRange(args, 1, args.length)));
        entry.put("trait", args[0].toLowerCase(java.util.Locale.ROOT));
        session.addToList("traits", entry);
        plugin.messages().send(s, "<green>Added trait <white>" + args[0] + "</white> (" + session.getList("traits").size() + " total).");
    }

    @Override
    public List<String> tab(AmazingMobs plugin, CommandSender sender, String[] args) {
        return args.length == 1 ? InfoSub.prefix(plugin.traitRegistry().ids(), args[0]) : List.of();
    }
}
