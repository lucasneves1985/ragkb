package br.lcn.ragkb.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Extrai datetime + resumo do pedido de lembrete em linguagem natural, usando o
 * ChatClient configurado (Groq via OpenAI-compatible) com structured output.
 * Timezone de referência: America/Sao_Paulo.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReminderExtractionService {

    private record Extraction(String datetime, String summary) {

    }

    private final ChatClient chatClient;

    public Optional<ExtractionResult> extract(String userText) {
        String today = LocalDate.now().toString();
        String time = LocalTime.now().toString();
        try {
            Extraction extraction = chatClient.prompt()
                    .system("""
                            Você extrai data/hora de pedidos de lembrete em português brasileiro.
                            Hoje é %s e agora são %s (timezone America/Sao_Paulo).
                            Datas relativas ("hoje", "amanhã", "próxima segunda") devem ser
                            resolvidas contra esta referência.
                            Responda com: datetime (formato ISO local yyyy-MM-dd'T'HH:mm)
                            e summary (resumo imperativo curto do evento, ex.: "Reunião").
                            Se não conseguir determinar data E hora, devolva datetime vazio.
                            """.formatted(today, time.substring(0, Math.min(time.length(), 5))))
                    .user(userText)
                    .call()
                    .entity(Extraction.class);

            if (extraction == null || extraction.datetime() == null
                    || extraction.datetime().isBlank()) {
                return Optional.empty();
            }
            return Optional.of(new ExtractionResult(
                    java.time.LocalDateTime.parse(extraction.datetime().trim()), extraction.summary()));
        } catch (Exception e) {
            log.error("Falha na extração de datetime do lembrete: {}", e.getMessage());
            return Optional.empty();
        }
    }

    public record ExtractionResult(java.time.LocalDateTime localDateTime, String summary) {

    }
}
