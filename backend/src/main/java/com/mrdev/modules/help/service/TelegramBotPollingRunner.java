package com.mrdev.modules.help.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Slf4j
@Component
@ConditionalOnProperty(name = "app.telegram.polling-enabled", havingValue = "true", matchIfMissing = true)
public class TelegramBotPollingRunner {

    private final TelegramBotCommandService commandService;
    private final TelegramNotificationService notificationService;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String botToken;
    private final String authorizedChatId;
    private long lastUpdateId = 0;
    private boolean webhookCleared = false;

    public TelegramBotPollingRunner(
            TelegramBotCommandService commandService,
            TelegramNotificationService notificationService,
            RestTemplateBuilder restTemplateBuilder,
            ObjectMapper objectMapper,
            @Value("${app.telegram.bot-token:}") String botToken,
            @Value("${app.telegram.chat-id:}") String authorizedChatId) {
        this.commandService = commandService;
        this.notificationService = notificationService;
        this.objectMapper = objectMapper;
        this.botToken = botToken != null ? botToken.trim() : "";
        this.authorizedChatId = authorizedChatId != null ? authorizedChatId.trim() : "";
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(10))
                .build();

        if (!this.botToken.isBlank()) {
            log.info("Telegram Bot Polling Runner ACTIVE for mentor chatId={} (polling every 3s)", this.authorizedChatId);
        } else {
            log.warn("Telegram Bot Token is blank. Polling is disabled.");
        }
    }

    @EventListener(ApplicationReadyEvent.class)
    public void ensureWebhookDeletedOnStartup() {
        if (botToken.isBlank()) {
            return;
        }
        clearWebhookIfNeeded();
    }

    private void clearWebhookIfNeeded() {
        if (webhookCleared || botToken.isBlank()) {
            return;
        }
        try {
            String deleteWebhookUrl = "https://api.telegram.org/bot" + botToken + "/deleteWebhook?drop_pending_updates=false";
            restTemplate.getForObject(deleteWebhookUrl, String.class);
            webhookCleared = true;
            log.info("Telegram webhook cleared to enable getUpdates long-polling.");
        } catch (Exception e) {
            log.warn("Could not clear Telegram webhook on startup: {}", e.getMessage());
        }
    }

    @Scheduled(fixedDelay = 3000, initialDelay = 3000)
    public void pollTelegramUpdates() {
        if (botToken.isBlank()) {
            return;
        }

        if (!webhookCleared) {
            clearWebhookIfNeeded();
        }

        try {
            String url = "https://api.telegram.org/bot" + botToken + "/getUpdates?offset=" + (lastUpdateId + 1) + "&timeout=2";
            String rawResponse = restTemplate.getForObject(url, String.class);
            if (rawResponse == null) return;

            JsonNode root = objectMapper.readTree(rawResponse);
            if (!root.path("ok").asBoolean(false)) return;

            JsonNode results = root.path("result");
            if (!results.isArray()) return;

            for (JsonNode update : results) {
                long updateId = update.path("update_id").asLong();
                if (updateId > lastUpdateId) {
                    lastUpdateId = updateId;
                }

                JsonNode message = update.path("message");
                if (message.isMissingNode() || message.isNull()) {
                    message = update.path("edited_message");
                }
                if (message.isMissingNode() || message.isNull()) continue;

                String chatId = String.valueOf(message.path("chat").path("id").asLong());
                String fromUsername = message.path("from").hasNonNull("username") ? message.path("from").path("username").asText() : null;
                String text = message.path("text").asText("");

                if (!text.isBlank()) {
                    log.info("Received Telegram message from chatId={} (username={}): {}", chatId, fromUsername, text);
                    String reply = commandService.processCommand(chatId, text, fromUsername);
                    if (reply != null && !reply.isBlank()) {
                        notificationService.sendDirectMessage(chatId, reply);
                    }
                }
            }
        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().contains("409")) {
                webhookCleared = false;
                clearWebhookIfNeeded();
            }
            log.warn("Telegram polling error: {}", e.getMessage());
        }
    }
}
