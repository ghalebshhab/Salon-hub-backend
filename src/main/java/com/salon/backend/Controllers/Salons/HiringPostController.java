package com.salon.backend.Controllers.Salons;

import com.salon.backend.DTOs.ApiResponse;
import com.salon.backend.DTOs.Salon.HiringPost.ApplyToHiringPostRequest;
import com.salon.backend.DTOs.Salon.HiringPost.CreateHiringPostRequest;
import com.salon.backend.DTOs.Salon.HiringPost.HiringApplicationResponse;
import com.salon.backend.DTOs.Salon.HiringPost.HiringPostResponse;
import com.salon.backend.DTOs.Salon.HiringPost.RejectHiringApplicationRequest;
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

    @PostMapping
    public ApiResponse<HiringPostResponse> createHiringPost(
            @RequestBody CreateHiringPostRequest request,
            Authentication authentication) {

        return hiringPostService.createHiringPost(
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
}
