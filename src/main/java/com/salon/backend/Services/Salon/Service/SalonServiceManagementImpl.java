package com.salon.backend.Services.Salon.Service;

import com.salon.backend.DTOs.ApiResponse;
import com.salon.backend.DTOs.Salon.Service.CreateSalonServiceRequest;
import com.salon.backend.DTOs.Salon.Service.SalonServiceResponse;
import com.salon.backend.DTOs.Salon.Service.UpdateSalonServiceRequest;
import com.salon.backend.Entities.salons.Salon;
import com.salon.backend.Entities.salons.SalonStatus;
import com.salon.backend.Entities.salons.services.SalonService;
import com.salon.backend.Repositories.Salon.SalonRepo;
import com.salon.backend.Repositories.Salon.SalonServiceRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SalonServiceManagementImpl implements SalonServiceManagement {

    private static final int MAX_NAME_LENGTH = 100;
    private static final int MAX_DESCRIPTION_LENGTH = 1000;

    private final SalonServiceRepo salonServiceRepo;
    private final SalonRepo salonRepo;

    @Override
    @Transactional
    public ApiResponse<SalonServiceResponse> createService(
            CreateSalonServiceRequest request,
            Long ownerId) {

        Salon salon = findManageableSalon(ownerId);
        if (salon == null) {
            return ApiResponse.error(
                    "An active, approved salon owned by you is required to create services"
            );
        }

        ApiResponse<Void> validation = validateService(
                request == null ? null : request.getName(),
                request == null ? null : request.getDescription(),
                request == null ? null : request.getPrice(),
                request == null ? null : request.getDurationMinutes()
        );
        if (!validation.isSuccess()) {
            return ApiResponse.error(validation.getMessage());
        }

        LocalDateTime now = LocalDateTime.now();
        SalonService salonService = new SalonService();
        salonService.setSalon(salon);
        salonService.setName(request.getName().trim());
        salonService.setDescription(normalizeDescription(request.getDescription()));
        salonService.setPrice(request.getPrice());
        salonService.setDurationMinutes(request.getDurationMinutes());
        salonService.setActive(true);
        salonService.setCreatedAt(now);
        salonService.setUpdatedAt(now);

        salonServiceRepo.save(salonService);

        return ApiResponse.success(
                "Salon service created successfully",
                mapToResponse(salonService)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<List<SalonServiceResponse>> getActiveSalonServices(Long salonId) {
        if (salonId == null) {
            return ApiResponse.error("Salon id is required");
        }

        Salon salon = salonRepo.findById(salonId).orElse(null);
        if (!isActiveSalon(salon)) {
            return ApiResponse.error("Active salon not found");
        }

        List<SalonServiceResponse> services = salonServiceRepo
                .findBySalonIdAndActiveTrueOrderByNameAsc(salonId)
                .stream()
                .map(this::mapToResponse)
                .toList();

        return ApiResponse.success("Active salon services fetched successfully", services);
    }

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<List<SalonServiceResponse>> getMySalonServices(Long ownerId) {
        Salon salon = findManageableSalon(ownerId);
        if (salon == null) {
            return ApiResponse.error(
                    "An active, approved salon owned by you is required to manage services"
            );
        }

        List<SalonServiceResponse> services = salonServiceRepo
                .findBySalonIdOrderByCreatedAtDesc(salon.getId())
                .stream()
                .map(this::mapToResponse)
                .toList();

        return ApiResponse.success("Salon services fetched successfully", services);
    }

    @Override
    @Transactional
    public ApiResponse<SalonServiceResponse> updateService(
            Long serviceId,
            UpdateSalonServiceRequest request,
            Long ownerId) {

        SalonService salonService = findOwnedService(serviceId, ownerId);
        if (salonService == null) {
            return ApiResponse.error("Salon service owned by you was not found");
        }
        if (!isActiveSalon(salonService.getSalon())) {
            return ApiResponse.error("Only an active, approved salon can update services");
        }

        ApiResponse<Void> validation = validateService(
                request == null ? null : request.getName(),
                request == null ? null : request.getDescription(),
                request == null ? null : request.getPrice(),
                request == null ? null : request.getDurationMinutes()
        );
        if (!validation.isSuccess()) {
            return ApiResponse.error(validation.getMessage());
        }
        if (request.getIsActive() == null) {
            return ApiResponse.error("Service active status is required");
        }

        salonService.setName(request.getName().trim());
        salonService.setDescription(normalizeDescription(request.getDescription()));
        salonService.setPrice(request.getPrice());
        salonService.setDurationMinutes(request.getDurationMinutes());
        salonService.setActive(request.getIsActive());
        salonService.setUpdatedAt(LocalDateTime.now());

        salonServiceRepo.save(salonService);

        return ApiResponse.success(
                "Salon service updated successfully",
                mapToResponse(salonService)
        );
    }

    @Override
    @Transactional
    public ApiResponse<SalonServiceResponse> disableService(Long serviceId, Long ownerId) {
        SalonService salonService = findOwnedService(serviceId, ownerId);
        if (salonService == null) {
            return ApiResponse.error("Salon service owned by you was not found");
        }
        if (!isActiveSalon(salonService.getSalon())) {
            return ApiResponse.error("Only an active, approved salon can disable services");
        }

        salonService.setActive(false);
        salonService.setUpdatedAt(LocalDateTime.now());
        salonServiceRepo.save(salonService);

        return ApiResponse.success(
                "Salon service disabled successfully",
                mapToResponse(salonService)
        );
    }

    private Salon findOwnedSalon(Long ownerId) {
        if (ownerId == null) {
            return null;
        }
        return salonRepo.findByOwnerId(ownerId);
    }

    private Salon findManageableSalon(Long ownerId) {
        Salon salon = findOwnedSalon(ownerId);
        return isActiveSalon(salon) ? salon : null;
    }

    private SalonService findOwnedService(Long serviceId, Long ownerId) {
        if (serviceId == null || ownerId == null) {
            return null;
        }

        return salonServiceRepo.findByIdAndSalonOwnerId(serviceId, ownerId).orElse(null);
    }

    private boolean isActiveSalon(Salon salon) {
        return salon != null
                && salon.getStatus() == SalonStatus.Accepted
                && !salon.isBlockedByAdmin()
                && !salon.isBlockedByOwner();
    }

    private ApiResponse<Void> validateService(
            String name,
            String description,
            BigDecimal price,
            Integer durationMinutes) {

        if (name == null || name.isBlank()) {
            return ApiResponse.error("Service name is required");
        }
        if (name.trim().length() > MAX_NAME_LENGTH) {
            return ApiResponse.error("Service name cannot exceed 100 characters");
        }
        if (description != null && description.trim().length() > MAX_DESCRIPTION_LENGTH) {
            return ApiResponse.error("Service description cannot exceed 1000 characters");
        }
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            return ApiResponse.error("Service price must be greater than 0");
        }
        if (price.scale() > 2) {
            return ApiResponse.error("Service price cannot have more than 2 decimal places");
        }
        if (price.precision() - price.scale() > 8) {
            return ApiResponse.error("Service price is too large");
        }
        if (durationMinutes == null || durationMinutes <= 0) {
            return ApiResponse.error("Service duration must be greater than 0 minutes");
        }

        return ApiResponse.success("Salon service information is valid", null);
    }

    private String normalizeDescription(String description) {
        return description == null || description.isBlank() ? null : description.trim();
    }

    private SalonServiceResponse mapToResponse(SalonService salonService) {
        return new SalonServiceResponse(
                salonService.getId(),
                salonService.getSalon().getId(),
                salonService.getName(),
                salonService.getDescription(),
                salonService.getPrice(),
                salonService.getDurationMinutes(),
                salonService.isActive(),
                salonService.getCreatedAt(),
                salonService.getUpdatedAt()
        );
    }
}
