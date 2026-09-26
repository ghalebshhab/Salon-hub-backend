package com.salon.backend.DTOs.Salon.HiringPost;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RejectHiringApplicationRequest {

    private String rejectionReason;
}
