package me.zygotecode.amazingmobs.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import java.util.ArrayList;
import java.util.List;

/**
 * Adventure / MiniMessage text helpers. All user-facing strings (names, lore, messages, titles)
 * are authored in MiniMessage so admins get full colour/formatting with a readable syntax.
 */
public final class Text {

    private static final MiniMessage MM = MiniMessage.miniMessage();
    private static final PlainTextComponentSerializer PLAIN = PlainTextComponentSerializer.plainText();

    private Text() {}

    /** Deserialize a MiniMessage string. Null/blank => empty component. */
    public static Component mm(String s) {
        return (s == null || s.isEmpty()) ? Component.empty() : MM.deserialize(s);
    }

    /** Deserialize with {@code <key>} placeholder resolvers. */
    public static Component mm(String s, TagResolver... resolvers) {
        return (s == null || s.isEmpty()) ? Component.empty() : MM.deserialize(s, resolvers);
    }

    /** A simple {@code <name>} -> value placeholder. */
    public static TagResolver ph(String name, String value) {
        return Placeholder.unparsed(name, value == null ? "" : value);
    }

    /** For item names/lore: like {@link #mm(String)} but with italics off (vanilla default is on). */
    public static Component item(String s) {
        return mm(s).decoration(TextDecoration.ITALIC, false);
    }

    public static List<Component> itemLore(List<String> lines) {
        List<Component> out = new ArrayList<>();
        if (lines != null) for (String l : lines) out.add(item(l));
        return out;
    }

    public static List<Component> mmList(List<String> lines) {
        List<Component> out = new ArrayList<>();
        if (lines != null) for (String l : lines) out.add(mm(l));
        return out;
    }

    /** Strip formatting to plain text (for logs, comparisons). */
    public static String plain(Component c) {
        return c == null ? "" : PLAIN.serialize(c);
    }

    public static String plain(String mini) {
        return plain(mm(mini));
    }

    /** Serialize a MiniMessage string to a legacy §-coded string (for APIs that need legacy text). */
    public static String legacy(String mini) {
        return LegacyComponentSerializer.legacySection().serialize(mm(mini));
    }
}
