package com.salon.backend.Services.Salon.Employment;

import com.salon.backend.Services.Email.WorkforceEmailNotifier;
import com.salon.backend.DTOs.ApiResponse;
import com.salon.backend.DTOs.Salon.Employment.Join.*;
import com.salon.backend.DTOs.Salon.Employment.Leave.CreateLeaveRequest;
import com.salon.backend.DTOs.Salon.Employment.Leave.LeaveRequestPageResponse;
import com.salon.backend.DTOs.Salon.Employment.Leave.LeaveRequestResponse;
import com.salon.backend.Entities.salons.Salon;
import com.salon.backend.Entities.salons.SalonStatus;
import com.salon.backend.Entities.salons.employment.*;
import com.salon.backend.Entities.salons.hiringposts.HiringPost;
import com.salon.backend.Entities.salons.hiringposts.HiringPostStatus;
import com.salon.backend.Entities.salons.hiringposts.HiringPostClosureReason;
import com.salon.backend.Entities.users.User;
import com.salon.backend.Entities.users.UserRole;
import com.salon.backend.Entities.users.UserStatus;
import com.salon.backend.Repositories.Salon.EmploymentRepo;
import com.salon.backend.Repositories.Salon.EmployeeServiceCapabilityRepo;
import com.salon.backend.Repositories.Salon.EmployeeScheduleRepo;
import com.salon.backend.Repositories.Salon.LeaveRequestRepo;
import com.salon.backend.Repositories.Salon.HiringPostRepo;
import com.salon.backend.Repositories.Salon.SalonRepo;
import com.salon.backend.Repositories.User.UserRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;
import java.util.List;


@Service
@RequiredArgsConstructor
public class EmploymentServiceImpl implements EmploymentService {
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;
    private final SalonRepo salonRepo;
    private final UserRepo userRepo;
    private final EmploymentRepo employmentRepo;
    private final LeaveRequestRepo  leaveRequestRepo;
    private final HiringPostRepo hiringPostRepo;
    private final EmployeeServiceCapabilityRepo capabilityRepo;
    private final EmployeeScheduleRepo scheduleRepo;
    private final WorkforceEmailNotifier workforceEmails;
    @Override
    public ApiResponse<EmployeeRequestResponse> joinSalon(JoinSalonRequest request, Long senderId) {
        var result = joinSalonInternal(request, senderId);
        workforceEmails.requestSent(result);
        return result;
    }

