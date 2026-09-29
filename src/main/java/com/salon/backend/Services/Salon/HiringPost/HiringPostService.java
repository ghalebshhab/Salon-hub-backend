package com.salon.backend.Services.Salon.HiringPost;

import com.salon.backend.DTOs.ApiResponse;
import com.salon.backend.DTOs.Salon.HiringPost.CreateHiringPostRequest;
import com.salon.backend.DTOs.Salon.HiringPost.ApplyToHiringPostRequest;
import com.salon.backend.DTOs.Salon.HiringPost.HiringApplicationResponse;
import com.salon.backend.DTOs.Salon.HiringPost.HiringPostResponse;
import com.salon.backend.DTOs.Salon.HiringPost.HiringPostPageResponse;
import com.salon.backend.DTOs.Salon.HiringPost.HiringPostSearchRequest;
import com.salon.backend.DTOs.Salon.HiringPost.RejectHiringApplicationRequest;
import com.salon.backend.DTOs.Salon.HiringPost.UpdateHiringPostRequest;
import com.salon.backend.DTOs.Salon.HiringPost.ReopenHiringPostRequest;
import com.salon.backend.DTOs.Salon.HiringPost.OwnerHiringPostResponse;
import com.salon.backend.Entities.salons.hiringposts.HiringPostStatus;

import java.util.List;

public interface HiringPostService {

    ApiResponse<HiringPostPageResponse> searchOpenHiringPosts(
            HiringPostSearchRequest request,
            String userEmail
    );

    ApiResponse<HiringPostResponse> createHiringPost(
            CreateHiringPostRequest request,
            String ownerEmail
    );

    ApiResponse<List<OwnerHiringPostResponse>> getOwnerHiringPosts(
            HiringPostStatus status,
            String ownerEmail
    );

    ApiResponse<OwnerHiringPostResponse> getOwnerHiringPost(
            Long hiringPostId,
            String ownerEmail
    );

    ApiResponse<HiringPostResponse> updateHiringPost(
            Long hiringPostId,
            UpdateHiringPostRequest request,
            String ownerEmail
    );

    ApiResponse<HiringPostResponse> closeHiringPost(
            Long hiringPostId,
            String ownerEmail
    );

    ApiResponse<HiringPostResponse> reopenHiringPost(
            Long hiringPostId,
            ReopenHiringPostRequest request,
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

    ApiResponse<List<HiringApplicationResponse>> getMyHiringApplications(
            String applicantEmail
    );

    ApiResponse<HiringApplicationResponse> cancelLinkedJoinRequest(
            Long applicationId,
            String applicantEmail
    );
}
