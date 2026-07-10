package me.zygotecode.amazingmobs.command.sub;

import me.zygotecode.amazingmobs.AmazingMobs;
import me.zygotecode.amazingmobs.command.SubCommand;
import me.zygotecode.amazingmobs.player.WeightService;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

/** Spend a maxed-out Weight to gain a permanent prestige tier (a heart + strength), resetting to base. */
public final class PrestigeSub implements SubCommand {

    @Override public String name() { return "prestige"; }
    @Override public String permission() { return "amazingmobs.use"; }
    @Override public String usage() { return "prestige"; }
    @Override public String description() { return "Bank a Weight Prestige (requires max weight)"; }

    @Override
    public void execute(AmazingMobs plugin, CommandSender s, String[] args) {
        if (!(s instanceof Player p)) { plugin.messages().send(s, "<red>Players only."); return; }
        WeightService w = plugin.weightService();
        if (w == null || !w.enabled()) { plugin.messages().send(s, "<red>The Weight system is disabled."); return; }
        switch (w.prestige(p)) {
            case OK -> { /* WeightService sends the fanfare message */ }
            case NOT_ENOUGH -> plugin.messages().send(s, "<red>You need <white>" + (int) w.goal()
                    + "kg</white> to prestige. <gray>You're at <white>" + (int) w.weightOf(p.getUniqueId()) + "kg</white>.</gray>");
            case DISABLED -> plugin.messages().send(s, "<red>The Weight system is disabled.");
        }
    }

    @Override
    public List<String> tab(AmazingMobs plugin, CommandSender sender, String[] args) { return List.of(); }
}
