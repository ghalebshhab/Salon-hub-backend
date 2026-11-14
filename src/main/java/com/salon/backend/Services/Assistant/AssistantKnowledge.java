package com.salon.backend.Services.Assistant;

import com.salon.backend.DTOs.Assistant.AssistantLink;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * What the assistant knows about Salonhub itself. Each topic has a short,
 * correct answer and the page where the user can do the task.
 */
public final class AssistantKnowledge {

    public record Topic(String title, List<String> keywords, String answer, AssistantLink link) {
    }

    public static final String ABOUT = """
            Salonhub is a salon booking and beauty community platform in Jordan. Customers \
            discover approved salons, book services with a chosen stylist, and review their \
            visits. Salon owners manage their salon, services, team, bookings and hiring. \
            Stylists keep a career profile, apply to hiring posts and manage their own \
            bookings. Everyone can share posts, connect with people and follow salons in the \
            Community.""";

    public static final List<Topic> TOPICS = List.of(
            new Topic("Book an appointment",
                    List.of("book", "booking", "appointment", "reserve", "حجز", "موعد"),
                    "Open a salon, press Book, choose the services and a stylist, then pick a "
                            + "free time. You can follow every booking in My bookings.",
                    new AssistantLink("Find a salon", "/#discover", "Browse approved salons")),
            new Topic("Change or cancel a booking",
                    List.of("cancel", "reschedule", "booking", "bookings", "الغاء", "إلغاء"),
                    "Go to My bookings. A booking that the salon has not finished can be "
                            + "cancelled there, and you can answer a new time the salon proposes.",
                    new AssistantLink("My bookings", "/my-bookings", "Your appointments")),
            new Topic("Write a review",
                    List.of("review", "reviews", "rate", "rating", "stars", "تقييم"),
                    "After a completed booking, open My bookings and rate the visit from 1 to "
                            + "5 stars. Reviews make the salon rankings for each governorate.",
                    new AssistantLink("My bookings", "/my-bookings", "Rate a finished visit")),
            new Topic("Find a job",
                    List.of("job", "jobs", "apply", "hiring", "vacancy", "career", "وظيفة", "وظائف"),
                    "Turn on your career profile with your skills, experience and preferred "
                            + "governorates. Hiring posts are then ranked by how well they match "
                            + "you, and you can apply in one step.",
                    new AssistantLink("Hiring posts", "/hiring-posts", "Open salon jobs")),
            new Topic("Career profile",
                    List.of("career", "profile", "skills", "resume", "cv", "portfolio"),
                    "Your career profile holds your skills, years of experience, preferred work "
                            + "locations, resume and portfolio. Owners see it when they hire.",
                    new AssistantLink("Career profile", "/profile/career", "Edit your skills")),
            new Topic("Hire staff",
                    List.of("hire", "hiring", "staff", "employee", "employees", "post", "recruit", "توظيف"),
                    "Create a hiring post with the skills, experience and number of positions "
                            + "you need. Review the ranked applications and accept the best match. "
                            + "Accepted people join your salon team.",
                    new AssistantLink("Create a hiring post", "/owner/create-hiring-post",
                            "Find new team members")),
            new Topic("Open a salon",
                    List.of("create", "open", "register", "salon", "owner", "approval"),
                    "Create your salon with its contact details, location and hours. An admin "
                            + "reviews it, and after approval it is visible to customers.",
                    new AssistantLink("Create a salon", "/owner/create-salon", "Register your salon")),
            new Topic("Community",
                    List.of("community", "post", "posts", "friends", "follow", "people", "مجتمع"),
                    "In the Community you can share posts, comment, connect with people and "
                            + "follow the salons you like.",
                    new AssistantLink("Community", "/community", "Posts and people")),
            new Topic("Notifications",
                    List.of("notification", "notifications", "alerts", "اشعارات"),
                    "Booking changes, hiring answers and community activity arrive in "
                            + "Notifications. The bell in the header shows the unread count.",
                    new AssistantLink("Notifications", "/notifications", "Your updates")),
            new Topic("Account security",
                    List.of("password", "security", "email", "account", "login", "sign", "كلمة"),
                    "Change your password and check your account in Account security. If you "
                            + "forgot the password, use Forgot password on the sign-in page.",
                    new AssistantLink("Account security", "/account/security", "Password and sign-in"))
    );

    private AssistantKnowledge() {
    }

    /** The topic that shares the most words with the message, if any. */
    public static Optional<Topic> bestTopic(String message) {
        Set<String> words = AssistantIntentDetector.words(message);
        return TOPICS.stream()
                .map(topic -> new Object() {
                    final Topic value = topic;
                    final long score = topic.keywords().stream()
                            .map(JordanGovernorate::normalize)
                            .filter(words::contains)
                            .count();
                })
                .filter(scored -> scored.score > 0)
                .max(Comparator.comparingLong(scored -> scored.score))
                .map(scored -> scored.value);
    }

    /** All topics as plain text, for the language model's instructions. */
    public static String asText() {
        return TOPICS.stream()
                .map(topic -> "- " + topic.title() + ": " + topic.answer()
                        + " (page: " + topic.link().to() + ")")
                .collect(Collectors.joining("\n"));
    }
}
