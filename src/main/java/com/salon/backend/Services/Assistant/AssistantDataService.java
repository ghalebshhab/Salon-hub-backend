package com.salon.backend.Services.Assistant;

import com.salon.backend.DTOs.Assistant.AssistantCandidateCard;
import com.salon.backend.DTOs.Assistant.AssistantGovernorateRanking;
import com.salon.backend.DTOs.Assistant.AssistantJobCard;
import com.salon.backend.DTOs.Assistant.AssistantSalonCard;
import com.salon.backend.DTOs.Salon.HiringPost.HiringPostDiscoveryResponse;
import com.salon.backend.DTOs.Salon.HiringPost.HiringPostSearchRequest;
import com.salon.backend.Entities.salons.Salon;
import com.salon.backend.Entities.salons.SalonOpenningStatus;
import com.salon.backend.Entities.salons.SalonStatus;
import com.salon.backend.Entities.salons.hiringposts.HiringPost;
import com.salon.backend.Entities.salons.hiringposts.HiringPostStatus;
import com.salon.backend.Entities.users.User;
import com.salon.backend.Entities.users.UserRole;
import com.salon.backend.Entities.users.UserStatus;
import com.salon.backend.Repositories.Salon.HiringPostRepo;
import com.salon.backend.Repositories.Salon.SalonRatingProjection;
import com.salon.backend.Repositories.Salon.SalonRepo;
import com.salon.backend.Repositories.Salon.SalonReviewRepo;
import com.salon.backend.Repositories.User.UserRepo;
import com.salon.backend.Services.Salon.HiringPost.HiringPostService;
import com.salon.backend.Services.Salon.SalonOpenStatusResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Reads the real records the assistant shows: rated salons, open jobs,
 * salons with free seats and members open to work.
 */
@Service
@RequiredArgsConstructor
public class AssistantDataService {

    /** Reviews a salon needs before its own average counts more than the mean. */
    static final double RATING_CONFIDENCE = 3.0;

    private final SalonRepo salonRepo;
    private final SalonReviewRepo salonReviewRepo;
    private final HiringPostRepo hiringPostRepo;
    private final HiringPostService hiringPostService;
    private final UserRepo userRepo;
    private final SalonOpenStatusResolver salonOpenStatusResolver;

