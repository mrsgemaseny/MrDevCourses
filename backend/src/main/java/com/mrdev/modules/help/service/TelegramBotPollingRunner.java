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
public class TelegramBotPollingRunner {

    private final TelegramBotCommandService commandService;
    private final TelegramNotificationService notificationService;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String botToken;
    private final String authorizedChatId;
    private final boolean pollingEnabled;
    private final String webhookBaseUrl;
    private long lastUpdateId = 0;
    private boolean webhookCleared = false;

    public TelegramBotPollingRunner(
            TelegramBotCommandService commandService,
            TelegramNotificationService notificationService,
            RestTemplateBuilder restTemplateBuilder,
            ObjectMapper objectMapper,
            @Value("${app.telegram.bot-token:}") String botToken,
            @Value("${app.telegram.chat-id:}") String authorizedChatId,
            @Value("${app.telegram.polling-enabled:true}") boolean pollingEnabled,
            @Value("${app.telegram.webhook-base-url:https://mrdevcourses.onrender.com}") String webhookBaseUrl) {
        this.commandService = commandService;
        this.notificationService = notificationService;
        this.objectMapper = objectMapper;
        this.botToken = botToken != null ? botToken.trim() : "";
        this.authorizedChatId = authorizedChatId != null ? authorizedChatId.trim() : "";
        this.pollingEnabled = pollingEnabled;
        this.webhookBaseUrl = webhookBaseUrl != null ? webhookBaseUrl.trim() : "https://mrdevcourses.onrender.com";
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(10))
                .build();

        if (!this.botToken.isBlank()) {
            log.info("Telegram Bot Runner initialized (pollingEnabled={}, mentorChatId={})", this.pollingEnabled, this.authorizedChatId);
        } else {
            log.warn("Telegram Bot Token is blank.");
        }
    }

    @EventListener(ApplicationReadyEvent.class)
    public void initializeTelegramTransport() {
        ensureTransportActive();
    }

    public String ensureTransportActive() {
        if (botToken.isBlank()) {
            return "NO_TOKEN";
        }
        if (pollingEnabled) {
            clearWebhookIfNeeded();
            return "POLLING_ACTIVE";
        } else {
            return registerWebhook();
        }
    }

    public String registerWebhook() {
        if (botToken.isBlank()) {
            return "NO_TOKEN";
        }
        try {
            String cleanBase = webhookBaseUrl.endsWith("/") ? webhookBaseUrl.substring(0, webhookBaseUrl.length() - 1) : webhookBaseUrl;
            String targetWebhook = cleanBase + "/api/v1/telegram/webhook";
            String setWebhookUrl = "https://api.telegram.org/bot" + botToken + "/setWebhook?url=" + targetWebhook;
            String resp = restTemplate.getForObject(setWebhookUrl, String.class);
            log.info("Telegram webhook registered to {}: {}", targetWebhook, resp);
            return "WEBHOOK_REGISTERED: " + targetWebhook;
        } catch (Exception e) {
            log.warn("Could not register Telegram webhook on startup: {}", e.getMessage());
            return "WEBHOOK_ERROR: " + e.getMessage();
        }
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
        if (botToken.isBlank() || !pollingEnabled) {
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

    @Scheduled(fixedDelay = 600000, initialDelay = 60000)
    public void keepAliveSelfPing() {
        if (webhookBaseUrl == null || webhookBaseUrl.isBlank() || webhookBaseUrl.contains("localhost")) {
            return;
        }
        try {
            String cleanBase = webhookBaseUrl.endsWith("/") ? webhookBaseUrl.substring(0, webhookBaseUrl.length() - 1) : webhookBaseUrl;
            restTemplate.getForObject(cleanBase + "/api/actuator/health", String.class);
        } catch (Exception ignored) {
        }
    }
}
