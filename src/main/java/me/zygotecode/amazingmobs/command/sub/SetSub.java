package me.zygotecode.amazingmobs.command.sub;

import me.zygotecode.amazingmobs.AmazingMobs;
import me.zygotecode.amazingmobs.command.MobEditSession;
import me.zygotecode.amazingmobs.command.SessionManager;
import me.zygotecode.amazingmobs.command.SubCommand;
import me.zygotecode.amazingmobs.mob.MobParser;
import org.bukkit.command.CommandSender;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public final class SetSub implements SubCommand {

    private static final List<String> PATHS = List.of("name", "tier", "category",
            "stats.health", "stats.damage", "stats.speed", "stats.armor", "stats.armor-toughness",
            "stats.knockback-resistance", "stats.scale", "stats.follow-range", "stats.regen-per-second",
            "stats.crit-chance", "stats.crit-multiplier", "stats.fire-immune", "stats.knockback-immune",
            "ai.aggression", "ai.movement", "ai.aggro-range",
            "equipment.main-hand.material", "equipment.helmet.material", "equipment.chestplate.material",
            "equipment.leggings.material", "equipment.boots.material",
            "presentation.glow", "presentation.glow-color", "presentation.boss-bar", "presentation.boss-bar-color",
            "scaling.health-per-player", "scaling.damage-per-player", "scaling.max-players");

    private final SessionManager sessions;

    public SetSub(SessionManager sessions) { this.sessions = sessions; }

    @Override public String name() { return "set"; }
    @Override public String permission() { return "amazingmobs.mob.edit"; }
    @Override public String usage() { return "set <path> <value...>"; }
    @Override public String description() { return "Set a field on the mob you're editing"; }

    @Override
    public void execute(AmazingMobs plugin, CommandSender s, String[] args) {
        MobEditSession session = sessions.get(s);
        if (session == null) { plugin.messages().send(s, "<red>No active session — use <white>/am create</white> or <white>/am edit</white>."); return; }
        if (args.length < 2) { plugin.messages().send(s, "<red>Usage: /am " + usage()); return; }
        String path = args[0];
        String value = Arrays.stream(args).skip(1).collect(Collectors.joining(" "));
        if (path.equalsIgnoreCase("id")) {
            String id = MobParser.sanitizeId(value);
            if (id == null) { plugin.messages().send(s, "<red>Invalid id."); return; }
            session.setId(id);
        } else {
            session.set(path, value);
        }
        plugin.messages().send(s, "<green>Set <white>" + path + "</white> = <white>" + value + "</white>");
    }

    @Override
    public List<String> tab(AmazingMobs plugin, CommandSender sender, String[] args) {
        if (args.length == 1) {
            String p = args[0].toLowerCase(Locale.ROOT);
            return PATHS.stream().filter(x -> x.startsWith(p)).collect(Collectors.toList());
        }
        return List.of();
    }
}
