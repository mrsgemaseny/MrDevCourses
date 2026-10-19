package com.mrdev.modules.lesson.service;

import com.mrdev.common.exception.LessonLockedException;
import com.mrdev.modules.audit.service.AuditService;
import com.mrdev.modules.auth.model.Role;
import com.mrdev.modules.auth.model.User;
import com.mrdev.modules.auth.repository.UserRepository;
import com.mrdev.modules.course.model.Course;
import com.mrdev.modules.course.model.Enrollment;
import com.mrdev.modules.course.repository.CourseRepository;
import com.mrdev.modules.course.repository.EnrollmentRepository;
import com.mrdev.modules.lesson.dto.LessonDetailDto;
import com.mrdev.modules.lesson.dto.LessonSummaryDto;
import com.mrdev.modules.lesson.model.Lesson;
import com.mrdev.modules.lesson.model.LessonProgress;
import com.mrdev.modules.lesson.repository.LessonMaterialRepository;
import com.mrdev.modules.lesson.repository.LessonProgressRepository;
import com.mrdev.modules.lesson.repository.LessonRepository;
import com.mrdev.modules.quiz.repository.QuizRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LessonServiceTest {

    @Mock
    private LessonRepository lessonRepository;
    @Mock
    private LessonProgressRepository lessonProgressRepository;
    @Mock
    private CourseRepository courseRepository;
    @Mock
    private EnrollmentRepository enrollmentRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private LessonMaterialRepository lessonMaterialRepository;
    @Mock
    private QuizRepository quizRepository;
    @Mock
    private AuditService auditService;

    private LessonService lessonService;

    private Course course;
    private User student;
    private Lesson lesson1;
    private Lesson lesson2;
    private Lesson lesson3;

    @BeforeEach
    void setUp() {
        lessonService = new LessonService(
                lessonRepository,
                lessonProgressRepository,
                courseRepository,
                enrollmentRepository,
                userRepository,
                lessonMaterialRepository,
                quizRepository,
                auditService
        );

        course = Course.builder()
                .id(1L)
                .title("AI Vibe Coding")
                .slug("ai-vibe-coding")
                .active(true)
                .build();

        student = User.builder()
                .id(10L)
                .email("student@test.com")
                .role(Role.STUDENT)
                .build();

        lesson1 = Lesson.builder()
                .id(1L)
                .course(course)
                .title("Day 1 Lesson")
                .dayNumber(1)
                .sortOrder(1)
                .isFreePreview(true)
                .build();

        lesson2 = Lesson.builder()
                .id(2L)
                .course(course)
                .title("Day 2 Lesson")
                .dayNumber(2)
                .sortOrder(2)
                .isFreePreview(false)
                .build();

        lesson3 = Lesson.builder()
                .id(3L)
                .course(course)
                .title("Day 3 Lesson")
                .dayNumber(3)
                .sortOrder(3)
                .isFreePreview(false)
                .build();
    }

    @Test
    @DisplayName("Student can complete Lesson 2 on Day 1 if Lesson 1 is already completed")
    void completeLesson_UnlocksSequentiallyAfterPreviousCompleted() {
        Enrollment enrollment = Enrollment.builder()
                .id(100L)
                .user(student)
                .course(course)
                .enrolledAt(Instant.now())
                .build();

        LessonProgress lesson1Progress = LessonProgress.builder()
                .id(50L)
                .user(student)
                .lesson(lesson1)
                .completedAt(Instant.now())
                .build();

        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
        when(lessonRepository.findByIdAndCourseId(2L, 1L)).thenReturn(Optional.of(lesson2));
        when(userRepository.findById(10L)).thenReturn(Optional.of(student));
        when(enrollmentRepository.findByUserIdAndCourseId(10L, 1L)).thenReturn(Optional.of(enrollment));
        when(lessonRepository.findByCourseIdOrderBySortOrderAscDayNumberAsc(1L))
                .thenReturn(List.of(lesson1, lesson2, lesson3));
        when(lessonProgressRepository.findAllByUserIdAndCourseId(10L, 1L))
                .thenReturn(List.of(lesson1Progress));
        when(lessonProgressRepository.findByUserIdAndLessonId(10L, 2L))
                .thenReturn(Optional.empty());
        when(lessonProgressRepository.save(any(LessonProgress.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        LessonSummaryDto result = lessonService.completeLesson(1L, 2L, 10L, Role.STUDENT);

        assertThat(result.isCompleted()).isTrue();
        assertThat(result.isAccessible()).isTrue();
        assertThat(result.getId()).isEqualTo(2L);
    }

    @Test
    @DisplayName("Student can complete a free preview lesson even if day_number > 1")
    void completeLesson_AllowsFreePreviewLesson() {
        lesson2.setIsFreePreview(true);

        Enrollment enrollment = Enrollment.builder()
                .id(100L)
                .user(student)
                .course(course)
                .enrolledAt(Instant.now())
                .build();

        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
        when(lessonRepository.findByIdAndCourseId(2L, 1L)).thenReturn(Optional.of(lesson2));
        when(userRepository.findById(10L)).thenReturn(Optional.of(student));
        when(enrollmentRepository.findByUserIdAndCourseId(10L, 1L)).thenReturn(Optional.of(enrollment));
        when(lessonRepository.findByCourseIdOrderBySortOrderAscDayNumberAsc(1L))
                .thenReturn(List.of(lesson1, lesson2, lesson3));
        when(lessonProgressRepository.findAllByUserIdAndCourseId(10L, 1L))
                .thenReturn(List.of());
        when(lessonProgressRepository.findByUserIdAndLessonId(10L, 2L))
                .thenReturn(Optional.empty());
        when(lessonProgressRepository.save(any(LessonProgress.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        LessonSummaryDto result = lessonService.completeLesson(1L, 2L, 10L, Role.STUDENT);

        assertThat(result.isCompleted()).isTrue();
        assertThat(result.getId()).isEqualTo(2L);
    }

    @Test
    @DisplayName("Student cannot skip to Lesson 3 on Day 1 if Lesson 2 is not completed")
    void completeLesson_ThrowsLockedWhenPreviousNotCompletedAndFutureDay() {
        Enrollment enrollment = Enrollment.builder()
                .id(100L)
                .user(student)
                .course(course)
                .enrolledAt(Instant.now())
                .build();

        LessonProgress lesson1Progress = LessonProgress.builder()
                .id(50L)
                .user(student)
                .lesson(lesson1)
                .completedAt(Instant.now())
                .build();

        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
        when(lessonRepository.findByIdAndCourseId(3L, 1L)).thenReturn(Optional.of(lesson3));
        when(userRepository.findById(10L)).thenReturn(Optional.of(student));
        when(enrollmentRepository.findByUserIdAndCourseId(10L, 1L)).thenReturn(Optional.of(enrollment));
        when(lessonRepository.findByCourseIdOrderBySortOrderAscDayNumberAsc(1L))
                .thenReturn(List.of(lesson1, lesson2, lesson3));
        when(lessonProgressRepository.findAllByUserIdAndCourseId(10L, 1L))
                .thenReturn(List.of(lesson1Progress));

        assertThatThrownBy(() -> lessonService.completeLesson(1L, 3L, 10L, Role.STUDENT))
                .isInstanceOf(LessonLockedException.class);
    }

    @Test
    @DisplayName("getLessonDetail allows access to Lesson 2 on Day 1 when Lesson 1 is completed")
    void getLessonDetail_UnlocksLesson2AfterLesson1Completed() {
        Enrollment enrollment = Enrollment.builder()
                .id(100L)
                .user(student)
                .course(course)
                .enrolledAt(Instant.now())
                .build();

        LessonProgress lesson1Progress = LessonProgress.builder()
                .id(50L)
                .user(student)
                .lesson(lesson1)
                .completedAt(Instant.now())
                .build();

        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
        when(lessonRepository.findByIdAndCourseId(2L, 1L)).thenReturn(Optional.of(lesson2));
        when(lessonRepository.findByCourseIdOrderBySortOrderAscDayNumberAsc(1L))
                .thenReturn(List.of(lesson1, lesson2, lesson3));
        when(lessonMaterialRepository.findByLessonIdOrderBySortOrderAsc(2L)).thenReturn(List.of());
        when(quizRepository.findByLessonId(2L)).thenReturn(Optional.empty());
        when(enrollmentRepository.findByUserIdAndCourseId(10L, 1L)).thenReturn(Optional.of(enrollment));
        when(lessonProgressRepository.findAllByUserIdAndCourseId(10L, 1L))
                .thenReturn(List.of(lesson1Progress));
        when(lessonProgressRepository.findByUserIdAndLessonId(10L, 2L)).thenReturn(Optional.empty());

        LessonDetailDto detail = lessonService.getLessonDetail(1L, 2L, 10L, Role.STUDENT);

        assertThat(detail.getId()).isEqualTo(2L);
        assertThat(detail.isAccessible()).isTrue();
    }
}
