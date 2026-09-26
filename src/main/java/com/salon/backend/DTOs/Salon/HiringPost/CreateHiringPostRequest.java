package com.salon.backend.DTOs.Salon.HiringPost;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateHiringPostRequest {

    private String title;
    private String description;
    private List<String> requiredSkills;
    private Integer minimumAge;
    private Integer maximumAge;
    private String employeeLocation;
    private Integer minimumYearsOfExperience;
    private Integer numOfPositions;
    private LocalDateTime expiresAt;
}
