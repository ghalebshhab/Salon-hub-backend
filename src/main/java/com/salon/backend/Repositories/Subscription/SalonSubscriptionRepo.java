package com.salon.backend.Repositories.Subscription;

import com.salon.backend.Entities.subscriptions.SalonSubscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SalonSubscriptionRepo extends JpaRepository<SalonSubscription, Long> {

    Optional<SalonSubscription> findBySalonId(Long salonId);

    boolean existsBySalonId(Long salonId);

    java.util.List<SalonSubscription> findBySalonIdIn(java.util.Collection<Long> salonIds);

    @org.springframework.data.jpa.repository.Query(
            "SELECT s.id FROM SalonSubscription s WHERE s.status IN :statuses")
    java.util.List<Long> findIdsByStatusIn(
            @org.springframework.data.repository.query.Param("statuses")
            java.util.Collection<com.salon.backend.Entities.subscriptions.SubscriptionStatus> statuses);
}
