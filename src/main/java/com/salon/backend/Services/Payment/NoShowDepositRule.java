package com.salon.backend.Services.Payment;

import com.salon.backend.Entities.subscriptions.PlanFeature;
import com.salon.backend.Services.Subscription.PlanLimits;
import com.salon.backend.DTOs.Payment.PaymentOptionsResponse;
import com.salon.backend.Entities.payments.PaymentMethod;
import com.salon.backend.Entities.salons.Salon;
import com.salon.backend.Entities.salons.bookings.BookingStatus;
import com.salon.backend.Repositories.Salon.BookingRepo;
import com.salon.backend.Repositories.Salon.SalonRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * The salon's no-show deposit rule. A customer who reached the salon's limit
 * of missed visits must pay in advance by card. While no card provider is
 * connected, that customer cannot book online and must contact the salon.
 */
@Component
@RequiredArgsConstructor
public class NoShowDepositRule {

    private final BookingRepo bookingRepo;
    private final SalonRepo salonRepo;
    private final PaymentSettingsService settingsService;
    private final PaymentGateway paymentGateway;
    private final PlanLimits planLimits;

    /** The rule's result for one customer; {@code restricted} false means no limit applies. */
    public record Decision(boolean restricted, boolean prepaidAvailable, String message) {

        static final Decision NONE = new Decision(false, false, null);
    }

    public Decision decide(Salon salon, Long customerId) {
        if (salon == null || customerId == null || planLimits.locked(salon, PlanFeature.MISSED_VISIT_RULE)) {
            return Decision.NONE;
        }
        var settings = settingsService.effectiveSettings(salon);
        Integer limit = settings.getNoShowLimit();
        if (limit == null || limit < 1) {
            return Decision.NONE;
        }
        long missed = bookingRepo.countBySalonIdAndCustomerIdAndStatus(
                salon.getId(), customerId, BookingStatus.NO_SHOW);
        if (missed < limit) {
            return Decision.NONE;
        }
        String visits = missed == 1 ? "1 visit" : missed + " visits";
        boolean prepaid = paymentGateway.isConfigured() && settings.allows(PaymentMethod.CARD_PREPAID);
        return new Decision(true, prepaid, prepaid
                ? "You missed " + visits + " at this salon, so it asks you to pay in advance by card."
                : "You missed " + visits + " at this salon, so you cannot book online for now. "
                        + "Please contact the salon to book.");
    }

    /** Null when the customer may book with this method, else the reason. */
    public String bookingError(Salon salon, Long customerId, PaymentMethod method) {
        Decision decision = decide(salon, customerId);
        if (!decision.restricted()) {
            return null;
        }
        return decision.prepaidAvailable() && method == PaymentMethod.CARD_PREPAID
                ? null
                : decision.message();
    }

    /** The salon's public options, narrowed for a signed-in customer the rule limits. */
    @Transactional(readOnly = true)
    public PaymentOptionsResponse forCustomer(PaymentOptionsResponse options, Long customerId) {
        if (options == null || customerId == null) {
            return options;
        }
        Salon salon = salonRepo.findById(options.salonId()).orElse(null);
        Decision decision = decide(salon, customerId);
        if (!decision.restricted()) {
            return options;
        }
        List<PaymentMethod> methods = decision.prepaidAvailable()
                ? List.of(PaymentMethod.CARD_PREPAID)
                : List.of();
        return new PaymentOptionsResponse(
                options.salonId(),
                methods,
                methods.isEmpty() ? null : methods.get(0),
                options.currency(),
                options.freeCancelHours(),
                true,
                decision.message()
        );
    }
}
