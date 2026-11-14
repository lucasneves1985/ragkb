package br.lcn.ragkb.assistant;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import br.lcn.ragkb.conversation.api.ConversationApi;
import br.lcn.ragkb.identity.api.UserLookupApi;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AssistantContextResolver {

    private final UserLookupApi userLookupApi;
    private final ConversationApi conversationApi;

    public AssistantContext resolve(String conversationId, String question, Authentication auth) {
        List<String> roles = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
        String userId = auth.getName();
        String userSector = userLookupApi.findSectorByUsername(userId);
        String effectiveConversationId = conversationApi.getOrCreateConversationId(
                conversationId, userId, question);
        return new AssistantContext(roles, userId, userSector, effectiveConversationId);
    }
}
