package com.salon.backend.DTOs.Salon;

import com.salon.backend.Entities.salons.SalonOpenningStatus;
import com.salon.backend.Entities.salons.SalonStatus;

import java.time.LocalDate;
import java.time.LocalTime;

public record SalonResponse(
        Long id,
        String name,
        String salonPhoneNumber,
        String email,
        String location,
        String googleMapsUrl,
        Double latitude,
        Double longitude,
        String description,
        /** Arabic description, or null when it is not translated yet. */
        String descriptionAr,
        Long ownerId,
        String ownerName,
        String ownerPhoneNumber,
        Long currentEmployeesNumber,
        Long maxEmployeesNumber,
        LocalDate createdAt,
        LocalTime openingTime,
        LocalTime closingTime,
        String profilePictureUrl,
        SalonStatus status,
        SalonOpenningStatus openningStatus,
        boolean blockedByAdmin,
        boolean blockedByOwner,
        String rejectionReason,
        int resubmissionCount,
        int remainingResubmissions,
        /** "PRO" or "BUSINESS" when the salon's plan shows a badge, else null. */
        String planBadge
) {

    /** Without a plan badge; kept for callers that do not know the plan. */
    public SalonResponse(
            Long id, String name, String salonPhoneNumber, String email, String location,
            String googleMapsUrl, Double latitude, Double longitude, String description,
            Long ownerId, String ownerName, String ownerPhoneNumber,
            Long currentEmployeesNumber, Long maxEmployeesNumber, LocalDate createdAt,
            LocalTime openingTime, LocalTime closingTime, String profilePictureUrl,
            SalonStatus status, SalonOpenningStatus openningStatus,
            boolean blockedByAdmin, boolean blockedByOwner, String rejectionReason,
            int resubmissionCount, int remainingResubmissions) {
        this(id, name, salonPhoneNumber, email, location, googleMapsUrl, latitude, longitude, description,
                null, ownerId, ownerName, ownerPhoneNumber, currentEmployeesNumber, maxEmployeesNumber, createdAt,
                openingTime, closingTime, profilePictureUrl, status, openningStatus, blockedByAdmin,
                blockedByOwner, rejectionReason, resubmissionCount, remainingResubmissions, null);
    }
}
