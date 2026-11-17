package com.salon.backend.Services.Email;

import com.salon.backend.Entities.notifications.NotificationEventType;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import static com.salon.backend.Services.Email.EmailPriority.*;

/**
 * All SalonHub email content in one place: account, security, salon,
 * booking, employment, hiring and community emails.
 */
@Component
public class EmailTemplates {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("EEE, d MMM yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH);
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("EEE, d MMM yyyy 'at' HH:mm", Locale.ENGLISH);
    private static final String CODE_MINUTES = "10";

    // ---------------------------------------------------------------- account

    public EmailTemplate verificationCode(String code) {
        return EmailTemplate.builder(CRITICAL, "Your SalonHub verification code: " + code)
                .heading("Confirm your email")
                .preheader("Use this code to finish creating your SalonHub account.")
                .paragraph("Enter this code in SalonHub to confirm your email address.")
                .code(code)
                .footnote("The code expires in " + CODE_MINUTES + " minutes. If you did not create a SalonHub account, you can ignore this email.")
                .build();
    }

    public EmailTemplate passwordResetCode(String code) {
        return EmailTemplate.builder(CRITICAL, "Your SalonHub password reset code: " + code)
                .heading("Reset your password")
                .preheader("Use this code to set a new password.")
                .paragraph("We got a request to reset the password of your SalonHub account. Enter this code to continue.")
                .code(code)
                .footnote("The code expires in " + CODE_MINUTES + " minutes. If you did not ask for a reset, ignore this email: your password stays the same. Never share this code with anyone.")
                .build();
    }

    public EmailTemplate passwordChanged(boolean byReset, ZonedDateTime when) {
        return EmailTemplate.builder(CRITICAL, "Your SalonHub password was changed")
                .heading("Your password was changed")
                .paragraph(byReset
                        ? "The password of your SalonHub account was reset with a security code."
                        : "The password of your SalonHub account was changed from your account settings.")
                .paragraph("For your safety, you are signed out on your other devices.")
                .detail("Time (Jordan)", when.format(DATE_TIME))
                .cta("Reset my password", "/forgot-password")
                .footnote("If this was you, you do not need to do anything. If it was not you, reset your password now.")
                .build();
    }

    public EmailTemplate welcome() {
        return EmailTemplate.builder(NORMAL, "Welcome to the SalonHub community")
                .heading("Welcome to SalonHub")
                .preheader("Your account is ready. Here is what you can do first.")
                .paragraph("Your email is confirmed and your account is ready. We are happy to have you in the community.")
                .paragraph("Discover salons near you, book appointments in a few taps, share ideas with the community and find beauty career opportunities across Jordan.")
                .cta("Explore SalonHub", "/")
                .footnote("Own a salon? You can create your salon page from the menu after you sign in.")
                .build();
    }

    public EmailTemplate loginAlert(ZonedDateTime when) {
        return EmailTemplate.builder(NORMAL, "New sign-in to your SalonHub account")
                .heading("New sign-in to your account")
                .preheader("We noticed a new sign-in to your account.")
                .paragraph("Your SalonHub account was just used to sign in.")
                .detail("Time (Jordan)", when.format(DATE_TIME))
                .cta("Not you? Reset your password", "/forgot-password")
                .footnote("If this was you, you do not need to do anything.")
                .build();
    }

    // ---------------------------------------------------------- notifications

    /** Data copied from an in-app notification, safe to use after commit. */
    public record NotificationEmailData(
            NotificationEventType eventType,
            String message,
            String salonName,
            LocalDate bookingDate,
            LocalTime bookingTime,
            LocalDate suggestedDate,
            LocalTime suggestedTime,
            String reason,
            String targetPath) {}

    /** Every in-app notification type mapped to an email with a priority. */
    public EmailTemplate forNotification(NotificationEmailData data) {
        Spec spec = spec(data.eventType());
        EmailTemplate.Builder builder = EmailTemplate.builder(spec.priority(), spec.subject(data.salonName()))
                .heading(spec.heading())
                .paragraph(data.message())
                .detail("Salon", data.salonName());
        if (data.bookingDate() != null) builder.detail("Date", data.bookingDate().format(DATE));
        if (data.bookingTime() != null) builder.detail("Time", data.bookingTime().format(TIME));
        if (data.suggestedDate() != null || data.suggestedTime() != null) {
            String suggested = (data.suggestedDate() == null ? "" : data.suggestedDate().format(DATE))
                    + (data.suggestedDate() != null && data.suggestedTime() != null ? " at " : "")
                    + (data.suggestedTime() == null ? "" : data.suggestedTime().format(TIME));
            builder.detail("Suggested time", suggested);
        }
        builder.detail("Reason", data.reason());
        return builder.cta(spec.ctaLabel(), data.targetPath() == null ? "/notifications" : data.targetPath()).build();
    }

    private record Spec(EmailPriority priority, String subjectText, String heading, String ctaLabel) {
        String subject(String salonName) {
            return salonName == null || salonName.isBlank() ? subjectText : subjectText + " · " + salonName;
        }
    }

    private Spec spec(NotificationEventType type) {
        return switch (type) {
            case SALON_REQUEST_SUBMITTED -> new Spec(NORMAL, "We received your salon request", "Your salon request is in review", "View my salon");
            case SALON_REQUEST_RESUBMITTED -> new Spec(NORMAL, "We received your updated salon request", "Your updated request is in review", "View my salon");
            case SALON_REQUEST_ACCEPTED -> new Spec(HIGH, "Your salon is approved", "Congratulations, your salon is live", "Open my salon");
            case SALON_REQUEST_REJECTED -> new Spec(HIGH, "Your salon request needs changes", "Your salon request was not approved", "Review and resubmit");
            case SALON_BLOCKED -> new Spec(HIGH, "Your salon was blocked", "Your salon is blocked", "Open my salon");
            case SALON_UNBLOCKED -> new Spec(HIGH, "Your salon is active again", "Your salon was unblocked", "Open my salon");

            case BOOKING_REQUESTED -> new Spec(HIGH, "New booking request", "You have a new booking request", "Review request");
            case BOOKING_ACCEPTED -> new Spec(HIGH, "Your booking is confirmed", "Your appointment is confirmed", "View booking");
            case BOOKING_REJECTED -> new Spec(HIGH, "Your booking request was declined", "The salon declined your request", "Book another time");
            case BOOKING_TIME_SUGGESTED -> new Spec(HIGH, "The salon suggested a new time", "A new time was suggested", "Review the new time");
            case BOOKING_RESCHEDULE_PROPOSED -> new Spec(HIGH, "A new time is proposed for your booking", "Please review the new time", "Review the new time");
            case BOOKING_RESCHEDULE_ACCEPTED -> new Spec(HIGH, "The new booking time is confirmed", "The new time is confirmed", "View booking");
            case BOOKING_RESCHEDULE_REJECTED -> new Spec(HIGH, "The new booking time was declined", "The new time was declined", "View booking");
            case BOOKING_EMPLOYEE_REASSIGNED -> new Spec(HIGH, "Your booking has a new professional", "Your professional changed", "View booking");
            case BOOKING_CANCELLED -> new Spec(HIGH, "A booking was cancelled", "Booking cancelled", "View bookings");
            case BOOKING_ASSIGNED -> new Spec(HIGH, "New appointment assigned to you", "You have a new appointment", "View my appointments");
            case BOOKING_EXPIRED -> new Spec(HIGH, "A booking request expired", "The booking request expired", "View bookings");
            case BOOKING_NO_SHOW -> new Spec(HIGH, "You missed your appointment", "Marked as a missed appointment", "View booking");
            case BOOKING_REMINDER -> new Spec(NORMAL, "Reminder: your appointment is coming up", "Your appointment is coming up", "View booking");
            case BOOKING_COMPLETED -> new Spec(NORMAL, "Thank you for your visit", "Your appointment is complete", "Leave a review");
            case PAYMENT_RECEIVED -> new Spec(NORMAL, "Payment received · your receipt is ready", "Payment received", "View receipt");

            case FRIEND_REQUEST_RECEIVED -> new Spec(NORMAL, "You have a new friend request", "New friend request", "View request");

            // Live tracking and small community updates stay in the app.
            case BOOKING_CUSTOMER_ON_THE_WAY, BOOKING_CUSTOMER_ARRIVED, BOOKING_SERVICE_STARTED,
                 FRIEND_REQUEST_ACCEPTED, FRIEND_REQUEST_DECLINED ->
                    new Spec(LOW, "SalonHub update", "SalonHub update", "Open SalonHub");
        };
    }

    // ------------------------------------------------------------- employment

    public EmailTemplate joinRequestReceived(String applicantName, String salonName) {
        return EmailTemplate.builder(HIGH, applicantName + " wants to join " + salonName)
                .heading("New request to join your team")
                .paragraph(applicantName + " asked to join " + salonName + " as a team member.")
                .detail("Salon", salonName)
                .cta("Review request", "/owner/employment")
                .build();
    }

    public EmailTemplate invitationReceived(String ownerName, String salonName) {
        return EmailTemplate.builder(HIGH, "You are invited to join " + salonName)
                .heading("You have a job invitation")
                .paragraph(ownerName + " invited you to join the team of " + salonName + ".")
                .detail("Salon", salonName)
                .cta("View invitation", "/employment")
                .build();
    }

    public EmailTemplate employmentAccepted(String otherName, String salonName, boolean invitation, String jobTitle) {
        return EmailTemplate.builder(HIGH, invitation
                        ? otherName + " accepted your invitation"
                        : "Welcome to the team of " + salonName)
                .heading(invitation ? "Your invitation was accepted" : "Your request was accepted")
                .paragraph(invitation
                        ? otherName + " accepted your invitation and is now part of the " + salonName + " team."
                        : "Your request to join " + salonName + " was accepted. You are now part of the team.")
                .detail("Salon", salonName)
                .detail("Job", jobTitle)
                .cta(invitation ? "Open my team" : "Open employment center", invitation ? "/owner/employees" : "/employment")
                .build();
    }

    public EmailTemplate employmentRejected(String otherName, String salonName, boolean invitation, String jobTitle, String reason) {
        return EmailTemplate.builder(HIGH, invitation
                        ? otherName + " declined your invitation"
                        : "Update on your request to join " + salonName)
                .heading(invitation ? "Your invitation was declined" : "Your request was not accepted")
                .paragraph(invitation
                        ? otherName + " declined the invitation to join " + salonName + "."
                        : "Your request to join " + salonName + " was not accepted this time.")
                .detail("Salon", salonName)
                .detail("Job", jobTitle)
                .detail("Reason", reason)
                .cta(invitation ? "Find candidates" : "Browse career opportunities",
                        invitation ? "/owner/employment" : "/hiring-posts")
                .build();
    }

    public EmailTemplate leaveRequested(String employeeName, String salonName, String reason) {
        return EmailTemplate.builder(HIGH, employeeName + " asked to leave " + salonName)
                .heading("New leave request")
                .paragraph(employeeName + " sent a request to leave the " + salonName + " team.")
                .detail("Reason", reason)
                .cta("Review request", "/owner/employment")
                .build();
    }

    public EmailTemplate leaveAccepted(String salonName) {
        return EmailTemplate.builder(HIGH, "Your leave request was accepted")
                .heading("Your leave request was accepted")
                .paragraph("Your request to leave " + salonName + " was accepted.")
                .cta("Open employment center", "/employment")
                .build();
    }

    // ----------------------------------------------------------------- hiring

    public EmailTemplate applicationReceived(String applicantName, String postTitle, String salonName) {
        return EmailTemplate.builder(HIGH, "New application: " + postTitle)
                .heading("You have a new job application")
                .paragraph(applicantName + " applied to your job post at " + salonName + ".")
                .detail("Job", postTitle)
                .cta("Review application", "/owner/applications")
                .build();
    }
}
