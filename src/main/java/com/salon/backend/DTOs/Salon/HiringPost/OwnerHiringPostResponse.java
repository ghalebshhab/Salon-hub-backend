package com.salon.backend.DTOs.Salon.HiringPost;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OwnerHiringPostResponse {

    private HiringPostResponse hiringPost;
    private Long totalApplications;
    private Long requestedApplications;
    private Long pendingTransferApplications;
    private Long acceptedApplications;
    private Long rejectedApplications;
    private Long cancelledApplications;
}
