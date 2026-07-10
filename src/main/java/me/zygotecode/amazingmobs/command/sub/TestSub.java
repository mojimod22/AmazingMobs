package me.zygotecode.amazingmobs.command.sub;

import me.zygotecode.amazingmobs.AmazingMobs;
import me.zygotecode.amazingmobs.command.SubCommand;
import me.zygotecode.amazingmobs.mob.MobDefinition;
import me.zygotecode.amazingmobs.mob.runtime.SpawnMeta;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.stream.Collectors;

public final class TestSub implements SubCommand {

    @Override public String name() { return "test"; }
    @Override public String permission() { return "amazingmobs.mob.test"; }
    @Override public String usage() { return "test <mobId>"; }
    @Override public String description() { return "Spawn one mob and print its resolved stats"; }

    @Override
    public void execute(AmazingMobs plugin, CommandSender s, String[] args) {
        if (!(s instanceof Player p)) { plugin.messages().send(s, "<red>Players only."); return; }
        if (args.length < 1) { plugin.messages().send(s, "<red>Usage: /am " + usage()); return; }
        MobDefinition def = plugin.mobRegistry().get(args[0]);
        if (def == null) { plugin.messages().send(s, "<red>No mob '" + args[0] + "'."); return; }

        LivingEntity e = plugin.mobManager().spawn(def.id(), SpawnSub.targetLocation(p), SpawnMeta.SOLO);
        if (e == null) { plugin.messages().send(s, "<red>Could not spawn (cap reached or invalid base type)."); return; }
        plugin.messages().send(s, "<green>Test spawn:</green> " + def.tier().color() + def.id());
        plugin.messages().sendRaw(s, "  <gray>health</gray> <red>" + e.getHealth() + "</red>  <gray>skills</gray> <white>"
                + (def.skills().isEmpty() ? "none" : def.skills().stream()
                .map(sk -> sk.skillId()).collect(Collectors.joining(", "))) + "</white>");
        plugin.messages().sendRaw(s, "  <gray>uuid</gray> <dark_gray>" + e.getUniqueId() + "</dark_gray>");
    }

    @Override
    public List<String> tab(AmazingMobs plugin, CommandSender sender, String[] args) {
        return args.length == 1 ? InfoSub.prefix(plugin.mobRegistry().ids(), args[0]) : List.of();
    }
}
