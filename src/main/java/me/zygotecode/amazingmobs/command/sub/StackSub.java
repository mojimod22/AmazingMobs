package me.zygotecode.amazingmobs.command.sub;

import me.zygotecode.amazingmobs.AmazingMobs;
import me.zygotecode.amazingmobs.command.SubCommand;
import me.zygotecode.amazingmobs.mob.runtime.SpawnMeta;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.List;

/** {@code stack <baseMobId> <mob2> [mob3...]} — build a vertical mob column (passenger chain). */
public final class StackSub implements SubCommand {

    @Override public String name() { return "stack"; }
    @Override public String permission() { return "amazingmobs.mob.spawn"; }
    @Override public String usage() { return "stack <baseMobId> <mob2> [mob3...]"; }
    @Override public String description() { return "Spawn a column of stacked mobs (test stacking)"; }

    @Override
    public void execute(AmazingMobs plugin, CommandSender s, String[] args) {
        if (!(s instanceof Player p)) { plugin.messages().send(s, "<red>Players only."); return; }
        if (args.length < 2) { plugin.messages().send(s, "<red>Usage: /am " + usage()); return; }
        Location loc = SpawnSub.targetLocation(p);
        LivingEntity base = plugin.mobManager().spawnAny(args[0], loc, SpawnMeta.SOLO);
        if (base == null) { plugin.messages().send(s, "<red>Could not spawn base '" + args[0] + "'."); return; }
        LivingEntity top = base;
        int n = 1;
        for (int i = 1; i < Math.min(args.length, 8); i++) {
            LivingEntity rider = plugin.mobManager().spawnAny(args[i], loc, SpawnMeta.SOLO);
            if (rider == null) continue;
            top.addPassenger(rider);
            top = rider;
            n++;
        }
        plugin.messages().send(s, "<green>Stacked <white>" + n + "</white> mobs in a column.");
    }

    @Override
    public List<String> tab(AmazingMobs plugin, CommandSender sender, String[] args) {
        return args.length >= 1 ? InfoSub.prefix(plugin.mobRegistry().ids(), args[args.length - 1]) : List.of();
    }
}
