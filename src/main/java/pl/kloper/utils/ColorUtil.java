package pl.kloper.utils;

import org.bukkit.ChatColor;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ColorUtil {

    private static final Pattern HEX_PATTERN = Pattern.compile("&x(&[a-fA-F0-9]){6}");

    private ColorUtil() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static String color(String text) {
        if (text == null) return "";

        Matcher matcher = HEX_PATTERN.matcher(text);
        while (matcher.find()) {
            String hex = matcher.group().replace("&x", "").replace("&", "");
            text = text.replace(matcher.group(), "§x" +
                    "§" + hex.charAt(0) + "§" + hex.charAt(1) +
                    "§" + hex.charAt(2) + "§" + hex.charAt(3) +
                    "§" + hex.charAt(4) + "§" + hex.charAt(5));
        }

        return ChatColor.translateAlternateColorCodes('&', text);
    }
}