package com.salon.backend.Services.Assistant;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AssistantRateLimiterTest {

    @Test
    void stopsAtTheLimitAndRecoversAfterTheWindow() {
        AtomicReference<Instant> now = new AtomicReference<>(Instant.parse("2026-11-14T10:00:00Z"));
        Clock clock = new Clock() {
            @Override
            public ZoneId getZone() {
                return ZoneOffset.UTC;
            }

            @Override
            public Clock withZone(ZoneId zone) {
                return this;
            }

            @Override
            public Instant instant() {
                return now.get();
            }
        };
        AssistantRateLimiter limiter = new AssistantRateLimiter(clock);

        for (int i = 0; i < AssistantRateLimiter.MAX_REPLIES; i++) {
            assertTrue(limiter.tryAcquire(7L));
        }
        assertFalse(limiter.tryAcquire(7L));
        assertTrue(limiter.tryAcquire(8L), "other members have their own limit");

        now.set(now.get().plus(AssistantRateLimiter.WINDOW).plusSeconds(1));
        assertTrue(limiter.tryAcquire(7L));
    }

    @Test
    void guestsNeverGetModelReplies() {
        assertFalse(new AssistantRateLimiter().tryAcquire(null));
    }
}
