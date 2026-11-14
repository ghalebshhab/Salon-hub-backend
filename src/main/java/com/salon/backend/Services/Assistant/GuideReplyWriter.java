package com.salon.backend.Services.Assistant;

import com.salon.backend.DTOs.Assistant.AssistantSalonCard;

import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Writes short replies from fixed sentences and the loaded records. It needs
 * no external service, so the assistant always answers.
 */
public final class GuideReplyWriter {

    private GuideReplyWriter() {
    }

    public static String write(AssistantReplyContext context) {
        String place = context.governorate().map(JordanGovernorate::getDisplayName).orElse(null);
        return switch (context.intent()) {
            case GREETING -> greeting(context);
            case TOP_SALONS -> topSalons(context, place);
            case FIND_SALONS -> findSalons(context, place);
            case JOBS -> jobs(context, place);
            case HIRING -> hiring(context);
            case APP_HELP -> context.topic()
                    .map(AssistantKnowledge.Topic::answer)
                    .orElse(AssistantKnowledge.ABOUT);
            case UNKNOWN -> context.topic()
                    .map(AssistantKnowledge.Topic::answer)
                    .orElse("I can find salons, show the best rated salons in each governorate, "
                            + "suggest jobs or team members, and explain how Salonhub works. "
                            + "Try one of the suggestions below.");
        };
    }

    private static String greeting(AssistantReplyContext context) {
        String name = context.viewer().firstName();
        String hello = name == null || name.isBlank() ? "Welcome! " : "Welcome, " + name + "! ";
        return hello + "I am " + AssistantIdentity.NAME + ", your Salonhub AI assistant. Ask me for the best salons in your "
                + "governorate, open jobs, team members for your salon, or how something works.";
    }

    private static String topSalons(AssistantReplyContext context, String place) {
        if (place == null) {
            if (context.rankings().isEmpty()) {
                return "No salon has reviews yet, so there is no ranking. You can still browse all salons.";
            }
            return "These are the best rated salons in each governorate, based on customer reviews. "
                    + "Name a governorate to see more salons there.";
        }
        var rated = context.salons().stream().filter(salon -> salon.reviewCount() > 0).toList();
        if (rated.isEmpty()) {
            return "No salon in " + place + " has reviews yet. These salons are in " + place + ":";
        }
        AssistantSalonCard best = rated.get(0);
        return "The best rated salon in " + place + " is " + best.name() + " with "
                + stars(best) + ". Here are the top salons there:";
    }

    private static String findSalons(AssistantReplyContext context, String place) {
        if (context.salons().isEmpty()) {
            return place == null
                    ? "There are no approved salons yet."
                    : "I found no approved salons in " + place + " yet. Try a nearby governorate.";
        }
        String where = place == null ? "" : " in " + place;
        return "I found " + count(context.salons().size(), "salon") + where
                + ". The best rated come first. Open one to see services and book.";
    }

    private static String jobs(AssistantReplyContext context, String place) {
        String where = place == null ? "" : " in " + place;
        StringBuilder reply = new StringBuilder();
        if (context.jobs().isEmpty()) {
            reply.append("There are no open hiring posts").append(where).append(" right now.");
        } else {
            reply.append("I found ").append(count(context.jobs().size(), "open job")).append(where);
            reply.append(context.viewer().careerProfile()
                    ? ", ranked by how well they match your career profile."
                    : ".");
        }
        if (!context.salons().isEmpty()) {
            reply.append(" These salons also have free seats in their team: ")
                    .append(context.salons().stream().map(AssistantSalonCard::name)
                            .limit(3).collect(Collectors.joining(", ")))
                    .append('.');
        }
        if (!context.viewer().isGuest() && !context.viewer().careerProfile()) {
            reply.append(" Turn on your career profile to get jobs ranked for you.");
        }
        if (context.viewer().isGuest()) {
            reply.append(" Sign in to apply.");
        }
        return reply.toString();
    }

    private static String hiring(AssistantReplyContext context) {
        if (!context.viewer().isOwner()) {
            return "Hiring is for salon owners. Owners create a hiring post and accept the best "
                    + "applications. If you are looking for work, ask me for open jobs.";
        }
        StringBuilder reply = new StringBuilder();
        context.ownedSalon().ifPresentOrElse(salon -> {
            if (salon.openSeats() != null && salon.openSeats() > 0) {
                reply.append(salon.name()).append(" has ")
                        .append(count(salon.openSeats().intValue(), "free seat"))
                        .append(" in its team. ");
            } else if (salon.openSeats() != null) {
                reply.append(salon.name()).append(" is at full capacity. ");
            }
        }, () -> reply.append("Create your salon first, then you can hire. "));
        if (context.candidates().isEmpty()) {
            reply.append("No member with a career profile is open to work right now. "
                    + "A hiring post lets people apply to you.");
        } else {
            reply.append("These members are open to work")
                    .append(context.candidates().size() > 1 ? ", people near your salon first." : ".");
        }
        return reply.toString();
    }

    private static String stars(AssistantSalonCard salon) {
        return String.format(Locale.ROOT, "%.1f stars from %s", salon.averageRating(),
                count((int) salon.reviewCount(), "review"));
    }

    private static String count(int value, String noun) {
        return value + " " + noun + (value == 1 ? "" : "s");
    }
}
