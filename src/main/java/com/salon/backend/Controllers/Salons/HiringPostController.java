package com.salon.backend.Controllers.Salons;

import com.salon.backend.DTOs.ApiResponse;
import com.salon.backend.DTOs.Salon.HiringPost.ApplyToHiringPostRequest;
import com.salon.backend.DTOs.Salon.HiringPost.CreateHiringPostRequest;
import com.salon.backend.DTOs.Salon.HiringPost.HiringApplicationResponse;
import com.salon.backend.DTOs.Salon.HiringPost.HiringPostResponse;
import com.salon.backend.DTOs.Salon.HiringPost.HiringPostPageResponse;
import com.salon.backend.DTOs.Salon.HiringPost.HiringPostSearchRequest;
import com.salon.backend.DTOs.Salon.HiringPost.RejectHiringApplicationRequest;
import com.salon.backend.DTOs.Salon.HiringPost.UpdateHiringPostRequest;
import com.salon.backend.DTOs.Salon.HiringPost.ReopenHiringPostRequest;
import com.salon.backend.DTOs.Salon.HiringPost.OwnerHiringPostResponse;
import com.salon.backend.Entities.salons.hiringposts.HiringPostStatus;
import com.salon.backend.Services.Salon.HiringPost.HiringPostService;
import com.salon.backend.Services.Auth.Security.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/hiring-posts")
@RequiredArgsConstructor
public class HiringPostController {

    private final HiringPostService hiringPostService;

    @GetMapping
    public ApiResponse<HiringPostPageResponse> searchOpenHiringPosts(
            @ModelAttribute HiringPostSearchRequest request,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {

        return hiringPostService.searchOpenHiringPosts(
                request,
                currentUser.getId()
        );
    }

    @PostMapping
    public ApiResponse<HiringPostResponse> createHiringPost(
            @RequestBody CreateHiringPostRequest request,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {

        return hiringPostService.createHiringPost(
                request,
                currentUser.getId()
        );
    }

    @GetMapping("/owner")
    public ApiResponse<List<OwnerHiringPostResponse>> getOwnerHiringPosts(
            @RequestParam(required = false) HiringPostStatus status,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {

        return hiringPostService.getOwnerHiringPosts(status, currentUser.getId());
    }

    @GetMapping("/owner/{hiringPostId}")
    public ApiResponse<OwnerHiringPostResponse> getOwnerHiringPost(
            @PathVariable Long hiringPostId,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {

        return hiringPostService.getOwnerHiringPost(
                hiringPostId,
                currentUser.getId()
        );
    }

    @PutMapping("/owner/{hiringPostId}")
    public ApiResponse<HiringPostResponse> updateHiringPost(
            @PathVariable Long hiringPostId,
            @RequestBody UpdateHiringPostRequest request,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {

        return hiringPostService.updateHiringPost(
                hiringPostId,
                request,
                currentUser.getId()
        );
    }

    @PostMapping("/owner/{hiringPostId}/close")
    public ApiResponse<HiringPostResponse> closeHiringPost(
            @PathVariable Long hiringPostId,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {

        return hiringPostService.closeHiringPost(
                hiringPostId,
                currentUser.getId()
        );
    }

    @PostMapping("/owner/{hiringPostId}/reopen")
    public ApiResponse<HiringPostResponse> reopenHiringPost(
            @PathVariable Long hiringPostId,
            @RequestBody ReopenHiringPostRequest request,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {

        return hiringPostService.reopenHiringPost(
                hiringPostId,
                request,
                currentUser.getId()
        );
    }

    @PostMapping("/{hiringPostId}/applications")
    public ApiResponse<HiringApplicationResponse> applyToHiringPost(
            @PathVariable Long hiringPostId,
            @RequestBody ApplyToHiringPostRequest request,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {

        return hiringPostService.applyToHiringPost(
                hiringPostId,
                request,
                currentUser.getId()
        );
    }

    @GetMapping("/{hiringPostId}/applications")
    public ApiResponse<List<HiringApplicationResponse>> getHiringPostApplications(
            @PathVariable Long hiringPostId,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {

        return hiringPostService.getHiringPostApplications(
                hiringPostId,
                currentUser.getId()
        );
    }

    @PostMapping("/applications/{applicationId}/accept")
    public ApiResponse<HiringApplicationResponse> acceptHiringApplication(
            @PathVariable Long applicationId,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {

        return hiringPostService.acceptHiringApplication(
                applicationId,
                currentUser.getId()
        );
    }

    @PostMapping("/applications/{applicationId}/reject")
    public ApiResponse<HiringApplicationResponse> rejectHiringApplication(
            @PathVariable Long applicationId,
            @RequestBody RejectHiringApplicationRequest request,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {

        return hiringPostService.rejectHiringApplication(
                applicationId,
                request,
                currentUser.getId()
        );
    }

    @GetMapping("/applications/me")
    public ApiResponse<List<HiringApplicationResponse>> getMyHiringApplications(
            @AuthenticationPrincipal AuthenticatedUser currentUser) {

        return hiringPostService.getMyHiringApplications(currentUser.getId());
    }

    @PostMapping("/applications/{applicationId}/cancel-join")
    public ApiResponse<HiringApplicationResponse> cancelLinkedJoinRequest(
            @PathVariable Long applicationId,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {

        return hiringPostService.cancelLinkedJoinRequest(
                applicationId,
                currentUser.getId()
        );
    }
}
