package me.zygotecode.amazingmobs.command.sub;

import me.zygotecode.amazingmobs.AmazingMobs;
import me.zygotecode.amazingmobs.command.MobEditSession;
import me.zygotecode.amazingmobs.command.SessionManager;
import me.zygotecode.amazingmobs.command.SubCommand;
import me.zygotecode.amazingmobs.config.ConfigSource;
import org.bukkit.command.CommandSender;

import java.io.File;

public final class SaveSub implements SubCommand {

    private final SessionManager sessions;

    public SaveSub(SessionManager sessions) { this.sessions = sessions; }

    @Override public String name() { return "save"; }
    @Override public String permission() { return "amazingmobs.mob.save"; }
    @Override public String usage() { return "save"; }
    @Override public String description() { return "Write the mob you're editing to /mobs and load it"; }

    @Override
    public void execute(AmazingMobs plugin, CommandSender s, String[] args) {
        MobEditSession session = sessions.get(s);
        if (session == null) { plugin.messages().send(s, "<red>No active session — use <white>/am create</white> first."); return; }
        File file = new File(plugin.mobsDir(), session.id() + ".yml");
        try {
            ConfigSource.write(file, session.tree());
        } catch (Exception e) {
            plugin.messages().send(s, "<red>Could not write file: " + e.getMessage());
            return;
        }
        plugin.reloadDefinitions();
        boolean loaded = plugin.mobRegistry().contains(session.id());
        if (loaded) {
            plugin.messages().send(s, "<green>Saved & loaded <white>" + session.id() + "</white> → <gray>mobs/"
                    + session.id() + ".yml</gray>");
            sessions.clear(s);
        } else {
            plugin.messages().send(s, "<yellow>Saved to mobs/" + session.id()
                    + ".yml but it did not load — run <white>/am validate</white> to see why. Session kept.");
        }
    }
}
