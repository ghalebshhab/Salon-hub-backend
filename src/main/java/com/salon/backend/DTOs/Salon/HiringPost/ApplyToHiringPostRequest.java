package com.salon.backend.DTOs.Salon.HiringPost;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ApplyToHiringPostRequest {

    private LocalDate dateOfBirth;
    private String currentLocation;
    private Integer yearsOfExperience;
    private List<String> skills;
    private String resumeUrl;
    private String portfolioUrl;
    private String applicationNote;
}
