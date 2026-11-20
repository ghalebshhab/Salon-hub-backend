package com.salon.backend.Services.Subscription;

import com.salon.backend.DTOs.ApiResponse;
import com.salon.backend.DTOs.Subscription.CreateSubscriptionRequest;
import com.salon.backend.DTOs.Subscription.PlanUsage;
import com.salon.backend.DTOs.Subscription.SubscriptionRequestResponse;
import com.salon.backend.DTOs.Subscription.SubscriptionResponse;
import com.salon.backend.Entities.salons.Salon;
import com.salon.backend.Entities.salons.SalonStatus;
import com.salon.backend.Entities.salons.hiringposts.HiringPostStatus;
import com.salon.backend.Entities.subscriptions.SalonSubscription;
import com.salon.backend.Entities.subscriptions.SubscriptionPayment;
import com.salon.backend.Entities.subscriptions.SubscriptionPaymentMethod;
import com.salon.backend.Entities.subscriptions.SubscriptionPaymentStatus;
import com.salon.backend.Entities.subscriptions.SubscriptionPlan;
import com.salon.backend.Entities.subscriptions.SubscriptionStatus;
import com.salon.backend.Repositories.Salon.HiringPostRepo;
import com.salon.backend.Repositories.Salon.SalonRepo;
import com.salon.backend.Repositories.Salon.SalonServiceRepo;
import com.salon.backend.Repositories.Subscription.SalonSubscriptionRepo;
import com.salon.backend.Repositories.Subscription.SubscriptionPaymentRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * The owner side of the salon plan: the plan with usage, and the payment
 * requests that an admin confirms later.
 */
@Service
@RequiredArgsConstructor
public class OwnerPlanService {

    private static final int RECENT_REQUESTS = 5;
    private static final int MAX_REFERENCE_LENGTH = 120;
    private static final String NO_SALON = "An active, approved salon owned by you is required";

    private final SalonRepo salonRepo;
    private final SalonSubscriptionRepo subscriptionRepo;
    private final SubscriptionPaymentRepo paymentRepo;
    private final SalonServiceRepo salonServiceRepo;
    private final HiringPostRepo hiringPostRepo;
    private final BillingDetails billingDetails;
    private final Clock clock;

    @Transactional(readOnly = true)
    public ApiResponse<SubscriptionResponse> getMine(Long ownerId) {
        Salon salon = ownedActiveSalon(ownerId);
        if (salon == null) {
            return ApiResponse.error(NO_SALON);
        }
        return ApiResponse.success("Subscription fetched successfully", toResponse(salon));
    }

    /** Saves a request for a paid plan. A salon has at most one open request. */
    @Transactional
    public ApiResponse<SubscriptionResponse> requestPlan(Long ownerId, CreateSubscriptionRequest request) {
        Salon salon = ownedActiveSalon(ownerId);
        if (salon == null) {
            return ApiResponse.error(NO_SALON);
        }
        String error = validate(request);
        if (error != null) {
            return ApiResponse.error(error);
        }
        LocalDateTime now = now();
        SalonSubscription subscription = subscriptionRepo.findBySalonId(salon.getId())
                .orElseGet(() -> newFreeSubscription(salon, now));
        if (subscription.getId() != null && paymentRepo
                .findFirstBySubscriptionIdAndStatus(subscription.getId(), SubscriptionPaymentStatus.PENDING)
                .isPresent()) {
            return ApiResponse.error("You already have a request waiting. Cancel it first to send a new one.");
        }
        // Saving the subscription raises its version, so two requests sent at
        // the same time cannot both pass the check above.
        subscription.setUpdatedAt(now);
        subscriptionRepo.saveAndFlush(subscription);

        SubscriptionPayment payment = new SubscriptionPayment();
        payment.setSubscription(subscription);
        payment.setPlan(request.plan());
        payment.setBillingCycle(request.billingCycle());
        payment.setAmountFils(request.plan().priceFils(request.billingCycle()));
        payment.setMethod(request.method());
        payment.setReference(cleanReference(request.reference()));
        payment.setStatus(SubscriptionPaymentStatus.PENDING);
        payment.setRequestedBy(salon.getOwner());
        payment.setCreatedAt(now);
        payment.setUpdatedAt(now);
        paymentRepo.save(payment);

        return ApiResponse.success("Request sent. We turn on your plan when we confirm the payment.",
                toResponse(salon));
    }

    @Transactional
    public ApiResponse<SubscriptionResponse> cancelRequest(Long ownerId, Long requestId) {
        Salon salon = ownedActiveSalon(ownerId);
        if (salon == null) {
            return ApiResponse.error(NO_SALON);
        }
        SubscriptionPayment payment = requestId == null ? null : paymentRepo.findById(requestId).orElse(null);
        if (payment == null
                || !Objects.equals(payment.getSubscription().getSalon().getId(), salon.getId())) {
            return ApiResponse.error("Request not found");
        }
        if (payment.getStatus() != SubscriptionPaymentStatus.PENDING) {
            return ApiResponse.error("Only a request that is still waiting can be cancelled");
        }
        payment.setStatus(SubscriptionPaymentStatus.CANCELLED);
        payment.setUpdatedAt(now());
        paymentRepo.save(payment);
        return ApiResponse.success("Request cancelled", toResponse(salon));
    }

