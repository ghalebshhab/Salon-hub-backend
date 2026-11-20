package com.salon.backend.Controllers.Payments;

import com.salon.backend.DTOs.ApiResponse;
import com.salon.backend.DTOs.Payment.PaymentOptionsResponse;
import com.salon.backend.DTOs.Payment.PaymentSettingsRequest;
import com.salon.backend.DTOs.Payment.PaymentSettingsResponse;
import com.salon.backend.Services.Auth.Security.AuthenticatedUser;
import com.salon.backend.Services.Payment.NoShowDepositRule;
import com.salon.backend.Services.Payment.PaymentSettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Payment methods a customer can choose, and the owner's payment settings. */
@RestController
@RequestMapping("/api/salons/{salonId}")
@RequiredArgsConstructor
public class SalonPaymentSettingsController {

    private final PaymentSettingsService settingsService;
    private final NoShowDepositRule noShowDepositRule;

    /**
     * Public: the booking wizard shows these choices. For a signed-in customer
     * the salon's no-show rule can narrow them.
     */
    @GetMapping("/payment-options")
    public ApiResponse<PaymentOptionsResponse> getOptions(
            @PathVariable Long salonId,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        ApiResponse<PaymentOptionsResponse> options = settingsService.getOptions(salonId);
        if (!options.isSuccess() || currentUser == null) {
            return options;
        }
        return ApiResponse.success(
                options.getMessage(),
                noShowDepositRule.forCustomer(options.getData(), currentUser.getId())
        );
    }

    @GetMapping("/payment-settings")
    public ApiResponse<PaymentSettingsResponse> getSettings(
            @PathVariable Long salonId,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return settingsService.getSettings(salonId, currentUser.getId());
    }

    @PutMapping("/payment-settings")
    public ApiResponse<PaymentSettingsResponse> updateSettings(
            @PathVariable Long salonId,
            @RequestBody PaymentSettingsRequest request,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return settingsService.updateSettings(salonId, request, currentUser.getId());
    }
}
