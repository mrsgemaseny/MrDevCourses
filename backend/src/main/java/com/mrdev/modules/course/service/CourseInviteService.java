package com.mrdev.modules.course.service;

import com.mrdev.common.exception.ApiException;
import com.mrdev.common.exception.ResourceNotFoundException;
import com.mrdev.modules.audit.service.AuditService;
import com.mrdev.modules.auth.model.User;
import com.mrdev.modules.auth.repository.UserRepository;
import com.mrdev.modules.course.dto.CourseInviteDto;
import com.mrdev.modules.course.dto.CreateCourseInviteRequest;
import com.mrdev.modules.course.dto.RedeemInviteResultDto;
import com.mrdev.modules.course.model.Cohort;
import com.mrdev.modules.course.model.Course;
import com.mrdev.modules.course.model.CourseInvite;
import com.mrdev.modules.course.model.CourseInviteActivation;
import com.mrdev.modules.course.model.Enrollment;
import com.mrdev.modules.course.repository.CohortRepository;
import com.mrdev.modules.course.repository.CourseInviteActivationRepository;
import com.mrdev.modules.course.repository.CourseInviteRepository;
import com.mrdev.modules.course.repository.CourseRepository;
import com.mrdev.modules.course.repository.EnrollmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class CourseInviteService {

    private static final String CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final CourseInviteRepository inviteRepository;
    private final CourseInviteActivationRepository activationRepository;
    private final CourseRepository courseRepository;
    private final CohortRepository cohortRepository;
    private final UserRepository userRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final AuditService auditService;

    @Transactional
    public CourseInviteDto createInvite(CreateCourseInviteRequest request, Long adminId) {
        Course course = courseRepository.findById(request.getCourseId())
                .orElseThrow(() -> new ResourceNotFoundException("Course", "id", request.getCourseId()));

        Cohort cohort = null;
        if (request.getCohortId() != null) {
            cohort = cohortRepository.findById(request.getCohortId())
                    .orElseThrow(() -> new ResourceNotFoundException("Cohort", "id", request.getCohortId()));
        }

        User admin = adminId != null ? userRepository.findById(adminId).orElse(null) : null;

        String code = request.getCustomCode() != null && !request.getCustomCode().isBlank()
                ? sanitizeCode(request.getCustomCode())
                : generateUniqueCode(course.getSlug());

        if (inviteRepository.findByCode(code).isPresent()) {
            throw new ApiException("Инвайт-код '" + code + "' уже существует", HttpStatus.CONFLICT);
        }

        Instant expiresAt = null;
        if (request.getExpiresInDays() != null && request.getExpiresInDays() > 0) {
            expiresAt = Instant.now().plus(request.getExpiresInDays(), ChronoUnit.DAYS);
        }

        CourseInvite invite = CourseInvite.builder()
                .code(code)
                .course(course)
                .cohort(cohort)
                .maxUses(request.getMaxUses() != null && request.getMaxUses() > 0 ? request.getMaxUses() : 1)
                .usedCount(0)
                .expiresAt(expiresAt)
                .note(request.getNote())
                .createdBy(admin)
                .build();

        invite = inviteRepository.save(invite);

        if (adminId != null) {
            auditService.logAction(adminId, "COURSE_INVITE_CREATED", "CourseInvite", invite.getId(),
                    "Created invite code " + code + " for course " + course.getTitle(), null);
        }

        return mapToDto(invite);
    }

    @Transactional(readOnly = true)
    public CourseInviteDto getInviteDetails(String code) {
        String cleanCode = sanitizeCode(code);
        CourseInvite invite = inviteRepository.findByCodeWithCourseAndCohort(cleanCode)
                .orElseThrow(() -> new ApiException("Инвайт-ссылка не найдена или не существует", HttpStatus.NOT_FOUND));

        return mapToDto(invite);
    }

    @Transactional
    public RedeemInviteResultDto redeemInvite(String code, Long userId) {
        String cleanCode = sanitizeCode(code);
        CourseInvite invite = inviteRepository.findByCodeWithCourseAndCohort(cleanCode)
                .orElseThrow(() -> new ApiException("Инвайт-ссылка не найдена", HttpStatus.NOT_FOUND));

        if (invite.isExpired()) {
            throw new ApiException("Срок действия этой инвайт-ссылки истек", HttpStatus.GONE);
        }

        if (invite.isExhausted()) {
            throw new ApiException("Лимит активаций по этой инвайт-ссылке исчерпан", HttpStatus.GONE);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        Course course = invite.getCourse();
        Cohort cohort = invite.getCohort();

        boolean alreadyEnrolled = enrollmentRepository.existsByUserIdAndCourseId(userId, course.getId());
        boolean alreadyActivated = activationRepository.existsByInviteIdAndUserId(invite.getId(), userId);

        if (alreadyActivated || alreadyEnrolled) {
            return RedeemInviteResultDto.builder()
                    .courseId(course.getId())
                    .courseTitle(course.getTitle())
                    .courseSlug(course.getSlug())
                    .cohortId(cohort != null ? cohort.getId() : null)
                    .cohortName(cohort != null ? cohort.getName() : null)
                    .enrolledAt(Instant.now())
                    .alreadyEnrolled(true)
                    .message("Вы уже записаны на курс \"" + course.getTitle() + "\"")
                    .build();
        }

        // 1. Create enrollment
        Enrollment enrollment = Enrollment.builder()
                .user(user)
                .course(course)
                .cohort(cohort)
                .enrolledAt(Instant.now())
                .build();
        enrollmentRepository.save(enrollment);

        // 2. Increment usedCount
        invite.setUsedCount(invite.getUsedCount() + 1);
        inviteRepository.save(invite);

        // 3. Record activation
        CourseInviteActivation activation = CourseInviteActivation.builder()
                .invite(invite)
                .user(user)
                .activatedAt(Instant.now())
                .build();
        activationRepository.save(activation);

        auditService.logAction(userId, "COURSE_INVITE_REDEEMED", "CourseInvite", invite.getId(),
                "User " + user.getEmail() + " activated invite " + invite.getCode() + " for course " + course.getTitle(), null);

        log.info("Invite code {} successfully redeemed by user {} for course {}", invite.getCode(), user.getEmail(), course.getTitle());

        return RedeemInviteResultDto.builder()
                .courseId(course.getId())
                .courseTitle(course.getTitle())
                .courseSlug(course.getSlug())
                .cohortId(cohort != null ? cohort.getId() : null)
                .cohortName(cohort != null ? cohort.getName() : null)
                .enrolledAt(enrollment.getEnrolledAt())
                .alreadyEnrolled(false)
                .message("Доступ к курсу \"" + course.getTitle() + "\" успешно активирован")
                .build();
    }

    @Transactional(readOnly = true)
    public List<CourseInviteDto> getAllInvites() {
        return inviteRepository.findAllWithCourseAndCohort().stream()
                .map(this::mapToDto)
                .toList();
    }

    @Transactional
    public void deleteInvite(Long inviteId, Long adminId) {
        CourseInvite invite = inviteRepository.findById(inviteId)
                .orElseThrow(() -> new ResourceNotFoundException("CourseInvite", "id", inviteId));

        inviteRepository.delete(invite);
        if (adminId != null) {
            auditService.logAction(adminId, "COURSE_INVITE_DELETED", "CourseInvite", inviteId,
                    "Deleted invite " + invite.getCode(), null);
        }
    }

    private String generateUniqueCode(String courseSlug) {
        String prefix = (courseSlug != null && !courseSlug.isBlank())
                ? courseSlug.replaceAll("[^a-zA-Z0-9]", "").toUpperCase(Locale.ROOT)
                : "MRDEV";
        if (prefix.length() > 6) {
            prefix = prefix.substring(0, 6);
        }

        for (int attempts = 0; attempts < 10; attempts++) {
            StringBuilder sb = new StringBuilder(prefix).append("-");
            for (int i = 0; i < 4; i++) {
                sb.append(CODE_ALPHABET.charAt(RANDOM.nextInt(CODE_ALPHABET.length())));
            }
            sb.append("-");
            for (int i = 0; i < 4; i++) {
                sb.append(CODE_ALPHABET.charAt(RANDOM.nextInt(CODE_ALPHABET.length())));
            }
            String candidate = sb.toString();
            if (inviteRepository.findByCode(candidate).isEmpty()) {
                return candidate;
            }
        }
        return prefix + "-" + System.currentTimeMillis();
    }

    private String sanitizeCode(String code) {
        if (code == null) return "";
        return code.trim().toUpperCase(Locale.ROOT);
    }

    private CourseInviteDto mapToDto(CourseInvite invite) {
        return CourseInviteDto.builder()
                .id(invite.getId())
                .code(invite.getCode())
                .courseId(invite.getCourse().getId())
                .courseTitle(invite.getCourse().getTitle())
                .courseSlug(invite.getCourse().getSlug())
                .cohortId(invite.getCohort() != null ? invite.getCohort().getId() : null)
                .cohortName(invite.getCohort() != null ? invite.getCohort().getName() : null)
                .maxUses(invite.getMaxUses())
                .usedCount(invite.getUsedCount())
                .expiresAt(invite.getExpiresAt())
                .note(invite.getNote())
                .valid(invite.isValid())
                .expired(invite.isExpired())
                .exhausted(invite.isExhausted())
                .createdAt(invite.getCreatedAt())
                .build();
    }
}
