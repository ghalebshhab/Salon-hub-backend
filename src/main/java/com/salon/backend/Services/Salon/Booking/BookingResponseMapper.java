package com.salon.backend.Services.Salon.Booking;

import com.salon.backend.DTOs.Salon.Booking.BookingRescheduleProposalResponse;
import com.salon.backend.DTOs.Salon.Booking.BookingResponse;
import com.salon.backend.DTOs.Salon.Booking.BookingServiceItemResponse;
import com.salon.backend.Entities.salons.bookings.Booking;
import com.salon.backend.Entities.salons.bookings.BookingStatus;
import com.salon.backend.Entities.salons.bookings.BookingServiceItem;
import com.salon.backend.Entities.salons.bookings.BookingTracking;
import com.salon.backend.Entities.salons.services.SalonService;
import com.salon.backend.Entities.users.User;
import com.salon.backend.Repositories.Salon.SalonServiceRepo;
import com.salon.backend.Services.Payment.PaymentRules;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
public class BookingResponseMapper {

    /** Null in unit tests that build the mapper by hand: names stay English only. */
    private final SalonServiceRepo salonServiceRepo;

    public BookingResponseMapper() {
        this(null);
    }

    @Autowired
    public BookingResponseMapper(SalonServiceRepo salonServiceRepo) {
        this.salonServiceRepo = salonServiceRepo;
    }

    public List<BookingResponse> map(List<Booking> bookings) {
        Map<Long, SalonService> sources = sourceServices(bookings);
        return bookings.stream().map(booking -> map(booking, null, sources)).toList();
    }

    /**
     * Owner lists: adds each customer's earlier no-show count at the salon.
     * The counts come from one grouped query; key is {@link #noShowKey}.
     */
    public List<BookingResponse> map(List<Booking> bookings, Map<String, Long> noShowCounts) {
        Map<Long, SalonService> sources = sourceServices(bookings);
        return bookings.stream()
                .map(booking -> map(booking, noShowCounts.getOrDefault(
                        noShowKey(booking.getCustomer().getId(), booking.getSalon().getId()), 0L), sources))
                .toList();
    }

    public static String noShowKey(Long customerId, Long salonId) {
        return customerId + ":" + salonId;
    }

    public BookingResponse map(Booking booking) {
        return map(booking, null);
    }

    public BookingResponse map(Booking booking, Long customerNoShowCount) {
        return map(booking, customerNoShowCount, sourceServices(List.of(booking)));
    }

    /**
     * The salon's current services behind the booked items, in one query, so
     * a booking can show the Arabic service name next to its saved English name.
     */
    private Map<Long, SalonService> sourceServices(Collection<Booking> bookings) {
        if (salonServiceRepo == null || bookings.isEmpty()) {
            return Map.of();
        }
        List<Long> ids = bookings.stream()
                .flatMap(booking -> booking.getServiceItems().stream())
                .map(BookingServiceItem::getSourceServiceId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, SalonService> services = new HashMap<>();
        if (!ids.isEmpty()) {
            salonServiceRepo.findAllById(ids).forEach(service -> services.put(service.getId(), service));
        }
        return services;
    }

    /** The Arabic name only while the service still has the booked English name. */
    private static String arabicName(BookingServiceItem item, Map<Long, SalonService> sources) {
        SalonService source = item.getSourceServiceId() == null ? null : sources.get(item.getSourceServiceId());
        return source != null && Objects.equals(source.getName(), item.getServiceName())
                ? source.getNameAr()
                : null;
    }

    private BookingResponse map(Booking booking, Long customerNoShowCount, Map<Long, SalonService> sources) {
        User customer = booking.getCustomer();
        User employee = booking.getAssignedEmployee();
        BookingTracking tracking = booking.getTracking();
        List<BookingServiceItemResponse> services = booking.getServiceItems().stream()
                .map(item -> new BookingServiceItemResponse(
                        item.getId(),
                        item.getSourceServiceId(),
                        item.getServiceName(),
                        arabicName(item, sources),
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
