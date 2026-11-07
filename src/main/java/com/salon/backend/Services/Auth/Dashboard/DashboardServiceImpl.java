package com.salon.backend.Services.Auth.Dashboard;

import com.salon.backend.DTOs.ApiResponse;
import com.salon.backend.DTOs.Auth.Dashboard.DashboardLoginRequest;
import com.salon.backend.DTOs.Auth.Dashboard.DashboardLoginResponse;
import com.salon.backend.DTOs.Salon.SalonResponse;
import com.salon.backend.Entities.salons.Salon;
import com.salon.backend.Entities.salons.SalonStatus;
import com.salon.backend.Entities.users.User;
import com.salon.backend.Entities.users.UserRole;
import com.salon.backend.Entities.users.UserStatus;
import com.salon.backend.Repositories.Salon.SalonRepo;
import com.salon.backend.Repositories.User.UserRepo;
import com.salon.backend.Services.Auth.Security.AdminAuthorizationService;
import com.salon.backend.Services.Auth.Security.JwtService;
import com.salon.backend.Services.Salon.SalonService;
import com.salon.backend.Services.Notification.SalonNotificationService;
import com.salon.backend.Services.Activity.UserActivityTrackingService;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@RequiredArgsConstructor
@Service
public class DashboardServiceImpl implements DashboardService {
    private final UserRepo userRepo;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;
    private final JwtService jwtService;
    private final SalonRepo salonRepo;
    private final SalonService salonService;
    private final AdminAuthorizationService adminAuthorizationService;
    private final SalonNotificationService salonNotificationService;
    private final UserActivityTrackingService userActivityTrackingService;
    @Override
    public ApiResponse<DashboardLoginResponse> DashboardLogin(
            DashboardLoginRequest dashboardLoginRequest) {

        ApiResponse<User> adminResponse =
                validateAdmin(
                        dashboardLoginRequest.getAdminEmail());

        if (!adminResponse.isSuccess()) {
            return ApiResponse.error(adminResponse.getMessage());
        }

        User user = adminResponse.getData();

        if (!bCryptPasswordEncoder.matches(
                dashboardLoginRequest.getAdminPassword(),
                user.getPassword())) {

            return ApiResponse.error(
                    "Wrong Password , Please Try Again");
        }

        String token =
                jwtService.generateToken(user);

        try {
            userActivityTrackingService.recordSuccessfulLogin(user);
        } catch (RuntimeException ignored) {
            // Activity analytics must never prevent a valid administrator from signing in.
        }

        DashboardLoginResponse response =
                new DashboardLoginResponse(
                        token,
                        "Bearer"
                );

        return ApiResponse.success(
                "Dashboard Login Success",
                response
        );
    }
    @Override
    @Transactional
    public ApiResponse<SalonResponse> acceptSalonRequest(Long salonId, Long adminId) {
        ApiResponse<User> adminResponse = adminAuthorizationService.authorize(adminId);
        if (!adminResponse.isSuccess()) {
            return ApiResponse.error(adminResponse.getMessage());
        }

        if (salonId == null) {
            return ApiResponse.error("Salon id is required");
        }

        Optional<Salon> optionalSalon = salonRepo.findByIdForUpdate(salonId);
        if(optionalSalon.isEmpty()){
            return ApiResponse.error("Salon not found");
        }
        Salon salon = optionalSalon.get();
        if(salon.getStatus()!=SalonStatus.Requested){
            return ApiResponse.error("Salon not requested");
        }

        User salonOwner = salon.getOwner();
        if (salonOwner == null || salonOwner.getId() <= 0) {
            return ApiResponse.error("Salon owner not found");
        }

        Optional<User> optionalOwner = userRepo.findByIdForUpdate(salonOwner.getId());
        if (optionalOwner.isEmpty()) {
            return ApiResponse.error("Salon owner not found");
        }

        User owner = optionalOwner.get();
        salon.setStatus(SalonStatus.Accepted);
        salon.setRejectionReason(null);
        owner.setRole(UserRole.OWNER);

        userRepo.saveAndFlush(owner);
        salonRepo.saveAndFlush(salon);
        salonNotificationService.notifyRequestAccepted(salon);

        ApiResponse<SalonResponse> salonResponse =
                salonService.getSalonByOwnerId(owner.getId());
        return ApiResponse.success("Salon Creation Request Accepted", salonResponse.getData());
    }

