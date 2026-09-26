package com.salon.backend.DTOs.Salon.HiringPost;

import com.salon.backend.Entities.salons.hiringposts.HiringPostStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class HiringPostResponse {

    private Long id;
    private String title;
    private String description;
    private List<String> requiredSkills;
    private Integer minimumAge;
    private Integer maximumAge;
    private String employeeLocation;
    private Integer minimumYearsOfExperience;
    private Integer numOfPositions;
    private HiringPostStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private Long salonId;
    private String salonName;
    private String salonLocation;
    private Long ownerId;
    private String ownerName;
}
