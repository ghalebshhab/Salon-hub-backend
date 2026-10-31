package com.salon.backend.Controllers.Salons;


import com.salon.backend.DTOs.ApiResponse;
import com.salon.backend.DTOs.Salon.Employment.Join.*;
import com.salon.backend.DTOs.Salon.Employment.Leave.CreateLeaveRequest;
import com.salon.backend.DTOs.Salon.Employment.Leave.LeaveRequestResponse;
import com.salon.backend.DTOs.Salon.Employment.Leave.LeaveRequestPageResponse;
import com.salon.backend.Services.Salon.Employment.EmploymentService;
import com.salon.backend.Services.Auth.Security.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/employment")
@RequiredArgsConstructor
public class EmploymentController {

    private final EmploymentService employmentService;

    @GetMapping("/me")
    public ApiResponse<EmploymentRequestPageResponse> getMyEmploymentRequests(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return employmentService.getMyEmploymentRequests(
                currentUser.getId(),
                page,
                size
        );
    }

    @GetMapping("/my-salon")
    public ApiResponse<EmploymentRequestPageResponse> getOwnedSalonEmploymentRequests(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return employmentService.getOwnedSalonEmploymentRequests(
                currentUser.getId(),
                page,
                size
        );
    }

    @GetMapping("/candidates")
    public ApiResponse<EmploymentCandidatePageResponse> searchEmploymentCandidates(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return employmentService.searchEmploymentCandidates(
                currentUser.getId(),
                search,
                page,
                size
        );
    }

    @GetMapping("/leave/me")
    public ApiResponse<LeaveRequestPageResponse> getMyLeaveRequests(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return employmentService.getMyLeaveRequests(
                currentUser.getId(),
                page,
                size
        );
    }

    @GetMapping("/leave/my-salon")
    public ApiResponse<LeaveRequestPageResponse> getOwnedSalonLeaveRequests(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return employmentService.getOwnedSalonLeaveRequests(
                currentUser.getId(),
                page,
                size
        );
    }




    @PostMapping("/join")
    public ResponseEntity<ApiResponse<EmployeeRequestResponse>> joinSalon(
            @RequestBody JoinSalonRequest request,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {

        return ResponseEntity.ok(
                employmentService.joinSalon(
                        request,
                        currentUser.getId()
                )
        );
    }




    @PostMapping("/invite/{salonId}")
    public ResponseEntity<ApiResponse<EmployeeRequestResponse>> sendInvitation(
            @RequestBody SentInvitation request,
            @PathVariable Long salonId,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {

        return ResponseEntity.ok(
                employmentService.sentInvitation(
                        request,
                        salonId,
                        currentUser.getId()
                )
        );
    }




    @PostMapping("/accept")
    public ResponseEntity<ApiResponse<EmployeeRequestResponse>> acceptRequest(
            @RequestBody AcceptRequest request,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {

        return ResponseEntity.ok(
                employmentService.acceptRequest(
                        request,
                        currentUser.getId()
                )
        );
    }



    @PostMapping("/reject")
    public ResponseEntity<ApiResponse<EmployeeRequestResponse>> rejectRequest(
            @RequestBody RejectRequest request,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {

        return ResponseEntity.ok(
                employmentService.rejectRequest(
                        request,
                        currentUser.getId()
                )
        );
    }




    @PostMapping("/cancel")
    public ResponseEntity<ApiResponse<EmployeeRequestResponse>> cancelRequest(
            @RequestBody CancelRequest request,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {

        return ResponseEntity.ok(
                employmentService.cancelRequest(
                        request,
                        currentUser.getId()
                )
        );
    }



    @PostMapping("/leave/request")
    public ResponseEntity<ApiResponse<LeaveRequestResponse>> requestLeaveSalon(
            @RequestBody CreateLeaveRequest request,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {

        return ResponseEntity.ok(
                employmentService.createLeaveRequest(
                        request,
                        currentUser.getId()
                )
        );
    }




    @PostMapping("/leave/accept/{requestId}")
    public ResponseEntity<ApiResponse<LeaveRequestResponse>> acceptLeaveRequest(
            @PathVariable Long requestId,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {

        return ResponseEntity.ok(
                employmentService.acceptLeaveRequest(
                        requestId,
                        currentUser.getId()
                )
        );
    }




    @PostMapping("/leave/cancel/{requestId}")
    public ResponseEntity<ApiResponse<LeaveRequestResponse>> cancelLeaveRequest(
            @PathVariable Long requestId,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {

        return ResponseEntity.ok(
                employmentService.cancelLeaveRequest(
                        requestId,
                        currentUser.getId()
                )
        );
    }




    @PostMapping("/invitation/accept-and-leave")
    public ResponseEntity<ApiResponse<EmployeeRequestResponse>>
    acceptInvitationAndRequestLeave(
            @RequestBody AcceptRequest request,
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @RequestParam(required = false) String reason) {

        return ResponseEntity.ok(
                employmentService.acceptInvitationAndRequestLeave(
                        request.requestId(),
                        currentUser.getId(),
                        reason
                )
        );
    }
}
