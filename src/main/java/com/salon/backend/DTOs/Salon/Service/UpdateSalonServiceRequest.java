package com.salon.backend.DTOs.Salon.Service;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateSalonServiceRequest {

    private String name;
    private String description;
    /** Optional. Blank means "translate for me"; absent keeps the saved Arabic. */
    private String nameAr;
    private String descriptionAr;
    private BigDecimal price;
    private Integer durationMinutes;
    private Boolean isActive;
}
