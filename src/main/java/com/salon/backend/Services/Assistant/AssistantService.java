package com.salon.backend.Services.Assistant;

import com.salon.backend.DTOs.ApiResponse;
import com.salon.backend.DTOs.Assistant.AssistantCandidateCard;
import com.salon.backend.DTOs.Assistant.AssistantChatRequest;
import com.salon.backend.DTOs.Assistant.AssistantChatResponse;
import com.salon.backend.DTOs.Assistant.AssistantGovernorateRanking;
import com.salon.backend.DTOs.Assistant.AssistantInsightsResponse;
import com.salon.backend.DTOs.Assistant.AssistantJobCard;
import com.salon.backend.DTOs.Assistant.AssistantLink;
import com.salon.backend.DTOs.Assistant.AssistantOpportunity;
import com.salon.backend.DTOs.Assistant.AssistantSalonCard;
import com.salon.backend.DTOs.Assistant.AssistantTurn;
import com.salon.backend.Entities.users.User;
import com.salon.backend.Entities.users.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The Salonhub assistant ("Lamsa"). It finds the intent, loads the matching
 * real records, and lets Claude (when configured) or the built-in guide
 * phrase the reply. The server keeps no conversation.
 */
@Service
@RequiredArgsConstructor
public class AssistantService {

    static final int MAX_MESSAGE_LENGTH = 500;
    static final int MAX_TURN_LENGTH = 2000;
    static final int MAX_HISTORY = 12;

    private final AssistantDataService data;
    private final ClaudeReplyWriter claude;
    private final AssistantRateLimiter rateLimiter;

    public ApiResponse<AssistantChatResponse> chat(AssistantChatRequest request, Long userId) {
        String message = request == null || request.message() == null ? "" : request.message().trim();
        if (message.isEmpty()) {
            return ApiResponse.error("Write a message first");
        }
        if (message.length() > MAX_MESSAGE_LENGTH) {
            return ApiResponse.error("Keep the message under " + MAX_MESSAGE_LENGTH + " characters");
        }

        Optional<User> user = data.findUser(userId);
        AssistantReplyContext.Viewer viewer = user.map(AssistantService::viewerOf)
                .orElse(AssistantReplyContext.Viewer.guest());
        AssistantIntent intent = AssistantIntentDetector.detect(message);
        Optional<JordanGovernorate> governorate = JordanGovernorate.mentionedIn(message);

        List<AssistantSalonCard> salons = List.of();
        List<AssistantJobCard> jobs = List.of();
        List<AssistantCandidateCard> candidates = List.of();
        List<AssistantGovernorateRanking> rankings = List.of();
        Optional<AssistantSalonCard> ownedSalon = Optional.empty();
        List<AssistantLink> links = new ArrayList<>();

        switch (intent) {
            case TOP_SALONS -> {
                if (governorate.isPresent()) {
                    salons = data.rankedSalons(governorate).stream().limit(5).toList();
                } else {
                    rankings = data.rankingsByGovernorate(3);
                }
                links.add(new AssistantLink("All salons", "/#discover", "Browse every salon"));
            }
            case FIND_SALONS -> {
                salons = data.rankedSalons(governorate).stream().limit(6).toList();
                links.add(new AssistantLink("All salons", "/#discover", "Browse every salon"));
            }
            case JOBS -> {
                jobs = data.openJobs(viewer.id(), governorate, 5);
                salons = data.salonsWithOpenSeats(governorate, 3);
                links.add(new AssistantLink("Hiring posts", "/hiring-posts", "Every open job"));
                if (!viewer.isGuest() && !viewer.careerProfile()) {
                    links.add(new AssistantLink("Career profile", "/profile/career", "Get jobs ranked for you"));
                }
            }
            case HIRING -> {
                if (viewer.isOwner() && user.isPresent()) {
                    ownedSalon = data.ownedSalon(user.get());
                    candidates = data.candidatesFor(user.get(), 5);
                    links.add(new AssistantLink("Create a hiring post", "/owner/create-hiring-post",
                            "Let people apply"));
                    links.add(new AssistantLink("Applications", "/owner/applications", "Review applicants"));
                } else {
                    links.add(new AssistantLink("Hiring posts", "/hiring-posts", "Open salon jobs"));
                }
            }
            default -> {
            }
        }

        Optional<AssistantKnowledge.Topic> topic = intent == AssistantIntent.APP_HELP
                || intent == AssistantIntent.UNKNOWN
                ? AssistantKnowledge.bestTopic(message)
                : Optional.empty();
        topic.ifPresent(found -> links.add(0, found.link()));

        AssistantReplyContext context = new AssistantReplyContext(
                message, cleanHistory(request.history()), intent, governorate, viewer,
                salons, jobs, candidates, rankings, topic, List.copyOf(links), ownedSalon);

        Optional<String> aiReply = claude.isEnabled() && rateLimiter.tryAcquire(viewer.id())
                ? claude.write(context)
                : Optional.empty();

        return ApiResponse.success("Assistant reply", new AssistantChatResponse(
                aiReply.orElseGet(() -> GuideReplyWriter.write(context)),
                intent.name(),
                aiReply.isPresent() ? "ai" : "guide",
                salons,
                jobs,
                candidates,
                rankings,
                context.links(),
                suggestionsFor(intent, viewer, governorate, message)));
    }

