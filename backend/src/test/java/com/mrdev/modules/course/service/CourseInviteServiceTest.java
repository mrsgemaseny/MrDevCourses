package com.mrdev.modules.course.service;

import com.mrdev.common.exception.ApiException;
import com.mrdev.modules.audit.service.AuditService;
import com.mrdev.modules.auth.model.Role;
import com.mrdev.modules.auth.model.User;
import com.mrdev.modules.auth.repository.UserRepository;
import com.mrdev.modules.course.dto.CourseInviteDto;
import com.mrdev.modules.course.dto.CreateCourseInviteRequest;
import com.mrdev.modules.course.dto.RedeemInviteResultDto;
import com.mrdev.modules.course.model.Course;
import com.mrdev.modules.course.model.CourseInvite;
import com.mrdev.modules.course.model.CourseInviteActivation;
import com.mrdev.modules.course.model.Enrollment;
import com.mrdev.modules.course.repository.CohortRepository;
import com.mrdev.modules.course.repository.CourseInviteActivationRepository;
import com.mrdev.modules.course.repository.CourseInviteRepository;
import com.mrdev.modules.course.repository.CourseRepository;
import com.mrdev.modules.course.repository.EnrollmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CourseInviteServiceTest {

    @Mock
    private CourseInviteRepository inviteRepository;

    @Mock
    private CourseInviteActivationRepository activationRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private CohortRepository cohortRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private CourseInviteService inviteService;

    private Course course;
    private User admin;
    private User student;

    @BeforeEach
    void setUp() {
        course = Course.builder()
                .id(1L)
                .title("Вайбкодинг с нуля")
                .slug("vibecoding-zero")
                .active(true)
                .build();

        admin = User.builder()
                .id(100L)
                .email("admin@mrdev.com")
                .role(Role.ADMIN)
                .build();

        student = User.builder()
                .id(200L)
                .email("student@test.com")
                .role(Role.STUDENT)
                .build();
    }

    @Test
    @DisplayName("createInvite should generate unique code, persist invite, and log audit")
    void createInvite_Success() {
        CreateCourseInviteRequest req = CreateCourseInviteRequest.builder()
                .courseId(1L)
                .maxUses(1)
                .expiresInDays(7)
                .note("Kaspi перевод от Султана")
                .build();

        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
        when(userRepository.findById(100L)).thenReturn(Optional.of(admin));
        when(inviteRepository.findByCode(anyString())).thenReturn(Optional.empty());
        when(inviteRepository.save(any(CourseInvite.class))).thenAnswer(inv -> {
            CourseInvite ci = inv.getArgument(0);
            ci.setId(10L);
            return ci;
        });

        CourseInviteDto result = inviteService.createInvite(req, 100L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(10L);
        assertThat(result.getCourseId()).isEqualTo(1L);
        assertThat(result.getCode()).startsWith("VIBECO");
        assertThat(result.getMaxUses()).isEqualTo(1);
        assertThat(result.getNote()).isEqualTo("Kaspi перевод от Султана");
        assertThat(result.isValid()).isTrue();

        verify(inviteRepository).save(any(CourseInvite.class));
        verify(auditService).logAction(eq(100L), eq("COURSE_INVITE_CREATED"), eq("CourseInvite"), eq(10L), any(), any());
    }

    @Test
    @DisplayName("redeemInvite should enroll student, increment count, and log activation")
    void redeemInvite_Success() {
        CourseInvite invite = CourseInvite.builder()
                .id(10L)
                .code("VIBE-A1B2-C3D4")
                .course(course)
                .maxUses(1)
                .usedCount(0)
                .expiresAt(Instant.now().plus(7, ChronoUnit.DAYS))
                .build();

        when(inviteRepository.findByCodeWithCourseAndCohort("VIBE-A1B2-C3D4")).thenReturn(Optional.of(invite));
        when(userRepository.findById(200L)).thenReturn(Optional.of(student));
        when(enrollmentRepository.existsByUserIdAndCourseId(200L, 1L)).thenReturn(false);
        when(activationRepository.existsByInviteIdAndUserId(10L, 200L)).thenReturn(false);

        RedeemInviteResultDto result = inviteService.redeemInvite("vibe-a1b2-c3d4", 200L);

        assertThat(result).isNotNull();
        assertThat(result.getCourseId()).isEqualTo(1L);
        assertThat(result.isAlreadyEnrolled()).isFalse();
        assertThat(result.getMessage()).contains("успешно активирован");
        assertThat(invite.getUsedCount()).isEqualTo(1);

        verify(enrollmentRepository).save(any(Enrollment.class));
        verify(activationRepository).save(any(CourseInviteActivation.class));
        verify(inviteRepository).save(invite);
        verify(auditService).logAction(eq(200L), eq("COURSE_INVITE_REDEEMED"), eq("CourseInvite"), eq(10L), any(), any());
    }

    @Test
    @DisplayName("redeemInvite when already enrolled should return alreadyEnrolled=true without error")
    void redeemInvite_WhenAlreadyEnrolled_ReturnsFriendlyMessage() {
        CourseInvite invite = CourseInvite.builder()
                .id(10L)
                .code("VIBE-A1B2-C3D4")
                .course(course)
                .maxUses(1)
                .usedCount(0)
                .expiresAt(Instant.now().plus(7, ChronoUnit.DAYS))
                .build();

        when(inviteRepository.findByCodeWithCourseAndCohort("VIBE-A1B2-C3D4")).thenReturn(Optional.of(invite));
        when(userRepository.findById(200L)).thenReturn(Optional.of(student));
        when(enrollmentRepository.existsByUserIdAndCourseId(200L, 1L)).thenReturn(true);

        RedeemInviteResultDto result = inviteService.redeemInvite("VIBE-A1B2-C3D4", 200L);

        assertThat(result).isNotNull();
        assertThat(result.isAlreadyEnrolled()).isTrue();
        assertThat(result.getMessage()).contains("Вы уже записаны");

        verify(enrollmentRepository, never()).save(any(Enrollment.class));
    }

    @Test
    @DisplayName("redeemInvite when expired should throw 410 GONE")
    void redeemInvite_WhenExpired_ThrowsGone() {
        CourseInvite invite = CourseInvite.builder()
                .id(10L)
                .code("VIBE-EXPIRED")
                .course(course)
                .maxUses(1)
                .usedCount(0)
                .expiresAt(Instant.now().minus(1, ChronoUnit.DAYS))
                .build();

        when(inviteRepository.findByCodeWithCourseAndCohort("VIBE-EXPIRED")).thenReturn(Optional.of(invite));

        assertThatThrownBy(() -> inviteService.redeemInvite("VIBE-EXPIRED", 200L))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.GONE);
    }

    @Test
    @DisplayName("redeemInvite when exhausted should throw 410 GONE")
    void redeemInvite_WhenExhausted_ThrowsGone() {
        CourseInvite invite = CourseInvite.builder()
                .id(10L)
                .code("VIBE-USED")
                .course(course)
                .maxUses(1)
                .usedCount(1)
                .build();

        when(inviteRepository.findByCodeWithCourseAndCohort("VIBE-USED")).thenReturn(Optional.of(invite));

        assertThatThrownBy(() -> inviteService.redeemInvite("VIBE-USED", 200L))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.GONE);
    }
}
