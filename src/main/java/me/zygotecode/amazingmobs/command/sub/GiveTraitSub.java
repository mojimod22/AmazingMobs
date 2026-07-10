package me.zygotecode.amazingmobs.command.sub;

import me.zygotecode.amazingmobs.AmazingMobs;
import me.zygotecode.amazingmobs.command.CliEntries;
import me.zygotecode.amazingmobs.command.SubCommand;
import me.zygotecode.amazingmobs.config.ConfigSection;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;

/** {@code give-trait <traitId> [key=val...]} — inject a trait onto the live mob you're looking at. */
public final class GiveTraitSub implements SubCommand {

    @Override public String name() { return "give-trait"; }
    @Override public String permission() { return "amazingmobs.mob.edit"; }
    @Override public String usage() { return "give-trait <traitId> [key=val...]"; }
    @Override public String description() { return "Inject a trait onto the mob you look at (live test)"; }

    @Override
    public void execute(AmazingMobs plugin, CommandSender s, String[] args) {
        if (!(s instanceof Player p)) { plugin.messages().send(s, "<red>Players only."); return; }
        if (args.length < 1) { plugin.messages().send(s, "<red>Usage: /am " + usage()); return; }
        if (!plugin.traitRegistry().contains(args[0])) { plugin.messages().send(s, "<red>Unknown trait '" + args[0] + "'."); return; }
        Entity looked = p.getTargetEntity(32);
        if (looked == null || !plugin.mobManager().isCustomMob(looked)) {
            plugin.messages().send(s, "<red>Look at an AmazingMobs mob first."); return;
        }
        ConfigSection params = ConfigSection.of(CliEntries.flat(tail(args)));
        boolean ok = plugin.mobManager().giveTrait(looked, args[0], params);
        plugin.messages().send(s, ok ? "<green>Injected trait <white>" + args[0] + "</white> onto the mob."
                : "<red>Could not inject trait.");
    }

    private static List<String> tail(String[] args) {
        return Arrays.asList(Arrays.copyOfRange(args, 1, args.length));
    }

    @Override
    public List<String> tab(AmazingMobs plugin, CommandSender sender, String[] args) {
        return args.length == 1 ? InfoSub.prefix(plugin.traitRegistry().ids(), args[0]) : List.of();
    }
}
