package com.salon.backend.DTOs.Salon.HiringPost;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class HiringPostSearchRequest {

    private String keyword;
    private String employeeLocation;
    private String requiredSkill;
    private Integer maximumRequiredExperience;
    private Integer page = 0;
    private Integer size = 10;
    private String sortBy = "newest";
}
