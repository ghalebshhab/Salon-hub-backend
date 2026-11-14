package com.salon.backend.DTOs.Assistant;

public record AssistantSalonCard(
        Long id,
        String name,
        String location,
        String governorate,
        String profilePictureUrl,
        Double averageRating,
        long reviewCount,
        boolean open,
        Long openSeats
) {
}
