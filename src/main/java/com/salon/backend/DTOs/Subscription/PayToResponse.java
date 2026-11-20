package com.salon.backend.DTOs.Subscription;

/** Where the owner sends the plan payment. A null alias means CliQ is not set up yet. */
public record PayToResponse(
        String cliqAlias,
        String cliqName,
        String supportPhone
) {
}
