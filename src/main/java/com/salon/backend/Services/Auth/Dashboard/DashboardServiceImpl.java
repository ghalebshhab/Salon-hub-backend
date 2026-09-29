package com.salon.backend.Services.Auth.Dashboard;

import com.salon.backend.DTOs.ApiResponse;
import com.salon.backend.DTOs.Auth.Dashboard.DashboardLoginRequest;
import com.salon.backend.DTOs.Auth.Dashboard.DashboardLoginResponse;
import com.salon.backend.DTOs.Salon.CreateSalonRequest;
import com.salon.backend.DTOs.Salon.CreateSalonResponse;
import com.salon.backend.Entities.salons.Salon;
import com.salon.backend.Entities.salons.SalonStatus;
import com.salon.backend.Entities.users.User;
import com.salon.backend.Entities.users.UserRole;
import com.salon.backend.Entities.users.UserStatus;
import com.salon.backend.Repositories.Salon.SalonRepo;
import com.salon.backend.Repositories.User.UserRepo;
import com.salon.backend.Services.Auth.Security.JwtService;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@RequiredArgsConstructor
@Service
public class DashboardServiceImpl implements DashboardService {
    private final UserRepo userRepo;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;
    private final JwtService jwtService;
    private final SalonRepo salonRepo;
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
    public ApiResponse<CreateSalonResponse> AcceptSalonRequest(CreateSalonRequest createSalonRequest,String adminEmail) {
        User user=validateAdmin(adminEmail).getData();
        Optional<User> own=userRepo.findById(createSalonRequest.getOwnerId());
        if(own.isEmpty()){
            return ApiResponse.error("Owner not found");
        }

        Salon salon=salonRepo.findByEmail(own.get().getEmail());
        if(salon==null){
            return ApiResponse.error("Salon not found");
        }
        if(salon.getStatus()!=SalonStatus.Requested){
            return ApiResponse.error("Salon not requested");
        }
        salon.setStatus(SalonStatus.Accepted);
        salonRepo.save(salon);
        own.get().setRole(UserRole.OWNER);
        userRepo.save(own.get());

        return ApiResponse.success("Salon Creation Request Accepted",new CreateSalonResponse(
                salon.getId(),
                salon.getName(),
                salon.getSalonPhoneNumber(),
                salon.getEmail(),
                salon.getLocation(),
                salon.getOwnerPhoneNumber(),
                salon.getOwner().getUserName(),
                salon.getOpeningTime(),
                salon.getClosingTime(),
                salon.getProfilePictureUrl(),
                salon.getCurrentEmployeesNumber(),
                salon.getMaxEmployeesNumber()
        ));
    }

    @Override
    public ApiResponse<CreateSalonResponse> RejectSalonRequest(CreateSalonRequest createSalonRequest,String adminEmail, String rejectionReason) {
        User user=validateAdmin(adminEmail).getData();
        Optional<User> own=userRepo.findById(createSalonRequest.getOwnerId());
        if(own.isEmpty()){
            return ApiResponse.error("Owner not found");
        }

        Salon salon=salonRepo.findByOwnerEmail(own.get().getEmail());
        if(salon==null){
            return ApiResponse.error("Salon not found");
        }
        if(salon.getStatus()!=SalonStatus.Requested){
            return ApiResponse.error("Salon not requested");
        }
        salon.setStatus(SalonStatus.Rejected);
        salonRepo.save(salon);


        return ApiResponse.success("Salon Creation Request Rejected , "+rejectionReason,new CreateSalonResponse(
                salon.getId(),
                salon.getName(),
                salon.getSalonPhoneNumber(),
                salon.getEmail(),
                salon.getLocation(),
                salon.getOwnerPhoneNumber(),
                salon.getOwner().getUserName(),
                salon.getOpeningTime(),
                salon.getClosingTime(),
                salon.getProfilePictureUrl(),
                salon.getCurrentEmployeesNumber(),
                salon.getMaxEmployeesNumber()
        ));


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
