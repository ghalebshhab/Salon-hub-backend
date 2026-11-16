package com.salon.backend.Services.Payment;

import org.springframework.stereotype.Component;

/**
 * Default gateway while no card provider is chosen. A real provider bean
 * marked {@code @Primary} replaces it without other code changes.
 */
@Component
public class UnconfiguredPaymentGateway implements PaymentGateway {

    @Override
    public boolean isConfigured() {
        return false;
    }

    @Override
    public String providerName() {
        return "none";
    }
}
