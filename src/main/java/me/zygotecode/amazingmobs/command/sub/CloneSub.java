package me.zygotecode.amazingmobs.command.sub;

import me.zygotecode.amazingmobs.AmazingMobs;
import me.zygotecode.amazingmobs.command.MobEditSession;
import me.zygotecode.amazingmobs.command.SessionManager;
import me.zygotecode.amazingmobs.command.SubCommand;
import me.zygotecode.amazingmobs.config.ConfigSection;
import me.zygotecode.amazingmobs.config.ConfigSource;
import me.zygotecode.amazingmobs.config.validation.ValidationReport;
import me.zygotecode.amazingmobs.mob.MobParser;
import org.bukkit.command.CommandSender;

import java.io.File;
import java.util.List;
import java.util.Map;

public final class CloneSub implements SubCommand {

    private final SessionManager sessions;

    public CloneSub(SessionManager sessions) { this.sessions = sessions; }

    @Override public String name() { return "clone"; }
    @Override public String permission() { return "amazingmobs.mob.create"; }
    @Override public String usage() { return "clone <sourceId> <newId>"; }
    @Override public String description() { return "Copy an existing mob into a new edit session"; }

    @Override
    public void execute(AmazingMobs plugin, CommandSender s, String[] args) {
        if (args.length < 2) { plugin.messages().send(s, "<red>Usage: /am " + usage()); return; }
        String src = args[0].toLowerCase(java.util.Locale.ROOT);
        String newId = MobParser.sanitizeId(args[1]);
        if (newId == null) { plugin.messages().send(s, "<red>Invalid new id."); return; }
        File file = new File(plugin.mobsDir(), src + ".yml");
        if (!file.isFile()) { plugin.messages().send(s, "<red>Clone needs a file-backed mob (mobs/" + src + ".yml not found)."); return; }
        ConfigSection root = ConfigSource.load(file, new ValidationReport(src + ".yml"));
        Map<String, Object> map = root.rawMap();
        map.put("id", newId);
        sessions.put(s, new MobEditSession(newId, map));
        plugin.messages().send(s, "<green>Cloned <white>" + src + "</white> → editing <white>" + newId
                + "</white>. <gray>/am save</gray> to persist.");
    }

    @Override
    public List<String> tab(AmazingMobs plugin, CommandSender sender, String[] args) {
        return args.length == 1 ? InfoSub.prefix(plugin.mobRegistry().ids(), args[0]) : List.of();
    }
}
