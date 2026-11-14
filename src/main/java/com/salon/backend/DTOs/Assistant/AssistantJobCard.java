package com.salon.backend.DTOs.Assistant;

import java.util.List;

public record AssistantJobCard(
        Long id,
        String title,
        Long salonId,
        String salonName,
        String location,
        List<String> skills,
        Integer positions,
        Integer matchScore
) {
}