    private ApiResponse<EmployeeRequestResponse> joinSalonInternal(JoinSalonRequest request, Long senderId) {
        Optional<Salon> optionalsalon=salonRepo.findById(request.getSalonId());
        Optional<User> optionalUser=userRepo.findById(senderId);

        if(optionalsalon.isEmpty()){
            return ApiResponse.error("Salon not found");
        }
        if(optionalUser.isEmpty()){
            return ApiResponse.error("Sender not found");
        }
        User user=optionalUser.get();
        Salon salon=optionalsalon.get();


        if(user.getRole()!=UserRole.USER){
            return ApiResponse.error("Your role dont allow you to join the salon as employee");
        }
        if(salon.getStatus() != SalonStatus.Accepted){
            return ApiResponse.error("Salon is not active , you can not join ");
        }
        if(salon.getCurrentEmployeesNumber()>= salon.getMaxEmployeesNumber()){
            return ApiResponse.error("Salon is already full");
        }

        Optional<EmploymentRequest> existingRequest =
                employmentRepo.findPendingRequest(
                        salon,
                        user,
                        EmploymentRequestStatus.Requested
                );

        if (existingRequest.isPresent()) {

            EmploymentRequest emprequest = existingRequest.get();

            if (emprequest.getRequestType() == RequestType.Owner_Invite) {
                return ApiResponse.error(
                        "This salon has already invited you."
                );
            }

            return ApiResponse.error(
                    "You already have a pending request for this salon."
            );
        }

        EmploymentRequest employmentRequest=new EmploymentRequest(
        );
        employmentRequest.setSalon(salon);
        employmentRequest.setRequestType(RequestType.User_Request);
        employmentRequest.setRequestSource(EmploymentRequestSource.SALON_PROFILE);
        employmentRequest.setReceiver(salon.getOwner());
        employmentRequest.setSender(user);
        employmentRequest.setStatus(EmploymentRequestStatus.Requested);
        employmentRequest.setCreatedAt(LocalDateTime.now());

        employmentRepo.save(employmentRequest);

        return ApiResponse.success("Request to this salon Successfully done", mapEmploymentRequest(employmentRequest));
    }

//    This  accept request method is bidirectional > so the user can accept the invite from the salon owner  ,
//    and the salon owwner  can accept the user request  .
@Override
@Transactional
public ApiResponse<EmployeeRequestResponse> acceptRequest(
        AcceptRequest acceptRequest,
        Long currentUserId) {
    var result = acceptRequestInternal(acceptRequest, currentUserId);
    workforceEmails.requestAccepted(result);
    return result;
}

private ApiResponse<EmployeeRequestResponse> acceptRequestInternal(
        AcceptRequest acceptRequest,
        Long currentUserId) {

    Optional<User> optionalCurrentUser = userRepo.findById(currentUserId);
    if (optionalCurrentUser.isEmpty()) {
        return ApiResponse.error("User not found");
    }
    User currentUser = optionalCurrentUser.get();

    Optional<EmploymentRequest> optionalRequest =
            employmentRepo.findByIdForUpdate(acceptRequest.requestId());

    if (optionalRequest.isEmpty()) {
        return ApiResponse.error(
                "This Employment Request is not found"
        );
    }

    EmploymentRequest employmentRequest =
            optionalRequest.get();

    // Request must still be pending
    if (employmentRequest.getStatus()
            != EmploymentRequestStatus.Requested) {

        return ApiResponse.error(
                "This Employment Request is no longer pending"
        );
    }

    Salon targetSalon = employmentRequest.getSalon();

    if (targetSalon == null) {
        return ApiResponse.error(
                "Salon associated with this request was not found"
        );
    }

    // Target salon must be active
    if (targetSalon.getStatus() != SalonStatus.Accepted) {
        return ApiResponse.error(
                "Salon is not active, you cannot accept this request"
        );
    }

    User employee;

    /*
     * ============================================================
     * 1. USER REQUEST
     * ============================================================
     *
     * User sent the request to the salon owner.
     * Therefore:
     *
     * sender   = employee
     * receiver = salon owner
     *
     * Only the salon owner can accept it.
     */
    if (employmentRequest.getRequestType()
            == RequestType.User_Request) {

        if (!Objects.equals(targetSalon.getOwner().getId(), currentUser.getId())) {

            return ApiResponse.error(
                    "Only the salon owner can accept this request"
            );
        }

        employee = employmentRequest.getSender();
    }

    /*
     * ============================================================
     * 2. OWNER INVITATION
     * ============================================================
     *
     * Owner sent the invitation to the user.
     * Therefore:
     *
     * sender   = salon owner
     * receiver = employee
     *
     * Only the invited user can accept it.
     */
    else if (employmentRequest.getRequestType()
            == RequestType.Owner_Invite) {

        if (!Objects.equals(employmentRequest.getReceiver().getId(), currentUser.getId())) {

            return ApiResponse.error(
                    "Only the invited user can accept this invitation"
            );
        }

        employee = employmentRequest.getReceiver();
    }

    else {
        return ApiResponse.error(
                "Invalid employment request type"
        );
    }

    /*
     * ============================================================
     * 3. EMPLOYEE VALIDATION
     * ============================================================
     */

    if (employee.getRole() != UserRole.USER
            && employee.getRole() != UserRole.Employee) {

        return ApiResponse.error(
                "This user cannot join the salon"
        );
    }

    /*
     * ============================================================
     * 4. TARGET SALON CAPACITY
     * ============================================================
     *
     * Check this before joining.
     */
    if (targetSalon.getCurrentEmployeesNumber()
            >= targetSalon.getMaxEmployeesNumber()) {

        return ApiResponse.error(
                "Salon is already full"
        );
    }

    /*
     * ============================================================
     * 5. USER ALREADY WORKS IN A SALON
     * ============================================================
     */

    if (employee.getSalon() != null) {

        /*
         * The user cannot directly move.
         *
         * We first mark the employment request as:
         *
         * Accepted_Pending_Leave
         *
         * Then create a LeaveRequest.
         *
         * The current salon owner must accept the leave.
         */

        if (leaveRequestRepo.existsByEmployeeAndStatus(
                employee,
                LeaveRequestStatus.Requested)) {

            return ApiResponse.error(
                    "This employee already has a pending leave request"
            );
        }

        LeaveRequest leaveRequest = new LeaveRequest();

        leaveRequest.setEmployee(employee);

        // Current salon
        leaveRequest.setCurrentSalon(
                employee.getSalon()
        );

        // Salon the employee wants to join
        leaveRequest.setTargetSalon(
                targetSalon
        );

        // Connect leave request with this employment request
        leaveRequest.setEmploymentRequest(
                employmentRequest
        );

        // Default reason
        leaveRequest.setReason(
                "Another salon needs me."
        );

        leaveRequest.setStatus(
                LeaveRequestStatus.Requested
        );

        leaveRequest.setCreatedAt(
                LocalDateTime.now()
        );

        /*
         * Invitation/request was accepted logically,
         * but the employee has not joined the target salon yet.
         */
        employmentRequest.setStatus(
                EmploymentRequestStatus.Accepted_Pending_Leave
        );

        leaveRequestRepo.save(leaveRequest);
        employmentRepo.save(employmentRequest);

        return ApiResponse.success(
                "Request accepted. A leave request has been sent to your current salon owner.",
                mapEmploymentRequest(employmentRequest)
        );
    }

    /*
     * ============================================================
     * 6. USER DOES NOT CURRENTLY WORK IN A SALON
     * ============================================================
     *
     * Therefore the user can join immediately.
     */

    employee.setSalon(targetSalon);
    employee.setRole(UserRole.Employee);

    targetSalon.setCurrentEmployeesNumber(
            targetSalon.getCurrentEmployeesNumber() + 1
    );

    employmentRequest.setStatus(
            EmploymentRequestStatus.Accepted
    );

    employmentRepo.save(employmentRequest);
    salonRepo.save(targetSalon);
    userRepo.save(employee);

    return ApiResponse.success(
            "Employment Request Accepted successfully",
            mapEmploymentRequest(employmentRequest)
    );
}
    @Override
    public ApiResponse<EmployeeRequestResponse> sentInvitation(SentInvitation sentInvitation, Long salonId, Long senderId) {
        var result = sentInvitationInternal(sentInvitation, salonId, senderId);
        workforceEmails.requestSent(result);
        return result;
    }

