package br.lcn.ragkb.assistant;

import java.util.List;

public record AssistantContext(
        List<String> roles,
        String userId,
        String userSector,
        String conversationId) {
}
