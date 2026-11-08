package com.salon.backend.DTOs.Community;

public record CreatePostRequest(String content, String mediaUrl, Long salonId) {}