    private String validate(CreateSubscriptionRequest request) {
        if (request == null || request.plan() == null || request.billingCycle() == null || request.method() == null) {
            return "Choose a plan, a billing period and a payment method";
        }
        if (!request.plan().isPaid()) {
            return "The Free plan needs no request";
        }
        if (request.method() == SubscriptionPaymentMethod.COMPLIMENTARY) {
            return "Choose CliQ or cash";
        }
        if (request.method() == SubscriptionPaymentMethod.CLIQ && !billingDetails.cliqReady()) {
            return "CliQ payment is not available yet. Choose cash";
        }
        if (request.reference() != null && request.reference().trim().length() > MAX_REFERENCE_LENGTH) {
            return "The reference must be at most " + MAX_REFERENCE_LENGTH + " characters";
        }
        return null;
    }

    private static String cleanReference(String reference) {
        if (reference == null) {
            return null;
        }
        String clean = reference.replaceAll("\\p{Cntrl}", " ").trim();
        return clean.isEmpty() ? null : clean;
    }

    private SalonSubscription newFreeSubscription(Salon salon, LocalDateTime now) {
        SalonSubscription subscription = new SalonSubscription();
        subscription.setSalon(salon);
        subscription.setPlan(SubscriptionPlan.FREE);
        subscription.setStatus(SubscriptionStatus.ENDED);
        subscription.setCreatedAt(now);
        subscription.setUpdatedAt(now);
        return subscription;
    }

    private SubscriptionResponse toResponse(Salon salon) {
        LocalDateTime now = now();
        SalonSubscription subscription = subscriptionRepo.findBySalonId(salon.getId()).orElse(null);
        List<SubscriptionPayment> payments = subscription == null
                ? List.of()
                : paymentRepo.findBySubscriptionIdOrderByCreatedAtDesc(subscription.getId());
        SubscriptionRequestResponse open = payments.stream()
                .filter(payment -> payment.getStatus() == SubscriptionPaymentStatus.PENDING)
                .findFirst()
                .map(OwnerPlanService::toRequestResponse)
                .orElse(null);
        List<SubscriptionRequestResponse> recent = payments.stream()
                .limit(RECENT_REQUESTS)
                .map(OwnerPlanService::toRequestResponse)
                .toList();

        SubscriptionPlan plan = subscription == null ? SubscriptionPlan.FREE : subscription.planAt(now);
        // A trial or period that passed is shown as ended, even before the daily job runs.
        boolean paidTime = subscription != null && plan != SubscriptionPlan.FREE;
        SubscriptionStatus status = paidTime ? subscription.getStatus() : SubscriptionStatus.ENDED;
        LocalDateTime accessEnd = paidTime ? subscription.accessEndsAt() : null;
        Long daysLeft = accessEnd == null
                ? null
                : Math.max(0, (long) Math.ceil(Duration.between(now, accessEnd).toHours() / 24.0));
        Optional<SalonSubscription> stored = Optional.ofNullable(subscription);
        return new SubscriptionResponse(
                salon.getId(),
                plan,
                stored.map(SalonSubscription::getPlan).orElse(SubscriptionPlan.FREE),
                status,
                stored.map(SalonSubscription::getBillingCycle).orElse(null),
                stored.map(SalonSubscription::getTrialEndsAt).orElse(null),
                stored.map(SalonSubscription::getCurrentPeriodEnd).orElse(null),
                stored.map(SalonSubscription::getGraceEndsAt).orElse(null),
                accessEnd,
                daysLeft,
                SubscriptionService.toPlanResponse(plan),
                usage(salon),
                open,
                recent,
                billingDetails.payTo()
        );
    }

    private PlanUsage usage(Salon salon) {
        long team = salon.getCurrentEmployeesNumber() == null ? 1 : salon.getCurrentEmployeesNumber();
        return new PlanUsage(
                team,
                salonServiceRepo.countBySalonIdAndActiveTrue(salon.getId()),
                hiringPostRepo.countBySalonIdAndStatus(salon.getId(), HiringPostStatus.Open)
        );
    }

    static SubscriptionRequestResponse toRequestResponse(SubscriptionPayment payment) {
        return new SubscriptionRequestResponse(
                payment.getId(),
                payment.getPlan(),
                payment.getBillingCycle(),
                payment.getAmountFils(),
                payment.getCurrency(),
                payment.getMethod(),
                payment.getReference(),
                payment.getStatus(),
                payment.getRejectionReason(),
                payment.getCreatedAt(),
                payment.getReviewedAt(),
                payment.getPeriodEnd()
        );
    }

    private Salon ownedActiveSalon(Long ownerId) {
        if (ownerId == null) {
            return null;
        }
        Salon salon = salonRepo.findByOwnerId(ownerId);
        return salon != null
                && salon.getStatus() == SalonStatus.Accepted
                && !salon.isBlockedByAdmin()
                && !salon.isBlockedByOwner()
                ? salon
                : null;
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }
}
