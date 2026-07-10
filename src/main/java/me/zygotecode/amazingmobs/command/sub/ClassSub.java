package me.zygotecode.amazingmobs.command.sub;

import me.zygotecode.amazingmobs.AmazingMobs;
import me.zygotecode.amazingmobs.clazz.ClassMenu;
import me.zygotecode.amazingmobs.clazz.PlayerClass;
import me.zygotecode.amazingmobs.command.SubCommand;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** {@code /am class [id]} — open the class picker, or select a class directly. */
public final class ClassSub implements SubCommand {

    @Override public String name() { return "class"; }
    @Override public String permission() { return "amazingmobs.use"; }
    @Override public String usage() { return "class [classId]"; }
    @Override public String description() { return "Choose / view your class (no arg = open the menu)"; }

    @Override
    public void execute(AmazingMobs plugin, CommandSender s, String[] args) {
        if (!(s instanceof Player p)) { plugin.messages().send(s, "<red>Players only."); return; }
        if (!plugin.classService().enabled()) { plugin.messages().send(s, "<red>The class system is disabled."); return; }
        if (args.length == 0) {
            PlayerClass cur = plugin.classService().classOf(p);
            p.openInventory(new ClassMenu(plugin.classRegistry(), plugin.classManager(), cur).getInventory());
            return;
        }
        switch (plugin.classService().setClass(p, args[0])) {
            case OK -> {} // service announces it
            case SAME -> plugin.messages().send(s, "<gray>That's already your class.");
            case LOCKED -> plugin.messages().send(s, "<red>Class changing is disabled on this server.");
            case UNKNOWN -> plugin.messages().send(s, "<red>Unknown class. Try: <white>" + ids(plugin));
        }
    }

    @Override
    public List<String> tab(AmazingMobs plugin, CommandSender sender, String[] args) {
        if (args.length == 1) {
            String pre = args[0].toLowerCase(Locale.ROOT);
            List<String> out = new ArrayList<>();
            for (PlayerClass c : plugin.classRegistry().classes()) if (c.id().startsWith(pre)) out.add(c.id());
            return out;
        }
        return List.of();
    }

    private static String ids(AmazingMobs plugin) {
        List<String> out = new ArrayList<>();
        for (PlayerClass c : plugin.classRegistry().classes()) out.add(c.id());
        return String.join(", ", out);
    }
}
