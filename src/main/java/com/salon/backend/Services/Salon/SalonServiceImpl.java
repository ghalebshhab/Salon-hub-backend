package com.salon.backend.Services.Salon;

import com.salon.backend.Services.Subscription.SubscriptionService;
import com.salon.backend.Services.Translation.ArabicText;
import com.salon.backend.Services.Translation.ContentTranslationService;
import com.salon.backend.Entities.subscriptions.SubscriptionPlan;
import java.util.Comparator;
import java.util.Map;
import com.salon.backend.DTOs.ApiResponse;
import com.salon.backend.DTOs.Salon.CreateSalonRequest;
import com.salon.backend.DTOs.Salon.CreateSalonResponse;
import com.salon.backend.DTOs.Salon.UpdateSalonLocationRequest;
import com.salon.backend.DTOs.Salon.UpdateSalonRequest;
import com.salon.backend.DTOs.Salon.SalonResponse;
import com.salon.backend.Entities.salons.Salon;
import com.salon.backend.Entities.salons.SalonStatus;
import com.salon.backend.Entities.users.User;
import com.salon.backend.Entities.users.UserRole;
import com.salon.backend.Repositories.Salon.SalonRepo;
import com.salon.backend.Repositories.User.UserRepo;
import com.salon.backend.Services.Auth.Security.AdminAuthorizationService;
import com.salon.backend.Services.Notification.SalonNotificationService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Service
@AllArgsConstructor
public class SalonServiceImpl implements SalonService {

    private static final int MAX_RESUBMISSIONS = 3;

