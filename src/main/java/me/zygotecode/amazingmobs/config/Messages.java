package me.zygotecode.amazingmobs.config;

import me.zygotecode.amazingmobs.util.Text;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.command.CommandSender;

/**
 * Loads {@code messages.yml} and renders MiniMessage strings with a configurable prefix. Command
 * feedback passes literal MiniMessage; the {@code prefix} (and any keyed strings) are admin-editable.
 */
public final class Messages {

    private String prefix = "<gradient:#ff5e62:#ff9966><bold>AmazingMobs</bold></gradient> <dark_gray>»</dark_gray> ";

    public void load(ConfigSection root) {
        String p = root.getString("prefix");
        if (p != null) this.prefix = p;
    }

    /** Render a raw MiniMessage string with the prefix prepended. */
    public Component prefixed(String mini, TagResolver... ph) {
        return Text.mm(prefix + mini, ph);
    }

    /** Render a raw MiniMessage string with no prefix. */
    public Component plain(String mini, TagResolver... ph) {
        return Text.mm(mini, ph);
    }

    public void send(CommandSender sender, String mini, TagResolver... ph) {
        sender.sendMessage(prefixed(mini, ph));
    }

    public void sendRaw(CommandSender sender, String mini, TagResolver... ph) {
        sender.sendMessage(plain(mini, ph));
    }
}
