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
        String rejectionReason,
        int resubmissionCount,
        int remainingResubmissions
) {}
