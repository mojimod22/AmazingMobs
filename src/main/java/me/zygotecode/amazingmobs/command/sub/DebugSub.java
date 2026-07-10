package me.zygotecode.amazingmobs.command.sub;

import me.zygotecode.amazingmobs.AmazingMobs;
import me.zygotecode.amazingmobs.command.SubCommand;
import me.zygotecode.amazingmobs.mob.runtime.ActiveMob;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public final class DebugSub implements SubCommand {

    @Override public String name() { return "debug"; }
    @Override public String permission() { return "amazingmobs.debug"; }
    @Override public String usage() { return "debug"; }
    @Override public String description() { return "Diagnostics (and inspect the mob you look at)"; }

    @Override
    public void execute(AmazingMobs plugin, CommandSender s, String[] args) {
        plugin.messages().send(s, "<aqua>Debug</aqua>");
        plugin.messages().sendRaw(s, "  <gray>registries:</gray> mobs <white>" + plugin.mobRegistry().size()
                + "</white>, hordes <white>" + plugin.hordeRegistry().size() + "</white>, skills <white>"
                + plugin.skillRegistry().size() + "</white>");
        plugin.messages().sendRaw(s, "  <gray>runtime:</gray> mobs <white>" + plugin.mobManager().activeCount()
                + "/" + plugin.mobManager().maxActiveMobs() + "</white>, hordes <white>"
                + plugin.hordeManager().activeCount() + "</white>");
        plugin.messages().sendRaw(s, "  <gray>perf:</gray> mob-period <white>" + plugin.config().mobControllerPeriod
                + "t</white>, horde-tick <white>" + plugin.config().hordeTickInterval + "t</white>");
        for (var inst : plugin.hordeManager().activeInstances()) {
            plugin.messages().sendRaw(s, "  <gray>horde</gray> <gold>" + inst.instanceId()
                    + "</gold> <gray>director x</gray><white>"
                    + String.format(java.util.Locale.ROOT, "%.2f", inst.spawnMultiplier()) + "</white>"
                    + (inst.paused() ? " <yellow>(paused)</yellow>" : ""));
        }

        if (s instanceof Player p) {
            Entity looked = p.getTargetEntity(32);
            if (looked instanceof LivingEntity le) {
                ActiveMob am = plugin.mobManager().get(le);
                if (am != null) {
                    plugin.messages().sendRaw(s, "  <gray>looking at:</gray> " + am.definition().tier().color()
                            + am.definition().id() + " <gray>health</gray> <red>"
                            + String.format(java.util.Locale.ROOT, "%.1f", le.getHealth()) + "</red>"
                            + (am.meta().hordeInstanceId() != null ? " <gray>horde</gray> <gold>" + am.meta().hordeInstanceId() + "</gold>" : ""));
                    plugin.messages().sendRaw(s, "  <gray>traits:</gray> <white>"
                            + (am.definition().traits().isEmpty() ? "none"
                            : am.definition().traits().stream().map(t -> t.id()).collect(java.util.stream.Collectors.joining(", ")))
                            + "</white>"
                            + (le.getVehicle() != null ? " <aqua>mounted</aqua>" : "")
                            + (le.getPassengers().isEmpty() ? "" : " <aqua>carrying " + le.getPassengers().size() + "</aqua>"));
                    if (le instanceof org.bukkit.entity.Mob mob) {
                        var tgt = mob.getTarget();
                        plugin.messages().sendRaw(s, "  <gray>target:</gray> <white>" + (tgt != null ? tgt.getName() : "none")
                                + "</white>" + (tgt != null ? " <gray>@</gray> <white>"
                                + String.format(java.util.Locale.ROOT, "%.1f", le.getLocation().distance(tgt.getLocation()))
                                + "m</white>" : "") + " <gray>path:</gray> <white>" + mob.getPathfinder().hasPath() + "</white>");
                    }
                } else {
                    plugin.messages().sendRaw(s, "  <gray>looking at a non-AmazingMobs entity.</gray>");
                }
            }
        }
    }
}
