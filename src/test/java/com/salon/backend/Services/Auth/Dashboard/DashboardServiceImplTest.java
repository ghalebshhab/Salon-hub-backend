package com.salon.backend.Services.Auth.Dashboard;

import com.salon.backend.DTOs.ApiResponse;
import com.salon.backend.DTOs.Auth.Dashboard.DashboardLoginRequest;
import com.salon.backend.DTOs.Salon.SalonResponse;
import com.salon.backend.Entities.salons.Salon;
import com.salon.backend.Entities.salons.SalonStatus;
import com.salon.backend.Entities.users.User;
import com.salon.backend.Entities.users.UserRole;
import com.salon.backend.Entities.users.UserStatus;
import com.salon.backend.Repositories.Salon.SalonRepo;
import com.salon.backend.Repositories.User.UserRepo;
import com.salon.backend.Services.Auth.Security.JwtService;
import com.salon.backend.Services.Auth.Security.AdminAuthorizationService;
import org.junit.jupiter.api.BeforeEach;
import com.salon.backend.Services.Salon.SalonService;
import com.salon.backend.Services.Notification.SalonNotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import com.salon.backend.Services.Subscription.SubscriptionService;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DashboardServiceImplTest {

    private static final Long ADMIN_ID = 99L;

    @Mock private UserRepo userRepo;
    @Mock private BCryptPasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private SalonRepo salonRepo;
    @Mock private SalonService salonService;
    @Mock private AdminAuthorizationService adminAuthorizationService;
    @Mock private SalonNotificationService salonNotificationService;
    @Mock private SubscriptionService subscriptionService;
    @InjectMocks private DashboardServiceImpl service;

    @BeforeEach
    void authorizeAdmin() {
        User admin = new User();
        admin.setId(ADMIN_ID);
        admin.setRole(UserRole.ADMIN);
        admin.setStatus(UserStatus.Active);
        lenient().when(adminAuthorizationService.authorize(anyLong()))
                .thenReturn(ApiResponse.success("Admin authorized", admin));
    }

    @Test
    void rejectionStoresTrimmedReasonForTheOwner() {
        User admin = new User();
        admin.setId(3L);
        admin.setEmail("admin@example.com");
        admin.setRole(UserRole.ADMIN);
        admin.setStatus(UserStatus.Active);

        User owner = new User();
        owner.setEmail("owner@example.com");

        Salon salon = new Salon();
        salon.setId(9L);
        salon.setOwner(owner);
        salon.setStatus(SalonStatus.Requested);

        SalonResponse mappedResponse = new SalonResponse(
                salon.getId(), null, null, null, null, null, null, null, null,
                owner.getId(), null, null, null, null, null,
                null, null, null, SalonStatus.Rejected, null,
                false, false,
                "Missing license", 0, 3
        );

        when(salonRepo.findByIdForUpdate(salon.getId())).thenReturn(Optional.of(salon));
        when(salonService.getSalonByOwnerId(owner.getId()))
                .thenReturn(ApiResponse.success("Salon Fetched Successfully", mappedResponse));

        ApiResponse<SalonResponse> response = service.rejectSalonRequest(
                salon.getId(),
                admin.getId(),
                "  Missing license  "
        );

        assertTrue(response.isSuccess());
        assertEquals(SalonStatus.Rejected, salon.getStatus());
        assertEquals("Missing license", salon.getRejectionReason());
        verify(salonRepo).save(salon);
        verify(salonNotificationService).notifyRequestRejected(salon, "Missing license");
    }

    @Test
    void acceptingSalonPromotesTheLockedOwnerToOwnerRole() {
        User owner = new User();
        owner.setId(12L);
        owner.setEmail("owner@example.com");
        owner.setRole(UserRole.USER);
        owner.setStatus(UserStatus.Active);

        Salon salon = new Salon();
        salon.setId(9L);
        salon.setOwner(owner);
        salon.setStatus(SalonStatus.Requested);
        salon.setRejectionReason("Previous reason");

        when(salonRepo.findByIdForUpdate(salon.getId())).thenReturn(Optional.of(salon));
        when(userRepo.findByIdForUpdate(owner.getId())).thenReturn(Optional.of(owner));
        when(salonService.getSalonByOwnerId(owner.getId()))
                .thenReturn(ApiResponse.success("Salon fetched", null));

        ApiResponse<SalonResponse> response = service.acceptSalonRequest(salon.getId(), ADMIN_ID);

        assertTrue(response.isSuccess());
        assertEquals(UserRole.OWNER, owner.getRole());
        assertEquals(SalonStatus.Accepted, salon.getStatus());
        assertNull(salon.getRejectionReason());
        verify(userRepo).saveAndFlush(owner);
        verify(salonRepo).saveAndFlush(salon);
        verify(salonNotificationService).notifyRequestAccepted(salon);
    }

    @Test
    void blockingOwnerTemporarilyDisablesOwnedSalon() {
        User owner = new User();
        owner.setId(15L);
        owner.setEmail("owner@example.com");
        owner.setRole(UserRole.OWNER);
        owner.setStatus(UserStatus.Active);

        Salon salon = new Salon();
        salon.setId(4L);
        salon.setOwner(owner);
        salon.setStatus(SalonStatus.Accepted);

        when(userRepo.findByIdForUpdate(owner.getId())).thenReturn(Optional.of(owner));
        when(salonRepo.findByOwnerId(owner.getId())).thenReturn(salon);
        when(salonRepo.findByIdForUpdate(salon.getId())).thenReturn(Optional.of(salon));

        ApiResponse<String> response = service.blockUser(owner.getId(), ADMIN_ID);

        assertTrue(response.isSuccess());
        assertEquals(UserStatus.Blocked, owner.getStatus());
        assertEquals(SalonStatus.Inactive, salon.getStatus());
        assertEquals(SalonStatus.Accepted, salon.getStatusBeforeBlock());
        assertTrue(salon.isBlockedByOwner());
        verify(userRepo).save(owner);
        verify(salonRepo).save(salon);
    }

    @Test
    void unblockingOwnerKeepsSalonInactiveWhileAdminBlockRemains() {
        User owner = new User();
        owner.setId(15L);
        owner.setEmail("owner@example.com");
        owner.setStatus(UserStatus.Blocked);

        Salon salon = new Salon();
        salon.setId(4L);
        salon.setOwner(owner);
        salon.setStatus(SalonStatus.Inactive);
        salon.setStatusBeforeBlock(SalonStatus.Accepted);
        salon.setBlockedByOwner(true);
        salon.setBlockedByAdmin(true);

        when(userRepo.findByIdForUpdate(owner.getId())).thenReturn(Optional.of(owner));
        when(salonRepo.findByOwnerId(owner.getId())).thenReturn(salon);
        when(salonRepo.findByIdForUpdate(salon.getId())).thenReturn(Optional.of(salon));

        ApiResponse<String> response = service.unblockUser(owner.getId(), ADMIN_ID);

        assertTrue(response.isSuccess());
        assertEquals(UserStatus.Active, owner.getStatus());
        assertEquals(SalonStatus.Inactive, salon.getStatus());
        assertFalse(salon.isBlockedByOwner());
        assertTrue(salon.isBlockedByAdmin());
        assertEquals(SalonStatus.Accepted, salon.getStatusBeforeBlock());
    }

    @Test
    void salonReturnsToItsPreviousStateAfterAdminUnblocksIt() {
        User owner = new User();
        owner.setId(18L);
        owner.setRole(UserRole.OWNER);

        Salon salon = new Salon();
        salon.setId(8L);
        salon.setStatus(SalonStatus.Rejected);
        salon.setOwner(owner);

        when(salonRepo.findByIdForUpdate(salon.getId())).thenReturn(Optional.of(salon));
        when(userRepo.findByIdForUpdate(owner.getId())).thenReturn(Optional.of(owner));

        ApiResponse<String> blockResponse = service.blockSalon(salon.getId(), ADMIN_ID);

        assertEquals(UserRole.USER, owner.getRole());
        assertEquals(UserRole.OWNER, salon.getOwnerRoleBeforeAdminBlock());

        ApiResponse<String> unblockResponse = service.unblockSalon(salon.getId(), ADMIN_ID);

        assertTrue(blockResponse.isSuccess());
        assertTrue(unblockResponse.isSuccess());
        assertEquals(SalonStatus.Rejected, salon.getStatus());
        assertNull(salon.getStatusBeforeBlock());
        assertNull(salon.getOwnerRoleBeforeAdminBlock());
        assertFalse(salon.isBlockedByAdmin());
        assertEquals(UserRole.OWNER, owner.getRole());
        verify(salonRepo, times(2)).save(salon);
        verify(userRepo, times(2)).save(owner);
    }

    @Test
    void adminUserCannotBeBlocked() {
        User admin = new User();
        admin.setId(20L);
        admin.setRole(UserRole.ADMIN);
        admin.setStatus(UserStatus.Active);

        when(userRepo.findByIdForUpdate(admin.getId())).thenReturn(Optional.of(admin));

        ApiResponse<String> response = service.blockUser(admin.getId(), ADMIN_ID);

        assertFalse(response.isSuccess());
        assertEquals("Admin users cannot be blocked", response.getMessage());
        assertEquals(UserStatus.Active, admin.getStatus());
        verify(userRepo, never()).save(admin);
        verifyNoInteractions(salonRepo);
    }

    @Test
    void blockedAdminCannotLogInToDashboard() {
        User admin = new User();
        admin.setEmail("admin@example.com");
        admin.setRole(UserRole.ADMIN);
        admin.setStatus(UserStatus.Blocked);

        DashboardLoginRequest request = new DashboardLoginRequest();
        request.setAdminEmail(admin.getEmail());
        request.setAdminPassword("password");

        when(userRepo.findByemail(admin.getEmail())).thenReturn(admin);

        ApiResponse<?> response = service.DashboardLogin(request);

        assertFalse(response.isSuccess());
        assertEquals("Admin account is not active", response.getMessage());
        verifyNoInteractions(passwordEncoder, jwtService);
    }

    @Test
    void unauthorizedCallerCannotBlockAUser() {
        when(adminAuthorizationService.authorize(5L))
                .thenReturn(ApiResponse.error("You are not Admin , Access Denied"));

        ApiResponse<String> response = service.blockUser(15L, 5L);

        assertFalse(response.isSuccess());
        assertEquals("You are not Admin , Access Denied", response.getMessage());
        verifyNoInteractions(salonRepo);
        verify(userRepo, never()).findByIdForUpdate(anyLong());
    }
}
