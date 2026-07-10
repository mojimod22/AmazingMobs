package me.zygotecode.amazingmobs.command;

import me.zygotecode.amazingmobs.AmazingMobs;
import me.zygotecode.amazingmobs.command.sub.AddDropSub;
import me.zygotecode.amazingmobs.command.sub.AddSkillSub;
import me.zygotecode.amazingmobs.command.sub.AddTraitSub;
import me.zygotecode.amazingmobs.command.sub.ClassSub;
import me.zygotecode.amazingmobs.command.sub.CloneSub;
import me.zygotecode.amazingmobs.command.sub.CreateSub;
import me.zygotecode.amazingmobs.command.sub.DebugSub;
import me.zygotecode.amazingmobs.command.sub.DeleteSub;
import me.zygotecode.amazingmobs.command.sub.EditSub;
import me.zygotecode.amazingmobs.command.sub.ExportSub;
import me.zygotecode.amazingmobs.command.sub.FreezeSub;
import me.zygotecode.amazingmobs.command.sub.GiveSkillSub;
import me.zygotecode.amazingmobs.command.sub.GiveSub;
import me.zygotecode.amazingmobs.command.sub.GiveTraitSub;
import me.zygotecode.amazingmobs.command.sub.InfoSub;
import me.zygotecode.amazingmobs.command.sub.ListSub;
import me.zygotecode.amazingmobs.command.sub.MountSub;
import me.zygotecode.amazingmobs.command.sub.ReloadSub;
import me.zygotecode.amazingmobs.command.sub.RmEntrySub;
import me.zygotecode.amazingmobs.command.sub.PrestigeSub;
import me.zygotecode.amazingmobs.command.sub.SaveSub;
import me.zygotecode.amazingmobs.command.sub.SetArenaPosSub;
import me.zygotecode.amazingmobs.command.sub.SetSub;
import me.zygotecode.amazingmobs.command.sub.SkillsSub;
import me.zygotecode.amazingmobs.command.sub.ShowSessionSub;
import me.zygotecode.amazingmobs.command.sub.SpawnSub;
import me.zygotecode.amazingmobs.command.sub.StackSub;
import me.zygotecode.amazingmobs.command.sub.StartSub;
import me.zygotecode.amazingmobs.command.sub.StatusSub;
import me.zygotecode.amazingmobs.command.sub.StopSub;
import me.zygotecode.amazingmobs.command.sub.TestSub;
import me.zygotecode.amazingmobs.command.sub.TpAllSub;
import me.zygotecode.amazingmobs.command.sub.ValidateSub;
import me.zygotecode.amazingmobs.command.sub.VariantSub;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Root {@code /amazingmobs} (alias {@code /am}) executor + tab completer. Routes to registered
 * {@link SubCommand}s with per-sub permission checks and delegated, context-aware tab completion.
 */
public final class AmazingMobsCommand implements CommandExecutor, TabCompleter {

    private final AmazingMobs plugin;
    private final SessionManager sessions = new SessionManager();
    private final Map<String, SubCommand> subs = new LinkedHashMap<>();

    public AmazingMobsCommand(AmazingMobs plugin) {
        this.plugin = plugin;
        add(new ReloadSub());
        add(new ValidateSub());
        add(new ListSub());
        add(new InfoSub());
        add(new SpawnSub());
        add(new TestSub());
        add(new GiveSub());
        add(new MountSub());
        add(new StackSub());
        add(new VariantSub());
        add(new CreateSub(sessions));
        add(new SetSub(sessions));
        add(new AddSkillSub(sessions));
        add(new AddTraitSub(sessions));
        add(new AddDropSub(sessions));
        add(new RmEntrySub(sessions));
        add(new ShowSessionSub(sessions));
        add(new SaveSub(sessions));
        add(new EditSub(sessions));
        add(new CloneSub(sessions));
        add(new GiveTraitSub());
        add(new GiveSkillSub());
        add(new DeleteSub());
        add(new ExportSub());
        add(new StartSub());
        add(new StopSub());
        add(new FreezeSub(true));
        add(new FreezeSub(false));
        add(new StatusSub());
        add(new DebugSub());
        add(new SetArenaPosSub());
        add(new TpAllSub());
        add(new PrestigeSub());
        add(new ClassSub());
        add(new SkillsSub());
    }

    private void add(SubCommand sub) {
        subs.put(sub.name(), sub);
    }

    /** The shared edit-session manager (also a Listener — registered so sessions clear on quit). */
    public SessionManager sessions() {
        return sessions;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            help(sender);
            return true;
        }
        SubCommand sub = subs.get(args[0].toLowerCase(Locale.ROOT));
        if (sub == null) {
            plugin.messages().send(sender, "<red>Unknown subcommand '" + args[0] + "'. Try <white>/am help</white>.");
            return true;
        }
        if (sub.permission() != null && !sender.hasPermission(sub.permission())) {
            plugin.messages().send(sender, "<red>You don't have permission for that.");
            return true;
        }
        String[] rest = Arrays.copyOfRange(args, 1, args.length);
        try {
            sub.execute(plugin, sender, rest);
        } catch (Exception ex) {
            plugin.messages().send(sender, "<red>Error running /am " + sub.name() + ": " + ex.getMessage());
            plugin.getLogger().warning("Command /am " + sub.name() + " failed: " + ex);
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String pre = args[0].toLowerCase(Locale.ROOT);
            List<String> out = new ArrayList<>();
            out.add("help");
            for (SubCommand sub : subs.values()) {
                if ((sub.permission() == null || sender.hasPermission(sub.permission())) && sub.name().startsWith(pre)) {
                    out.add(sub.name());
                }
            }
            return out;
        }
        SubCommand sub = subs.get(args[0].toLowerCase(Locale.ROOT));
        if (sub == null || (sub.permission() != null && !sender.hasPermission(sub.permission()))) return List.of();
        return sub.tab(plugin, sender, Arrays.copyOfRange(args, 1, args.length));
    }

    private void help(CommandSender sender) {
        plugin.messages().sendRaw(sender, "<gradient:#ff5e62:#ff9966><bold>AmazingMobs</bold></gradient> <gray>— commands</gray>");
        for (SubCommand sub : subs.values()) {
            if (sub.permission() != null && !sender.hasPermission(sub.permission())) continue;
            plugin.messages().sendRaw(sender, "  <white>/am " + sub.usage() + "</white> <dark_gray>·</dark_gray> <gray>"
                    + sub.description() + "</gray>");
        }
    }
}
