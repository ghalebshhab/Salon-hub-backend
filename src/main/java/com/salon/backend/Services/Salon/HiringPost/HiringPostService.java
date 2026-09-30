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
            Long userId
    );

    ApiResponse<HiringPostResponse> createHiringPost(
            CreateHiringPostRequest request,
            Long ownerId
    );

    ApiResponse<List<OwnerHiringPostResponse>> getOwnerHiringPosts(
            HiringPostStatus status,
            Long ownerId
    );

    ApiResponse<OwnerHiringPostResponse> getOwnerHiringPost(
            Long hiringPostId,
            Long ownerId
    );

    ApiResponse<HiringPostResponse> updateHiringPost(
            Long hiringPostId,
            UpdateHiringPostRequest request,
            Long ownerId
    );

    ApiResponse<HiringPostResponse> closeHiringPost(
            Long hiringPostId,
            Long ownerId
    );

    ApiResponse<HiringPostResponse> reopenHiringPost(
            Long hiringPostId,
            ReopenHiringPostRequest request,
            Long ownerId
    );

    ApiResponse<HiringApplicationResponse> applyToHiringPost(
            Long hiringPostId,
            ApplyToHiringPostRequest request,
            Long applicantId
    );

    ApiResponse<List<HiringApplicationResponse>> getHiringPostApplications(
            Long hiringPostId,
            Long ownerId
    );

    ApiResponse<HiringApplicationResponse> acceptHiringApplication(
            Long applicationId,
            Long ownerId
    );

    ApiResponse<HiringApplicationResponse> rejectHiringApplication(
            Long applicationId,
            RejectHiringApplicationRequest request,
            Long ownerId
    );

    ApiResponse<List<HiringApplicationResponse>> getMyHiringApplications(
            Long applicantId
    );

    ApiResponse<HiringApplicationResponse> cancelLinkedJoinRequest(
            Long applicationId,
            Long applicantId
    );
}
