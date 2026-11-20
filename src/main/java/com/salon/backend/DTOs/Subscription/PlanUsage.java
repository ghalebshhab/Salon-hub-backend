package com.salon.backend.DTOs.Subscription;

/** What the salon uses now, to compare with the limits of its plan. */
public record PlanUsage(
        /** The owner and the employees. */
        long teamMembers,
        long activeServices,
        long openHiringPosts
) {
}