    @Override
    @Transactional
    public ApiResponse<SalonResponse> rejectSalonRequest(
            Long salonId,
            Long adminId,
            String rejectionReason) {
        ApiResponse<User> adminResponse = adminAuthorizationService.authorize(adminId);
        if (!adminResponse.isSuccess()) {
            return ApiResponse.error(adminResponse.getMessage());
        }

        if (salonId == null) {
            return ApiResponse.error("Salon id is required");
        }
        if (rejectionReason == null || rejectionReason.isBlank()) {
            return ApiResponse.error("Rejection reason is required");
        }
        if (rejectionReason.trim().length() > 1000) {
            return ApiResponse.error("Rejection reason cannot exceed 1000 characters");
        }

        Optional<Salon> optionalSalon = salonRepo.findByIdForUpdate(salonId);
        if(optionalSalon.isEmpty()){
            return ApiResponse.error("Salon not found");
        }
        Salon salon = optionalSalon.get();
        if(salon.getStatus()!=SalonStatus.Requested){
            return ApiResponse.error("Salon not requested");
        }
        salon.setStatus(SalonStatus.Rejected);
        salon.setRejectionReason(rejectionReason.trim());
        salonRepo.save(salon);
        salonNotificationService.notifyRequestRejected(salon, salon.getRejectionReason());

        ApiResponse<SalonResponse> salonResponse =
                salonService.getSalonByOwnerId(salon.getOwner().getId());
        return ApiResponse.success("Salon Creation Request Rejected", salonResponse.getData());
    }
    @Override
    @Transactional
    public ApiResponse<String> blockUser(Long userId, Long adminId) {
        ApiResponse<User> adminResponse = adminAuthorizationService.authorize(adminId);
        if (!adminResponse.isSuccess()) {
            return ApiResponse.error(adminResponse.getMessage());
        }

        if (userId == null) {
            return ApiResponse.error("User id is required");
        }

        Optional<User> userOptional = userRepo.findByIdForUpdate(userId);
        if (userOptional.isEmpty()) {
            return ApiResponse.error("User not found");
        }

        User user = userOptional.get();
        if (user.getStatus() == UserStatus.Deleted) {
            return ApiResponse.error("Deleted user cannot be blocked");
        }
        if (user.getRole() == UserRole.ADMIN) {
            return ApiResponse.error("Admin users cannot be blocked");
        }
        if (user.getStatus() == UserStatus.Blocked) {
            return ApiResponse.error("User already blocked");
        }

        Salon ownedSalon = salonRepo.findByOwnerId(user.getId());
        if (ownedSalon != null && ownedSalon.getStatus() != SalonStatus.DELETED) {
            Optional<Salon> salonOptional = salonRepo.findByIdForUpdate(ownedSalon.getId());
            if (salonOptional.isPresent()) {
                Salon salon = salonOptional.get();
                blockSalonState(salon, false);
                salonRepo.save(salon);
            }
        }

        user.setStatus(UserStatus.Blocked);
        userRepo.save(user);
        return ApiResponse.success("User Blocked", null);
    }

    @Override
    @Transactional
    public ApiResponse<String> unblockUser(Long userId, Long adminId) {
        ApiResponse<User> adminResponse = adminAuthorizationService.authorize(adminId);
        if (!adminResponse.isSuccess()) {
            return ApiResponse.error(adminResponse.getMessage());
        }

        if (userId == null) {
            return ApiResponse.error("User id is required");
        }

        Optional<User> userOptional = userRepo.findByIdForUpdate(userId);
        if (userOptional.isEmpty()) {
            return ApiResponse.error("User not found");
        }

        User user = userOptional.get();
        if (user.getStatus() == UserStatus.Deleted) {
            return ApiResponse.error("Deleted user cannot be unblocked");
        }
        if (user.getStatus() != UserStatus.Blocked) {
            return ApiResponse.error("User is not blocked");
        }

        Salon ownedSalon = salonRepo.findByOwnerId(user.getId());
        if (ownedSalon != null && ownedSalon.getStatus() != SalonStatus.DELETED) {
            Optional<Salon> salonOptional = salonRepo.findByIdForUpdate(ownedSalon.getId());
            if (salonOptional.isPresent()) {
                Salon salon = salonOptional.get();
                salon.setBlockedByOwner(false);
                restoreSalonWhenFullyUnblocked(salon);
                salonRepo.save(salon);
            }
        }

        user.setStatus(UserStatus.Active);
        userRepo.save(user);
        return ApiResponse.success("User Unblocked", null);
    }

