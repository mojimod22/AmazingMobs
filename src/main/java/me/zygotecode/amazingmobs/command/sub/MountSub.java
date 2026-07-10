package me.zygotecode.amazingmobs.command.sub;

import me.zygotecode.amazingmobs.AmazingMobs;
import me.zygotecode.amazingmobs.command.SubCommand;
import me.zygotecode.amazingmobs.mob.runtime.SpawnMeta;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.List;

/** {@code mount <riderMobId> <mountMobId>} — spawn a rider seated on a mount (mob or custom mob). */
public final class MountSub implements SubCommand {

    @Override public String name() { return "mount"; }
    @Override public String permission() { return "amazingmobs.mob.spawn"; }
    @Override public String usage() { return "mount <riderMobId> <mountMobId>"; }
    @Override public String description() { return "Spawn a mob riding another mob (test mounting)"; }

    @Override
    public void execute(AmazingMobs plugin, CommandSender s, String[] args) {
        if (!(s instanceof Player p)) { plugin.messages().send(s, "<red>Players only."); return; }
        if (args.length < 2) { plugin.messages().send(s, "<red>Usage: /am " + usage()); return; }
        Location loc = SpawnSub.targetLocation(p);
        LivingEntity mount = plugin.mobManager().spawnAny(args[1], loc, SpawnMeta.SOLO);
        LivingEntity rider = plugin.mobManager().spawnAny(args[0], loc, SpawnMeta.SOLO);
        if (mount == null || rider == null) {
            plugin.messages().send(s, "<red>Could not spawn (unknown id or capacity cap).");
            if (mount != null) mount.remove();
            if (rider != null) rider.remove();
            return;
        }
        mount.addPassenger(rider);
        plugin.messages().send(s, "<green>Mounted <white>" + args[0] + "</white> on <white>" + args[1] + "</white>.");
    }

    @Override
    public List<String> tab(AmazingMobs plugin, CommandSender sender, String[] args) {
        if (args.length == 1 || args.length == 2) return InfoSub.prefix(plugin.mobRegistry().ids(), args[args.length - 1]);
        return List.of();
    }
}
