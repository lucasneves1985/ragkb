package br.lcn.ragkb.assistant;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import br.lcn.ragkb.integration.api.IntegrationAssistantApi;
import br.lcn.ragkb.integration.api.IntegrationAssistantResult;
import br.lcn.ragkb.ticket.api.TicketSuggestionApi;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class IntegrationAnswerHandler {

    private final IntegrationAssistantApi integrationAssistantApi;
    private final TicketSuggestionApi ticketSuggestionApi;
    private final AssistantConversationRecorder conversationRecorder;

    public Optional<AnswerResponse> handleIfRouted(String question, AssistantContext context) {
        return integrationAssistantApi.handle(question, context.conversationId())
                .map(result -> toResponse(result, question, context));
    }

    private AnswerResponse toResponse(IntegrationAssistantResult result,
            String question, AssistantContext context) {
        String conversationId = context.conversationId();
        if (result.kind() == IntegrationAssistantResult.Kind.PARAM_REQUIRED) {
            AnswerResponse response = new AnswerResponse("PARAM_REQUIRED", result.answer(),
                    List.of(result.integrationName()),
                    List.of(SourceReferenceDto.fromDocument(result.integrationName())),
                    null, conversationId);
            conversationRecorder.recordAssistantOnlyInRedis(
                    conversationId, result.interactionQuestion(), response);
            return response;
        }
        if (result.kind() == IntegrationAssistantResult.Kind.FAILED) {
            var suggestion = ticketSuggestionApi.suggestTicket(question, context.userId(), conversationId,
                    "A consulta à integração '" + result.integrationName()
                    + "' falhou no momento (" + result.answer() + ").");
            AnswerResponse response = AnswerResponse.withTicketSuggestion(
                    suggestion.message(), suggestion.suggestion(), conversationId);
            conversationRecorder.record(conversationId, result.interactionQuestion(), response);
            return response;
        }
        AnswerResponse response = new AnswerResponse("INTEGRATION_RESULT", result.answer(),
                List.of(result.integrationName()),
                List.of(SourceReferenceDto.fromDocument(result.integrationName())),
                null, conversationId);
        conversationRecorder.record(conversationId, result.interactionQuestion(), response);
        return response;
    }
}