    private ApiResponse<EmployeeRequestResponse> sentInvitationInternal(SentInvitation sentInvitation, Long salonId, Long senderId) {
     Optional<Salon> optionalsalon=salonRepo.findById(salonId);
     Optional<User> optionalSender=userRepo.findById(senderId);
     Optional<User> optionalReciever=userRepo.findById(sentInvitation.getUserId());

     if(optionalsalon.isEmpty()){
         return ApiResponse.error("Salon not found");
     }

     if(optionalSender.isEmpty()){
         return ApiResponse.error("Sender not found");
     }

     if(optionalReciever.isEmpty()){
         return ApiResponse.error("Receiver not found");
     }

     Salon salon=optionalsalon.get();
     User sender=optionalSender.get();
     User receiver=optionalReciever.get();

     if (salon.getStatus() != SalonStatus.Accepted) {
         return ApiResponse.error("Salon is not active.");
     }

        if (!Objects.equals(salon.getOwner().getId(), sender.getId())) {
            return ApiResponse.error(
                    "You are not the owner of this salon"
            );
        }

     if (Objects.equals(sender.getId(), receiver.getId())) {
         return ApiResponse.error("You can not invite yourself");
     }

     if (receiver.getStatus() != UserStatus.Active) {
         return ApiResponse.error("Only active users can be invited to a salon.");
     }

     if (!receiver.isCareerProfileEnabled()) {
         return ApiResponse.error("This user must enable their career profile before they can be invited.");
     }

     if (receiver.getRole() != UserRole.USER && receiver.getRole() != UserRole.Employee) {
         return ApiResponse.error("Only career users or employees can be invited to a salon.");
     }

     if (receiver.getSalon() != null
             && Objects.equals(receiver.getSalon().getId(), salon.getId())) {
         return ApiResponse.error("This user already works at your salon.");
     }

        Optional<EmploymentRequest> existingRequest =
                employmentRepo.findPendingRequest(
                        salon,
                        receiver,
                        EmploymentRequestStatus.Requested
                );

        if (existingRequest.isPresent()) {

            EmploymentRequest request = existingRequest.get();

            if (request.getRequestType() == RequestType.User_Request) {
                return ApiResponse.error(
                        "This user has already requested to join your salon."
                );
            }

            return ApiResponse.error(
                    "This user already has a pending invitation."
            );
        }

     if (salon.getCurrentEmployeesNumber() >= salon.getMaxEmployeesNumber()) {
         return ApiResponse.error("Salon is already full");
     }

     EmploymentRequest employmentRequest=new EmploymentRequest();
     employmentRequest.setSalon(salon);
     employmentRequest.setRequestType(RequestType.Owner_Invite);
     employmentRequest.setRequestSource(EmploymentRequestSource.SALON_PROFILE);
     employmentRequest.setReceiver(receiver);
     employmentRequest.setSender(sender);
     employmentRequest.setStatus(EmploymentRequestStatus.Requested);
     employmentRequest.setCreatedAt(LocalDateTime.now());
     employmentRepo.save(employmentRequest);

        return ApiResponse.success("Invitation sent successfully", mapEmploymentRequest(employmentRequest));
    }

    @Override
    @Transactional
    public ApiResponse<EmployeeRequestResponse> rejectRequest(
            RejectRequest rejectRequest,
            Long currentUserId) {
        var result = rejectRequestInternal(rejectRequest, currentUserId);
        workforceEmails.requestRejected(result);
        return result;
    }

