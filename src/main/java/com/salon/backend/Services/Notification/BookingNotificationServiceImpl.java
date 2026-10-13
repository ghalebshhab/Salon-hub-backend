package com.salon.backend.Services.Notification;

import com.salon.backend.Entities.notifications.NotificationEventType;
import com.salon.backend.Entities.notifications.UserNotification;
import com.salon.backend.Entities.salons.bookings.Booking;
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
        saveSuggestedTimeNotification(booking, "accepted");
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
        saveSuggestedTimeNotification(booking, "rejected");
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

    private void saveSuggestedTimeNotification(Booking booking, String decision) {
        if (booking.getOwnerSuggestedTime() == null) {
            return;
        }
        saveNotification(
                booking,
                booking.getCustomer(),
                NotificationEventType.BOOKING_TIME_SUGGESTED,
                "booking:" + booking.getId() + ":suggested-time:" + decision,
                booking.getSalon().getName() + " suggested "
                        + booking.getOwnerSuggestedTime()
                        + " for your booking on " + booking.getBookingDate() + ".",
                customerBookingPath(booking)
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
        notification.setEventType(eventType);
        notification.setMessage(message);
        notification.setEventKey(eventKey);
        notification.setSalonId(booking.getSalon().getId());
        notification.setSalonName(booking.getSalon().getName());
        notification.setBookingDate(booking.getBookingDate());
        notification.setBookingTime(booking.getBookingTime());
        notification.setSuggestedTime(booking.getOwnerSuggestedTime());
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
