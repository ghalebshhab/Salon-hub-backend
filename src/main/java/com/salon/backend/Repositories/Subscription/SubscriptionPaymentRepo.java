package com.salon.backend.Repositories.Subscription;

import com.salon.backend.Entities.subscriptions.SubscriptionPayment;
import com.salon.backend.Entities.subscriptions.SubscriptionPaymentStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SubscriptionPaymentRepo extends JpaRepository<SubscriptionPayment, Long> {

    List<SubscriptionPayment> findBySubscriptionIdOrderByCreatedAtDesc(Long subscriptionId);

    /** Admin queue, oldest first, with the salon and owner loaded in the same query. */
    @EntityGraph(attributePaths = {"subscription", "subscription.salon", "subscription.salon.owner"})
    List<SubscriptionPayment> findByStatusOrderByCreatedAtAsc(SubscriptionPaymentStatus status);

    /** Admin history, newest first. */
    @EntityGraph(attributePaths = {"subscription", "subscription.salon", "subscription.salon.owner"})
    List<SubscriptionPayment> findTop100ByOrderByCreatedAtDesc();

    List<SubscriptionPayment> findByStatus(SubscriptionPaymentStatus status);

    /** The owner's open request, if any; a salon has at most one at a time. */
    Optional<SubscriptionPayment> findFirstBySubscriptionIdAndStatus(
            Long subscriptionId,
            SubscriptionPaymentStatus status
    );
}
