package com.salon.backend.Services.Assistant;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JordanGovernorateTest {

    @Test
    void readsStoredSalonLocations() {
        assertEquals(Optional.of(JordanGovernorate.AMMAN), JordanGovernorate.fromLocation("Amman"));
        assertEquals(Optional.of(JordanGovernorate.BALQA), JordanGovernorate.fromLocation("As-Salt"));
        assertEquals(Optional.of(JordanGovernorate.KARAK), JordanGovernorate.fromLocation("Al-Karak - Jordan"));
        assertEquals(Optional.of(JordanGovernorate.MAAN), JordanGovernorate.fromLocation("Ma'an"));
    }

    @Test
    void findsTheGovernorateInAQuestion() {
        assertEquals(Optional.of(JordanGovernorate.IRBID),
                JordanGovernorate.mentionedIn("best salons in IRBID please"));
        assertEquals(Optional.of(JordanGovernorate.AQABA),
                JordanGovernorate.mentionedIn("أفضل صالون في العقبة"));
        assertEquals(Optional.of(JordanGovernorate.IRBID),
                JordanGovernorate.mentionedIn("صالونات إربد"));
    }

    @Test
    void matchesWholeWordsOnly() {
        assertEquals(Optional.of(JordanGovernorate.AMMAN), JordanGovernorate.mentionedIn("salons in amman"));
        assertEquals(Optional.empty(), JordanGovernorate.mentionedIn("a salty haircut"));
        assertEquals(Optional.empty(), JordanGovernorate.mentionedIn(null));
    }

    @Test
    void takesTheFirstPlaceNamed() {
        assertEquals(Optional.of(JordanGovernorate.ZARQA),
                JordanGovernorate.mentionedIn("zarqa or amman"));
    }
}
