package br.lcn.ragkb.assistant;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AssistantOrchestrator {

    private final AssistantContextResolver contextResolver;
    private final ReminderAnswerHandler reminderAnswerHandler;
    private final IntegrationAnswerHandler integrationAnswerHandler;
    private final KnowledgeAnswerHandler knowledgeAnswerHandler;

    public AnswerResponse ask(String question, String conversationId, Authentication auth) {
        AssistantContext context = contextResolver.resolve(conversationId, question, auth);
        if (reminderAnswerHandler.matches(question)) {
            return reminderAnswerHandler.handle(question, context);
        }
        return integrationAnswerHandler.handleIfRouted(question, context)
                .orElseGet(() -> knowledgeAnswerHandler.handle(question, context));
    }
}
