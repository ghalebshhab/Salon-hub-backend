package com.salon.backend.Services.Salon.Employment;

import com.salon.backend.DTOs.ApiResponse;
import com.salon.backend.DTOs.Salon.Employment.Join.EmployeeRequestResponse;
import com.salon.backend.DTOs.Salon.Employment.Join.JoinSalonRequest;
import com.salon.backend.DTOs.Salon.Employment.Join.RejectRequest;
import com.salon.backend.Entities.salons.Salon;
import com.salon.backend.Entities.salons.SalonStatus;
import com.salon.backend.Entities.salons.employment.EmploymentRequest;
import com.salon.backend.Entities.salons.employment.EmploymentRequestStatus;
import com.salon.backend.Entities.salons.employment.LeaveRequest;
import com.salon.backend.Entities.salons.employment.LeaveRequestStatus;
import com.salon.backend.Entities.salons.employment.RequestType;
import com.salon.backend.Entities.users.User;
import com.salon.backend.Entities.users.UserRole;
import com.salon.backend.Repositories.Salon.EmploymentRepo;
import com.salon.backend.Repositories.Salon.EmployeeServiceCapabilityRepo;
import com.salon.backend.Repositories.Salon.HiringPostRepo;
import com.salon.backend.Repositories.Salon.LeaveRequestRepo;
import com.salon.backend.Repositories.Salon.SalonRepo;
import com.salon.backend.Repositories.User.UserRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmploymentServiceImplTest {

    @Mock private SalonRepo salonRepo;
    @Mock private UserRepo userRepo;
    @Mock private EmploymentRepo employmentRepo;
    @Mock private LeaveRequestRepo leaveRequestRepo;
    @Mock private HiringPostRepo hiringPostRepo;
    @Mock private EmployeeServiceCapabilityRepo capabilityRepo;

    private EmploymentServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new EmploymentServiceImpl(
                salonRepo,
                userRepo,
                employmentRepo,
                leaveRequestRepo,
                hiringPostRepo,
                capabilityRepo
        );
    }

    @Test
    void joinSalonUsesAuthenticatedUserIdAndReturnsDto() {
        User owner = user(1L, "owner@example.com", "owner", UserRole.OWNER);
        User applicant = user(2L, "applicant@example.com", "applicant", UserRole.USER);
        Salon salon = salon(10L, owner);
        JoinSalonRequest request = new JoinSalonRequest();
        request.setSalonId(salon.getId());

        when(userRepo.findById(applicant.getId())).thenReturn(Optional.of(applicant));
        when(salonRepo.findById(salon.getId())).thenReturn(Optional.of(salon));
        when(employmentRepo.findPendingRequest(
                salon,
                applicant,
                EmploymentRequestStatus.Requested
        )).thenReturn(Optional.empty());

        ApiResponse<EmployeeRequestResponse> response =
                service.joinSalon(request, applicant.getId());

        assertTrue(response.isSuccess());
        assertEquals(applicant.getId(), response.getData().senderId());
        assertEquals(owner.getId(), response.getData().receiverId());
        assertEquals(RequestType.User_Request, response.getData().requestType());

        ArgumentCaptor<EmploymentRequest> savedRequest =
                ArgumentCaptor.forClass(EmploymentRequest.class);
        verify(employmentRepo).save(savedRequest.capture());
        assertSame(applicant, savedRequest.getValue().getSender());
    }

    @Test
    void rejectRequestDoesNotTrustAnotherUsersIdentity() {
        User owner = user(1L, "owner@example.com", "owner", UserRole.OWNER);
        User attacker = user(3L, "attacker@example.com", "attacker", UserRole.USER);
        User applicant = user(2L, "applicant@example.com", "applicant", UserRole.USER);
        Salon salon = salon(10L, owner);
        EmploymentRequest employmentRequest = new EmploymentRequest();
        employmentRequest.setId(20L);
        employmentRequest.setSalon(salon);
        employmentRequest.setSender(applicant);
        employmentRequest.setReceiver(owner);
        employmentRequest.setRequestType(RequestType.User_Request);
        employmentRequest.setStatus(EmploymentRequestStatus.Requested);

        when(userRepo.findById(attacker.getId())).thenReturn(Optional.of(attacker));
        when(employmentRepo.findByIdForUpdate(employmentRequest.getId()))
                .thenReturn(Optional.of(employmentRequest));

        ApiResponse<EmployeeRequestResponse> response = service.rejectRequest(
                new RejectRequest(employmentRequest.getId(), "Not suitable"),
                attacker.getId()
        );

        assertFalse(response.isSuccess());
        assertEquals("Only the salon owner can reject this request", response.getMessage());
        assertEquals(EmploymentRequestStatus.Requested, employmentRequest.getStatus());
        verify(employmentRepo, never()).save(any());
    }

    @Test
    void acceptedLeaveClearsCapabilitiesFromThePreviousSalon() {
        User owner = user(1L, "owner@example.com", "owner", UserRole.OWNER);
        User employee = user(2L, "employee@example.com", "employee", UserRole.Employee);
        Salon salon = salon(10L, owner);
        salon.setCurrentEmployeesNumber(1L);
        employee.setSalon(salon);

        LeaveRequest leaveRequest = new LeaveRequest();
        leaveRequest.setId(30L);
        leaveRequest.setEmployee(employee);
        leaveRequest.setCurrentSalon(salon);
        leaveRequest.setStatus(LeaveRequestStatus.Requested);
        leaveRequest.setReason("Leaving");
        leaveRequest.setCreatedAt(LocalDateTime.now());

        when(userRepo.findById(owner.getId())).thenReturn(Optional.of(owner));
        when(leaveRequestRepo.findById(30L)).thenReturn(Optional.of(leaveRequest));

        var response = service.acceptLeaveRequest(30L, owner.getId());

        assertTrue(response.isSuccess());
        assertNull(employee.getSalon());
        assertEquals(UserRole.USER, employee.getRole());
        verify(capabilityRepo).deleteByEmployeeId(employee.getId());
    }

    private User user(long id, String email, String username, UserRole role) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        user.setUserName(username);
        user.setRole(role);
        return user;
    }

    private Salon salon(long id, User owner) {
        Salon salon = new Salon();
        salon.setId(id);
        salon.setName("Salon");
        salon.setOwner(owner);
        salon.setStatus(SalonStatus.Accepted);
        salon.setCurrentEmployeesNumber(0L);
        salon.setMaxEmployeesNumber(5L);
        return salon;
    }
}
