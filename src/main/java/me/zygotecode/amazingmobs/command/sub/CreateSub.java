package me.zygotecode.amazingmobs.command.sub;

import me.zygotecode.amazingmobs.AmazingMobs;
import me.zygotecode.amazingmobs.command.MobEditSession;
import me.zygotecode.amazingmobs.command.SessionManager;
import me.zygotecode.amazingmobs.command.SubCommand;
import me.zygotecode.amazingmobs.mob.MobParser;
import me.zygotecode.amazingmobs.util.Resolvers;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class CreateSub implements SubCommand {

    private static final List<String> COMMON = List.of("zombie", "skeleton", "wither_skeleton", "husk",
            "spider", "cave_spider", "creeper", "blaze", "vindicator", "pillager", "piglin", "piglin_brute",
            "zombified_piglin", "vex", "ravager", "phantom", "ghast", "stray", "drowned", "evoker", "witch",
            "enderman", "slime", "magma_cube", "hoglin", "zoglin", "warden", "iron_golem", "guardian");

    private final SessionManager sessions;

    public CreateSub(SessionManager sessions) { this.sessions = sessions; }

    @Override public String name() { return "create"; }
    @Override public String permission() { return "amazingmobs.mob.create"; }
    @Override public String usage() { return "create <id> <entityType>"; }
    @Override public String description() { return "Start a command-built mob from a base entity"; }

    @Override
    public void execute(AmazingMobs plugin, CommandSender s, String[] args) {
        if (args.length < 2) { plugin.messages().send(s, "<red>Usage: /am " + usage()); return; }
        String id = MobParser.sanitizeId(args[0]);
        if (id == null) { plugin.messages().send(s, "<red>Invalid id."); return; }
        EntityType type = Resolvers.entityType(args[1], null);
        if (type == null) { plugin.messages().send(s, "<red>Unknown entity type '" + args[1] + "'."); return; }
        Class<? extends Entity> cls = type.getEntityClass();
        if (cls == null || !LivingEntity.class.isAssignableFrom(cls)) {
            plugin.messages().send(s, "<red>'" + args[1] + "' is not a living entity.");
            return;
        }
        if (plugin.mobRegistry().contains(id)) {
            plugin.messages().send(s, "<yellow>Note: a mob '" + id + "' already exists — saving will overwrite it.");
        }
        sessions.put(s, MobEditSession.fresh(id, type.getKey().getKey()));
        plugin.messages().send(s, "<green>Editing new mob <white>" + id + "</white> (<white>" + type.getKey().getKey()
                + "</white>). Use <white>/am set <path> <value></white>, then <white>/am save</white>.");
        plugin.messages().sendRaw(s, "<gray>e.g. /am set name <red>My Boss</red> · /am set stats.health 200 · /am set presentation.glow true");
    }

    @Override
    public List<String> tab(AmazingMobs plugin, CommandSender sender, String[] args) {
        if (args.length == 2) {
            List<String> out = new ArrayList<>();
            for (String t : COMMON) if (t.startsWith(args[1].toLowerCase(Locale.ROOT))) out.add(t);
            return out;
        }
        return List.of();
    }
}
