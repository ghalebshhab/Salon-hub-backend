package com.salon.backend.Services.Salon.HiringPost;

import com.salon.backend.Entities.salons.hiringposts.ApplicationMatchLevel;
import com.salon.backend.Entities.salons.hiringposts.HiringPost;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Compares an applicant (or a career profile) with a hiring post: which
 * skills match, which requirements are missing, the match level and a score
 * from 0 to 100. Pure logic with no database access, so it is easy to test.
 */
final class HiringPostMatcher {

    private HiringPostMatcher() {
    }

    static Result match(
            HiringPost hiringPost,
            LocalDate dateOfBirth,
            List<String> preferredWorkLocations,
            Integer yearsOfExperience,
            List<String> skills,
            boolean resumeUploaded) {

        int age = Period.between(
                dateOfBirth,
                LocalDate.now()
        ).getYears();

        Map<String, String> requiredSkills = toNormalizedSkillMap(
                hiringPost.getRequiredSkills()
        );
        Map<String, String> applicantSkills = toNormalizedSkillMap(
                skills
        );

        List<String> matchedSkills = requiredSkills.entrySet()
                .stream()
                .filter(entry -> applicantSkills.containsKey(entry.getKey()))
                .map(Map.Entry::getValue)
                .toList();

        List<String> missingSkills = requiredSkills.entrySet()
                .stream()
                .filter(entry -> !applicantSkills.containsKey(entry.getKey()))
                .map(Map.Entry::getValue)
                .toList();

        List<String> additionalSkills = applicantSkills.entrySet()
                .stream()
                .filter(entry -> !requiredSkills.containsKey(entry.getKey()))
                .map(Map.Entry::getValue)
                .toList();

        boolean experienceMatched = yearsOfExperience
                >= hiringPost.getMinimumYearsOfExperience();
        boolean ageMatched = age >= hiringPost.getMinimumAge()
                && age <= hiringPost.getMaximumAge();
        boolean locationMatched = containsIgnoreCase(
                preferredWorkLocations,
                hiringPost.getEmployeeLocation()
        );
        boolean allRequiredSkillsMatched = missingSkills.isEmpty();

        List<String> missingRequirements = new ArrayList<>();

        if (!missingSkills.isEmpty()) {
            missingRequirements.add(
                    "Missing required skills: " + String.join(", ", missingSkills)
            );
        }

        if (!experienceMatched) {
            missingRequirements.add(
                            "Needs " + hiringPost.getMinimumYearsOfExperience()
                            + " years of experience; applicant has "
                            + yearsOfExperience
            );
        }

        if (!ageMatched) {
            missingRequirements.add(
                    "Required age is " + hiringPost.getMinimumAge()
                            + " to " + hiringPost.getMaximumAge()
                            + "; applicant age is " + age
            );
        }

        if (!locationMatched) {
            missingRequirements.add(
                            "Required location is " + hiringPost.getEmployeeLocation()
                            + "; applicant preferred locations are "
                            + String.join(", ", preferredWorkLocations)
            );
        }

        boolean allRequirementsMatched = allRequiredSkillsMatched
                && experienceMatched
                && ageMatched
                && locationMatched;

        boolean exceedsExperience = yearsOfExperience
                > hiringPost.getMinimumYearsOfExperience();
        boolean hasAdditionalSkills = !additionalSkills.isEmpty();

        ApplicationMatchLevel level;

        if (allRequirementsMatched && exceedsExperience && hasAdditionalSkills) {
            level = ApplicationMatchLevel.SUGGESTED;
        } else if (allRequirementsMatched) {
            level = ApplicationMatchLevel.MATCHED;
        } else {
            level = ApplicationMatchLevel.MISSING_REQUIREMENTS;
        }

        int score = calculateMatchScore(
                matchedSkills.size(),
                requiredSkills.size(),
                additionalSkills.size(),
                yearsOfExperience,
                hiringPost.getMinimumYearsOfExperience(),
                ageMatched,
                locationMatched,
                resumeUploaded
        );

        return new Result(
                level,
                score,
                matchedSkills,
                additionalSkills,
                missingSkills,
                missingRequirements
        );
    }

    private static boolean containsIgnoreCase(List<String> values, String expectedValue) {
        if (values == null || expectedValue == null) {
            return false;
        }

        return values.stream()
                .filter(Objects::nonNull)
                .anyMatch(value -> value.trim().equalsIgnoreCase(expectedValue.trim()));
    }

    private static int calculateMatchScore(
            int matchedSkillCount,
            int requiredSkillCount,
            int additionalSkillCount,
            int applicantExperience,
            int requiredExperience,
            boolean ageMatched,
            boolean locationMatched,
            boolean resumeUploaded) {

        double skillCoverage = requiredSkillCount == 0
                ? 1
                : (double) matchedSkillCount / requiredSkillCount;
        double experienceCoverage = requiredExperience == 0
                ? 1
                : Math.min(1, (double) applicantExperience / requiredExperience);

        int skillScore = (int) Math.round(skillCoverage * 50);
        int experienceScore = (int) Math.round(experienceCoverage * 25);
        int ageScore = ageMatched ? 10 : 0;
        int locationScore = locationMatched ? 10 : 0;
        int additionalSkillScore = Math.min(5, additionalSkillCount);
        int resumeScore = resumeUploaded ? 5 : 0;

        return Math.min(100, skillScore
                + experienceScore
                + ageScore
                + locationScore
                + additionalSkillScore
                + resumeScore);
    }

    private static Map<String, String> toNormalizedSkillMap(List<String> skills) {

        Map<String, String> normalizedSkills = new LinkedHashMap<>();

        for (String skill : skills) {
            normalizedSkills.putIfAbsent(
                    skill.trim().toLowerCase(Locale.ROOT),
                    skill.trim()
            );
        }

        return normalizedSkills;
    }

    static int matchLevelOrder(ApplicationMatchLevel level) {
        return switch (level) {
            case SUGGESTED -> 0;
            case MATCHED -> 1;
            case MISSING_REQUIREMENTS -> 2;
        };
    }

    record Result(
            ApplicationMatchLevel level,
            Integer score,
            List<String> matchedSkills,
            List<String> additionalSkills,
            List<String> missingSkills,
            List<String> missingRequirements
    ) {
    }
}
