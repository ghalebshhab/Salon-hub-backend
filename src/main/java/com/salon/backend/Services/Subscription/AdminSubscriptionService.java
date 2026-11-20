package com.salon.backend.Services.Subscription;

import com.salon.backend.DTOs.ApiResponse;
import com.salon.backend.DTOs.Subscription.AdminPlanRequestResponse;
import com.salon.backend.DTOs.Subscription.AdminSalonPlanResponse;
import com.salon.backend.DTOs.Subscription.ChangePlanRequest;
import com.salon.backend.DTOs.Subscription.GrantPlanRequest;
import com.salon.backend.DTOs.Subscription.RejectPlanRequest;
import com.salon.backend.Entities.salons.Salon;
import com.salon.backend.Entities.salons.SalonStatus;
import com.salon.backend.Entities.subscriptions.BillingCycle;
import com.salon.backend.Entities.subscriptions.SalonSubscription;
import com.salon.backend.Entities.subscriptions.SubscriptionPayment;
import com.salon.backend.Entities.subscriptions.SubscriptionPaymentMethod;
import com.salon.backend.Entities.subscriptions.SubscriptionPaymentStatus;
import com.salon.backend.Entities.subscriptions.SubscriptionPlan;
import com.salon.backend.Entities.subscriptions.SubscriptionStatus;
import com.salon.backend.Entities.users.User;
import com.salon.backend.Repositories.Salon.SalonRepo;
import com.salon.backend.Repositories.Subscription.SalonSubscriptionRepo;
import com.salon.backend.Repositories.Subscription.SubscriptionPaymentRepo;
import com.salon.backend.Repositories.User.UserRepo;
import com.salon.backend.Services.Email.EmailNames;
import com.salon.backend.Services.Email.EmailService;
import com.salon.backend.Services.Email.EmailTemplates;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * The admin side of salon plans: the queue of owner requests, confirming or
 * refusing a payment, free months, and moving a salon to another plan.
 * Every change records the admin and the time on the payment row.
 */
@Service
@RequiredArgsConstructor
public class AdminSubscriptionService {

    static final int MAX_GRANT_MONTHS = 12;
    private static final int MAX_TEXT = 500;
    private static final int MAX_NOTE = 120;
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("d MMM yyyy");

    private final SalonRepo salonRepo;
    private final UserRepo userRepo;
    private final SalonSubscriptionRepo subscriptionRepo;
    private final SubscriptionPaymentRepo paymentRepo;
    private final EmailService emailService;
    private final EmailTemplates emailTemplates;
    private final Clock clock;

    /** Waiting requests oldest first, or the latest 100 of every status when {@code all}. */
    @Transactional(readOnly = true)
    public ApiResponse<List<AdminPlanRequestResponse>> getRequests(boolean all) {
        LocalDateTime now = now();
        List<SubscriptionPayment> payments = all
                ? paymentRepo.findTop100ByOrderByCreatedAtDesc()
                : paymentRepo.findByStatusOrderByCreatedAtAsc(SubscriptionPaymentStatus.PENDING);
        List<AdminPlanRequestResponse> rows = payments.stream()
                .map(payment -> {
                    Salon salon = payment.getSubscription().getSalon();
                    User owner = salon.getOwner();
                    return new AdminPlanRequestResponse(
                            salon.getId(),
                            salon.getName(),
                            EmailNames.fullName(owner),
                            owner == null ? null : owner.getEmail(),
                            owner == null ? null : owner.getPhoneNumber(),
                            payment.getSubscription().planAt(now),
                            OwnerPlanService.toRequestResponse(payment)
                    );
                })
                .toList();
        return ApiResponse.success("Plan requests fetched successfully", rows);
    }