    @Override
    @Transactional
    public ApiResponse<String> blockSalon(Long salonId, Long adminId) {
        ApiResponse<User> adminResponse = adminAuthorizationService.authorize(adminId);
        if (!adminResponse.isSuccess()) {
            return ApiResponse.error(adminResponse.getMessage());
        }

        if (salonId == null) {
            return ApiResponse.error("Salon id is required");
        }

        Optional<Salon> salonOptional = salonRepo.findByIdForUpdate(salonId);
        if (salonOptional.isEmpty()) {
            return ApiResponse.error("Salon not found");
        }

        Salon salon = salonOptional.get();
        if (salon.getStatus() == SalonStatus.DELETED) {
            return ApiResponse.error("Deleted salon cannot be blocked");
        }
        if (salon.isBlockedByAdmin()) {
            return ApiResponse.error("Salon already blocked");
        }

        blockSalonState(salon, true);
        User owner = salon.getOwner();
        if (owner != null) {
            Optional<User> ownerOptional = userRepo.findByIdForUpdate(owner.getId());
            if (ownerOptional.isPresent()) {
                User lockedOwner = ownerOptional.get();
                salon.setOwnerRoleBeforeAdminBlock(lockedOwner.getRole());
                lockedOwner.setRole(UserRole.USER);
                userRepo.save(lockedOwner);
            }
        }
        salonRepo.save(salon);
        salonNotificationService.notifySalonBlocked(salon);
        return ApiResponse.success("Salon Blocked", null);
    }

    @Override
    @Transactional
    public ApiResponse<String> unblockSalon(Long salonId, Long adminId) {
        ApiResponse<User> adminResponse = adminAuthorizationService.authorize(adminId);
        if (!adminResponse.isSuccess()) {
            return ApiResponse.error(adminResponse.getMessage());
        }

        if (salonId == null) {
            return ApiResponse.error("Salon id is required");
        }

        Optional<Salon> salonOptional = salonRepo.findByIdForUpdate(salonId);
        if (salonOptional.isEmpty()) {
            return ApiResponse.error("Salon not found");
        }

        Salon salon = salonOptional.get();
        if (salon.getStatus() == SalonStatus.DELETED) {
            return ApiResponse.error("Deleted salon cannot be unblocked");
        }
        if (!salon.isBlockedByAdmin()) {
            return ApiResponse.error("Salon is not blocked by admin");
        }

        salon.setBlockedByAdmin(false);
        User owner = salon.getOwner();
        if (owner != null && salon.getOwnerRoleBeforeAdminBlock() != null) {
            Optional<User> ownerOptional = userRepo.findByIdForUpdate(owner.getId());
            if (ownerOptional.isPresent()) {
                User lockedOwner = ownerOptional.get();
                lockedOwner.setRole(salon.getOwnerRoleBeforeAdminBlock());
                userRepo.save(lockedOwner);
            }
            salon.setOwnerRoleBeforeAdminBlock(null);
        }
        restoreSalonWhenFullyUnblocked(salon);
        salonRepo.save(salon);
        salonNotificationService.notifySalonUnblocked(salon);
        return ApiResponse.success("Salon Unblocked", null);
    }

    private void blockSalonState(Salon salon, boolean blockedByAdmin) {
        if (!salon.isBlockedByAdmin() && !salon.isBlockedByOwner()) {
            salon.setStatusBeforeBlock(salon.getStatus());
        }
        if (blockedByAdmin) {
            salon.setBlockedByAdmin(true);
        } else {
            salon.setBlockedByOwner(true);
        }
        salon.setStatus(SalonStatus.Inactive);
    }

    private void restoreSalonWhenFullyUnblocked(Salon salon) {
        if (salon.isBlockedByAdmin() || salon.isBlockedByOwner()) {
            salon.setStatus(SalonStatus.Inactive);
            return;
        }

        if (salon.getStatusBeforeBlock() != null) {
            salon.setStatus(salon.getStatusBeforeBlock());
            salon.setStatusBeforeBlock(null);
        }
    }

    private ApiResponse<User> validateAdmin(String email){
        User admin = userRepo.findByemail(email);
        return validateAdminUser(admin);
    }

    private ApiResponse<User> validateAdminUser(User admin){

        if(admin == null){
            return ApiResponse.error("Admin not found");
        }

        if(admin.getRole() != UserRole.ADMIN){
          return ApiResponse.error("You are not Admin , Access Denied");
        }

        if (admin.getStatus() != UserStatus.Active) {
            return ApiResponse.error("Admin account is not active");
        }

        return ApiResponse.success("Admin Founded",admin);
    }
}
