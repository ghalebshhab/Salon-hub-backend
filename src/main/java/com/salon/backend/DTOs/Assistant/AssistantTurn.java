package com.salon.backend.DTOs.Assistant;

/** One earlier message of the conversation: role is "user" or "assistant". */
public record AssistantTurn(String role, String text) {
}