    private ApiResponse<EmployeeRequestResponse> rejectRequestInternal(
            RejectRequest rejectRequest,
            Long currentUserId) {

        Optional<User> optionalCurrentUser = userRepo.findById(currentUserId);
        if (optionalCurrentUser.isEmpty()) {
            return ApiResponse.error("User not found");
        }
        User currentUser = optionalCurrentUser.get();

        Optional<EmploymentRequest> optionalRequest =
                employmentRepo.findByIdForUpdate(rejectRequest.getRequestId());

        if (optionalRequest.isEmpty()) {
            return ApiResponse.error(
                    "This Employment Request is not found"
            );
        }

        EmploymentRequest employmentRequest =
                optionalRequest.get();

        /*
         * Request must still be pending.
         *
         * We do not allow rejection after:
         *
         * Accepted
         * Accepted_Pending_Leave
         * Cancelled
         * Rejected
         */
        if (employmentRequest.getStatus()
                != EmploymentRequestStatus.Requested) {

            return ApiResponse.error(
                    "This Employment Request is no longer pending"
            );
        }

        Salon salon = employmentRequest.getSalon();

        if (salon == null) {
            return ApiResponse.error(
                    "Salon associated with this request was not found"
            );
        }

        User sender = employmentRequest.getSender();
        User receiver = employmentRequest.getReceiver();

        /*
         * ============================================================
         * USER REQUEST
         * ============================================================
         *
         * sender   = employee
         * receiver = salon owner
         *
         * Only the salon owner should reject the request.
         */
        if (employmentRequest.getRequestType()
                == RequestType.User_Request) {

            if (!Objects.equals(salon.getOwner().getId(), currentUser.getId())) {

                return ApiResponse.error(
                        "Only the salon owner can reject this request"
                );
            }
        }

        /*
         * ============================================================
         * OWNER INVITATION
         * ============================================================
         *
         * sender   = salon owner
         * receiver = invited user
         *
         * Only the invited user should decline it.
         */
        else if (employmentRequest.getRequestType()
                == RequestType.Owner_Invite) {

            if (!Objects.equals(receiver.getId(), currentUser.getId())) {

                return ApiResponse.error(
                        "Only the invited user can reject this invitation"
                );
            }
        }

        else {
            return ApiResponse.error(
                    "Invalid employment request type"
            );
        }

        /*
         * Reject the request
         */
        employmentRequest.setStatus(
                EmploymentRequestStatus.Rejected
        );

        /*
         * Save rejection reason if the entity contains it.
         */
        if (rejectRequest.getRejectionReason() != null
                && !rejectRequest.getRejectionReason().isBlank()) {

            employmentRequest.setRejectionReason(
                    rejectRequest.getRejectionReason()
            );
        }

        employmentRepo.save(employmentRequest);

        return ApiResponse.success(
                "Employment Request rejected successfully",
                mapEmploymentRequest(employmentRequest)
        );
    }

    @Override
    @Transactional
    public ApiResponse<EmployeeRequestResponse> cancelRequest(CancelRequest cancelRequest, Long currentUserId) {
        Optional<User> optionalCurrentUser = userRepo.findById(currentUserId);
        if (optionalCurrentUser.isEmpty()) {
            return ApiResponse.error("User not found");
        }
        User currentUser = optionalCurrentUser.get();
        Optional<EmploymentRequest> empreq=employmentRepo.findByIdForUpdate(cancelRequest.requestId());
        if (empreq.isEmpty()) {
            return ApiResponse.error("This Employment Request is not found");
        }
        EmploymentRequest employmentRequest = empreq.get();
        if (employmentRequest.getStatus()!=EmploymentRequestStatus.Requested) {
            return ApiResponse.error("This Employment Request is no longer pending");
        }
        User sender = employmentRequest.getSender();
        if (!Objects.equals(sender.getId(), currentUser.getId())) {
            return ApiResponse.error("You are not the sender of this request");
        }
        employmentRequest.setStatus(EmploymentRequestStatus.Cancelled);
        employmentRepo.save(employmentRequest);
        return ApiResponse.success("The request is cancelled successfully", mapEmploymentRequest(employmentRequest));
    }

    @Override
    public ApiResponse<LeaveRequestResponse> createLeaveRequest(
            CreateLeaveRequest request,
            Long currentUserId) {
        var result = createLeaveRequestInternal(request, currentUserId);
        workforceEmails.leaveRequested(result);
        return result;
    }

    private ApiResponse<LeaveRequestResponse> createLeaveRequestInternal(
            CreateLeaveRequest request,
            Long currentUserId) {

        Optional<User> optionalEmployee = userRepo.findById(currentUserId);
        if (optionalEmployee.isEmpty()) {
            return ApiResponse.error("User not found");
        }
        User employee = optionalEmployee.get();

        if (employee.getRole() != UserRole.Employee) {
            return ApiResponse.error(
                    "You are not currently an employee"
            );
        }

        Salon currentSalon = employee.getSalon();

        if (currentSalon == null) {
            return ApiResponse.error(
                    "You are not currently working in a salon"
            );
        }

        if (leaveRequestRepo.existsByEmployeeAndStatus(
                employee,
                LeaveRequestStatus.Requested)) {

            return ApiResponse.error(
                    "You already have a pending leave request"
            );
        }

        LeaveRequest leaveRequest = new LeaveRequest();

        leaveRequest.setEmployee(employee);
        leaveRequest.setCurrentSalon(currentSalon);
        leaveRequest.setTargetSalon(null);
        leaveRequest.setEmploymentRequest(null);
        leaveRequest.setReason(request.getReason());
        leaveRequest.setStatus(LeaveRequestStatus.Requested);
        leaveRequest.setCreatedAt(LocalDateTime.now());

        leaveRequestRepo.save(leaveRequest);

        return ApiResponse.success(
                "Leave request sent successfully",
                mapLeaveRequest(leaveRequest)
        );
    }


