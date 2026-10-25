package pl.duckmc.core;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class Util {

    public static final MiniMessage MM = MiniMessage.miniMessage();
    public static final String SHARD = "<gradient:#7b2ff7:#f107a3>Smoczy Odłamek</gradient>";
    public static final String PREFIX = "<gradient:#ff9a00:#ff3d77>DuckMC</gradient> <dark_gray>» ";

    private Util() {}

    /** Zwykly komponent (chat, tytuly). */
    public static Component mm(String s) {
        return MM.deserialize(s);
    }

    /** Komponent bez kursywy (nazwy i lore itemow). */
    public static Component item(String s) {
        return MM.deserialize(s).decoration(TextDecoration.ITALIC, false);
    }

    /** 0.5 -> "0.5", 3.0 -> "3", 2.58 -> "2.58". */
    public static String pct(double d) {
        return BigDecimal.valueOf(d).setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }

    public static double round2(double d) {
        return Math.round(d * 100.0) / 100.0;
    }

    /** 6100 -> "$6.1k". */
    public static String money(double v) {
        if (v >= 1_000_000_000) return "$" + trim(v / 1_000_000_000) + "b";
        if (v >= 1_000_000) return "$" + trim(v / 1_000_000) + "m";
        if (v >= 1_000) return "$" + trim(v / 1_000) + "k";
        return "$" + trim(v);
    }

    private static String trim(double v) {
        return BigDecimal.valueOf(v).setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }

    /** Wysrodkowana linia na czacie (przyblizenie szerokosci znakow). */
    public static Component center(String mmText) {
        String plain = PlainTextComponentSerializer.plainText().serialize(MM.deserialize(mmText));
        int w = 0;
        for (char c : plain.toCharArray()) w += charWidth(c) + 1;
        int spaces = Math.max(0, (320 - w) / 2 / 4);
        return MM.deserialize(" ".repeat(spaces) + mmText);
    }

    private static int charWidth(char c) {
        switch (c) {
            case 'i': case '!': case '.': case ',': case ':': case ';': case '\'': case '|': return 1;
            case 'l': return 2;
            case 'I': case 't': case ' ': case '"': case '*': case '(': case ')': case '[': case ']': return 3;
            case 'f': case 'k': case '<': case '>': case '{': case '}': return 4;
            case '@': case '~': return 6;
            default: return 5;
        }
    }
}
