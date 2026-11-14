package br.lcn.ragkb.assistant;

import java.util.List;

import org.springframework.stereotype.Service;

import br.lcn.ragkb.conversation.api.ConversationApi;
import br.lcn.ragkb.conversation.api.InteractionRecord;
import br.lcn.ragkb.conversation.api.RedisChatMessageDto;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AssistantConversationRecorder {

    private final ConversationApi conversationApi;

    public void record(String conversationId, String userQuestion, AnswerResponse response) {
        conversationApi.recordInteraction(conversationId, userQuestion, toInteractionRecord(response));
        conversationApi.addHistoryMessage(conversationId, "USER", userQuestion);
        conversationApi.addHistoryMessage(conversationId, "ASSISTANT", response.answer());
    }

    public void recordAssistantOnlyInRedis(String conversationId, String userQuestion, AnswerResponse response) {
        conversationApi.recordInteraction(conversationId, userQuestion, toInteractionRecord(response));
        conversationApi.addHistoryMessage(conversationId, "ASSISTANT", response.answer());
    }

    public List<RedisChatMessageDto> getHistory(String conversationId) {
        return conversationApi.getHistory(conversationId);
    }

    public String getFormattedHistory(String conversationId) {
        return conversationApi.getFormattedHistory(conversationId);
    }

    private InteractionRecord toInteractionRecord(AnswerResponse response) {
        List<InteractionRecord.Source> sources = response.sources() == null ? List.of()
                : response.sources().stream()
                        .map(source -> new InteractionRecord.Source(
                                source.type(), source.label(), source.url()))
                        .toList();
        String ticketQuestion = response.ticketSuggestion() == null
                ? null : response.ticketSuggestion().description();
        String ticketUserId = response.ticketSuggestion() == null
                ? null : response.ticketSuggestion().requester();
        return new InteractionRecord("ASSISTANT", response.answer(), response.status(),
                sources, ticketQuestion, ticketUserId);
    }
}
