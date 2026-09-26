package com.salon.backend.Services.Salon.Employment;

import com.salon.backend.DTOs.ApiResponse;
import com.salon.backend.DTOs.Salon.Employment.Join.*;
import com.salon.backend.DTOs.Salon.Employment.Leave.CreateLeaveRequest;
import com.salon.backend.Entities.salons.Salon;
import com.salon.backend.Entities.salons.SalonStatus;
import com.salon.backend.Entities.salons.employment.*;
import com.salon.backend.Entities.salons.hiringposts.HiringPost;
import com.salon.backend.Entities.salons.hiringposts.HiringPostStatus;
import com.salon.backend.Entities.users.User;
import com.salon.backend.Entities.users.UserRole;
import com.salon.backend.Repositories.Salon.EmploymentRepo;
import com.salon.backend.Repositories.Salon.LeaveRequestRepo;
import com.salon.backend.Repositories.Salon.HiringPostRepo;
import com.salon.backend.Repositories.Salon.SalonRepo;
import com.salon.backend.Repositories.User.UserRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;


@Service
@RequiredArgsConstructor
public class EmploymentServiceImpl implements EmploymentService {
    private final SalonRepo salonRepo;
    private final UserRepo userRepo;
    private final EmploymentRepo employmentRepo;
    private final LeaveRequestRepo  leaveRequestRepo;
    private final HiringPostRepo hiringPostRepo;
    @Override
    public ApiResponse<EmploymentRequest> joinSalon(JoinSalonRequest request, Long senderId) {
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

        return ApiResponse.success("Request to this salon Successfully done" , employmentRequest);
    }

//    This  accept request method is bidirectional > so the user can accept the invite from the salon owner  ,
//    and the salon owwner  can accept the user request  .
@Override
@Transactional
public ApiResponse<EmploymentRequest> acceptRequest(
        AcceptRequest acceptRequest,
        Long currentUserId) {

    Optional<EmploymentRequest> optionalRequest =
            employmentRepo.findById(acceptRequest.requestId());

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

        if (targetSalon.getOwner().getId()!= currentUserId) {

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

        if (employmentRequest.getReceiver()
                .getId()!= currentUserId) {

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
                employmentRequest
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
            employmentRequest
    );
}
    @Override
    public ApiResponse<EmploymentRequest> sentInvitation(SentInvitation sentInvitation, Long salonId, Long senderId) {
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

        if (salon.getOwner().getId()!= sender.getId()) {
            return ApiResponse.error(
                    "You are not the owner of this salon"
            );
        }

     if(sender.getId()==receiver.getId()){
         return ApiResponse.error("You can not invite yourself");
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

        return ApiResponse.success("Invetation Sented Sucessfully",employmentRequest);
    }

    @Override
    @Transactional
    public ApiResponse<EmploymentRequest> rejectRequest(
            RejectRequest rejectRequest,
            Long currentUserId) {

        Optional<EmploymentRequest> optionalRequest =
                employmentRepo.findById(rejectRequest.getRequestId());

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

            if (salon.getOwner().getId()!= currentUserId) {

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

            if (receiver.getId() != currentUserId) {

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
                employmentRequest
        );
    }

    @Override
    public ApiResponse<EmploymentRequest> cancelRequest(CancelRequest cancelRequest, Long currentUserId) {
        Optional<EmploymentRequest> empreq=employmentRepo.findById(cancelRequest.requestId());
        if (empreq.isEmpty()) {
            return ApiResponse.error("This Employment Request is not found");
        }
        EmploymentRequest employmentRequest = empreq.get();
        if (employmentRequest.getStatus()!=EmploymentRequestStatus.Requested) {
            return ApiResponse.error("This Employment Request is no longer pending");
        }
        User sender = employmentRequest.getSender();
        if (sender.getId()!=currentUserId) {
            return ApiResponse.error("You are not the sender of this request");
        }
        employmentRequest.setStatus(EmploymentRequestStatus.Cancelled);
        employmentRepo.save(employmentRequest);
        return ApiResponse.success("The  requestis cancelled successfully",employmentRequest);
    }

    @Override
    public ApiResponse<LeaveRequest> createLeaveRequest(
            CreateLeaveRequest request,
            Long currentUserId) {

        Optional<User> optionalUser =
                userRepo.findById(currentUserId);

        if (optionalUser.isEmpty()) {
            return ApiResponse.error("User not found");
        }

        User employee = optionalUser.get();

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
                leaveRequest
        );
    }


    @Override
    @Transactional
    public ApiResponse<LeaveRequest> acceptLeaveRequest(
            Long requestId,
            Long currentUserId) {

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

        if (currentSalon.getOwner().getId()!=currentUserId) {
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
                    leaveRequest
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
                leaveRequest
        );
    }
    @Override
    @Transactional
    public ApiResponse<LeaveRequest> cancelLeaveRequest(
            Long requestId,
            Long currentUserId) {

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

        if (employee.getId()!=currentUserId) {
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

            employmentRequest.setStatus(
                    EmploymentRequestStatus.Requested
            );

            if (employmentRequest.getRequestSource()
                    == EmploymentRequestSource.HIRING_POST
                    && employmentRequest.getHiringPost() != null) {

                HiringPost hiringPost = employmentRequest.getHiringPost();
                hiringPost.setNumOfPositions(
                        hiringPost.getNumOfPositions() + 1
                );

                if (hiringPost.getStatus() == HiringPostStatus.Closed
                        && hiringPost.getExpiresAt().isAfter(LocalDateTime.now())) {
                    hiringPost.setStatus(HiringPostStatus.Open);
                }

                hiringPostRepo.save(hiringPost);
            }

            employmentRepo.save(employmentRequest);
        }

        leaveRequestRepo.save(leaveRequest);

        return ApiResponse.success(
                "Leave request cancelled successfully",
                leaveRequest
        );
    }
    @Override
    @Transactional
    public ApiResponse<EmploymentRequest>
    acceptInvitationAndRequestLeave(
            Long requestId,
            Long currentUserId,
            String reason) {

        Optional<EmploymentRequest> optionalRequest =
                employmentRepo.findById(requestId);

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

        if (receiver.getId() != currentUserId) {
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
                    employmentRequest
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
                employmentRequest
        );
    }
}