    public ApiResponse<AssistantInsightsResponse> insights(Long userId) {
        Optional<User> user = data.findUser(userId);
        AssistantReplyContext.Viewer viewer = user.map(AssistantService::viewerOf)
                .orElse(AssistantReplyContext.Viewer.guest());
        return ApiResponse.success("Assistant insights", new AssistantInsightsResponse(
                data.rankingsByGovernorate(3),
                opportunityFor(viewer, user)));
    }

    /** Whether replies are written by Claude (true) or the built-in guide. */
    public boolean aiEnabled() {
        return claude.isEnabled();
    }

    private AssistantOpportunity opportunityFor(AssistantReplyContext.Viewer viewer, Optional<User> user) {
        if (viewer.isOwner() && user.isPresent()) {
            Optional<AssistantSalonCard> salon = data.ownedSalon(user.get());
            if (salon.isEmpty()) {
                return new AssistantOpportunity("owner", "Open your salon on Salonhub",
                        "Register your salon to get bookings and build your team.",
                        new AssistantLink("Create a salon", "/owner/create-salon", null),
                        List.of(), List.of(), List.of());
            }
            Long seats = salon.get().openSeats();
            String summary = seats != null && seats > 0
                    ? "Your team has " + seats + (seats == 1 ? " free seat" : " free seats")
                    + ". These members are open to work."
                    : "Your team is full. These members are open to work if you grow.";
            return new AssistantOpportunity("owner", "Grow your team", summary,
                    new AssistantLink("Create a hiring post", "/owner/create-hiring-post", null),
                    List.of(), List.of(salon.get()), data.candidatesFor(user.get(), 4));
        }
        List<AssistantJobCard> jobs = data.openJobs(viewer.id(), Optional.empty(), 4);
        List<AssistantSalonCard> seats = data.salonsWithOpenSeats(Optional.empty(), 3);
        boolean seeker = viewer.careerProfile() || "Employee".equals(viewer.role());
        String title = seeker ? "Jobs picked for you" : "Salons hiring now";
        String summary = viewer.careerProfile()
                ? "Ranked by how well they match your career profile."
                : "Open jobs and salons with free seats in their team.";
        AssistantLink action = viewer.isGuest() || viewer.careerProfile()
                ? new AssistantLink("See all jobs", "/hiring-posts", null)
                : new AssistantLink("Turn on career profile", "/profile/career", null);
        return new AssistantOpportunity(seeker ? "seeker" : "everyone", title, summary, action,
                jobs, seats, List.of());
    }

    static List<String> suggestionsFor(AssistantIntent intent, AssistantReplyContext.Viewer viewer,
                                       Optional<JordanGovernorate> governorate, String message) {
        String place = governorate.map(JordanGovernorate::getDisplayName).orElse("Amman");
        List<String> suggestions = new ArrayList<>();
        switch (intent) {
            case TOP_SALONS, FIND_SALONS -> {
                suggestions.add("Best salons in " + (place.equals("Amman") ? "Irbid" : "Amman"));
                suggestions.add("How do I book?");
            }
            case JOBS -> {
                suggestions.add("Jobs in " + place);
                suggestions.add("How do I apply?");
            }
            case HIRING -> {
                suggestions.add("How do I create a hiring post?");
                suggestions.add("Best salons in " + place);
            }
            default -> {
                suggestions.add("Top rated salons");
                suggestions.add("Best salons in " + place);
            }
        }
        suggestions.add(viewer.isOwner() ? "Find employees for my salon" : "Open jobs for me");
        // Never offer the question the user just asked.
        return suggestions.stream()
                .distinct()
                .filter(suggestion -> !normalized(suggestion).equals(normalized(message)))
                .limit(3)
                .toList();
    }

    private static String normalized(String text) {
        return text == null ? "" : JordanGovernorate.normalize(text);
    }

    static List<AssistantTurn> cleanHistory(List<AssistantTurn> history) {
        if (history == null) {
            return List.of();
        }
        List<AssistantTurn> valid = history.stream()
                .filter(turn -> turn != null && turn.text() != null && !turn.text().isBlank())
                .filter(turn -> "user".equals(turn.role()) || "assistant".equals(turn.role()))
                .toList();
        return valid.subList(Math.max(0, valid.size() - MAX_HISTORY), valid.size()).stream()
                .map(turn -> new AssistantTurn(turn.role(), turn.text().length() > MAX_TURN_LENGTH
                        ? turn.text().substring(0, MAX_TURN_LENGTH)
                        : turn.text()))
                .toList();
    }

    private static AssistantReplyContext.Viewer viewerOf(User user) {
        UserRole role = user.getRole();
        return new AssistantReplyContext.Viewer(
                user.getId(),
                user.getFirstName(),
                role == null ? "USER" : role.name(),
                user.isCareerProfileEnabled());
    }
}
