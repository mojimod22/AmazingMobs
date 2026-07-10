package me.zygotecode.amazingmobs.command.sub;

import me.zygotecode.amazingmobs.AmazingMobs;
import me.zygotecode.amazingmobs.command.CliEntries;
import me.zygotecode.amazingmobs.command.MobEditSession;
import me.zygotecode.amazingmobs.command.SessionManager;
import me.zygotecode.amazingmobs.command.SubCommand;
import org.bukkit.command.CommandSender;

import java.util.Arrays;
import java.util.List;

/** {@code addskill <skillId> [key=val...]} — append a skill to the mob being edited (no file edit). */
public final class AddSkillSub implements SubCommand {

    private final SessionManager sessions;

    public AddSkillSub(SessionManager sessions) { this.sessions = sessions; }

    @Override public String name() { return "addskill"; }
    @Override public String permission() { return "amazingmobs.mob.edit"; }
    @Override public String usage() { return "addskill <skillId> [trigger/param=val...]"; }
    @Override public String description() { return "Add a skill to the edit session (e.g. addskill fireball cooldown=2s target=NEAREST_PLAYER count=3)"; }

    @Override
    public void execute(AmazingMobs plugin, CommandSender s, String[] args) {
        MobEditSession session = sessions.get(s);
        if (session == null) { plugin.messages().send(s, "<red>No edit session — use <white>/am create</white> or <white>/am edit</white>."); return; }
        if (args.length < 1) { plugin.messages().send(s, "<red>Usage: /am " + usage()); return; }
        if (!plugin.skillRegistry().contains(args[0])) { plugin.messages().send(s, "<red>Unknown skill '" + args[0] + "'."); return; }
        session.addToList("skills", CliEntries.skillEntry(args[0], Arrays.asList(Arrays.copyOfRange(args, 1, args.length))));
        plugin.messages().send(s, "<green>Added skill <white>" + args[0] + "</white> (" + session.getList("skills").size()
                + " total). <gray>/am show</gray> to preview, <gray>/am save</gray> to persist.");
    }

    @Override
    public List<String> tab(AmazingMobs plugin, CommandSender sender, String[] args) {
        return args.length == 1 ? InfoSub.prefix(plugin.skillRegistry().ids(), args[0]) : List.of();
    }
}
