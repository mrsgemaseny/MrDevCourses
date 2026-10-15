package com.mrdev.modules.auth.repository;

import com.mrdev.modules.auth.model.Role;
import com.mrdev.modules.auth.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {
    Optional<User> findByEmail(String email);
    Optional<User> findByEmailIgnoreCase(String email);
    Optional<User> findByGoogleId(String googleId);
    Optional<User> findByTelegramChatId(Long telegramChatId);
    Optional<User> findByTelegramUsernameIgnoreCase(String telegramUsername);
    boolean existsByEmail(String email);
    long countByRole(Role role);
    List<User> findAllByRole(Role role);
}
