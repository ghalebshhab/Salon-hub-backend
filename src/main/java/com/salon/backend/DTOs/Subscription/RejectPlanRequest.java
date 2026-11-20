package com.salon.backend.DTOs.Subscription;

/** Why the admin did not confirm the payment; the owner sees it. */
public record RejectPlanRequest(String reason) {
}
