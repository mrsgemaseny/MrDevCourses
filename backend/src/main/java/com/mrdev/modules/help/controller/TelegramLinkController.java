package com.mrdev.modules.help.controller;

import com.mrdev.common.dto.ApiResponse;
import com.mrdev.common.util.SecurityUtils;
import com.mrdev.modules.auth.model.User;
import com.mrdev.modules.auth.repository.UserRepository;
import com.mrdev.modules.help.service.TelegramLinkTokenService;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/telegram")
@RequiredArgsConstructor
public class TelegramLinkController {

    private final TelegramLinkTokenService linkTokenService;
    private final UserRepository userRepository;
    private final com.mrdev.modules.help.service.TelegramBotCommandService commandService;
    private final com.mrdev.modules.help.service.TelegramNotificationService notificationService;
    private final com.mrdev.modules.help.service.TelegramBotPollingRunner pollingRunner;

    @Value("${app.telegram.bot-username:MrDevelopersbot}")
    private String botUsername;

    @Value("${app.telegram.bot-token:}")
    private String botToken;

    @Value("${app.telegram.chat-id:}")
    private String mentorChatId;

    @Value("${app.telegram.polling-enabled:true}")
    private boolean pollingEnabled;

    @Data
    @Builder
    public static class TelegramLinkResponse {
        private String token;
        private String botUsername;
        private String linkUrl;
    }

    @GetMapping("/status")
    public ResponseEntity<ApiResponse<java.util.Map<String, Object>>> getBotStatus() {
        String transportState = pollingRunner.ensureTransportActive();
        java.util.Map<String, Object> info = new java.util.LinkedHashMap<>();
        info.put("botUsername", botUsername);
        info.put("tokenConfigured", botToken != null && !botToken.trim().isEmpty());
        info.put("mentorChatIdConfigured", mentorChatId != null && !mentorChatId.trim().isEmpty());
        info.put("pollingEnabled", pollingEnabled);
        info.put("transportState", transportState);
        return ResponseEntity.ok(ApiResponse.success(info));
    }

    @PostMapping("/webhook")
    public ResponseEntity<ApiResponse<String>> handleWebhook(@RequestBody com.fasterxml.jackson.databind.JsonNode update) {
        try {
            com.fasterxml.jackson.databind.JsonNode message = update.path("message");
            if (message.isMissingNode() || message.isNull()) {
                message = update.path("edited_message");
            }
            if (!message.isMissingNode() && !message.isNull()) {
                String chatId = String.valueOf(message.path("chat").path("id").asLong());
                String fromUsername = message.path("from").hasNonNull("username") ? message.path("from").path("username").asText() : null;
                String text = message.path("text").asText("");
                if (!text.isBlank()) {
                    String reply = commandService.processCommand(chatId, text, fromUsername);
                    if (reply != null && !reply.isBlank()) {
                        notificationService.sendDirectMessage(chatId, reply);
                    }
                    return ResponseEntity.ok(ApiResponse.success("processed", reply));
                }
            }
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.success("error", e.getMessage()));
        }
        return ResponseEntity.ok(ApiResponse.success("ignored", null));
    }

    @PostMapping("/link-token")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<TelegramLinkResponse>> generateLinkToken() {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        String token = linkTokenService.createToken(currentUserId);
        String linkUrl = "https://t.me/" + botUsername + "?start=LINK_" + token;

        return ResponseEntity.ok(ApiResponse.success(TelegramLinkResponse.builder()
                .token(token)
                .botUsername(botUsername)
                .linkUrl(linkUrl)
                .build()));
    }

    @DeleteMapping("/unlink")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> unlinkTelegram() {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        userRepository.findById(currentUserId).ifPresent(user -> {
            user.setTelegramChatId(null);
            user.setTelegramUsername(null);
            user.setTelegramLinkedAt(null);
            userRepository.save(user);
        });

        return ResponseEntity.ok(ApiResponse.success(null));
    }
}