    private final SalonRepo salonRepo;
    private  final UserRepo userRepo;
    private final AdminAuthorizationService adminAuthorizationService;
    private final SalonNotificationService salonNotificationService;
    private final SalonOpenStatusResolver salonOpenStatusResolver;
    private final SubscriptionService subscriptionService;
    private final ContentTranslationService contentTranslationService;
    private final List<String> JordanCities = List.of(
            "Amman", "Zarqa", "Irbid", "Aqaba", "Jerash",
            "Madaba", "Ajloun", "As-Salt", "Al-Karak",
            "Ma'an", "Tafilah", "Mafraq"
    );
    @Override
    @Transactional
    public ApiResponse<CreateSalonResponse> createSalon(CreateSalonRequest createSalonRequest,Long id) {
        Optional<User> user = userRepo.findById(id);
        if(user.isEmpty()) {
            return ApiResponse.error("User not found");
        }
        User newUser=user.get();
        Salon salo = salonRepo.findByOwnerId(newUser.getId());

        if (salo != null && salo.getStatus() == SalonStatus.Requested) {
            return ApiResponse.error("You already have a salon request under review");
        }
        if (salo != null && salo.getStatus() == SalonStatus.Rejected) {
            if (salo.getResubmissionCount() >= MAX_RESUBMISSIONS) {
                return ApiResponse.error("You have reached the maximum of 3 salon resubmissions");
            }
            return ApiResponse.error("Edit and resubmit your existing rejected salon request");
        }
        if (salo != null && salo.getStatus() != SalonStatus.DELETED) {
            return ApiResponse.error("You already have a salon");
        }
        if(newUser.getRole()== UserRole.OWNER) {
            return ApiResponse.error("You are Owner for an Another Salon");
        }
        if(createSalonRequest.getSalonName()==null||createSalonRequest.getSalonName().length()<3) {
            return ApiResponse.error("Salon Name is Required and must be more than 3 characters");
        }
        if(createSalonRequest.getSalonCity()==null|| !JordanCities.contains(createSalonRequest.getSalonCity())) {
            return ApiResponse.error("Salon City is Required , and must be a valid jordanian city .");
        }
        if(createSalonRequest.getSalonDescription()==null||createSalonRequest.getSalonDescription().length()<10) {
            return ApiResponse.error("Salon Description is Required and must be more than 10 characters");
        }
        if(createSalonRequest.getSalonEmail()==null||
                !createSalonRequest.getSalonEmail()
                        .matches("^[A-Za-z0-9+_.-]+@(.+)$")){
            return ApiResponse.error("Salon Email is Invalid");
        }
        if(salonRepo.existsByEmail(createSalonRequest.getSalonEmail())) {
            return ApiResponse.error("Salon Email Already Exists");
        }
        if (createSalonRequest.getSalonPhoneNumber() == null ||
                (!createSalonRequest.getSalonPhoneNumber().matches("^\\+9627\\d{8}$")
                        && !createSalonRequest.getSalonPhoneNumber().matches("^07\\d{8}$"))) {
            return ApiResponse.error("Phone number can not be null and must be like +9627XXXXXXXX or 07XXXXXXXX");
        }
        if(salonRepo.existsBySalonPhoneNumber(createSalonRequest.getSalonPhoneNumber())){
            return ApiResponse.error("Salon Phone Number already exists");
        }
        if(createSalonRequest.getOpeningTime()==null){
            return ApiResponse.error("Opening Time is Required");
        }
        if(createSalonRequest.getClosingTime()==null){
            return ApiResponse.error("Closing Time is Required");
        }
        if(!createSalonRequest.getOpeningTime().isBefore(createSalonRequest.getClosingTime())){
            return ApiResponse.error("Opening Time must be before Closing Time ");
        }
        if(createSalonRequest.getMaxNumberOfEmployees()==null||createSalonRequest.getMaxNumberOfEmployees()<=0){
            return ApiResponse.error("Max number of employees is required and must be greater than 0");

        }
        SalonMapLocation.Result mapLocation = SalonMapLocation.resolve(
                createSalonRequest.getGoogleMapsUrl(),
                createSalonRequest.getLatitude(),
                createSalonRequest.getLongitude()
        );
        if (!mapLocation.isValid()) {
            return ApiResponse.error(mapLocation.error());
        }
        Salon salon=new Salon();
        salon.setEmail(createSalonRequest.getSalonEmail());
        salon.setOpeningTime(createSalonRequest.getOpeningTime());
        salon.setClosingTime(createSalonRequest.getClosingTime());
        salon.setLocation(createSalonRequest.getSalonCity());
        applyMapLocation(salon, mapLocation);
        salon.setDescription(createSalonRequest.getSalonDescription());
        salon.setMaxEmployeesNumber(createSalonRequest.getMaxNumberOfEmployees());
        salon.setSalonPhoneNumber(createSalonRequest.getSalonPhoneNumber());
        // Profile media is attached only through the authenticated Cloudinary route.
        salon.setProfilePictureUrl(null);
        salon.setOwner(newUser);
        salon.setOwnerPhoneNumber(newUser.getPhoneNumber());
        salon.setStatus(SalonStatus.Requested);
        salon.setName(createSalonRequest.getSalonName());
        salon.setCurrentEmployeesNumber(1L);
        salon.setResubmissionCount(0);
        salon.setRejectionReason(null);
        salonRepo.save(salon);
        contentTranslationService.translateSalonLater(salon.getId());
        salonNotificationService.notifyRequestSubmitted(salon);

        CreateSalonResponse createSalonResponse=new CreateSalonResponse(
                salon.getId(),
                salon.getName(),
                salon.getSalonPhoneNumber(),
                salon.getEmail(),
                salon.getLocation(),
                salon.getOwnerPhoneNumber(),
                newUser.getUserName(),
                salon.getOpeningTime(),
                salon.getClosingTime(),
                salon.getProfilePictureUrl(),
                salon.getCurrentEmployeesNumber(),
                salon.getMaxEmployeesNumber()
        );


        return ApiResponse.success("Salon Creation Requested",createSalonResponse);
    }

