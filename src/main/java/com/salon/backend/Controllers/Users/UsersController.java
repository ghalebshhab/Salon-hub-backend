package com.salon.backend.Controllers.Users;


import com.salon.backend.DTOs.ApiResponse;
import com.salon.backend.DTOs.User.UserSummaryResponse;
import com.salon.backend.Services.User.UserDirectoryService;
import com.salon.backend.Services.Auth.Security.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:5174", "http://localhost:3000"})
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UsersController {
    private final UserDirectoryService userDirectoryService;


    @GetMapping
    public ApiResponse<List<UserSummaryResponse>> getUsers(
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return userDirectoryService.getUsers(currentUser.getId());
    }
    @GetMapping("/active")
    public ApiResponse<List<UserSummaryResponse>> getActiveUsers(
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return userDirectoryService.getActiveUsers(currentUser.getId());
    }
    @GetMapping("/blocked")
    public ApiResponse<List<UserSummaryResponse>> getBlockedUsers(
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return userDirectoryService.getBlockedUsers(currentUser.getId());
    }

}
