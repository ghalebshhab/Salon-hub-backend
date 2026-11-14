package com.salon.backend.Services.Assistant;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Limits how many model-written replies one member gets in a time window.
 * Over the limit the built-in guide answers, so the chat keeps working.
 */
@Component
public class AssistantRateLimiter {

    static final int MAX_REPLIES = 20;
    static final Duration WINDOW = Duration.ofMinutes(10);

    private final Map<Long, Deque<Instant>> calls = new ConcurrentHashMap<>();
    private final Clock clock;

    public AssistantRateLimiter() {
        this(Clock.systemUTC());
    }

    AssistantRateLimiter(Clock clock) {
        this.clock = clock;
    }

    public boolean tryAcquire(Long userId) {
        if (userId == null) {
            return false;
        }
        Instant now = clock.instant();
        Deque<Instant> times = calls.computeIfAbsent(userId, key -> new ArrayDeque<>());
        synchronized (times) {
            while (!times.isEmpty() && times.peekFirst().isBefore(now.minus(WINDOW))) {
                times.pollFirst();
            }
            if (times.size() >= MAX_REPLIES) {
                return false;
            }
            times.addLast(now);
            return true;
        }
    }
}