    @Override
    public ApiResponse<CreateSalonResponse> updateSalon(UpdateSalonRequest updateSalonRequest,
                                                        Long id,
                                                        Long ownerId)
    {
        User user=userRepo.findById(ownerId).orElse(null);
        if(user==null){
            return ApiResponse.error("User not found");
        }
        Optional<Salon> salo=salonRepo.findById(id);
        if(salo.isEmpty()){
            return ApiResponse.error("Salon Not Found");
        }
        Salon salon=salo.get();
        if(!salon.getOwner().equals(user)){
            return ApiResponse.error("You can only update your salon");
        }

        // Check every field first; change the salon only when all checks pass.
        String oldPhoneNumber=salon.getSalonPhoneNumber();
        String newPhoneNumber=updateSalonRequest.getSalonPhoneNumber();
        if(newPhoneNumber!=null && !newPhoneNumber.equals(oldPhoneNumber)){
            boolean valid = newPhoneNumber.matches("^\\+9627\\d{8}$")
                    || newPhoneNumber.matches("^07\\d{8}$");
            if (!valid) {
                return ApiResponse.error(
                        "Salon Phone Number must be in one of these formats: 07XXXXXXXX or +9627XXXXXXXX"
                );
            }
            if (salonRepo.existsBySalonPhoneNumber(newPhoneNumber)) {
                return ApiResponse.error("Phone number already exists.");
            }
        }
        if(updateSalonRequest.getSalonEmail()==null){
            return ApiResponse.error("Salon Email is Required , And must be in valid domain");
        }
        if(updateSalonRequest.getSalonCity()==null||!JordanCities.contains(updateSalonRequest.getSalonCity())){
            return ApiResponse.error("Salon City is Required , And must a valid jordanian city");
        }
        // An unchanged value is kept even if it was saved before a stricter rule;
        // the rule applies when the owner writes a new value.
        String description=updateSalonRequest.getSalonDescription();
        if(description==null
                || (description.length()<=10 && !description.equals(salon.getDescription()))){
            return ApiResponse.error("Salon Description is Required and must be more than 10 characters");
        }
        String name=updateSalonRequest.getSalonName();
        if(name==null || (name.length()<=3 && !name.equals(salon.getName()))){
            return ApiResponse.error("Salon Name is Required and must be more than 3 characters");
        }
        LocalTime opening=updateSalonRequest.getOpeningTime();
        LocalTime closing=updateSalonRequest.getClosingTime();
        if(opening==null){
            return ApiResponse.error("Opening Time is Required and must be before Closing Time ");
        }
        if(closing==null||!opening.isBefore(closing)){
            return ApiResponse.error("Closing Time is Required and must be After Opening Time ");
        }
        if (updateSalonRequest.getMaxNumberOfEmployees()==null||updateSalonRequest.getMaxNumberOfEmployees()<=0) {
            return ApiResponse.error("Max number of employees is required and must be greater than 0");
        }
        SalonMapLocation.Result mapLocation = SalonMapLocation.resolve(
                updateSalonRequest.getGoogleMapsUrl(),
                updateSalonRequest.getLatitude(),
                updateSalonRequest.getLongitude()
        );
        if (!mapLocation.isValid()) {
            return ApiResponse.error(mapLocation.error());
        }

        if(newPhoneNumber!=null){
            salon.setSalonPhoneNumber(newPhoneNumber);
        }
        salon.setEmail(updateSalonRequest.getSalonEmail());
        salon.setLocation(updateSalonRequest.getSalonCity());
        salon.setDescriptionAr(ArabicText.resolve(
                updateSalonRequest.getSalonDescriptionAr(), salon.getDescriptionAr(),
                salon.getDescription(), description, 1000));
        salon.setDescription(description);
        salon.setName(name);
        salon.setOpeningTime(opening);
        salon.setClosingTime(closing);
        salon.setMaxEmployeesNumber(updateSalonRequest.getMaxNumberOfEmployees());
        applyMapLocation(salon, mapLocation);
        salonRepo.save(salon);
        if (salon.getDescriptionAr() == null) {
            contentTranslationService.translateSalonLater(salon.getId());
        }

        CreateSalonResponse createSalonResponse=new CreateSalonResponse(
          id,
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

        );
        return ApiResponse.success("Salon updated Successfully", createSalonResponse);
    }

