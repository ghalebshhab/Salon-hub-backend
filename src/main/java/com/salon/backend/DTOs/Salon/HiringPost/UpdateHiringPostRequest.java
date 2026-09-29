package com.salon.backend.DTOs.Salon.HiringPost;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateHiringPostRequest {

    private String title;
    private String description;
    private LocalDateTime expiresAt;
    private Integer additionalPositions;
}
