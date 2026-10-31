package com.salon.backend.Services.Notification;

import com.salon.backend.Entities.notifications.NotificationEventType;
import com.salon.backend.Entities.notifications.NotificationDomain;
import com.salon.backend.Entities.notifications.UserNotification;
import com.salon.backend.Entities.salons.bookings.Booking;
import com.salon.backend.Entities.salons.bookings.BookingRescheduleProposal;
import com.salon.backend.Entities.users.User;
import com.salon.backend.Repositories.Notification.UserNotificationRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class BookingNotificationServiceImpl implements BookingNotificationService {

    private final UserNotificationRepo notificationRepo;

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void bookingAccepted(Booking booking) {
        saveNotification(
                booking,
                booking.getCustomer(),
                NotificationEventType.BOOKING_ACCEPTED,
                "booking:" + booking.getId() + ":accepted",
                "Your booking at " + booking.getSalon().getName()
                        + " on " + booking.getBookingDate()
                        + " at " + booking.getBookingTime() + " was accepted.",
                customerBookingPath(booking)
        );
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void bookingRejected(Booking booking) {
        saveNotification(
                booking,
                booking.getCustomer(),
                NotificationEventType.BOOKING_REJECTED,
                "booking:" + booking.getId() + ":rejected",
                "Your booking at " + booking.getSalon().getName()
                        + " on " + booking.getBookingDate()
                        + " at " + booking.getBookingTime() + " was rejected.",
                customerBookingPath(booking)
        );
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void bookingEmployeeReassigned(Booking booking, Long previousEmployeeId) {
        User employee = booking.getAssignedEmployee();
        if (employee == null || previousEmployeeId == null || previousEmployeeId == employee.getId()) {
            return;
        }

        saveNotification(
                booking,
                booking.getCustomer(),
                NotificationEventType.BOOKING_EMPLOYEE_REASSIGNED,
                "booking:" + booking.getId()
                        + ":reassigned:" + booking.getVersion()
                        + ":" + previousEmployeeId
                        + ":" + employee.getId(),
                "Your booking at " + booking.getSalon().getName()
                        + " was reassigned to " + fullName(employee) + ".",
                customerBookingPath(booking)
        );
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void bookingCancelled(Booking booking) {
        User salonOwner = booking.getSalon().getOwner();
        saveCancellationNotification(
                booking,
                salonOwner,
                "owner",
                "/owner/bookings?bookingId=" + booking.getId()
        );

        User assignedEmployee = booking.getAssignedEmployee();
        if (assignedEmployee != null
                && (salonOwner == null || assignedEmployee.getId() != salonOwner.getId())) {
            saveCancellationNotification(
                    booking,
                    assignedEmployee,
                    "employee",
                    "/employee/bookings?bookingId=" + booking.getId()
            );
        }
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void bookingRescheduleProposed(
            Booking booking,
            BookingRescheduleProposal proposal) {
        saveNotification(
                booking,
                booking.getCustomer(),
                NotificationEventType.BOOKING_RESCHEDULE_PROPOSED,
                "booking:" + booking.getId() + ":reschedule:" + proposal.getId() + ":proposed",
                booking.getSalon().getName() + " proposed moving your booking to "
                        + proposal.getProposedDate() + " at " + proposal.getProposedTime() + ".",
                customerBookingPath(booking),
                proposal.getProposedDate(),
                proposal.getProposedTime()
        );
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void bookingRescheduleAccepted(
            Booking booking,
            BookingRescheduleProposal proposal) {
        User owner = booking.getSalon().getOwner();
        saveNotification(
                booking,
                owner,
                NotificationEventType.BOOKING_RESCHEDULE_ACCEPTED,
                "booking:" + booking.getId() + ":reschedule:" + proposal.getId() + ":accepted:owner",
                fullName(booking.getCustomer()) + " accepted the new appointment on "
                        + booking.getBookingDate() + " at " + booking.getBookingTime() + ".",
                "/owner/bookings?bookingId=" + booking.getId(),
                proposal.getProposedDate(),
                proposal.getProposedTime()
        );

        User employee = booking.getAssignedEmployee();
        if (booking.getStatus() == com.salon.backend.Entities.salons.bookings.BookingStatus.ACCEPTED
                && employee != null
                && (owner == null || employee.getId() != owner.getId())) {
            saveNotification(
                    booking,
                    employee,
                    NotificationEventType.BOOKING_RESCHEDULE_ACCEPTED,
                    "booking:" + booking.getId() + ":reschedule:" + proposal.getId()
                            + ":accepted:employee:" + employee.getId(),
                    "Your assigned booking at " + booking.getSalon().getName()
                            + " moved to " + booking.getBookingDate()
                            + " at " + booking.getBookingTime() + ".",
                    "/employee/bookings?bookingId=" + booking.getId(),
                    proposal.getProposedDate(),
                    proposal.getProposedTime()
            );
        }
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void bookingRescheduleRejected(
            Booking booking,
            BookingRescheduleProposal proposal) {
        saveNotification(
                booking,
                booking.getSalon().getOwner(),
                NotificationEventType.BOOKING_RESCHEDULE_REJECTED,
                "booking:" + booking.getId() + ":reschedule:" + proposal.getId() + ":rejected",
                fullName(booking.getCustomer()) + " rejected the proposed appointment on "
                        + proposal.getProposedDate() + " at " + proposal.getProposedTime() + ".",
                "/owner/bookings?bookingId=" + booking.getId(),
                proposal.getProposedDate(),
                proposal.getProposedTime()
        );
    }

    private void saveCancellationNotification(
            Booking booking,
            User recipient,
            String recipientType,
            String targetPath) {
        if (recipient == null) {
            return;
        }
        saveNotification(
                booking,
                recipient,
                NotificationEventType.BOOKING_CANCELLED,
                "booking:" + booking.getId() + ":cancelled:" + recipientType + ":" + recipient.getId(),
                fullName(booking.getCustomer()) + " cancelled the booking at "
                        + booking.getSalon().getName()
                        + " on " + booking.getBookingDate()
                        + " at " + booking.getBookingTime() + ".",
                targetPath
        );
    }

    private void saveNotification(
            Booking booking,
            User recipient,
            NotificationEventType eventType,
            String eventKey,
            String message,
            String targetPath) {
        saveNotification(
                booking,
                recipient,
                eventType,
                eventKey,
                message,
                targetPath,
                null,
                null
        );
    }

    private void saveNotification(
            Booking booking,
            User recipient,
            NotificationEventType eventType,
            String eventKey,
            String message,
            String targetPath,
            java.time.LocalDate suggestedDate,
            java.time.LocalTime suggestedTime) {
        if (booking == null
                || booking.getId() == null
                || recipient == null
                || eventKey == null
                || notificationRepo.existsByEventKey(eventKey)) {
            return;
        }

        UserNotification notification = new UserNotification();
        notification.setRecipient(recipient);
        notification.setBooking(booking);
        notification.setDomain(NotificationDomain.BOOKING);
        notification.setEventType(eventType);
        notification.setMessage(message);
        notification.setEventKey(eventKey);
        notification.setSalonId(booking.getSalon().getId());
        notification.setSalonName(booking.getSalon().getName());
        notification.setBookingDate(booking.getBookingDate());
        notification.setBookingTime(booking.getBookingTime());
        notification.setSuggestedDate(suggestedDate);
        notification.setSuggestedTime(suggestedTime);
        notification.setBookingStatus(booking.getStatus());
        notification.setTargetPath(targetPath);
        notification.setCreatedAt(LocalDateTime.now());
        notificationRepo.save(notification);
    }

    private String customerBookingPath(Booking booking) {
        return "/my-bookings?bookingId=" + booking.getId();
    }

    private String fullName(User user) {
        String firstName = user.getFirstName() == null ? "" : user.getFirstName().trim();
        String lastName = user.getLastName() == null ? "" : user.getLastName().trim();
        String fullName = (firstName + " " + lastName).trim();
        return fullName.isEmpty() ? user.getUserName() : fullName;
    }
}