    /** Every approved salon with its plan; salons without a record are on Free. */
    @Transactional(readOnly = true)
    public ApiResponse<List<AdminSalonPlanResponse>> getSalons() {
        LocalDateTime now = now();
        Map<Long, SalonSubscription> bySalon = subscriptionRepo.findAll().stream()
                .collect(Collectors.toMap(subscription -> subscription.getSalon().getId(), Function.identity()));
        Set<Long> withRequest = paymentRepo.findByStatus(SubscriptionPaymentStatus.PENDING).stream()
                .map(payment -> payment.getSubscription().getSalon().getId())
                .collect(Collectors.toSet());
        List<AdminSalonPlanResponse> rows = salonRepo.findByStatus(SalonStatus.Accepted).stream()
                .map(salon -> toSalonPlan(salon, bySalon.get(salon.getId()), withRequest.contains(salon.getId()), now))
                .sorted(Comparator.comparing(AdminSalonPlanResponse::salonName,
                        Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .toList();
        return ApiResponse.success("Salon plans fetched successfully", rows);
    }

    /** The payment arrived: turn on the plan for the paid period. */
    @Transactional
    public ApiResponse<AdminPlanRequestResponse> confirm(Long requestId, Long adminId) {
        SubscriptionPayment payment = requestId == null ? null : paymentRepo.findById(requestId).orElse(null);
        if (payment == null) {
            return ApiResponse.error("Request not found");
        }
        if (payment.getStatus() != SubscriptionPaymentStatus.PENDING) {
            return ApiResponse.error("This request is already reviewed");
        }
        LocalDateTime now = now();
        SalonSubscription subscription = payment.getSubscription();
        BillingCycle cycle = payment.getBillingCycle();
        startPaidTime(subscription, payment.getPlan(), now,
                start -> cycle == BillingCycle.YEARLY ? start.plusYears(1) : start.plusMonths(1));
        subscription.setBillingCycle(cycle);
        subscriptionRepo.save(subscription);

        payment.setStatus(SubscriptionPaymentStatus.CONFIRMED);
        markReviewed(payment, adminId, now);
        payment.setPeriodStart(subscription.getCurrentPeriodStart());
        payment.setPeriodEnd(subscription.getCurrentPeriodEnd());
        paymentRepo.save(payment);

        emailOwner(subscription.getSalon(), emailTemplates.planActivated(
                payment.getPlan().getDisplayName(),
                subscription.getSalon().getName(),
                DATE.format(subscription.getCurrentPeriodEnd()),
                false));
        return ApiResponse.success("Payment confirmed and plan turned on", toRequestRow(payment, now));
    }

    /** The payment did not arrive or does not match; the owner sees the reason. */
    @Transactional
    public ApiResponse<AdminPlanRequestResponse> reject(Long requestId, RejectPlanRequest request, Long adminId) {
        SubscriptionPayment payment = requestId == null ? null : paymentRepo.findById(requestId).orElse(null);
        if (payment == null) {
            return ApiResponse.error("Request not found");
        }
        if (payment.getStatus() != SubscriptionPaymentStatus.PENDING) {
            return ApiResponse.error("This request is already reviewed");
        }
        String reason = clean(request == null ? null : request.reason(), MAX_TEXT);
        if (reason == null) {
            return ApiResponse.error("Write the reason the owner will see");
        }
        LocalDateTime now = now();
        payment.setStatus(SubscriptionPaymentStatus.REJECTED);
        payment.setRejectionReason(reason);
        markReviewed(payment, adminId, now);
        paymentRepo.save(payment);

        emailOwner(payment.getSubscription().getSalon(),
                emailTemplates.planRequestRejected(payment.getPlan().getDisplayName(), reason));
        return ApiResponse.success("Request refused", toRequestRow(payment, now));
    }

    /** Free months on a paid plan, recorded as a complimentary payment of 0. */
    @Transactional
    public ApiResponse<AdminSalonPlanResponse> grant(Long salonId, GrantPlanRequest request, Long adminId) {
        Salon salon = approvedSalon(salonId);
        if (salon == null) {
            return ApiResponse.error("Approved salon not found");
        }
        if (request == null || request.plan() == null || !request.plan().isPaid()) {
            return ApiResponse.error("Choose Pro or Business");
        }
        if (request.months() == null || request.months() < 1 || request.months() > MAX_GRANT_MONTHS) {
            return ApiResponse.error("Give 1 to " + MAX_GRANT_MONTHS + " months");
        }
        LocalDateTime now = now();
        SalonSubscription subscription = subscriptionFor(salon, now);
        int months = request.months();
        startPaidTime(subscription, request.plan(), now, start -> start.plusMonths(months));
        if (subscription.getBillingCycle() == null) {
            subscription.setBillingCycle(BillingCycle.MONTHLY);
        }
        subscriptionRepo.saveAndFlush(subscription);

        SubscriptionPayment payment = new SubscriptionPayment();
        payment.setSubscription(subscription);
        payment.setPlan(request.plan());
        payment.setBillingCycle(BillingCycle.MONTHLY);
        payment.setAmountFils(0);
        payment.setMethod(SubscriptionPaymentMethod.COMPLIMENTARY);
        String note = clean(request.note(), MAX_NOTE);
        payment.setReference(note == null ? months + " free months" : note);
        payment.setStatus(SubscriptionPaymentStatus.CONFIRMED);
        payment.setCreatedAt(now);
        markReviewed(payment, adminId, now);
        payment.setPeriodStart(subscription.getCurrentPeriodStart());
        payment.setPeriodEnd(subscription.getCurrentPeriodEnd());
        paymentRepo.save(payment);

        emailOwner(salon, emailTemplates.planActivated(
                request.plan().getDisplayName(), salon.getName(),
                DATE.format(subscription.getCurrentPeriodEnd()), true));
        return ApiResponse.success("Free months given", toSalonPlan(salon, subscription, hasOpenRequest(subscription), now));
    }

    /**
     * Moves the salon to another plan without a payment. Free ends the paid
     * time now. A paid plan keeps the current end date, so the salon must
     * have paid or trial time left.
     */
    @Transactional
    public ApiResponse<AdminSalonPlanResponse> changePlan(Long salonId, ChangePlanRequest request) {
        Salon salon = approvedSalon(salonId);
        if (salon == null) {
            return ApiResponse.error("Approved salon not found");
        }
        if (request == null || request.plan() == null) {
            return ApiResponse.error("Choose a plan");
        }
        LocalDateTime now = now();
        SalonSubscription subscription = subscriptionFor(salon, now);
        if (request.plan() == SubscriptionPlan.FREE) {
            subscription.setPlan(SubscriptionPlan.FREE);
            subscription.setStatus(SubscriptionStatus.ENDED);
            subscription.setGraceEndsAt(null);
        } else {
            if (subscription.planAt(now) == SubscriptionPlan.FREE) {
                return ApiResponse.error("This salon has no paid time left. Give free months or confirm a payment.");
            }
            subscription.setPlan(request.plan());
        }
        subscription.setUpdatedAt(now);
        subscriptionRepo.save(subscription);
        return ApiResponse.success("Plan changed", toSalonPlan(salon, subscription, hasOpenRequest(subscription), now));
    }

    /**
     * Sets the new paid period. The same plan continues after the time
     * still left (trial or paid), or after the end of the last period while
     * in its grace days, so a late renewal gets no free days. A different
     * plan starts now.
     */
    private void startPaidTime(
            SalonSubscription subscription,
            SubscriptionPlan plan,
            LocalDateTime now,
            Function<LocalDateTime, LocalDateTime> addTime) {
        LocalDateTime start = now;
        if (subscription.getPlan() == plan) {
            LocalDateTime end = subscription.getStatus() == SubscriptionStatus.GRACE
                    ? subscription.getCurrentPeriodEnd()
                    : subscription.accessEndsAt();
            // Paid time keeps its grace days; a trial has none.
            int grace = subscription.getStatus() == SubscriptionStatus.TRIAL ? 0 : SubscriptionPlan.GRACE_DAYS;
            if (end != null && end.plusDays(grace).isAfter(now)) {
                start = end;
            }
        }
        subscription.setPlan(plan);
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setCurrentPeriodStart(start);
        subscription.setCurrentPeriodEnd(addTime.apply(start));
        subscription.setGraceEndsAt(null);
        subscription.setUpdatedAt(now);
    }

    private SalonSubscription subscriptionFor(Salon salon, LocalDateTime now) {
        return subscriptionRepo.findBySalonId(salon.getId()).orElseGet(() -> {
            SalonSubscription subscription = new SalonSubscription();
            subscription.setSalon(salon);
            subscription.setPlan(SubscriptionPlan.FREE);
            subscription.setStatus(SubscriptionStatus.ENDED);
            subscription.setCreatedAt(now);
            subscription.setUpdatedAt(now);
            return subscription;
        });
    }

    private boolean hasOpenRequest(SalonSubscription subscription) {
        return subscription.getId() != null && paymentRepo
                .findFirstBySubscriptionIdAndStatus(subscription.getId(), SubscriptionPaymentStatus.PENDING)
                .isPresent();
    }

    private void markReviewed(SubscriptionPayment payment, Long adminId, LocalDateTime now) {
        payment.setReviewedBy(adminId == null ? null : userRepo.findById(adminId).orElse(null));
        payment.setReviewedAt(now);
        payment.setUpdatedAt(now);
    }

    private void emailOwner(Salon salon, com.salon.backend.Services.Email.EmailTemplate template) {
        User owner = salon == null ? null : salon.getOwner();
        if (owner == null || owner.getEmail() == null) {
            return;
        }
        emailService.queue(owner.getEmail(), EmailNames.firstName(owner), template);
    }

    private AdminPlanRequestResponse toRequestRow(SubscriptionPayment payment, LocalDateTime now) {
        Salon salon = payment.getSubscription().getSalon();
        User owner = salon.getOwner();
        return new AdminPlanRequestResponse(
                salon.getId(),
                salon.getName(),
                EmailNames.fullName(owner),
                owner == null ? null : owner.getEmail(),
                owner == null ? null : owner.getPhoneNumber(),
                payment.getSubscription().planAt(now),
                OwnerPlanService.toRequestResponse(payment)
        );
    }

    private static AdminSalonPlanResponse toSalonPlan(
            Salon salon,
            SalonSubscription subscription,
            boolean openRequest,
            LocalDateTime now) {
        SubscriptionPlan plan = subscription == null ? SubscriptionPlan.FREE : subscription.planAt(now);
        boolean paidTime = subscription != null && plan != SubscriptionPlan.FREE;
        LocalDateTime accessEnd = paidTime ? subscription.accessEndsAt() : null;
        Long daysLeft = accessEnd == null
                ? null
                : Math.max(0, (long) Math.ceil(Duration.between(now, accessEnd).toHours() / 24.0));
        return new AdminSalonPlanResponse(
                salon.getId(),
                salon.getName(),
                EmailNames.fullName(salon.getOwner()),
                plan,
                subscription == null ? SubscriptionPlan.FREE : subscription.getPlan(),
                paidTime ? subscription.getStatus() : SubscriptionStatus.ENDED,
                subscription == null ? null : subscription.getBillingCycle(),
                accessEnd,
                daysLeft,
                openRequest
        );
    }

    private Salon approvedSalon(Long salonId) {
        Salon salon = salonId == null ? null : salonRepo.findById(salonId).orElse(null);
        return salon != null && salon.getStatus() == SalonStatus.Accepted ? salon : null;
    }

    private static String clean(String value, int max) {
        if (value == null) {
            return null;
        }
        String clean = value.replaceAll("\\p{Cntrl}", " ").trim();
        if (clean.isEmpty()) {
            return null;
        }
        return clean.length() > max ? clean.substring(0, max) : clean;
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }
}