    @Override
    public ApiResponse<Void> deleteSalon(long salonId, Long ownerId) {
        Optional<Salon> opSalon=salonRepo.findById(salonId);
        if(opSalon.isEmpty()){
            return ApiResponse.error("Salon Not Found");
        }
        Salon salon=opSalon.get();
        if (salon.getOwner() == null || salon.getOwner().getId() != ownerId) {
            return ApiResponse.error("You can only delete your own salon");
        }
        salon.setStatus(SalonStatus.DELETED);
        salonRepo.save(salon);
        return ApiResponse.success("Salon Deleted Successfully",null);
    }

    @Override
    public ApiResponse<SalonResponse> getSalonById(long id) {
        Optional<Salon> opSalon=salonRepo.findById(id);
        if(opSalon.isEmpty()){
            return ApiResponse.error("Salon Not Found");
        }
        Salon salon=opSalon.get();
        if (salon.getStatus() != SalonStatus.Accepted) {
            return ApiResponse.error("Salon is not available");
        }
        return ApiResponse.success("Salon Fetched Successfully",
                mapSalonResponse(salon, subscriptionService.currentPlan(salon)));
    }

    @Override
    public ApiResponse<SalonResponse> getSalonByOwnerId(Long ownerId) {
        Salon salon=salonRepo.findByOwnerId(ownerId);
        if(salon==null){
            return ApiResponse.error("Salon Not Found");
        }
        return ApiResponse.success("Salon Fetched Successfully",
                mapSalonResponse(salon, subscriptionService.currentPlan(salon)));
    }

    @Override
    public ApiResponse<List<SalonResponse>> getAllSalons(Long adminId) {
        ApiResponse<User> authorization = adminAuthorizationService.authorize(adminId);
        if (!authorization.isSuccess()) {
            return ApiResponse.error(authorization.getMessage());
        }
        return ApiResponse.success("Salons Fetched Successfully", mapAll(salonRepo.findAll(), false));
    }

    @Override
    public ApiResponse<List<SalonResponse>> getActiveSalons() {
        // Salons whose plan includes the search boost come first; the rest keep their order.
        return ApiResponse.success("Active Salons Fetched Successfully",
                mapAll(salonRepo.findByStatus(SalonStatus.Accepted), true));
    }

    @Override
    public ApiResponse<List<SalonResponse>> getRequestedSalons(Long adminId) {
        ApiResponse<User> authorization = adminAuthorizationService.authorize(adminId);
        if (!authorization.isSuccess()) {
            return ApiResponse.error(authorization.getMessage());
        }
        List<SalonResponse> salons = salonRepo.findByStatus(SalonStatus.Requested).stream()
                .map(this::mapSalonResponse)
                .toList();
        return ApiResponse.success("Requested Salons Fetched Successfully", salons);
    }

    @Override
    @Transactional
    public ApiResponse<SalonResponse> resubmitRejectedSalon(Long salonId, Long ownerId) {
        if (salonId == null) {
            return ApiResponse.error("Salon id is required");
        }

        User owner = userRepo.findById(ownerId).orElse(null);
        if (owner == null) {
            return ApiResponse.error("User not found");
        }

        Optional<Salon> optionalSalon = salonRepo.findByIdForUpdate(salonId);
        if (optionalSalon.isEmpty()) {
            return ApiResponse.error("Salon Not Found");
        }

        Salon salon = optionalSalon.get();
        if (salon.getOwner() == null || salon.getOwner().getId() != owner.getId()) {
            return ApiResponse.error("You can only resubmit your salon request");
        }
        if (salon.getStatus() != SalonStatus.Rejected) {
            return ApiResponse.error("Only a rejected salon request can be resubmitted");
        }
        if (salon.getResubmissionCount() >= MAX_RESUBMISSIONS) {
            return ApiResponse.error("You have reached the maximum of 3 salon resubmissions");
        }

        salon.setResubmissionCount(salon.getResubmissionCount() + 1);
        salon.setRejectionReason(null);
        salon.setStatus(SalonStatus.Requested);
        salonRepo.save(salon);
        salonNotificationService.notifyRequestResubmitted(salon);

        return ApiResponse.success("Salon resubmitted successfully", mapSalonResponse(salon));
    }

