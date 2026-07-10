package me.zygotecode.amazingmobs.command.sub;

import me.zygotecode.amazingmobs.AmazingMobs;
import me.zygotecode.amazingmobs.command.SubCommand;
import me.zygotecode.amazingmobs.mob.MobDefinition;
import me.zygotecode.amazingmobs.mob.Variant;
import me.zygotecode.amazingmobs.mob.runtime.SpawnMeta;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** {@code variant <mobId> <variantId>} — force-spawn a mob with a specific variant applied (test). */
public final class VariantSub implements SubCommand {

    @Override public String name() { return "variant"; }
    @Override public String permission() { return "amazingmobs.mob.spawn"; }
    @Override public String usage() { return "variant <mobId> <variantId>"; }
    @Override public String description() { return "Force-spawn a mob's specific variant (test mutations)"; }

    @Override
    public void execute(AmazingMobs plugin, CommandSender s, String[] args) {
        if (!(s instanceof Player p)) { plugin.messages().send(s, "<red>Players only."); return; }
        if (args.length < 2) { plugin.messages().send(s, "<red>Usage: /am " + usage()); return; }
        MobDefinition def = plugin.mobRegistry().get(args[0]);
        if (def == null) { plugin.messages().send(s, "<red>No mob '" + args[0] + "'."); return; }
        Variant chosen = null;
        for (Variant v : def.variants()) {
            if (v.id().equalsIgnoreCase(args[1])) { chosen = v; break; }
        }
        if (chosen == null) {
            plugin.messages().send(s, "<red>Mob '" + args[0] + "' has no variant '" + args[1] + "'. Available: "
                    + variantIds(def));
            return;
        }
        MobDefinition eff = chosen.apply(def);
        var e = plugin.mobManager().spawnExact(eff, SpawnSub.targetLocation(p), SpawnMeta.SOLO);
        plugin.messages().send(s, e != null
                ? "<green>Spawned <white>" + args[0] + "</white> variant <white>" + chosen.id() + "</white>."
                : "<red>Could not spawn (capacity cap).");
    }

    private static String variantIds(MobDefinition def) {
        List<String> ids = new ArrayList<>();
        for (Variant v : def.variants()) ids.add(v.id());
        return ids.isEmpty() ? "(none)" : String.join(", ", ids);
    }

    @Override
    public List<String> tab(AmazingMobs plugin, CommandSender sender, String[] args) {
        if (args.length == 1) return InfoSub.prefix(plugin.mobRegistry().ids(), args[0]);
        if (args.length == 2) {
            MobDefinition def = plugin.mobRegistry().get(args[0]);
            if (def == null) return List.of();
            List<String> out = new ArrayList<>();
            String pre = args[1].toLowerCase(Locale.ROOT);
            for (Variant v : def.variants()) if (v.id().toLowerCase(Locale.ROOT).startsWith(pre)) out.add(v.id());
            return out;
        }
        return List.of();
    }
}
