package com.salon.backend.Services.Translation;

import com.salon.backend.Entities.salons.Salon;
import com.salon.backend.Entities.salons.services.SalonService;
import com.salon.backend.Repositories.Salon.SalonRepo;
import com.salon.backend.Repositories.Salon.SalonServiceRepo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

/**
 * Fills the Arabic copy of owner-written text in the background, so a save
 * never waits for Claude. A translation is stored only while the English it
 * came from is still the saved English, and only into empty Arabic fields,
 * so it never replaces the owner's own Arabic.
 */
@Slf4j
@Service
public class ContentTranslationService {

    private static final String NAME = "name";
    private static final String DESCRIPTION = "description";

    private final ContentTranslator translator;
    private final SalonServiceRepo salonServiceRepo;
    private final SalonRepo salonRepo;
    private final Executor executor;

    public ContentTranslationService(
            ContentTranslator translator,
            SalonServiceRepo salonServiceRepo,
            SalonRepo salonRepo,
            @Qualifier("translationExecutor") Executor executor) {
        this.translator = translator;
        this.salonServiceRepo = salonServiceRepo;
        this.salonRepo = salonRepo;
        this.executor = executor;
    }

    /** Queues the service for translation after the current transaction commits. */
    public void translateServiceLater(Long serviceId) {
        if (translator.isEnabled() && serviceId != null) {
            afterCommit(() -> translateService(serviceId));
        }
    }

    /** Queues the salon description for translation after the current transaction commits. */
    public void translateSalonLater(Long salonId) {
        if (translator.isEnabled() && salonId != null) {
            afterCommit(() -> translateSalon(salonId));
        }
    }

    /** One pass at start-up for rows saved before translation existed. */
    @EventListener(ApplicationReadyEvent.class)
    public void backfill() {
        if (!translator.isEnabled()) {
            return;
        }
        List<Long> services = salonServiceRepo.findIdsMissingArabic();
        List<Long> salons = salonRepo.findIdsMissingArabicDescription();
        if (!services.isEmpty() || !salons.isEmpty()) {
            log.info("Translating {} services and {} salon descriptions to Arabic", services.size(), salons.size());
        }
        services.forEach(id -> executor.execute(() -> translateService(id)));
        salons.forEach(id -> executor.execute(() -> translateSalon(id)));
    }

    void translateService(Long serviceId) {
        SalonService service = salonServiceRepo.findById(serviceId).orElse(null);
        if (service == null) {
            return;
        }
        Map<String, String> missing = new LinkedHashMap<>();
        if (ArabicText.clean(service.getNameAr()) == null && ArabicText.clean(service.getName()) != null) {
            missing.put(NAME, service.getName());
        }
        if (ArabicText.clean(service.getDescriptionAr()) == null && ArabicText.clean(service.getDescription()) != null) {
            missing.put(DESCRIPTION, service.getDescription());
        }
        if (missing.isEmpty()) {
            return;
        }
        translator.toArabic(missing).ifPresent(arabic -> salonServiceRepo.saveArabic(
                serviceId,
                service.getName(),
                service.getDescription(),
                limit(arabic.get(NAME), 200),
                limit(arabic.get(DESCRIPTION), 2000)));
    }

    void translateSalon(Long salonId) {
        Salon salon = salonRepo.findById(salonId).orElse(null);
        if (salon == null
                || ArabicText.clean(salon.getDescription()) == null
                || ArabicText.clean(salon.getDescriptionAr()) != null) {
            return;
        }
        translator.toArabic(Map.of(DESCRIPTION, salon.getDescription()))
                .ifPresent(arabic -> salonRepo.saveArabicDescription(
                        salonId, salon.getDescription(), limit(arabic.get(DESCRIPTION), 1000)));
    }

    private void afterCommit(Runnable task) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    executor.execute(task);
                }
            });
        } else {
            executor.execute(task);
        }
    }

    private static String limit(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() > max ? value.substring(0, max) : value;
    }
}
