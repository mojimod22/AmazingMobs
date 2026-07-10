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

/** {@code adddrop <material> [chance=.. amount=.. name=..]} — append a drop to the mob being edited. */
public final class AddDropSub implements SubCommand {

    private final SessionManager sessions;

    public AddDropSub(SessionManager sessions) { this.sessions = sessions; }

    @Override public String name() { return "adddrop"; }
    @Override public String permission() { return "amazingmobs.mob.edit"; }
    @Override public String usage() { return "adddrop <material> [chance=.. amount=.. name=..]"; }
    @Override public String description() { return "Add a drop to the edit session (e.g. adddrop DIAMOND chance=0.5 amount=1-3)"; }

    @Override
    public void execute(AmazingMobs plugin, CommandSender s, String[] args) {
        MobEditSession session = sessions.get(s);
        if (session == null) { plugin.messages().send(s, "<red>No edit session — use <white>/am create</white> or <white>/am edit</white>."); return; }
        if (args.length < 1) { plugin.messages().send(s, "<red>Usage: /am " + usage()); return; }
        Map<String, Object> entry = CliEntries.flat(Arrays.asList(Arrays.copyOfRange(args, 1, args.length)));
        entry.put("material", args[0].toUpperCase(java.util.Locale.ROOT));
        session.addToList("drops.items", entry);
        plugin.messages().send(s, "<green>Added drop <white>" + args[0] + "</white> (" + session.getList("drops.items").size() + " total).");
    }
}
