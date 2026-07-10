package me.zygotecode.amazingmobs.command.sub;

import me.zygotecode.amazingmobs.AmazingMobs;
import me.zygotecode.amazingmobs.command.SubCommand;
import me.zygotecode.amazingmobs.mob.Equipment;
import me.zygotecode.amazingmobs.mob.ItemSpec;
import me.zygotecode.amazingmobs.mob.MobDefinition;
import me.zygotecode.amazingmobs.util.Rng;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class GiveSub implements SubCommand {

    private static final List<String> SLOTS = List.of("main-hand", "off-hand", "helmet", "chestplate", "leggings", "boots", "all");

    @Override public String name() { return "give"; }
    @Override public String permission() { return "amazingmobs.mob.give"; }
    @Override public String usage() { return "give <mobId> <slot|all>"; }
    @Override public String description() { return "Receive a mob's configured gear item(s)"; }

    @Override
    public void execute(AmazingMobs plugin, CommandSender s, String[] args) {
        if (!(s instanceof Player p)) { plugin.messages().send(s, "<red>Players only."); return; }
        if (args.length < 2) { plugin.messages().send(s, "<red>Usage: /am " + usage()); return; }
        MobDefinition def = plugin.mobRegistry().get(args[0]);
        if (def == null) { plugin.messages().send(s, "<red>No mob '" + args[0] + "'."); return; }
        Equipment eq = def.equipment();
        String slot = args[1].toLowerCase(Locale.ROOT);
        Rng rng = Rng.shared();
        int given = 0;
        if (slot.equals("all")) {
            for (String sl : SLOTS.subList(0, 6)) {
                ItemSpec spec = eq.bySlot(sl);
                if (spec != null) { p.getInventory().addItem(spec.build(rng)); given++; }
            }
        } else {
            ItemSpec spec = eq.bySlot(slot);
            if (spec == null) { plugin.messages().send(s, "<red>No item in slot '" + slot + "' for that mob."); return; }
            p.getInventory().addItem(spec.build(rng));
            given = 1;
        }
        plugin.messages().send(s, given == 0 ? "<yellow>That mob has no gear."
                : "<green>Gave <white>" + given + "</white> item(s) from <white>" + def.id() + "</white>.");
    }

    @Override
    public List<String> tab(AmazingMobs plugin, CommandSender sender, String[] args) {
        if (args.length == 1) return InfoSub.prefix(plugin.mobRegistry().ids(), args[0]);
        if (args.length == 2) {
            List<String> out = new ArrayList<>();
            for (String sl : SLOTS) if (sl.startsWith(args[1].toLowerCase(Locale.ROOT))) out.add(sl);
            return out;
        }
        return List.of();
    }
}
