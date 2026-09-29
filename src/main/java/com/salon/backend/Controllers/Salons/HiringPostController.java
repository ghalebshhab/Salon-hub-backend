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
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
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
            Authentication authentication) {

        return hiringPostService.searchOpenHiringPosts(
                request,
                authentication.getName()
        );
    }

    @PostMapping
    public ApiResponse<HiringPostResponse> createHiringPost(
            @RequestBody CreateHiringPostRequest request,
            Authentication authentication) {

        return hiringPostService.createHiringPost(
                request,
                authentication.getName()
        );
    }

    @GetMapping("/owner")
    public ApiResponse<List<OwnerHiringPostResponse>> getOwnerHiringPosts(
            @RequestParam(required = false) HiringPostStatus status,
            Authentication authentication) {

        return hiringPostService.getOwnerHiringPosts(status, authentication.getName());
    }

    @GetMapping("/owner/{hiringPostId}")
    public ApiResponse<OwnerHiringPostResponse> getOwnerHiringPost(
            @PathVariable Long hiringPostId,
            Authentication authentication) {

        return hiringPostService.getOwnerHiringPost(
                hiringPostId,
                authentication.getName()
        );
    }

    @PutMapping("/owner/{hiringPostId}")
    public ApiResponse<HiringPostResponse> updateHiringPost(
            @PathVariable Long hiringPostId,
            @RequestBody UpdateHiringPostRequest request,
            Authentication authentication) {

        return hiringPostService.updateHiringPost(
                hiringPostId,
                request,
                authentication.getName()
        );
    }

    @PostMapping("/owner/{hiringPostId}/close")
    public ApiResponse<HiringPostResponse> closeHiringPost(
            @PathVariable Long hiringPostId,
            Authentication authentication) {

        return hiringPostService.closeHiringPost(
                hiringPostId,
                authentication.getName()
        );
    }

    @PostMapping("/owner/{hiringPostId}/reopen")
    public ApiResponse<HiringPostResponse> reopenHiringPost(
            @PathVariable Long hiringPostId,
            @RequestBody ReopenHiringPostRequest request,
            Authentication authentication) {

        return hiringPostService.reopenHiringPost(
                hiringPostId,
                request,
                authentication.getName()
        );
    }

    @PostMapping("/{hiringPostId}/applications")
    public ApiResponse<HiringApplicationResponse> applyToHiringPost(
            @PathVariable Long hiringPostId,
            @RequestBody ApplyToHiringPostRequest request,
            Authentication authentication) {

        return hiringPostService.applyToHiringPost(
                hiringPostId,
                request,
                authentication.getName()
        );
    }

    @GetMapping("/{hiringPostId}/applications")
    public ApiResponse<List<HiringApplicationResponse>> getHiringPostApplications(
            @PathVariable Long hiringPostId,
            Authentication authentication) {

        return hiringPostService.getHiringPostApplications(
                hiringPostId,
                authentication.getName()
        );
    }

    @PostMapping("/applications/{applicationId}/accept")
    public ApiResponse<HiringApplicationResponse> acceptHiringApplication(
            @PathVariable Long applicationId,
            Authentication authentication) {

        return hiringPostService.acceptHiringApplication(
                applicationId,
                authentication.getName()
        );
    }

    @PostMapping("/applications/{applicationId}/reject")
    public ApiResponse<HiringApplicationResponse> rejectHiringApplication(
            @PathVariable Long applicationId,
            @RequestBody RejectHiringApplicationRequest request,
            Authentication authentication) {

        return hiringPostService.rejectHiringApplication(
                applicationId,
                request,
                authentication.getName()
        );
    }

    @GetMapping("/applications/me")
    public ApiResponse<List<HiringApplicationResponse>> getMyHiringApplications(
            Authentication authentication) {

        return hiringPostService.getMyHiringApplications(authentication.getName());
    }

    @PostMapping("/applications/{applicationId}/cancel-join")
    public ApiResponse<HiringApplicationResponse> cancelLinkedJoinRequest(
            @PathVariable Long applicationId,
            Authentication authentication) {

        return hiringPostService.cancelLinkedJoinRequest(
                applicationId,
                authentication.getName()
        );
    }
}
