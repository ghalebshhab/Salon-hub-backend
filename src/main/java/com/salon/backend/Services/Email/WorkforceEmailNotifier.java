package com.salon.backend.Services.Email;

import com.salon.backend.DTOs.ApiResponse;
import com.salon.backend.DTOs.Salon.Employment.Join.EmployeeRequestResponse;
import com.salon.backend.DTOs.Salon.Employment.Leave.LeaveRequestResponse;
import com.salon.backend.DTOs.Salon.HiringPost.HiringApplicationResponse;
import com.salon.backend.Entities.salons.employment.RequestType;
import com.salon.backend.Entities.salons.Salon;
import com.salon.backend.Entities.users.User;
import com.salon.backend.Repositories.Salon.SalonRepo;
import com.salon.backend.Repositories.User.UserRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Emails for employment requests, leave requests and hiring applications.
 * Each method takes the service result and sends nothing when it failed.
 * Email problems never change the result of the action.
 */
@Component
@RequiredArgsConstructor
public class WorkforceEmailNotifier {

    private final EmailService emailService;
    private final EmailTemplates templates;
    private final UserRepo userRepo;
    private final SalonRepo salonRepo;

    // ------------------------------------------------------------ employment

    /** A join request or an invitation was sent: tell the receiver. */
    public void requestSent(ApiResponse<EmployeeRequestResponse> result) {
        EmployeeRequestResponse request = data(result);
        if (request == null) return;
        User sender = user(request.senderId());
        User receiver = user(request.receiverId());
        EmailTemplate template = request.requestType() == RequestType.Owner_Invite
                ? templates.invitationReceived(EmailNames.fullName(sender), request.salonName())
                : templates.joinRequestReceived(EmailNames.fullName(sender), request.salonName());
        send(receiver, template);
    }

    /** The receiver accepted: tell the sender. Covers hiring-linked requests too. */
    public void requestAccepted(ApiResponse<EmployeeRequestResponse> result) {
        EmployeeRequestResponse request = data(result);
        if (request == null) return;
        boolean invitation = request.requestType() == RequestType.Owner_Invite;
        send(user(request.senderId()), templates.employmentAccepted(
                EmailNames.fullName(user(request.receiverId())), request.salonName(), invitation,
                request.hiringPostTitle()));
    }

    /** The receiver rejected: tell the sender, with the reason. */
    public void requestRejected(ApiResponse<EmployeeRequestResponse> result) {
        EmployeeRequestResponse request = data(result);
        if (request == null) return;
        boolean invitation = request.requestType() == RequestType.Owner_Invite;
        send(user(request.senderId()), templates.employmentRejected(
                EmailNames.fullName(user(request.receiverId())), request.salonName(),
                invitation, request.hiringPostTitle(), request.rejectionReason()));
    }

    /** An employee asked to leave: tell the owner of the current salon. */
    public void leaveRequested(ApiResponse<LeaveRequestResponse> result) {
        LeaveRequestResponse leave = data(result);
        if (leave == null) return;
        send(owner(leave.currentSalonId()), templates.leaveRequested(
                leave.employeeName(), leave.currentSalonName(), leave.reason()));
    }

    /** The owner accepted a leave request: tell the employee. */
    public void leaveAccepted(ApiResponse<LeaveRequestResponse> result) {
        LeaveRequestResponse leave = data(result);
        if (leave == null) return;
        send(user(leave.employeeId()), templates.leaveAccepted(leave.currentSalonName()));
    }

    // ---------------------------------------------------------------- hiring
    // Accepting or rejecting an application goes through the employment
    // request, so requestAccepted/requestRejected send that email.

    public void applicationReceived(ApiResponse<HiringApplicationResponse> result) {
        HiringApplicationResponse application = data(result);
        if (application == null) return;
        send(owner(application.salonId()), templates.applicationReceived(
                application.applicantName(), application.hiringPostTitle(), application.salonName()));
    }

    // --------------------------------------------------------------- helpers

    private <D> D data(ApiResponse<D> result) {
        return result != null && result.isSuccess() ? result.getData() : null;
    }

    private void send(User recipient, EmailTemplate template) {
        if (recipient == null) return;
        emailService.queue(recipient.getEmail(), EmailNames.firstName(recipient), template);
    }

    private User user(Long id) {
        return id == null ? null : userRepo.findById(id).orElse(null);
    }

    private User owner(Long salonId) {
        Salon salon = salonId == null ? null : salonRepo.findById(salonId).orElse(null);
        return salon == null ? null : salon.getOwner();
    }
}