    @Override
    @Transactional
    public ApiResponse<LeaveRequestResponse> acceptLeaveRequest(
            Long requestId,
            Long currentUserId) {
        var result = acceptLeaveRequestInternal(requestId, currentUserId);
        workforceEmails.leaveAccepted(result);
        return result;
    }

    private ApiResponse<LeaveRequestResponse> acceptLeaveRequestInternal(
            Long requestId,
            Long currentUserId) {

        Optional<User> optionalCurrentUser = userRepo.findById(currentUserId);
        if (optionalCurrentUser.isEmpty()) {
            return ApiResponse.error("User not found");
        }
        User currentUser = optionalCurrentUser.get();

        Optional<LeaveRequest> optionalRequest =
                leaveRequestRepo.findById(requestId);

        if (optionalRequest.isEmpty()) {
            return ApiResponse.error(
                    "Leave request not found"
            );
        }

        LeaveRequest leaveRequest = optionalRequest.get();

        if (leaveRequest.getStatus()
                != LeaveRequestStatus.Requested) {

            return ApiResponse.error(
                    "This leave request is no longer pending"
            );
        }

        Salon currentSalon = leaveRequest.getCurrentSalon();

        if (!Objects.equals(currentSalon.getOwner().getId(), currentUser.getId())) {
            return ApiResponse.error(
                    "Only the salon owner can accept this leave request"
            );
        }

        User employee = leaveRequest.getEmployee();

        if (employee.getSalon() == null
                || !employee.getSalon().getId()
                .equals(currentSalon.getId())) {

            return ApiResponse.error(
                    "This employee is no longer working in this salon"
            );
        }

        Salon targetSalon = leaveRequest.getTargetSalon();

        // Normal leave
        if (targetSalon == null) {

            capabilityRepo.deleteByEmployeeId(employee.getId());
            scheduleRepo.deleteByEmployeeId(employee.getId());
            employee.setSalon(null);
            employee.setRole(UserRole.USER);

            if (currentSalon.getCurrentEmployeesNumber() > 0) {
                currentSalon.setCurrentEmployeesNumber(
                        currentSalon.getCurrentEmployeesNumber() - 1
                );
            }

            leaveRequest.setStatus(
                    LeaveRequestStatus.Accepted
            );

            userRepo.save(employee);
            salonRepo.save(currentSalon);
            leaveRequestRepo.save(leaveRequest);

            return ApiResponse.success(
                    "Leave request accepted successfully",
                    mapLeaveRequest(leaveRequest)
            );
        }

        // Transfer leave
        if (targetSalon.getCurrentEmployeesNumber()
                >= targetSalon.getMaxEmployeesNumber()) {

            return ApiResponse.error(
                    "The target salon is already full"
            );
        }

        EmploymentRequest employmentRequest =
                leaveRequest.getEmploymentRequest();

        if (employmentRequest == null
                || employmentRequest.getStatus()
                != EmploymentRequestStatus.Accepted_Pending_Leave) {

            return ApiResponse.error(
                    "The employment request is not ready for transfer"
            );
        }

        // Remove from old salon
        capabilityRepo.deleteByEmployeeId(employee.getId());
        scheduleRepo.deleteByEmployeeId(employee.getId());
        employee.setSalon(null);

        if (currentSalon.getCurrentEmployeesNumber() > 0) {
            currentSalon.setCurrentEmployeesNumber(
                    currentSalon.getCurrentEmployeesNumber() - 1
            );
        }

        // Add to new salon
        employee.setSalon(targetSalon);
        employee.setRole(UserRole.Employee);

        targetSalon.setCurrentEmployeesNumber(
                targetSalon.getCurrentEmployeesNumber() + 1
        );

        employmentRequest.setStatus(
                EmploymentRequestStatus.Accepted
        );

        leaveRequest.setStatus(
                LeaveRequestStatus.Accepted
        );

        userRepo.save(employee);
        salonRepo.save(currentSalon);
        salonRepo.save(targetSalon);
        employmentRepo.save(employmentRequest);
        leaveRequestRepo.save(leaveRequest);

        return ApiResponse.success(
                "Employee successfully transferred to the new salon",
                mapLeaveRequest(leaveRequest)
        );
    }
    @Override
    @Transactional
    public ApiResponse<LeaveRequestResponse> cancelLeaveRequest(
            Long requestId,
            Long currentUserId) {

        Optional<User> optionalCurrentUser = userRepo.findById(currentUserId);
        if (optionalCurrentUser.isEmpty()) {
            return ApiResponse.error("User not found");
        }
        User currentUser = optionalCurrentUser.get();

        Optional<LeaveRequest> optionalRequest =
                leaveRequestRepo.findById(requestId);

        if (optionalRequest.isEmpty()) {
            return ApiResponse.error(
                    "Leave request not found"
            );
        }

        LeaveRequest leaveRequest = optionalRequest.get();

        if (leaveRequest.getStatus()
                != LeaveRequestStatus.Requested) {

            return ApiResponse.error(
                    "This leave request is no longer pending"
            );
        }

        User employee = leaveRequest.getEmployee();

        if (!Objects.equals(employee.getId(), currentUser.getId())) {
            return ApiResponse.error(
                    "Only the employee that sent this leave request can cancel it"
            );
        }

        leaveRequest.setStatus(
                LeaveRequestStatus.Cancelled
        );

        // If this was part of a transfer
        EmploymentRequest employmentRequest =
                leaveRequest.getEmploymentRequest();

        if (employmentRequest != null
                && employmentRequest.getStatus()
                == EmploymentRequestStatus.Accepted_Pending_Leave) {

            if (employmentRequest.getRequestSource()
                    == EmploymentRequestSource.HIRING_POST
                    && employmentRequest.getHiringPost() != null) {

                employmentRequest.setStatus(
                        EmploymentRequestStatus.Cancelled
                );

                HiringPost hiringPost = hiringPostRepo.findByIdForUpdate(
                        employmentRequest.getHiringPost().getId()
                ).orElse(employmentRequest.getHiringPost());
                hiringPost.setNumOfPositions(
                        hiringPost.getNumOfPositions() + 1
                );

                if (hiringPost.getStatus() == HiringPostStatus.Closed
                        && hiringPost.getClosureReason()
                        == HiringPostClosureReason.POSITIONS_FILLED
                        && hiringPost.getExpiresAt().isAfter(LocalDateTime.now())) {
                    hiringPost.setStatus(HiringPostStatus.Open);
                    hiringPost.setClosureReason(null);
                }

                hiringPostRepo.save(hiringPost);
            } else {
                employmentRequest.setStatus(
                        EmploymentRequestStatus.Requested
                );
            }

            employmentRepo.save(employmentRequest);
        }

        leaveRequestRepo.save(leaveRequest);

        return ApiResponse.success(
                "Leave request cancelled successfully",
                mapLeaveRequest(leaveRequest)
        );
    }
    @Override
    @Transactional
    public ApiResponse<EmployeeRequestResponse> acceptInvitationAndRequestLeave(
            Long requestId,
            Long currentUserId,
            String reason) {
        var result = acceptInvitationAndRequestLeaveInternal(requestId, currentUserId, reason);
        workforceEmails.requestAccepted(result);
        return result;
    }

