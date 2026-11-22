package com.salon.backend.Entities.notifications;

public enum NotificationEventType {
    BOOKING_ACCEPTED,
    BOOKING_REJECTED,
    BOOKING_TIME_SUGGESTED,
    BOOKING_RESCHEDULE_PROPOSED,
    BOOKING_RESCHEDULE_ACCEPTED,
    BOOKING_RESCHEDULE_REJECTED,
    BOOKING_EMPLOYEE_REASSIGNED,
    BOOKING_CANCELLED,
    BOOKING_REQUESTED,
    BOOKING_ASSIGNED,
    BOOKING_CUSTOMER_ON_THE_WAY,
    BOOKING_CUSTOMER_ARRIVED,
    /** The customer confirmed, after a reminder, that they will come. */
    BOOKING_CUSTOMER_CONFIRMED,
    BOOKING_SERVICE_STARTED,
    BOOKING_COMPLETED,
    BOOKING_EXPIRED,
    BOOKING_REMINDER,
    BOOKING_NO_SHOW,
    /** "Leave on time": the customer should leave in a few minutes. */
    BOOKING_LEAVE_SOON,
    /** "Leave on time": the customer should leave now to be on time. */
    BOOKING_LEAVE_NOW,
    /** The employee is running late, so the customer's turn moved later. */
    BOOKING_SALON_DELAY,
    PAYMENT_RECEIVED,
    SALON_REQUEST_SUBMITTED,
    SALON_REQUEST_RESUBMITTED,
    SALON_REQUEST_ACCEPTED,
    SALON_REQUEST_REJECTED,
    SALON_BLOCKED,
    SALON_UNBLOCKED,
    FRIEND_REQUEST_RECEIVED,
    FRIEND_REQUEST_ACCEPTED,
    FRIEND_REQUEST_DECLINED
}