    /** Maps many salons with one plan query; {@code boostFirst} puts boosted plans on top. */
    private List<SalonResponse> mapAll(List<Salon> salons, boolean boostFirst) {
        Map<Long, SubscriptionPlan> plans = subscriptionService.currentPlans(
                salons.stream().map(Salon::getId).toList());
        var stream = salons.stream();
        if (boostFirst) {
            stream = stream.sorted(Comparator.comparing(
                    (Salon salon) -> !SubscriptionService.boosted(plans.get(salon.getId()))));
        }
        return stream.map(salon -> mapSalonResponse(salon, plans.get(salon.getId()))).toList();
    }

    private SalonResponse mapSalonResponse(Salon salon) {
        return mapSalonResponse(salon, null);
    }

    private SalonResponse mapSalonResponse(Salon salon, SubscriptionPlan plan) {
        User owner = salon.getOwner();
        return new SalonResponse(
                salon.getId(),
                salon.getName(),
                salon.getSalonPhoneNumber(),
                salon.getEmail(),
                salon.getLocation(),
                salon.getGoogleMapsUrl(),
                salon.getLatitude(),
                salon.getLongitude(),
                salon.getDescription(),
                salon.getDescriptionAr(),
                owner == null ? null : owner.getId(),
                owner == null ? null : owner.getUserName(),
                salon.getOwnerPhoneNumber(),
                salon.getCurrentEmployeesNumber(),
                salon.getMaxEmployeesNumber(),
                salon.getCreatedAt(),
                salon.getOpeningTime(),
                salon.getClosingTime(),
                salon.getProfilePictureUrl(),
                salon.getStatus(),
                salonOpenStatusResolver.resolve(salon),
                salon.isBlockedByAdmin(),
                salon.isBlockedByOwner(),
                salon.getRejectionReason(),
                salon.getResubmissionCount(),
                Math.max(0, MAX_RESUBMISSIONS - salon.getResubmissionCount()),
                SubscriptionService.badgeOf(plan)
        );
    }

    @Override
    @Transactional
    public ApiResponse<SalonResponse> updateSalonLocation(Long salonId,
                                                          Long ownerId,
                                                          UpdateSalonLocationRequest request) {
        if (request == null) {
            return ApiResponse.error("Location details are required");
        }
        Salon salon = salonRepo.findById(salonId).orElse(null);
        if (salon == null) {
            return ApiResponse.error("Salon Not Found");
        }
        if (salon.getOwner() == null || ownerId == null || salon.getOwner().getId() != ownerId) {
            return ApiResponse.error("You can only update your salon");
        }
        SalonMapLocation.Result mapLocation =
                SalonMapLocation.resolve(null, request.latitude(), request.longitude());
        if (!mapLocation.isValid()) {
            return ApiResponse.error(mapLocation.error());
        }
        applyMapLocation(salon, mapLocation);
        if (mapLocation.latitude() != null && request.city() != null
                && JordanCities.contains(request.city())) {
            salon.setLocation(request.city());
        }
        salonRepo.save(salon);
        String message = mapLocation.latitude() == null
                ? "Salon map pin removed"
                : "Salon location saved";
        return ApiResponse.success(message, mapSalonResponse(salon));
    }

    private void applyMapLocation(Salon salon, SalonMapLocation.Result mapLocation) {
        salon.setGoogleMapsUrl(mapLocation.googleMapsUrl());
        salon.setLatitude(mapLocation.latitude());
        salon.setLongitude(mapLocation.longitude());
    }
}
