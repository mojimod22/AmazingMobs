package me.zygotecode.amazingmobs.command.sub;

import me.zygotecode.amazingmobs.AmazingMobs;
import me.zygotecode.amazingmobs.command.MobEditSession;
import me.zygotecode.amazingmobs.command.SessionManager;
import me.zygotecode.amazingmobs.command.SubCommand;
import me.zygotecode.amazingmobs.config.ConfigSection;
import me.zygotecode.amazingmobs.config.ConfigSource;
import me.zygotecode.amazingmobs.config.validation.ValidationReport;
import org.bukkit.command.CommandSender;

import java.io.File;
import java.util.List;

public final class EditSub implements SubCommand {

    private final SessionManager sessions;

    public EditSub(SessionManager sessions) { this.sessions = sessions; }

    @Override public String name() { return "edit"; }
    @Override public String permission() { return "amazingmobs.mob.edit"; }
    @Override public String usage() { return "edit <mobId>"; }
    @Override public String description() { return "Load an existing mob file into an edit session"; }

    @Override
    public void execute(AmazingMobs plugin, CommandSender s, String[] args) {
        if (args.length < 1) { plugin.messages().send(s, "<red>Usage: /am " + usage()); return; }
        String id = args[0].toLowerCase(java.util.Locale.ROOT);
        File file = new File(plugin.mobsDir(), id + ".yml");
        if (!file.isFile()) { plugin.messages().send(s, "<red>No file mobs/" + id + ".yml to edit."); return; }
        ConfigSection root = ConfigSource.load(file, new ValidationReport(id + ".yml"));
        MobEditSession session = new MobEditSession(id, root.rawMap());
        session.setId(id);
        sessions.put(s, session);
        plugin.messages().send(s, "<green>Editing <white>" + id + "</white>. Use <white>/am set</white>, then <white>/am save</white>.");
    }

    @Override
    public List<String> tab(AmazingMobs plugin, CommandSender sender, String[] args) {
        return args.length == 1 ? InfoSub.prefix(plugin.mobRegistry().ids(), args[0]) : List.of();
    }
}
