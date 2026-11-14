package br.lcn.ragkb.conversation.api;

import java.util.List;

import br.lcn.ragkb.conversation.api.InteractionRecord;
import br.lcn.ragkb.conversation.api.RedisChatMessageDto;

public interface ConversationApi {
    String getOrCreateConversationId(String conversationId, String userId, String initialQuestion);

    void recordInteraction(String conversationId, String userQuestion, InteractionRecord response);

    void addHistoryMessage(String conversationId, String sender, String content);

    List<RedisChatMessageDto> getHistory(String conversationId);

    String getFormattedHistory(String conversationId);
}
