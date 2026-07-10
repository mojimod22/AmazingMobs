package me.zygotecode.amazingmobs.command.sub;

import me.zygotecode.amazingmobs.AmazingMobs;
import me.zygotecode.amazingmobs.command.CliEntries;
import me.zygotecode.amazingmobs.command.SubCommand;
import me.zygotecode.amazingmobs.config.ConfigSection;
import me.zygotecode.amazingmobs.config.validation.ValidationReport;
import me.zygotecode.amazingmobs.skill.SkillDefinition;
import me.zygotecode.amazingmobs.skill.SkillParser;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;

/**
 * {@code give-skill <skillId> [key=val...]} — inject a skill onto the live mob you look at. Trigger
 * keys (cooldown, target, max-range, types, duration…) route to the trigger; the rest are params.
 * e.g. {@code /am give-skill fireball cooldown=2s target=NEAREST_PLAYER count=3 max-range=30}
 */
public final class GiveSkillSub implements SubCommand {

    @Override public String name() { return "give-skill"; }
    @Override public String permission() { return "amazingmobs.mob.edit"; }
    @Override public String usage() { return "give-skill <skillId> [key=val...]"; }
    @Override public String description() { return "Inject a skill onto the mob you look at (live test)"; }

    @Override
    public void execute(AmazingMobs plugin, CommandSender s, String[] args) {
        if (!(s instanceof Player p)) { plugin.messages().send(s, "<red>Players only."); return; }
        if (args.length < 1) { plugin.messages().send(s, "<red>Usage: /am " + usage()); return; }
        if (!plugin.skillRegistry().contains(args[0])) { plugin.messages().send(s, "<red>Unknown skill '" + args[0] + "'."); return; }
        Entity looked = p.getTargetEntity(32);
        if (looked == null || !plugin.mobManager().isCustomMob(looked)) {
            plugin.messages().send(s, "<red>Look at an AmazingMobs mob first."); return;
        }
        var map = CliEntries.skillEntry(args[0], Arrays.asList(Arrays.copyOfRange(args, 1, args.length)));
        ValidationReport report = new ValidationReport("give-skill");
        SkillDefinition def = SkillParser.parse(ConfigSection.of(map), plugin.skillRegistry().ids(), report);
        if (def == null) { plugin.messages().send(s, "<red>Invalid skill spec."); return; }
        boolean ok = plugin.mobManager().giveSkill(looked, def);
        plugin.messages().send(s, ok ? "<green>Injected skill <white>" + args[0] + "</white> onto the mob."
                : "<red>Could not inject skill.");
    }

    @Override
    public List<String> tab(AmazingMobs plugin, CommandSender sender, String[] args) {
        return args.length == 1 ? InfoSub.prefix(plugin.skillRegistry().ids(), args[0]) : List.of();
    }
}
