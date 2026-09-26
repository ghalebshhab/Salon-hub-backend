package com.salon.backend.DTOs.Salon.HiringPost;

import com.salon.backend.Entities.salons.employment.EmploymentRequestStatus;
import com.salon.backend.Entities.salons.hiringposts.ApplicationMatchLevel;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record HiringApplicationResponse(
        Long applicationId,
        Long employmentRequestId,
        Long hiringPostId,
        String hiringPostTitle,
        Long salonId,
        String salonName,
        Long applicantId,
        String applicantName,
        String applicantUsername,
        String applicantEmail,
        String applicantPhoneNumber,
        LocalDate dateOfBirth,
        Integer age,
        String currentLocation,
        Integer yearsOfExperience,
        List<String> skills,
        String resumeUrl,
        Boolean resumeUploaded,
        String portfolioUrl,
        String applicationNote,
        ApplicationMatchLevel matchLevel,
        Integer matchScore,
        List<String> matchedSkills,
        List<String> additionalSkills,
        List<String> missingSkills,
        List<String> missingRequirements,
        EmploymentRequestStatus status,
        LocalDateTime appliedAt
) {
}
