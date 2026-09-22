package br.lcn.ragkb.service;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Estado da coleta de parâmetros (fase 2 da frente 5), por conversa. Mesmo
 * padrão do RedisChatHistoryService: StringRedisTemplate + ObjectMapper. TTL de
 * 5 minutos: coleta abandonada morre sozinha — não há cancelamento explícito na
 * v1 (limitação consciente: qualquer mensagem do usuário enquanto o estado está
 * vivo é tratada como resposta da coleta).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedisIntegrationParamStateService {

    private static final String KEY_PREFIX = "chat:integration-params:";
    private static final Duration STATE_TTL = Duration.ofMinutes(5);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public record PendingParamState(String integrationId, Map<String, String> params) {

    }

    public void save(String conversationId, String integrationId, Map<String, String> params) {
        if (conversationId == null || conversationId.isBlank()) {
            return;
        }
        try {
            String json = objectMapper.writeValueAsString(new PendingParamState(integrationId, params));
            redisTemplate.opsForValue().set(KEY_PREFIX + conversationId, json, STATE_TTL);
        } catch (Exception e) {
            log.error("Erro ao salvar estado de coleta de parâmetros da conversa {}", conversationId, e);
        }
    }

    public Optional<PendingParamState> get(String conversationId) {
        if (conversationId == null || conversationId.isBlank()) {
            return Optional.empty();
        }
        try {
            String json = redisTemplate.opsForValue().get(KEY_PREFIX + conversationId);
            if (json == null || json.isBlank()) {
                return Optional.empty();
            }
            return Optional.of(objectMapper.readValue(json, PendingParamState.class));
        } catch (Exception e) {
            log.error("Erro ao ler estado de coleta de parâmetros da conversa {}", conversationId, e);
            return Optional.empty();
        }
    }

    public void clear(String conversationId) {
        if (conversationId == null || conversationId.isBlank()) {
            return;
        }
        try {
            redisTemplate.delete(KEY_PREFIX + conversationId);
        } catch (Exception e) {
            log.error("Erro ao limpar estado de coleta da conversa {}", conversationId, e);
        }
    }
}
