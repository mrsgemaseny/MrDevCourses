package com.mrdev.modules.help.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class TelegramNotificationService {

    private final RestTemplate restTemplate;
    private final String botToken;
    private final String chatId;
    private final boolean enabled;

    public TelegramNotificationService(
            RestTemplateBuilder restTemplateBuilder,
            @Value("${app.telegram.bot-token:}") String botToken,
            @Value("${app.telegram.chat-id:}") String chatId) {
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(3))
                .setReadTimeout(Duration.ofSeconds(5))
                .build();
        this.botToken = botToken != null ? botToken.trim() : "";
        this.chatId = chatId != null ? chatId.trim() : "";
        this.enabled = !this.botToken.isBlank() && !this.chatId.isBlank();
    }

    @Async
    public void sendHelpAlert(String studentName, String studentEmail, String courseTitle, String lessonTitle,
                              Integer dayNumber, String stepTitle, String problemText, String errorLogs) {
        StringBuilder sb = new StringBuilder();
        sb.append("[SOS] Сигнал со страницы урока\n\n");
        sb.append("Студент: ").append(escapeMarkdown(studentName)).append(" (").append(escapeMarkdown(studentEmail)).append(")\n");
        sb.append("Курс: ").append(escapeMarkdown(courseTitle)).append("\n");
        sb.append("Урок: День ").append(dayNumber != null ? dayNumber : 1).append(" — ").append(escapeMarkdown(lessonTitle)).append("\n");
        sb.append("Шаг / Этап: ").append(escapeMarkdown(stepTitle != null ? stepTitle : "Не указан")).append("\n\n");
        sb.append("Суть проблемы:\n").append(escapeMarkdown(problemText)).append("\n");

        if (errorLogs != null && !errorLogs.isBlank()) {
            sb.append("\n[Логи ошибки]:\n```\n").append(errorLogs.length() > 500 ? errorLogs.substring(0, 500) + "..." : errorLogs).append("\n```\n");
        }

        sendMessage(sb.toString());
    }

    @Async
    public void sendHomeworkAlert(String studentName, String studentEmail, String courseTitle, String lessonTitle,
                                  String repoUrl, String liveDemoUrl, String notes) {
        StringBuilder sb = new StringBuilder();
        sb.append("[HOMEWORK] Новая сдача ДЗ на проверку\n\n");
        sb.append("Студент: ").append(escapeMarkdown(studentName)).append(" (").append(escapeMarkdown(studentEmail)).append(")\n");
        sb.append("Курс: ").append(escapeMarkdown(courseTitle)).append("\n");
        sb.append("Урок: ").append(escapeMarkdown(lessonTitle)).append("\n\n");
        sb.append("Репозиторий: ").append(repoUrl != null ? repoUrl : "Не указан").append("\n");
        sb.append("Live Demo: ").append(liveDemoUrl != null ? liveDemoUrl : "Не указан").append("\n");

        if (notes != null && !notes.isBlank()) {
            sb.append("\nКомментарий: ").append(escapeMarkdown(notes)).append("\n");
        }

        sendMessage(sb.toString());
    }

    @Async
    public void sendMessage(String markdownText) {
        if (!enabled) {
            log.info("[Telegram Disabled] Message preview:\n{}", markdownText);
            return;
        }

        String url = "https://api.telegram.org/bot" + botToken + "/sendMessage";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        try {
            Map<String, Object> body = new HashMap<>();
            body.put("chat_id", chatId);
            body.put("text", markdownText);
            body.put("parse_mode", "Markdown");

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            restTemplate.postForEntity(url, request, String.class);
            log.info("Telegram notification successfully dispatched to chatId={}", chatId);
        } catch (Exception e) {
            log.warn("Failed to dispatch Telegram notification with Markdown, retrying plain text: {}", e.getMessage());
            try {
                Map<String, Object> fallbackBody = new HashMap<>();
                fallbackBody.put("chat_id", chatId);
                fallbackBody.put("text", markdownText);

                HttpEntity<Map<String, Object>> fallbackRequest = new HttpEntity<>(fallbackBody, headers);
                restTemplate.postForEntity(url, fallbackRequest, String.class);
            } catch (Exception ex) {
                log.error("Failed to dispatch Telegram notification in fallback mode: {}", ex.getMessage());
            }
        }
    }

    @Async
    public void sendMentorAlert(String title, String details) {
        String msg = "[ALERT] *" + escapeMarkdown(title) + "*\n\n" + details;
        sendDirectMessage(chatId, msg);
    }

    @Async
    public void sendDirectMessage(String targetChatId, String text) {
        if (botToken.isBlank() || targetChatId == null || targetChatId.isBlank()) {
            log.info("[Telegram Disabled] Direct message to {}: {}", targetChatId, text);
            return;
        }

        String url = "https://api.telegram.org/bot" + botToken + "/sendMessage";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        try {
            Map<String, Object> body = new HashMap<>();
            body.put("chat_id", targetChatId);
            body.put("text", text);
            body.put("parse_mode", "Markdown");

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            restTemplate.postForEntity(url, request, String.class);
        } catch (Exception e) {
            log.warn("Failed to send direct Telegram message with Markdown, retrying plain text: {}", e.getMessage());
            try {
                Map<String, Object> fallbackBody = new HashMap<>();
                fallbackBody.put("chat_id", targetChatId);
                fallbackBody.put("text", text);

                HttpEntity<Map<String, Object>> fallbackRequest = new HttpEntity<>(fallbackBody, headers);
                restTemplate.postForEntity(url, fallbackRequest, String.class);
            } catch (Exception ex) {
                log.error("Failed to send direct Telegram message in fallback mode: {}", ex.getMessage());
            }
        }
    }

    private String escapeMarkdown(String text) {
        if (text == null) return "";
        return text.replace("_", "\\_")
                .replace("*", "\\*")
                .replace("[", "\\[")
                .replace("`", "\\`");
    }
}