    private ApiResponse<EmployeeRequestResponse> acceptInvitationAndRequestLeaveInternal(
            Long requestId,
            Long currentUserId,
            String reason) {

        Optional<User> optionalCurrentUser = userRepo.findById(currentUserId);
        if (optionalCurrentUser.isEmpty()) {
            return ApiResponse.error("User not found");
        }
        User currentUser = optionalCurrentUser.get();

        Optional<EmploymentRequest> optionalRequest =
                employmentRepo.findByIdForUpdate(requestId);

        if (optionalRequest.isEmpty()) {
            return ApiResponse.error(
                    "Employment request not found"
            );
        }

        EmploymentRequest employmentRequest =
                optionalRequest.get();

        if (employmentRequest.getStatus()
                != EmploymentRequestStatus.Requested) {

            return ApiResponse.error(
                    "This invitation is no longer pending"
            );
        }

        if (employmentRequest.getRequestType()
                != RequestType.Owner_Invite) {

            return ApiResponse.error(
                    "This request is not an invitation"
            );
        }

        User receiver = employmentRequest.getReceiver();

        if (!Objects.equals(receiver.getId(), currentUser.getId())) {
            return ApiResponse.error(
                    "You are not allowed to accept this invitation"
            );
        }

        Salon targetSalon = employmentRequest.getSalon();

        if (targetSalon.getStatus() != SalonStatus.Accepted) {
            return ApiResponse.error(
                    "The salon is not active"
            );
        }

        if (targetSalon.getCurrentEmployeesNumber()
                >= targetSalon.getMaxEmployeesNumber()) {

            return ApiResponse.error(
                    "The salon is already full"
            );
        }

        // User has no current salon
        if (receiver.getSalon() == null) {

            receiver.setSalon(targetSalon);
            receiver.setRole(UserRole.Employee);

            targetSalon.setCurrentEmployeesNumber(
                    targetSalon.getCurrentEmployeesNumber() + 1
            );

            employmentRequest.setStatus(
                    EmploymentRequestStatus.Accepted
            );

            userRepo.save(receiver);
            salonRepo.save(targetSalon);
            employmentRepo.save(employmentRequest);

            return ApiResponse.success(
                    "Invitation accepted successfully",
                    mapEmploymentRequest(employmentRequest)
            );
        }

        // User already works somewhere
        if (leaveRequestRepo.existsByEmployeeAndStatus(
                receiver,
                LeaveRequestStatus.Requested)) {

            return ApiResponse.error(
                    "You already have a pending leave request"
            );
        }

        if (leaveRequestRepo.existsByEmployeeAndStatus(
                receiver,
                LeaveRequestStatus.Accepted)) {

            return ApiResponse.error(
                    "You already have a completed leave request"
            );
        }

        LeaveRequest leaveRequest = new LeaveRequest();

        leaveRequest.setEmployee(receiver);
        leaveRequest.setCurrentSalon(receiver.getSalon());
        leaveRequest.setTargetSalon(targetSalon);
        leaveRequest.setEmploymentRequest(employmentRequest);

        if (reason == null || reason.isBlank()) {
            reason = "Another salon needs me.";
        }

        leaveRequest.setReason(reason);

        leaveRequest.setStatus(
                LeaveRequestStatus.Requested
        );

        leaveRequest.setCreatedAt(LocalDateTime.now());

        employmentRequest.setStatus(
                EmploymentRequestStatus.Accepted_Pending_Leave
        );

        leaveRequestRepo.save(leaveRequest);
        employmentRepo.save(employmentRequest);

        return ApiResponse.success(
                "Invitation accepted. A leave request has been sent to your current salon owner.",
                mapEmploymentRequest(employmentRequest)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<EmploymentRequestPageResponse> getMyEmploymentRequests(
            Long currentUserId,
            Integer page,
            Integer size
    ) {
        String validation = validatePage(page, size);
        if (validation != null) {
            return ApiResponse.error(validation);
        }
        if (!userRepo.existsById(currentUserId)) {
            return ApiResponse.error("Authenticated user was not found.");
        }

        Page<EmploymentRequest> requests = employmentRepo.findBySenderIdOrReceiverId(
                currentUserId,
                currentUserId,
                pageRequest(page, size)
        );
        return ApiResponse.success(
                "Employment requests fetched successfully.",
                mapEmploymentPage(requests)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<EmploymentRequestPageResponse> getOwnedSalonEmploymentRequests(
            Long currentUserId,
            Integer page,
            Integer size
    ) {
        String validation = validatePage(page, size);
        if (validation != null) {
            return ApiResponse.error(validation);
        }
        Salon salon = findActiveOwnedSalon(currentUserId);
        if (salon == null) {
            return ApiResponse.error("An active salon owned by you is required.");
        }

        Page<EmploymentRequest> requests = employmentRepo.findBySalonId(
                salon.getId(),
                pageRequest(page, size)
        );
        return ApiResponse.success(
                "Salon employment requests fetched successfully.",
                mapEmploymentPage(requests)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<LeaveRequestPageResponse> getMyLeaveRequests(
            Long currentUserId,
            Integer page,
            Integer size
    ) {
        String validation = validatePage(page, size);
        if (validation != null) {
            return ApiResponse.error(validation);
        }
        if (!userRepo.existsById(currentUserId)) {
            return ApiResponse.error("Authenticated user was not found.");
        }

        Page<LeaveRequest> requests = leaveRequestRepo.findByEmployeeId(
                currentUserId,
                pageRequest(page, size)
        );
        return ApiResponse.success(
                "Leave requests fetched successfully.",
                mapLeavePage(requests)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<LeaveRequestPageResponse> getOwnedSalonLeaveRequests(
            Long currentUserId,
            Integer page,
            Integer size
    ) {
        String validation = validatePage(page, size);
        if (validation != null) {
            return ApiResponse.error(validation);
        }
        Salon salon = findActiveOwnedSalon(currentUserId);
        if (salon == null) {
            return ApiResponse.error("An active salon owned by you is required.");
        }

        Page<LeaveRequest> requests = leaveRequestRepo.findByCurrentSalonId(
                salon.getId(),
                pageRequest(page, size)
        );
        return ApiResponse.success(
                "Salon leave requests fetched successfully.",
                mapLeavePage(requests)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<EmploymentCandidatePageResponse> searchEmploymentCandidates(
            Long currentUserId,
            String search,
            Integer page,
            Integer size
    ) {
        String validation = validatePage(page, size);
        if (validation != null) {
            return ApiResponse.error(validation);
        }
        Salon salon = findActiveOwnedSalon(currentUserId);
        if (salon == null) {
            return ApiResponse.error("An active salon owned by you is required.");
        }

        String normalizedSearch = search == null ? "" : search.trim();
        if (normalizedSearch.length() > 100) {
            return ApiResponse.error("Candidate search cannot exceed 100 characters.");
        }

        Page<User> candidates = userRepo.searchCareerCandidates(
                currentUserId,
                salon.getId(),
                UserStatus.Active,
                List.of(UserRole.USER, UserRole.Employee),
                normalizedSearch,
                PageRequest.of(
                        page == null ? 0 : page,
                        size == null ? DEFAULT_PAGE_SIZE : size,
                        Sort.by(
                                Sort.Order.asc("firstName"),
                                Sort.Order.asc("lastName"),
                                Sort.Order.asc("id")
                        )
                )
        );
        return ApiResponse.success(
                "Career candidates fetched successfully.",
                new EmploymentCandidatePageResponse(
                        candidates.getContent().stream()
                                .map(this::mapCandidate)
                                .toList(),
                        candidates.getNumber(),
                        candidates.getSize(),
                        candidates.getTotalElements(),
                        candidates.getTotalPages(),
                        candidates.isFirst(),
                        candidates.isLast()
                )
        );
    }

    private Salon findActiveOwnedSalon(Long currentUserId) {
        if (currentUserId == null) {
            return null;
        }
        User owner = userRepo.findById(currentUserId).orElse(null);
        if (owner == null || owner.getRole() != UserRole.OWNER) {
            return null;
        }
        Salon salon = salonRepo.findByOwnerId(currentUserId);
        return salon != null && salon.getStatus() == SalonStatus.Accepted ? salon : null;
    }

    private String validatePage(Integer page, Integer size) {
        int pageNumber = page == null ? 0 : page;
        int pageSize = size == null ? DEFAULT_PAGE_SIZE : size;
        if (pageNumber < 0) {
            return "Page cannot be negative.";
        }
        if (pageSize < 1 || pageSize > MAX_PAGE_SIZE) {
            return "Page size must be between 1 and 100.";
        }
        return null;
    }

    private PageRequest pageRequest(Integer page, Integer size) {
        return PageRequest.of(
                page == null ? 0 : page,
                size == null ? DEFAULT_PAGE_SIZE : size,
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))
        );
    }

    private EmploymentRequestPageResponse mapEmploymentPage(
            Page<EmploymentRequest> requests
    ) {
        return new EmploymentRequestPageResponse(
                requests.getContent().stream().map(this::mapEmploymentRequest).toList(),
                requests.getNumber(),
                requests.getSize(),
                requests.getTotalElements(),
                requests.getTotalPages(),
                requests.isFirst(),
                requests.isLast()
        );
    }

    private LeaveRequestPageResponse mapLeavePage(Page<LeaveRequest> requests) {
        return new LeaveRequestPageResponse(
                requests.getContent().stream().map(this::mapLeaveRequest).toList(),
                requests.getNumber(),
                requests.getSize(),
                requests.getTotalElements(),
                requests.getTotalPages(),
                requests.isFirst(),
                requests.isLast()
        );
    }

    private EmploymentCandidateResponse mapCandidate(User user) {
        Salon currentSalon = user.getSalon();
        return new EmploymentCandidateResponse(
                user.getId(),
                (user.getFirstName() + " " + user.getLastName()).trim(),
                user.getUserName(),
                user.getYearsOfExperience(),
                List.copyOf(user.getCareerSkills()),
                List.copyOf(user.getPreferredWorkLocations()),
                user.getProfessionalBio(),
                user.getResumeUrl(),
                user.getResumeUrl() != null && !user.getResumeUrl().isBlank(),
                user.getPortfolioUrl(),
                currentSalon == null ? null : currentSalon.getId(),
                currentSalon == null ? null : currentSalon.getName()
        );
    }

    private EmployeeRequestResponse mapEmploymentRequest(EmploymentRequest request) {
        HiringPost hiringPost = request.getHiringPost();
        return new EmployeeRequestResponse(
                request.getId(),
                request.getSalon().getId(),
                request.getSalon().getName(),
                request.getSender().getId(),
                request.getSender().getUserName(),
                request.getReceiver().getId(),
                request.getReceiver().getUserName(),
                hiringPost == null ? null : hiringPost.getId(),
                hiringPost == null ? null : hiringPost.getTitle(),
                request.getRequestType(),
                request.getRequestSource(),
                request.getStatus(),
                request.getRejectionReason(),
                request.getCreatedAt()
        );
    }

    private LeaveRequestResponse mapLeaveRequest(LeaveRequest request) {
        Salon targetSalon = request.getTargetSalon();
        EmploymentRequest employmentRequest = request.getEmploymentRequest();
        return new LeaveRequestResponse(
                request.getId(),
                request.getEmployee().getId(),
                request.getEmployee().getUserName(),
                request.getCurrentSalon().getId(),
                request.getCurrentSalon().getName(),
                targetSalon == null ? null : targetSalon.getId(),
                targetSalon == null ? null : targetSalon.getName(),
                employmentRequest == null ? null : employmentRequest.getId(),
                request.getStatus(),
                request.getReason(),
                request.getCreatedAt()
        );
    }
}
