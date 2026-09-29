package com.salon.backend.DTOs.Salon.Employment.Join;

import com.salon.backend.Entities.salons.employment.EmploymentRequestStatus;
import com.salon.backend.Entities.salons.employment.EmploymentRequestSource;
import com.salon.backend.Entities.salons.employment.RequestType;

import java.time.LocalDateTime;

public record EmployeeRequestResponse(
        Long requestId,
        Long salonId,
        String salonName,
        Long senderId,
        String senderName,
        Long receiverId,
        String receiverName,
        Long hiringPostId,
        String hiringPostTitle,
        RequestType requestType,
        EmploymentRequestSource requestSource,
        EmploymentRequestStatus status,
        String rejectionReason,
        LocalDateTime createdAt
) {}
