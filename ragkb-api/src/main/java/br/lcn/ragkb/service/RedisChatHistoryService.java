package br.lcn.ragkb.service;

import br.lcn.ragkb.dto.RedisChatMessageDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisChatHistoryService {

    private static final int MAX_MESSAGES = 5;
    private static final String KEY_PREFIX = "chat:history:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public void addMessage(String conversationId, String sender, String content) {
        if (conversationId == null || conversationId.isBlank()) {
            return;
        }
        String key = KEY_PREFIX + conversationId;
        try {
            RedisChatMessageDto dto = new RedisChatMessageDto(sender, content);
            String json = objectMapper.writeValueAsString(dto);
            redisTemplate.opsForList().rightPush(key, json);
            redisTemplate.opsForList().trim(key, -MAX_MESSAGES, -1);
        } catch (Exception e) {
            log.error("Erro ao salvar mensagem no Redis para conversa {}", conversationId, e);
        }
    }

    public List<RedisChatMessageDto> getHistory(String conversationId) {
        if (conversationId == null || conversationId.isBlank()) {
            return List.of();
        }
        String key = KEY_PREFIX + conversationId;
        try {
            List<String> rawMessages = redisTemplate.opsForList().range(key, 0, -1);
            if (rawMessages == null || rawMessages.isEmpty()) {
                return List.of();
            }
            List<RedisChatMessageDto> history = new ArrayList<>();
            for (String json : rawMessages) {
                try {
                    RedisChatMessageDto dto = objectMapper.readValue(json, RedisChatMessageDto.class);
                    history.add(dto);
                } catch (Exception ex) {
                    log.warn("Falha ao desserializar mensagem do Redis: {}", json, ex);
                }
            }
            return history;
        } catch (Exception e) {
            log.error("Erro ao ler histórico do Redis para conversa {}", conversationId, e);
            return List.of();
        }
    }

    public String getFormattedHistory(String conversationId) {
        List<RedisChatMessageDto> history = getHistory(conversationId);
        if (history.isEmpty()) {
            return "";
        }
        return history.stream()
                .map(msg -> String.format("[%s]: %s", msg.sender(), msg.content()))
                .collect(Collectors.joining("\n"));
    }

    public void clearHistory(String conversationId) {
        if (conversationId == null || conversationId.isBlank()) {
            return;
        }
        String key = KEY_PREFIX + conversationId;
        try {
            redisTemplate.delete(key);
        } catch (Exception e) {
            log.error("Erro ao limpar histórico do Redis para conversa {}", conversationId, e);
        }
    }
}
