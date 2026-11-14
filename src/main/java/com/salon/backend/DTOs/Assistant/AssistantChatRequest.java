package com.salon.backend.DTOs.Assistant;

import java.util.List;

/**
 * A message to the assistant. History holds the earlier turns that the client
 * keeps; the server stores no conversation.
 */
public record AssistantChatRequest(String message, List<AssistantTurn> history, String page) {
}