    /** Visible salons with their rating, best first. */
    @Transactional(readOnly = true)
    public List<AssistantSalonCard> rankedSalons(Optional<JordanGovernorate> governorate) {
        Map<Long, SalonRatingProjection> ratings = salonReviewRepo
                .summarizeRatingsBySalon(UserStatus.Active)
                .stream()
                .collect(Collectors.toMap(SalonRatingProjection::getSalonId, Function.identity()));

        double meanRating = ratings.values().stream()
                .filter(rating -> rating.getAverageRating() != null)
                .mapToDouble(SalonRatingProjection::getAverageRating)
                .average()
                .orElse(0);

        return visibleSalons().stream()
                .filter(salon -> governorate.isEmpty()
                        || JordanGovernorate.fromLocation(salon.getLocation()).equals(governorate))
                .map(salon -> toCard(salon, ratings.get(salon.getId())))
                .sorted(Comparator
                        .comparingDouble((AssistantSalonCard card) -> weightedRating(card, meanRating))
                        .reversed()
                        .thenComparing(AssistantSalonCard::reviewCount, Comparator.reverseOrder())
                        .thenComparing(AssistantSalonCard::name, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    /** The best rated salons of every governorate that has rated salons. */
    @Transactional(readOnly = true)
    public List<AssistantGovernorateRanking> rankingsByGovernorate(int perGovernorate) {
        Map<String, List<AssistantSalonCard>> grouped = new LinkedHashMap<>();
        for (AssistantSalonCard card : rankedSalons(Optional.empty())) {
            if (card.governorate() == null || card.reviewCount() == 0) {
                continue;
            }
            List<AssistantSalonCard> list = grouped.computeIfAbsent(card.governorate(), key -> new ArrayList<>());
            if (list.size() < perGovernorate) {
                list.add(card);
            }
        }
        return grouped.entrySet().stream()
                .map(entry -> new AssistantGovernorateRanking(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparingInt(this::governorateOrder))
                .toList();
    }

    /** Visible salons with fewer team members than their capacity. */
    @Transactional(readOnly = true)
    public List<AssistantSalonCard> salonsWithOpenSeats(Optional<JordanGovernorate> governorate, int limit) {
        return rankedSalons(governorate).stream()
                .filter(card -> card.openSeats() != null && card.openSeats() > 0)
                .limit(limit)
                .toList();
    }

    /**
     * Open hiring posts. Signed-in members get the posts ranked by their
     * career profile; guests get the newest posts.
     */
    @Transactional(readOnly = true)
    public List<AssistantJobCard> openJobs(Long userId, Optional<JordanGovernorate> governorate, int limit) {
        if (userId != null) {
            HiringPostSearchRequest request = new HiringPostSearchRequest();
            request.setSize(50);
            var response = hiringPostService.searchOpenHiringPosts(request, userId);
            if (response.isSuccess() && response.getData() != null) {
                return response.getData().getContent().stream()
                        .map(this::toJobCard)
                        .filter(job -> matchesGovernorate(job.location(), governorate))
                        .limit(limit)
                        .toList();
            }
        }
        LocalDateTime now = LocalDateTime.now();
        return hiringPostRepo.findAllByStatus(HiringPostStatus.Open).stream()
                .filter(post -> post.getExpiresAt() == null || post.getExpiresAt().isAfter(now))
                .sorted(Comparator.comparing(HiringPost::getCreatedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .map(this::toJobCard)
                .filter(job -> matchesGovernorate(job.location(), governorate))
                .limit(limit)
                .toList();
    }

    /** Members with a public career profile, best for the owner's salon first. */
    @Transactional(readOnly = true)
    public List<AssistantCandidateCard> candidatesFor(User owner, int limit) {
        Salon salon = salonRepo.findByOwnerId(owner.getId());
        Long salonId = salon == null ? -1L : salon.getId();
        Optional<JordanGovernorate> place = salon == null
                ? Optional.empty()
                : JordanGovernorate.fromLocation(salon.getLocation());

        return userRepo.searchCareerCandidates(
                        owner.getId(),
                        salonId,
                        UserStatus.Active,
                        List.of(UserRole.USER, UserRole.Employee),
                        "",
                        PageRequest.of(0, 50))
                .getContent()
                .stream()
                .sorted(Comparator
                        .comparing((User user) -> !prefersPlace(user, place))
                        .thenComparing(user -> user.getYearsOfExperience() == null ? 0 : user.getYearsOfExperience(),
                                Comparator.reverseOrder()))
                .limit(limit)
                .map(this::toCandidateCard)
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<User> findUser(Long userId) {
        return userId == null ? Optional.empty() : userRepo.findById(userId);
    }

    @Transactional(readOnly = true)
    public Optional<AssistantSalonCard> ownedSalon(User owner) {
        Salon salon = salonRepo.findByOwnerId(owner.getId());
        if (salon == null) {
            return Optional.empty();
        }
        var summary = salonReviewRepo.summarizeSalonReviews(salon.getId(), UserStatus.Active);
        Double average = summary == null ? null : summary.getAverageRating();
        long count = summary == null || summary.getTotalReviews() == null ? 0 : summary.getTotalReviews();
        return Optional.of(toCard(salon, average, count));
    }

    static double weightedRating(AssistantSalonCard card, double meanRating) {
        if (card.averageRating() == null || card.reviewCount() == 0) {
            return 0;
        }
        double votes = card.reviewCount();
        return (votes / (votes + RATING_CONFIDENCE)) * card.averageRating()
                + (RATING_CONFIDENCE / (votes + RATING_CONFIDENCE)) * meanRating;
    }

    private List<Salon> visibleSalons() {
        return salonRepo.findByStatus(SalonStatus.Accepted).stream()
                .filter(salon -> !salon.isBlockedByAdmin() && !salon.isBlockedByOwner())
                .toList();
    }

    private AssistantSalonCard toCard(Salon salon, SalonRatingProjection rating) {
        return toCard(salon,
                rating == null ? null : rating.getAverageRating(),
                rating == null || rating.getTotalReviews() == null ? 0 : rating.getTotalReviews());
    }

    private AssistantSalonCard toCard(Salon salon, Double average, long count) {
        Long openSeats = null;
        if (salon.getMaxEmployeesNumber() != null) {
            long current = salon.getCurrentEmployeesNumber() == null ? 0 : salon.getCurrentEmployeesNumber();
            openSeats = Math.max(0, salon.getMaxEmployeesNumber() - current);
        }
        return new AssistantSalonCard(
                salon.getId(),
                salon.getName(),
                salon.getLocation(),
                JordanGovernorate.fromLocation(salon.getLocation())
                        .map(JordanGovernorate::getDisplayName)
                        .orElse(null),
                salon.getProfilePictureUrl(),
                average == null ? null : Math.round(average * 10) / 10.0,
                count,
                salonOpenStatusResolver.resolve(salon) == SalonOpenningStatus.Open,
                openSeats);
    }

    private AssistantJobCard toJobCard(HiringPostDiscoveryResponse discovery) {
        var post = discovery.hiringPost();
        return new AssistantJobCard(
                post.getId(),
                post.getTitle(),
                post.getSalonId(),
                post.getSalonName(),
                post.getEmployeeLocation() != null ? post.getEmployeeLocation() : post.getSalonLocation(),
                post.getRequiredSkills() == null ? List.of() : List.copyOf(post.getRequiredSkills()),
                post.getNumOfPositions(),
                discovery.matchScore());
    }

    private AssistantJobCard toJobCard(HiringPost post) {
        Salon salon = post.getSalon();
        return new AssistantJobCard(
                post.getId(),
                post.getTitle(),
                salon == null ? null : salon.getId(),
                salon == null ? null : salon.getName(),
                post.getEmployeeLocation() != null || salon == null ? post.getEmployeeLocation() : salon.getLocation(),
                post.getRequiredSkills() == null ? List.of() : List.copyOf(post.getRequiredSkills()),
                post.getNumOfPositions(),
                null);
    }

    private AssistantCandidateCard toCandidateCard(User user) {
        return new AssistantCandidateCard(
                user.getId(),
                (safe(user.getFirstName()) + " " + safe(user.getLastName())).trim(),
                user.getUserName(),
                user.getCareerSkills() == null ? List.of() : List.copyOf(user.getCareerSkills()),
                user.getYearsOfExperience(),
                user.getPreferredWorkLocations() == null ? List.of() : List.copyOf(user.getPreferredWorkLocations()));
    }

    private static boolean prefersPlace(User user, Optional<JordanGovernorate> place) {
        if (place.isEmpty() || user.getPreferredWorkLocations() == null) {
            return false;
        }
        return user.getPreferredWorkLocations().stream()
                .map(JordanGovernorate::fromLocation)
                .anyMatch(place::equals);
    }

    private static boolean matchesGovernorate(String location, Optional<JordanGovernorate> governorate) {
        return governorate.isEmpty() || JordanGovernorate.fromLocation(location).equals(governorate);
    }

    private int governorateOrder(AssistantGovernorateRanking ranking) {
        for (JordanGovernorate governorate : JordanGovernorate.values()) {
            if (governorate.getDisplayName().equals(ranking.governorate())) {
                return governorate.ordinal();
            }
        }
        return Integer.MAX_VALUE;
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
