package me.zygotecode.amazingmobs.command.sub;

import me.zygotecode.amazingmobs.AmazingMobs;
import me.zygotecode.amazingmobs.command.MobEditSession;
import me.zygotecode.amazingmobs.command.SessionManager;
import me.zygotecode.amazingmobs.command.SubCommand;
import org.bukkit.command.CommandSender;

/** {@code show} — preview the YAML of the mob currently being edited (before /am save). */
public final class ShowSessionSub implements SubCommand {

    private final SessionManager sessions;

    public ShowSessionSub(SessionManager sessions) { this.sessions = sessions; }

    @Override public String name() { return "show"; }
    @Override public String permission() { return "amazingmobs.mob.edit"; }
    @Override public String usage() { return "show"; }
    @Override public String description() { return "Preview the YAML of the mob you're editing"; }

    @Override
    public void execute(AmazingMobs plugin, CommandSender s, String[] args) {
        MobEditSession session = sessions.get(s);
        if (session == null) { plugin.messages().send(s, "<red>No edit session — use <white>/am create</white> or <white>/am edit</white>."); return; }
        plugin.messages().send(s, "<aqua>Editing <white>" + session.id() + "</white></aqua> <gray>(preview)</gray>");
        String[] lines = session.preview().split("\n");
        int shown = 0;
        for (String line : lines) {
            if (shown++ >= 60) { plugin.messages().sendRaw(s, "<dark_gray>... (truncated; /am save to write the full file)</dark_gray>"); break; }
            plugin.messages().sendRaw(s, "<gray>" + line.replace("<", "\\<") + "</gray>");
        }
    }
}
