package me.zygotecode.amazingmobs.command.sub;

import me.zygotecode.amazingmobs.AmazingMobs;
import me.zygotecode.amazingmobs.command.SubCommand;
import me.zygotecode.amazingmobs.horde.runtime.HordeInstance;
import org.bukkit.command.CommandSender;

public final class StatusSub implements SubCommand {

    @Override public String name() { return "status"; }
    @Override public String permission() { return "amazingmobs.horde.status"; }
    @Override public String usage() { return "status"; }
    @Override public String description() { return "Show active hordes and runtime counts"; }

    @Override
    public void execute(AmazingMobs plugin, CommandSender s, String[] args) {
        plugin.messages().send(s, "<aqua>Runtime status</aqua>");
        plugin.messages().sendRaw(s, "  <gray>active custom mobs:</gray> <white>" + plugin.mobManager().activeCount()
                + "</white><gray>/" + plugin.mobManager().maxActiveMobs() + "</gray>");
        var hordes = plugin.hordeManager().activeInstances();
        plugin.messages().sendRaw(s, "  <gray>active hordes:</gray> <white>" + hordes.size() + "</white>");
        for (HordeInstance inst : hordes) {
            plugin.messages().sendRaw(s, "    <gold>" + inst.instanceId() + "</gold> <gray>·</gray> wave <white>"
                    + inst.waveNumber() + "/" + inst.definition().waves().size() + "</white> <gray>·</gray> alive <red>"
                    + inst.aliveCount() + "</red> <gray>·</gray> <dark_gray>" + inst.world().getName() + "</dark_gray>");
        }
    }
}
