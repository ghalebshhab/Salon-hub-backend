package com.salon.backend.Services.Translation;

import java.util.Objects;

/** The rule for the Arabic copy of an English field when the owner saves. */
public final class ArabicText {

    private ArabicText() {
    }

    /**
     * The Arabic value to store. Null means "translate it again".
     *
     * @param requested     the Arabic from the form: null when the form did not send it,
     *                      blank when the owner wants a new translation
     * @param savedArabic   the Arabic stored now
     * @param savedEnglish  the English stored now
     * @param newEnglish    the English the owner saves
     * @param maxLength     the size of the Arabic column
     */
    public static String resolve(
            String requested, String savedArabic, String savedEnglish, String newEnglish, int maxLength) {
        if (newEnglish == null || newEnglish.isBlank()) {
            return null;
        }
        boolean englishChanged = !Objects.equals(clean(savedEnglish), clean(newEnglish));
        if (requested == null) {
            return englishChanged ? null : clean(savedArabic);
        }
        String arabic = clean(requested);
        if (arabic == null) {
            return null;
        }
        // The form sent back the old Arabic for new English: it is out of date.
        if (englishChanged && arabic.equals(clean(savedArabic))) {
            return null;
        }
        return arabic.length() > maxLength ? arabic.substring(0, maxLength) : arabic;
    }

    static String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
