package com.salon.backend.Entities.subscriptions;

import com.salon.backend.Entities.salons.Salon;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/**
 * The plan of one salon. No row means Free (salons approved before plans
 * existed). The plan in use is worked out from the status and the dates, so
 * an ended trial or period counts as Free at once.
 */
@Entity
@Table(
        name = "salon_subscription",
        indexes = @Index(name = "idx_subscription_status", columnList = "status")
)
@Getter
@Setter
@NoArgsConstructor
public class SalonSubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "salon_id", nullable = false, unique = true)
    private Salon salon;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private SubscriptionPlan plan = SubscriptionPlan.FREE;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private SubscriptionStatus status = SubscriptionStatus.ENDED;

    /** Null during the trial and on Free. */
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "billing_cycle", length = 20)
    private BillingCycle billingCycle;

    @Column(name = "trial_ends_at")
    private LocalDateTime trialEndsAt;

    @Column(name = "current_period_start")
    private LocalDateTime currentPeriodStart;

    @Column(name = "current_period_end")
    private LocalDateTime currentPeriodEnd;

    @Column(name = "grace_ends_at")
    private LocalDateTime graceEndsAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * The last plan email sent, for example {@code TRIAL:7:2026-12-01}, so the
     * daily job never sends the same email twice.
     */
    @Column(name = "last_notice_key", length = 60)
    private String lastNoticeKey;

    /** The plan the salon may use at {@code now}. */
    public SubscriptionPlan planAt(LocalDateTime now) {
        return isPaidTimeLeft(now) ? plan : SubscriptionPlan.FREE;
    }

    /** End of the time the current plan still works, or null on Free. */
    public LocalDateTime accessEndsAt() {
        return switch (status) {
            case TRIAL -> trialEndsAt;
            case ACTIVE -> currentPeriodEnd;
            case GRACE -> graceEndsAt;
            case ENDED -> null;
        };
    }

    private boolean isPaidTimeLeft(LocalDateTime now) {
        // A paid period keeps working for the grace days, also before the
        // daily job moves it to GRACE.
        LocalDateTime end = status == SubscriptionStatus.ACTIVE && currentPeriodEnd != null
                ? currentPeriodEnd.plusDays(SubscriptionPlan.GRACE_DAYS)
                : accessEndsAt();
        return end != null && now.isBefore(end);
    }
}
