package br.lcn.ragkb.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import br.lcn.ragkb.dto.AnswerResponse;
import br.lcn.ragkb.dto.ConversationDetailDto;
import br.lcn.ragkb.dto.ConversationSummaryDto;
import br.lcn.ragkb.dto.SourceReferenceDto;
import br.lcn.ragkb.dto.TicketSuggestionDto;
import br.lcn.ragkb.entity.ChatMessage;
import br.lcn.ragkb.entity.Conversation;
import br.lcn.ragkb.repository.ConversationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final RedisChatHistoryService redisChatHistoryService;
    private final ObjectMapper objectMapper;

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
    public ConversationDetailDto getOrCreateConversation(String conversationId, String userId, String initialQuestion) {
        Conversation conversation = resolveConversation(conversationId, userId, initialQuestion);
        return ConversationDetailDto.fromEntity(conversation);
    }

    private Conversation resolveConversation(String conversationId, String userId, String initialQuestion) {
        if (conversationId != null && !conversationId.isBlank()) {
            return conversationRepository.findByIdAndUserId(conversationId, userId)
                    .orElseGet(() -> createNewConversationWithTitle(userId, initialQuestion));
        }
        return createNewConversationWithTitle(userId, initialQuestion);
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
        TicketSuggestionDto suggestion = response.ticketSuggestion();

        ChatMessage assistantMsg = new ChatMessage(conversation, "ASSISTANT", answer, status, sources);
        // Persiste as fontes estruturadas (type/label/url) — sem isso o reload
        // do histórico renderiza apenas labels, sem link para o artigo
        if (response.sources() != null && !response.sources().isEmpty()) {
            assistantMsg.setSourcesJson(serializeSources(response.sources()));
        }
        if (suggestion != null) {
            assistantMsg.setTicketQuestion(suggestion.description());
            assistantMsg.setTicketUserId(suggestion.requester());
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

    private String serializeSources(List<SourceReferenceDto> sources) {
        try {
            return objectMapper.writeValueAsString(sources);
        } catch (JsonProcessingException e) {
            // Falha de serialização não deve abortar a interação — o label
            // já está persistido em chat_message_sources (fallback do reload)
            log.warn("Falha ao serializar fontes estruturadas da conversa", e);
            return null;
        }
    }
}