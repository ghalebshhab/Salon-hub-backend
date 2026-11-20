package com.salon.backend.Services.Subscription;

import com.salon.backend.DTOs.Subscription.PayToResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** SalonHub's own payment details for plan payments, from the settings. */
@Component
public class BillingDetails {

    private final String cliqAlias;
    private final String cliqName;
    private final String supportPhone;

    public BillingDetails(
            @Value("${salon.billing.cliq-alias:}") String cliqAlias,
            @Value("${salon.billing.cliq-name:SalonHub}") String cliqName,
            @Value("${salon.billing.support-phone:}") String supportPhone) {
        this.cliqAlias = blankToNull(cliqAlias);
        this.cliqName = blankToNull(cliqName);
        this.supportPhone = blankToNull(supportPhone);
    }

    public boolean cliqReady() {
        return cliqAlias != null;
    }

    public PayToResponse payTo() {
        return new PayToResponse(cliqAlias, cliqName, supportPhone);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
