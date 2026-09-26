package com.salon.backend.DTOs.User.Profile;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CareerProfileRequest {

    private LocalDate dateOfBirth;
    private Integer yearsOfExperience;
    private List<String> skills;
    private List<String> preferredWorkLocations;
    private String resumeUrl;
    private String portfolioUrl;
    private String professionalBio;
}
