package com.mrdev.config;

import com.mrdev.modules.auth.model.Role;
import com.mrdev.modules.auth.model.User;
import com.mrdev.modules.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Creates or repairs the bootstrap admin from environment variables on any profile (including prod).
 * Without ADMIN_BOOTSTRAP_PASSWORD nothing happens, so no credentials ever live in source control.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminBootstrap {

    private final UserRepository userRepository;

    @Value("${ADMIN_BOOTSTRAP_EMAIL:admin@mrdev.com}")
    private String email;

    @Value("${ADMIN_BOOTSTRAP_PASSWORD:}")
    private String password;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void ensureAdmin() {
        if (password == null || password.isBlank()) {
            log.info("ADMIN_BOOTSTRAP_PASSWORD is not set, skipping admin bootstrap.");
            return;
        }
        String normalizedEmail = email.trim().toLowerCase();
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        User admin = userRepository.findByEmail(normalizedEmail).orElse(null);
        if (admin == null) {
            userRepository.save(User.builder()
                    .email(normalizedEmail)
                    .name("Администратор платформы")
                    .role(Role.ADMIN)
                    .passwordHash(encoder.encode(password))
                    .createdAt(Instant.now())
                    .build());
            log.info("Bootstrap admin created: {}", normalizedEmail);
            return;
        }
        boolean changed = false;
        if (admin.getRole() != Role.ADMIN) {
            admin.setRole(Role.ADMIN);
            changed = true;
        }
        if (admin.getPasswordHash() == null || admin.getPasswordHash().isBlank()) {
            admin.setPasswordHash(encoder.encode(password));
            changed = true;
        }
        if (changed) {
            userRepository.save(admin);
            log.info("Bootstrap admin repaired: {}", normalizedEmail);
        }
    }
}
