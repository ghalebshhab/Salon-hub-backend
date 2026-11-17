package com.salon.backend.Services.Notification;

import com.salon.backend.Services.Salon.Booking.BookingTimePolicy;

import com.salon.backend.Entities.notifications.NotificationEventType;
import com.salon.backend.Entities.notifications.NotificationDomain;
import com.salon.backend.Entities.notifications.UserNotification;
import com.salon.backend.Entities.salons.bookings.Booking;
import com.salon.backend.Entities.salons.bookings.BookingRescheduleProposal;
import com.salon.backend.Entities.users.User;
import com.salon.backend.Repositories.Notification.UserNotificationRepo;
import com.salon.backend.Services.Email.NotificationEmailBridge;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class BookingNotificationServiceImpl implements BookingNotificationService {

    private final UserNotificationRepo notificationRepo;
    private final NotificationEmailBridge emailBridge;

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

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void bookingRequested(Booking booking) {
        User owner = booking.getSalon().getOwner();
        String employeePart = booking.getAssignedEmployee() == null
                ? " Choose who will serve the customer."
                : " Requested employee: " + fullName(booking.getAssignedEmployee()) + ".";
        saveNotification(
                booking,
                owner,
                NotificationEventType.BOOKING_REQUESTED,
                "booking:" + booking.getId() + ":requested",
                fullName(booking.getCustomer()) + " requested an appointment on "
                        + booking.getBookingDate() + " at " + booking.getBookingTime() + "." + employeePart,
                ownerBookingPath(booking)
        );
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void bookingAssignedToEmployee(Booking booking) {
        User employee = booking.getAssignedEmployee();
        if (employee == null) {
            return;
        }
        saveNotification(
                booking,
                employee,
                NotificationEventType.BOOKING_ASSIGNED,
                "booking:" + booking.getId() + ":assigned:" + employee.getId(),
                "You will serve " + fullName(booking.getCustomer()) + " on "
                        + booking.getBookingDate() + " at " + booking.getBookingTime() + ".",
                employeeBookingPath(booking)
        );
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void customerOnTheWay(Booking booking) {
        User employee = booking.getAssignedEmployee();
        Integer eta = booking.getTracking().getEtaMinutes();
        saveNotification(
                booking,
                employee,
                NotificationEventType.BOOKING_CUSTOMER_ON_THE_WAY,
                "booking:" + booking.getId() + ":on-the-way",
                fullName(booking.getCustomer()) + " is on the way"
                        + (eta == null ? "." : ", about " + eta + " min away."),
                employeeBookingPath(booking)
        );
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void customerArrived(Booking booking) {
        saveNotification(
                booking,
                booking.getAssignedEmployee(),
                NotificationEventType.BOOKING_CUSTOMER_ARRIVED,
                "booking:" + booking.getId() + ":arrived",
                fullName(booking.getCustomer()) + " arrived at the salon.",
                employeeBookingPath(booking)
        );
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void serviceStarted(Booking booking) {
        User employee = booking.getAssignedEmployee();
        saveNotification(
                booking,
                booking.getCustomer(),
                NotificationEventType.BOOKING_SERVICE_STARTED,
                "booking:" + booking.getId() + ":started",
                (employee == null ? "Your stylist" : fullName(employee))
                        + " started your service at " + booking.getSalon().getName() + ".",
                customerBookingPath(booking)
        );
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void bookingCompleted(Booking booking) {
        saveNotification(
                booking,
                booking.getCustomer(),
                NotificationEventType.BOOKING_COMPLETED,
                "booking:" + booking.getId() + ":completed",
                "Your appointment at " + booking.getSalon().getName()
                        + " is complete. Share a review to help other guests.",
                customerBookingPath(booking)
        );
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void bookingExpired(Booking booking) {
        saveNotification(
                booking,
                booking.getCustomer(),
                NotificationEventType.BOOKING_EXPIRED,
                "booking:" + booking.getId() + ":expired:customer",
                booking.getSalon().getName() + " did not answer your request for "
                        + booking.getBookingDate() + " at " + booking.getBookingTime()
                        + " in time. The request expired; you can book another time.",
                customerBookingPath(booking)
        );
        saveNotification(
                booking,
                booking.getSalon().getOwner(),
                NotificationEventType.BOOKING_EXPIRED,
                "booking:" + booking.getId() + ":expired:owner",
                "The request from " + fullName(booking.getCustomer()) + " for "
                        + booking.getBookingDate() + " at " + booking.getBookingTime()
                        + " expired without an answer.",
                ownerBookingPath(booking)
        );
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void bookingReminder(
            Booking booking,
            BookingTimePolicy.ReminderType type,
            java.time.LocalDate today) {
        if (type == null) {
            return;
        }
        String when = booking.getBookingDate().equals(today)
                ? "today at " + booking.getBookingTime()
                : booking.getBookingDate().equals(today.plusDays(1))
                        ? "tomorrow at " + booking.getBookingTime()
                        : "on " + booking.getBookingDate() + " at " + booking.getBookingTime();
        String suffix = type == BookingTimePolicy.ReminderType.SOON
                ? " Share your trip when you leave so the salon knows when you arrive."
                : " Cancel early if your plans change.";
        saveNotification(
                booking,
                booking.getCustomer(),
                NotificationEventType.BOOKING_REMINDER,
                "booking:" + booking.getId() + ":reminder:" + type.name().toLowerCase() + ":customer",
                "Reminder: your appointment at " + booking.getSalon().getName() + " is " + when + "." + suffix,
                customerBookingPath(booking)
        );
        User employee = booking.getAssignedEmployee();
        if (type == BookingTimePolicy.ReminderType.SOON && employee != null) {
            saveNotification(
                    booking,
                    employee,
                    NotificationEventType.BOOKING_REMINDER,
                    "booking:" + booking.getId() + ":reminder:soon:employee",
                    "Coming up: " + fullName(booking.getCustomer()) + " " + when + ".",
                    employeeBookingPath(booking)
            );
        }
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void bookingNoShow(Booking booking) {
        saveNotification(
                booking,
                booking.getCustomer(),
                NotificationEventType.BOOKING_NO_SHOW,
                "booking:" + booking.getId() + ":no-show",
                booking.getSalon().getName() + " marked your appointment on "
                        + booking.getBookingDate() + " at " + booking.getBookingTime()
                        + " as missed. Contact the salon if this is a mistake.",
                customerBookingPath(booking)
        );
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void paymentReceived(Booking booking) {
        if (booking.getPayment() == null) {
            return;
        }
        String amount = java.math.BigDecimal.valueOf(booking.getPayment().getAmountFils(), 3).toPlainString()
                + " " + booking.getPayment().getCurrency();
        saveNotification(
                booking,
                booking.getCustomer(),
                NotificationEventType.PAYMENT_RECEIVED,
                "booking:" + booking.getId() + ":payment:paid",
                booking.getSalon().getName() + " received your payment of " + amount
                        + ". Your receipt is ready in My bookings.",
                customerBookingPath(booking)
        );
    }

    private String ownerBookingPath(Booking booking) {
        return "/owner/bookings?bookingId=" + booking.getId();
    }

    private String employeeBookingPath(Booking booking) {
        return "/employee/bookings?bookingId=" + booking.getId();
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
        emailBridge.notificationSaved(notification);
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
