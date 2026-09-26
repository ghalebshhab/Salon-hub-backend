package com.salon.backend.Services.Salon.HiringPost;

import com.salon.backend.DTOs.ApiResponse;
import com.salon.backend.DTOs.Salon.HiringPost.CreateHiringPostRequest;
import com.salon.backend.DTOs.Salon.HiringPost.ApplyToHiringPostRequest;
import com.salon.backend.DTOs.Salon.HiringPost.HiringApplicationResponse;
import com.salon.backend.DTOs.Salon.HiringPost.HiringPostResponse;
import com.salon.backend.DTOs.Salon.HiringPost.RejectHiringApplicationRequest;

import java.util.List;

public interface HiringPostService {

    ApiResponse<HiringPostResponse> createHiringPost(
            CreateHiringPostRequest request,
            String ownerEmail
    );

    ApiResponse<HiringApplicationResponse> applyToHiringPost(
            Long hiringPostId,
            ApplyToHiringPostRequest request,
            String applicantEmail
    );

    ApiResponse<List<HiringApplicationResponse>> getHiringPostApplications(
            Long hiringPostId,
            String ownerEmail
    );

    ApiResponse<HiringApplicationResponse> acceptHiringApplication(
            Long applicationId,
            String ownerEmail
    );

    ApiResponse<HiringApplicationResponse> rejectHiringApplication(
            Long applicationId,
            RejectHiringApplicationRequest request,
            String ownerEmail
    );
}
