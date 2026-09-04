package br.lcn.ragkb.service;

import br.lcn.ragkb.dto.AnswerResponse;
import br.lcn.ragkb.dto.ConversationDetailDto;
import br.lcn.ragkb.dto.ConversationSummaryDto;
import br.lcn.ragkb.dto.TicketSuggestion;
import br.lcn.ragkb.entity.ChatMessage;
import br.lcn.ragkb.entity.Conversation;
import br.lcn.ragkb.repository.ConversationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final RedisChatHistoryService redisChatHistoryService;

    @Transactional(readOnly = true)
    public List<ConversationSummaryDto> listUserConversations(String userId) {
        return conversationRepository.findByUserIdOrderByUpdatedAtDesc(userId).stream()
                .map(ConversationSummaryDto::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public ConversationDetailDto getConversation(String id, String userId) {
        Conversation conversation = conversationRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new IllegalArgumentException("Conversa não encontrada: " + id));
        return ConversationDetailDto.fromEntity(conversation);
    }

    @Transactional
    public ConversationDetailDto createConversation(String userId) {
        Conversation conversation = new Conversation(userId, "Nova conversa");
        conversation = conversationRepository.save(conversation);
        return ConversationDetailDto.fromEntity(conversation);
    }

    @Transactional
    public void deleteConversation(String id, String userId) {
        Conversation conversation = conversationRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new IllegalArgumentException("Conversa não encontrada: " + id));
        conversationRepository.delete(conversation);
        redisChatHistoryService.clearHistory(id);
    }

    @Transactional
    public Conversation getOrCreateConversation(String conversationId, String userId, String initialQuestion) {
        if (conversationId != null && !conversationId.isBlank()) {
            return conversationRepository.findByIdAndUserId(conversationId, userId)
                    .orElseGet(() -> createNewConversationWithTitle(userId, initialQuestion));
        } else {
            return createNewConversationWithTitle(userId, initialQuestion);
        }
    }

    private Conversation createNewConversationWithTitle(String userId, String question) {
        String title = question != null && question.length() > 40
                ? question.substring(0, 37) + "..."
                : (question != null && !question.isBlank() ? question : "Nova conversa");
        Conversation conv = new Conversation(userId, title);
        return conversationRepository.save(conv);
    }

    @Transactional
    public void recordInteraction(String conversationId, String userQuestion, AnswerResponse response) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("Conversa não encontrada: " + conversationId));

        // Save user message
        ChatMessage userMsg = new ChatMessage(conversation, "USER", userQuestion, null, List.of());
        conversation.addMessage(userMsg);

        // Save assistant message
        String status = response.status();
        String answer = response.answer();
        List<String> sources = response.sourceIds();
        TicketSuggestion suggestion = response.ticketSuggestion();

        ChatMessage assistantMsg = new ChatMessage(conversation, "ASSISTANT", answer, status, sources);
        if (suggestion != null) {
            assistantMsg.setTicketSector(suggestion.sector());
            assistantMsg.setTicketQuestion(suggestion.question());
            assistantMsg.setTicketUserId(suggestion.userId());
        }
        conversation.addMessage(assistantMsg);

        // If title is default, update title based on question
        if ("Nova conversa".equals(conversation.getTitle()) && userQuestion != null) {
            String title = userQuestion.length() > 40 ? userQuestion.substring(0, 37) + "..." : userQuestion;
            conversation.setTitle(title);
        }

        conversationRepository.save(conversation);
    }

    @Transactional
    public void recordInteraction(Conversation conversation, String userQuestion, AnswerResponse response) {
        recordInteraction(conversation.getId(), userQuestion, response);
    }
}
