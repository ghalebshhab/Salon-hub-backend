package com.salon.backend.Services.Salon.Booking;

import com.salon.backend.DTOs.Salon.Booking.BookingRescheduleProposalResponse;
import com.salon.backend.DTOs.Salon.Booking.BookingResponse;
import com.salon.backend.DTOs.Salon.Booking.BookingServiceItemResponse;
import com.salon.backend.Entities.salons.bookings.Booking;
import com.salon.backend.Entities.salons.bookings.BookingStatus;
import com.salon.backend.Entities.salons.bookings.BookingTracking;
import com.salon.backend.Entities.users.User;
import com.salon.backend.Services.Payment.PaymentRules;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class BookingResponseMapper {

    public List<BookingResponse> map(List<Booking> bookings) {
        return bookings.stream().map(this::map).toList();
    }

    /**
     * Owner lists: adds each customer's earlier no-show count at the salon.
     * The counts come from one grouped query; key is {@link #noShowKey}.
     */
    public List<BookingResponse> map(List<Booking> bookings, Map<String, Long> noShowCounts) {
        return bookings.stream()
                .map(booking -> map(booking, noShowCounts.getOrDefault(
                        noShowKey(booking.getCustomer().getId(), booking.getSalon().getId()), 0L)))
                .toList();
    }

    public static String noShowKey(Long customerId, Long salonId) {
        return customerId + ":" + salonId;
    }

    public BookingResponse map(Booking booking) {
        return map(booking, null);
    }

    public BookingResponse map(Booking booking, Long customerNoShowCount) {
        User customer = booking.getCustomer();
        User employee = booking.getAssignedEmployee();
        BookingTracking tracking = booking.getTracking();
        List<BookingServiceItemResponse> services = booking.getServiceItems().stream()
                .map(item -> new BookingServiceItemResponse(
                        item.getId(),
                        item.getSourceServiceId(),
                        item.getServiceName(),
                        item.getPrice(),
                        item.getDurationMinutes()
                ))
                .toList();

        return new BookingResponse(
                booking.getId(),
                booking.getSalon().getId(),
                booking.getSalon().getName(),
                booking.getSalon().getLocation(),
                customer.getId(),
                fullName(customer),
                customer.getEmail(),
                booking.getCustomerPhoneNumber(),
                booking.getCustomerLocation(),
                booking.getBookingDate(),
                booking.getBookingTime(),
                booking.getStatus(),
                services,
                booking.getTotalPrice(),
                booking.getTotalDurationMinutes(),
                employee == null ? null : employee.getId(),
                employee == null ? null : fullName(employee),
                employee == null ? null : specialty(employee),
                booking.getNote(),
                booking.getOwnerNote(),
                booking.getRescheduleProposals().stream()
                        .map(proposal -> new BookingRescheduleProposalResponse(
                                proposal.getId(),
                                proposal.getProposedDate(),
                                proposal.getProposedTime(),
                                proposal.getNote(),
                                proposal.getStatus(),
                                proposal.getCreatedAt(),
                                proposal.getRespondedAt()
                        ))
                        .toList(),
                booking.getCreatedAt(),
                booking.getUpdatedAt(),
                tracking.journeyStatusOrDefault(),
                tracking.getEtaMinutes(),
                tracking.getArrivedAt(),
                tracking.getServiceStartedAt(),
                tracking.getExpectedEndAt(),
                booking.getCompletedAt(),
                booking.getSalon().getLatitude(),
                booking.getSalon().getLongitude(),
                booking.getStatus() == BookingStatus.PENDING
                        ? BookingTimePolicy.responseDeadline(booking)
                        : null,
                booking.getNoShowAt(),
                PaymentRules.summary(booking),
                booking.getCustomerConfirmedAt(),
                customerNoShowCount
        );
    }

    public static String fullName(User user) {
        String firstName = user.getFirstName() == null ? "" : user.getFirstName().trim();
        String lastName = user.getLastName() == null ? "" : user.getLastName().trim();
        String fullName = (firstName + " " + lastName).trim();
        return fullName.isEmpty() ? user.getUserName() : fullName;
    }

    public static String specialty(User employee) {
        return employee.getCareerSkills() == null || employee.getCareerSkills().isEmpty()
                ? null
                : employee.getCareerSkills().get(0);
    }
}
