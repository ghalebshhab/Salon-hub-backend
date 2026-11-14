package com.salon.backend.Services.Assistant;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * The twelve governorates of Jordan. Salon locations are free text, so each
 * governorate keeps the English and Arabic spellings people use for it.
 */
public enum JordanGovernorate {
    AMMAN("Amman", List.of("amman", "عمان", "عمّان")),
    IRBID("Irbid", List.of("irbid", "اربد", "إربد")),
    ZARQA("Zarqa", List.of("zarqa", "az zarqa", "zarka", "الزرقاء", "زرقاء")),
    BALQA("As-Salt", List.of("as-salt", "as salt", "salt", "balqa", "al balqa", "السلط", "البلقاء")),
    MADABA("Madaba", List.of("madaba", "مادبا")),
    KARAK("Al-Karak", List.of("al-karak", "al karak", "karak", "الكرك", "كرك")),
    TAFILAH("Tafilah", List.of("tafilah", "tafila", "at tafilah", "الطفيلة", "طفيلة")),
    MAAN("Ma'an", List.of("ma'an", "maan", "معان")),
    AQABA("Aqaba", List.of("aqaba", "al aqabah", "العقبة", "عقبة")),
    MAFRAQ("Mafraq", List.of("mafraq", "al mafraq", "المفرق", "مفرق")),
    JERASH("Jerash", List.of("jerash", "jarash", "جرش")),
    AJLOUN("Ajloun", List.of("ajloun", "ajlun", "عجلون"));

    private static final Pattern MARKS = Pattern.compile("\\p{M}+");

    private final String displayName;
    private final List<String> aliases;

    JordanGovernorate(String displayName, List<String> aliases) {
        this.displayName = displayName;
        this.aliases = aliases;
    }

    public String getDisplayName() {
        return displayName;
    }

    /** Governorate of a stored salon location ("Amman", "Irbid - Jordan", ...). */
    public static Optional<JordanGovernorate> fromLocation(String location) {
        return mentionedIn(location);
    }

    /**
     * First governorate named in free text. Aliases match whole words only,
     * so "Ma'an" is not found inside "Amman".
     */
    public static Optional<JordanGovernorate> mentionedIn(String text) {
        if (text == null || text.isBlank()) {
            return Optional.empty();
        }
        String haystack = " " + normalize(text) + " ";
        JordanGovernorate best = null;
        int bestIndex = Integer.MAX_VALUE;
        for (JordanGovernorate governorate : values()) {
            for (String alias : governorate.aliases) {
                int index = haystack.indexOf(" " + normalize(alias) + " ");
                if (index >= 0 && index < bestIndex) {
                    best = governorate;
                    bestIndex = index;
                }
            }
        }
        return Optional.ofNullable(best);
    }

    static String normalize(String value) {
        String decomposed = Normalizer.normalize(value, Normalizer.Form.NFKD);
        return MARKS.matcher(decomposed)
                .replaceAll("")
                .toLowerCase(Locale.ROOT)
                .replace('’', '\'')
                .replaceAll("[^\\p{L}\\p{N}']+", " ")
                .trim();
    }
}
