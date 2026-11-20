package com.salon.backend.Entities.payments;

import com.salon.backend.Entities.salons.Salon;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** Payment choices and fee rules of one salon. No row means the defaults: cash only. */
@Entity
@Table(name = "salon_payment_settings")
@Getter
@Setter
@NoArgsConstructor
public class SalonPaymentSettings {

    public static final int DEFAULT_FREE_CANCEL_HOURS = 24;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "salon_id", nullable = false, unique = true)
    private Salon salon;

    @Column(name = "cash_enabled", nullable = false)
    private boolean cashEnabled = true;

    @Column(name = "card_prepaid_enabled", nullable = false)
    private boolean cardPrepaidEnabled;

    @Column(name = "card_postpaid_enabled", nullable = false)
    private boolean cardPostpaidEnabled;

    /** Free cancellation until this many hours before the start. */
    @Column(name = "free_cancel_hours", nullable = false)
    private int freeCancelHours = DEFAULT_FREE_CANCEL_HOURS;

    /** Part kept on a late cancellation of a booking paid by card in advance, 0 to 100. */
    @Column(name = "late_cancel_fee_percent", nullable = false)
    private int lateCancelFeePercent;

    /** Part kept on a no-show of a booking paid by card in advance, 0 to 100. */
    @Column(name = "no_show_fee_percent", nullable = false)
    private int noShowFeePercent;

    /**
     * Customers with this many no-shows at the salon must pay in advance by
     * card. Null turns the rule off.
     */
    @Column(name = "no_show_limit")
    private Integer noShowLimit;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public boolean allows(PaymentMethod method) {
        return switch (method) {
            case CASH -> cashEnabled;
            case CARD_PREPAID -> cardPrepaidEnabled;
            case CARD_POSTPAID -> cardPostpaidEnabled;
        };
    }
}
