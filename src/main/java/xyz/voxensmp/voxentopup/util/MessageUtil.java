package xyz.voxensmp.voxentopup.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;

import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Central renderer for configured messages, placeholders, legacy colours, and MiniMessage. */
public final class MessageUtil {
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final Pattern PLACEHOLDER = Pattern.compile("(?i)%([a-z0-9_-]+)%|<([a-z0-9_-]+)>");
    private static final Pattern LEGACY_HEX = Pattern.compile("(?i)&#([0-9a-f]{6})");
    private static final Map<Character, String> LEGACY_TAGS = Map.ofEntries(
            Map.entry('0', "black"), Map.entry('1', "dark_blue"), Map.entry('2', "dark_green"),
            Map.entry('3', "dark_aqua"), Map.entry('4', "dark_red"), Map.entry('5', "dark_purple"),
            Map.entry('6', "gold"), Map.entry('7', "gray"), Map.entry('8', "dark_gray"),
            Map.entry('9', "blue"), Map.entry('a', "green"), Map.entry('b', "aqua"),
            Map.entry('c', "red"), Map.entry('d', "light_purple"), Map.entry('e', "yellow"),
            Map.entry('f', "white"), Map.entry('k', "obfuscated"), Map.entry('l', "bold"),
            Map.entry('m', "strikethrough"), Map.entry('n', "underlined"), Map.entry('o', "italic"),
            Map.entry('r', "reset"));

    private MessageUtil() {
    }

    /**
     * Replaces only known values in both %name% and &lt;name&gt; forms (case-insensitive),
     * leaving all other angle tags for MiniMessage to interpret.
     */
    public static Component render(String message, Map<String, String> placeholders) {
        if (message == null || message.isEmpty()) return Component.empty();
        String rendered = normalizeLegacyColours(replacePlaceholders(message, placeholders));
        try {
            return MINI_MESSAGE.deserialize(rendered);
        } catch (RuntimeException exception) {
            // Invalid administrator formatting must never interrupt a transaction broadcast.
            return Component.text(message);
        }
    }

    public static void broadcast(Iterable<String> lines, Map<String, String> placeholders) {
        for (String line : lines) Bukkit.broadcast(render(line, placeholders));
    }

    private static String replacePlaceholders(String message, Map<String, String> placeholders) {
        Matcher matcher = PLACEHOLDER.matcher(message);
        StringBuffer output = new StringBuffer();
        while (matcher.find()) {
            String key = (matcher.group(1) == null ? matcher.group(2) : matcher.group(1)).toLowerCase(Locale.ROOT);
            String value = findIgnoreCase(placeholders, key);
            // Unknown angle tags, such as <gradient> and <bold>, remain untouched for MiniMessage.
            matcher.appendReplacement(output, Matcher.quoteReplacement(value == null ? matcher.group() : value));
        }
        matcher.appendTail(output);
        return output.toString();
    }

    private static String findIgnoreCase(Map<String, String> placeholders, String expectedKey) {
        String direct = placeholders.get(expectedKey);
        if (direct != null) return direct;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(expectedKey)) return entry.getValue();
        }
        return null;
    }

    private static String normalizeLegacyColours(String text) {
        Matcher hexMatcher = LEGACY_HEX.matcher(text);
        StringBuffer hexOutput = new StringBuffer();
        while (hexMatcher.find()) {
            hexMatcher.appendReplacement(hexOutput, Matcher.quoteReplacement("<#" + hexMatcher.group(1) + ">"));
        }
        hexMatcher.appendTail(hexOutput);

        StringBuilder output = new StringBuilder(hexOutput.length());
        for (int index = 0; index < hexOutput.length(); index++) {
            char character = hexOutput.charAt(index);
            if (character == '&' && index + 1 < hexOutput.length()) {
                String tag = LEGACY_TAGS.get(Character.toLowerCase(hexOutput.charAt(index + 1)));
                if (tag != null) {
                    output.append('<').append(tag).append('>');
                    index++;
                    continue;
                }
            }
            output.append(character);
        }
        return output.toString();
    }
}
