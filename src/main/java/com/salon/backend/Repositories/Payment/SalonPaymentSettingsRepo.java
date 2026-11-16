package com.salon.backend.Repositories.Payment;

import com.salon.backend.Entities.payments.SalonPaymentSettings;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SalonPaymentSettingsRepo extends JpaRepository<SalonPaymentSettings, Long> {

    Optional<SalonPaymentSettings> findBySalonId(Long salonId);
}
