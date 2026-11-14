package com.salon.backend.Controllers.Assistant;

import com.salon.backend.DTOs.ApiResponse;
import com.salon.backend.DTOs.Assistant.AssistantChatRequest;
import com.salon.backend.DTOs.Assistant.AssistantChatResponse;
import com.salon.backend.DTOs.Assistant.AssistantInsightsResponse;
import com.salon.backend.Services.Assistant.AssistantService;
import com.salon.backend.Services.Auth.Security.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** The assistant is open to guests; signed-in members get personal answers. */
@RestController
@RequestMapping("/api/assistant")
@RequiredArgsConstructor
public class AssistantController {

    private final AssistantService assistantService;

    @PostMapping("/chat")
    public ApiResponse<AssistantChatResponse> chat(
            @RequestBody AssistantChatRequest request,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return assistantService.chat(request, currentUser == null ? null : currentUser.getId());
    }

    @GetMapping("/insights")
    public ApiResponse<AssistantInsightsResponse> insights(
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return assistantService.insights(currentUser == null ? null : currentUser.getId());
    }
}
