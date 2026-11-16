package com.salon.backend.Entities.payments;

import com.salon.backend.Entities.users.User;
import jakarta.persistence.CascadeType;
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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * The payment of one booking. Amounts are whole fils (1 JD = 1000 fils), so
 * no rounding happens in calculations. The commission rate is copied at
 * creation, so a later rate change never rewrites old payments.
 */
@Entity
@Table(
        name = "booking_payment",
        indexes = @Index(name = "idx_booking_payment_status_paid", columnList = "status, paid_at")
)
@BatchSize(size = 50)
@Getter
@Setter
@NoArgsConstructor
public class BookingPayment {

    public static final String CURRENCY = "JOD";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private PaymentMethod method;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private PaymentStatus status;

    @Column(name = "amount_fils", nullable = false)
    private long amountFils;

    @Column(name = "refunded_fils", nullable = false)
    private long refundedFils;

    @Column(nullable = false, length = 3)
    private String currency = CURRENCY;

    /** Platform commission in basis points (100 = 1%), copied at creation. */
    @Column(name = "commission_rate_bps", nullable = false)
    private int commissionRateBps;

    @Column(name = "commission_fils", nullable = false)
    private long commissionFils;

    @Column(name = "receipt_number", unique = true, length = 40)
    private String receiptNumber;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "confirmed_by_id")
    private User confirmedBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "payment", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt ASC, id ASC")
    private List<PaymentTransaction> transactions = new ArrayList<>();

    public void addTransaction(PaymentTransaction transaction) {
        transaction.setPayment(this);
        transactions.add(transaction);
    }

    /** Moves to the next state, or throws when the move is not allowed. */
    public void moveTo(PaymentStatus next) {
        if (!status.canTransitionTo(next)) {
            throw new IllegalStateException("Payment cannot move from " + status + " to " + next);
        }
        status = next;
    }

    /** What the salon keeps: received money minus refunds and the platform commission. */
    public long netFils() {
        return amountFils - refundedFils - commissionFils;
    }
}
