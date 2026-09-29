package com.salon.backend.Services.Salon.Employment;

import com.salon.backend.DTOs.ApiResponse;
import com.salon.backend.DTOs.Salon.Employment.Join.*;
import com.salon.backend.DTOs.Salon.Employment.Leave.CreateLeaveRequest;
import com.salon.backend.DTOs.Salon.Employment.Leave.LeaveRequestResponse;

public interface EmploymentService {
ApiResponse<EmployeeRequestResponse> joinSalon(JoinSalonRequest request, Long senderId);

ApiResponse<EmployeeRequestResponse> sentInvitation(SentInvitation sentInvitation, Long salonId, Long senderId);

    ApiResponse<EmployeeRequestResponse> acceptRequest(
            AcceptRequest acceptRequest,
            Long currentUserId
    );

    ApiResponse<EmployeeRequestResponse> rejectRequest(
            RejectRequest rejectRequest,
            Long currentUserId
    );
    ApiResponse<EmployeeRequestResponse> cancelRequest(
            CancelRequest cancelRequest,
            Long currentUserId
    );

    ApiResponse<LeaveRequestResponse> createLeaveRequest(CreateLeaveRequest createLeaveRequest, Long currentUserId);

    ApiResponse<LeaveRequestResponse> acceptLeaveRequest(
            Long requestId,
            Long currentUserId
    );

    ApiResponse<LeaveRequestResponse> cancelLeaveRequest(Long requestId, Long currentUserId);

    ApiResponse<EmployeeRequestResponse> acceptInvitationAndRequestLeave(
            Long requestId,
            Long currentUserId,
            String reason
    );


}
