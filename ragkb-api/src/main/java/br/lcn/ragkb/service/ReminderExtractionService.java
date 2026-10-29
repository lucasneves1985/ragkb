package br.lcn.ragkb.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Optional;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Extrai datetime + resumo do pedido de lembrete em linguagem natural, usando o
 * ChatClient configurado (Groq via OpenAI-compatible) com structured output.
 * Timezone de referência: America/Sao_Paulo.
 *
 * P7 — rede de segurança determinística (lição da V11.1/frente 5): o prompt
 * pede ISO local yyyy-MM-dd'T'HH:mm, mas desvios comuns de LLM (espaço no lugar
 * do 'T', dd/MM/AAAA HH:mm) falhariam no parse estrito e rejeitariam o lembrete
 * inteiro. parseLenient aceita as variações de formato conhecidas; null = não
 * parseou → fail-safe para o usuário, nunca executa com valor duvidoso. Datas
 * relativas continuam resolvidas NA ORIGEM (prompt injeta "hoje é X e agora são
 * Y") — o parsing é segunda linha, não substitui.
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
            LocalDateTime parsed = parseLenient(extraction.datetime().trim());
            if (parsed == null) {
                // Fail-safe: datetime ilegível → usuário informa de novo.
                // (Validação de "não é passado" continua no ReminderService.)
                return Optional.empty();
            }
            return Optional.of(new ExtractionResult(parsed, extraction.summary()));
        } catch (Exception e) {
            log.error("Falha na extração de datetime do lembrete: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Parsing determinístico do datetime devolvido pelo LLM. Ordem: 1. ISO
     * local nativo (caminho esperado — 'T', com ou sem segundos); 2. espaço no
     * lugar do 'T' (o desvio de LLM mais comum); 3. formatos BR comuns
     * (dd/MM/AAAA HH:mm etc.).
     *
     * @return null se NENHUM formato casar — nunca inventa valor.
     */
    private LocalDateTime parseLenient(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(value);
        } catch (DateTimeParseException ignored) {
            // tenta as variações abaixo
        }
        // Espaço no lugar do 'T': "2026-09-25 15:00"
        try {
            return LocalDateTime.parse(value.replace(' ', 'T'));
        } catch (DateTimeParseException ignored) {
            // tenta os formatos abaixo
        }
        DateTimeFormatter[] fallbacks = {
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy'T'HH:mm"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")
        };
        for (DateTimeFormatter f : fallbacks) {
            try {
                return LocalDateTime.parse(value, f);
            } catch (DateTimeParseException ignored) {
                // próximo formato
            }
        }
        log.warn("Datetime do lembrete não parseou em nenhum formato conhecido: '{}'", value);
        return null;
    }

    public record ExtractionResult(java.time.LocalDateTime localDateTime, String summary) {

    }
}
