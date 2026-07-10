package me.zygotecode.amazingmobs.command.sub;

import me.zygotecode.amazingmobs.AmazingMobs;
import me.zygotecode.amazingmobs.clazz.SkillMenu;
import me.zygotecode.amazingmobs.command.SubCommand;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

/** {@code /am skills} — open the skill menu for your class (view + cast, with cooldowns). */
public final class SkillsSub implements SubCommand {

    @Override public String name() { return "skills"; }
    @Override public String permission() { return "amazingmobs.use"; }
    @Override public String usage() { return "skills"; }
    @Override public String description() { return "View your class skills, cooldowns, and cast them"; }

    @Override
    public void execute(AmazingMobs plugin, CommandSender s, String[] args) {
        if (!(s instanceof Player p)) { plugin.messages().send(s, "<red>Players only."); return; }
        if (!plugin.classService().enabled()) { plugin.messages().send(s, "<red>The class system is disabled."); return; }
        if (plugin.classService().classOf(p) == null) {
            plugin.messages().send(s, "<yellow>Pick a class first with <white>/am class</white>.");
            return;
        }
        p.openInventory(new SkillMenu(plugin.classManager(), plugin.classService(), p).getInventory());
    }
}
