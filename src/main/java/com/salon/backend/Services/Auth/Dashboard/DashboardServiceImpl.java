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
import com.salon.backend.Services.Auth.Security.JwtService;
import com.salon.backend.Services.Salon.SalonService;
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
    public ApiResponse<SalonResponse> acceptSalonRequest(Long salonId, String adminEmail) {
        ApiResponse<User> adminResponse = validateAdmin(adminEmail);
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
        salon.setStatus(SalonStatus.Accepted);
        salon.setRejectionReason(null);
        salonRepo.save(salon);
        User owner = salon.getOwner();
        owner.setRole(UserRole.OWNER);
        userRepo.save(owner);

        ApiResponse<SalonResponse> salonResponse =
                salonService.getSalonByOwnerEmail(owner.getEmail());
        return ApiResponse.success("Salon Creation Request Accepted", salonResponse.getData());
    }

    @Override
    @Transactional
    public ApiResponse<SalonResponse> rejectSalonRequest(
            Long salonId,
            String adminEmail,
            String rejectionReason) {
        ApiResponse<User> adminResponse = validateAdmin(adminEmail);
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

        ApiResponse<SalonResponse> salonResponse =
                salonService.getSalonByOwnerEmail(salon.getOwner().getEmail());
        return ApiResponse.success("Salon Creation Request Rejected", salonResponse.getData());
    }
    @Override
    public ApiResponse<String> blockUser(Long userId) {
    Optional<User> userOptional=userRepo.findById(userId);
    if(userOptional.isEmpty()){
        return ApiResponse.error("User not found");
    }
    User user=userOptional.get();
    if(user.getStatus()!= UserStatus.Active){
        return ApiResponse.error("User not active");
    }
    if(user.getRole()==UserRole.OWNER){
        Optional<Salon> opsalon=salonRepo.findById(user.getId());
        if(opsalon.isEmpty()){
            return ApiResponse.error("Salon not found");
        }
        Salon salon=opsalon.get();
        salon.setStatus(SalonStatus.Inactive);
        salonRepo.save(salon);
    }
    user.setStatus(UserStatus.Blocked);
    userRepo.save(user);

        return ApiResponse.success("User Blocked",null);
    }

    private ApiResponse<User> validateAdmin(String email){
        User admin = userRepo.findByemail(email);

        if(admin == null){
            return ApiResponse.error("Admin not found");
        }

        if(admin.getRole() != UserRole.ADMIN){
          return ApiResponse.error("You are not Admin , Access Denied");
        }

        return ApiResponse.success("Admin Founded",admin);
    }
}
