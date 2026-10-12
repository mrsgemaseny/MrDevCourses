package com.mrdev.modules.admin.service;

import com.mrdev.common.dto.PageResponse;
import com.mrdev.common.exception.ApiException;
import com.mrdev.common.exception.ResourceNotFoundException;
import com.mrdev.modules.admin.dto.StudentDto;
import com.mrdev.modules.admin.dto.StudentProgressDetailDto;
import com.mrdev.modules.audit.service.AuditService;
import com.mrdev.modules.auth.model.Role;
import com.mrdev.modules.auth.model.User;
import com.mrdev.modules.auth.repository.UserRepository;
import com.mrdev.modules.course.dto.EnrollmentDto;
import com.mrdev.modules.course.model.Course;
import com.mrdev.modules.course.model.Enrollment;
import com.mrdev.modules.course.repository.CourseRepository;
import com.mrdev.modules.course.repository.EnrollmentRepository;
import com.mrdev.modules.homework.model.HomeworkSubmission;
import com.mrdev.modules.homework.model.SubmissionStatus;
import com.mrdev.modules.homework.repository.HomeworkSubmissionRepository;
import com.mrdev.modules.lesson.model.Lesson;
import com.mrdev.modules.lesson.model.LessonProgress;
import com.mrdev.modules.lesson.repository.LessonProgressRepository;
import com.mrdev.modules.lesson.repository.LessonRepository;
import com.mrdev.modules.progress.dto.CourseProgressDto;
import com.mrdev.modules.progress.service.ProgressService;
import com.mrdev.modules.quiz.model.Quiz;
import com.mrdev.modules.quiz.model.QuizSubmission;
import com.mrdev.modules.quiz.repository.QuizSubmissionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminStudentServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private LessonRepository lessonRepository;

    @Mock
    private LessonProgressRepository lessonProgressRepository;

    @Mock
    private QuizSubmissionRepository quizSubmissionRepository;

    @Mock
    private HomeworkSubmissionRepository homeworkSubmissionRepository;

    @Mock
    private ProgressService progressService;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private AdminStudentService adminStudentService;

    private User adminUser;
    private User studentUser;
    private Course course;
    private Lesson lesson;

    @BeforeEach
    void setUp() {
        adminUser = User.builder()
                .id(1L)
                .email("admin@test.com")
                .name("Admin User")
                .role(Role.ADMIN)
                .lastActiveDate(LocalDate.now())
                .createdAt(Instant.now())
                .build();

        studentUser = User.builder()
                .id(2L)
                .email("student@test.com")
                .name("Student User")
                .role(Role.STUDENT)
                .lastActiveDate(LocalDate.now())
                .createdAt(Instant.now())
                .build();

        course = Course.builder()
                .id(10L)
                .title("Java Pro Course")
                .slug("java-pro")
                .active(true)
                .createdAt(Instant.now())
                .build();

        lesson = Lesson.builder()
                .id(100L)
                .course(course)
                .title("Day 1: Basics")
                .dayNumber(1)
                .sortOrder(1)
                .build();

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(adminUser.getId(), null, List.of())
        );
    }

    @Test
    @DisplayName("searchStudents with query and role should filter and paginate correctly")
    void searchStudents_WithFilter_ShouldReturnPage() {
        when(userRepository.findAll()).thenReturn(List.of(adminUser, studentUser));
        when(enrollmentRepository.findAllByUserIdsWithCourse(List.of(2L))).thenReturn(List.of());
        when(lessonProgressRepository.findAllByUserIdsWithLesson(List.of(2L))).thenReturn(List.of());

        PageResponse<StudentDto> page = adminStudentService.searchStudents("student", Role.STUDENT, null, 0, 10);

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getEmail()).isEqualTo("student@test.com");
        assertThat(page.getContent().get(0).getCurrentLessonTitle()).isEqualTo("Не начат");
    }

    @Test
    @DisplayName("searchStudents without role filter should return both admins and students")
    void searchStudents_WithoutRoleFilter_ShouldReturnAllUsers() {
        when(userRepository.findAll()).thenReturn(List.of(adminUser, studentUser));
        when(enrollmentRepository.findAllByUserIdsWithCourse(any())).thenReturn(List.of());
        when(lessonProgressRepository.findAllByUserIdsWithLesson(any())).thenReturn(List.of());

        PageResponse<StudentDto> page = adminStudentService.searchStudents(null, null, null, 0, 10);

        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent()).hasSize(2);
    }

    @Test
    @DisplayName("updateStudentRole should successfully promote student to ADMIN and log audit")
    void updateStudentRole_PromoteStudent_ShouldSucceed() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(studentUser));
        when(userRepository.save(any(User.class))).thenReturn(studentUser);
        when(enrollmentRepository.findAllByUserIdWithCourse(2L)).thenReturn(List.of());

        StudentDto result = adminStudentService.updateStudentRole(2L, Role.ADMIN);

        assertThat(result.getRole()).isEqualTo(Role.ADMIN);
        verify(auditService).logAction(eq(1L), eq("ADMIN_UPDATE_ROLE"), eq("User"), eq(2L), contains("Role changed"), isNull());
    }

    @Test
    @DisplayName("updateStudentRole should throw FORBIDDEN on self-demotion")
    void updateStudentRole_SelfDemotion_ShouldThrowForbidden() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));

        assertThatThrownBy(() -> adminStudentService.updateStudentRole(1L, Role.STUDENT))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Cannot demote yourself from admin role");
    }

    @Test
    @DisplayName("updateStudentRole should throw BAD_REQUEST when demoting the last admin")
    void updateStudentRole_LastAdmin_ShouldThrowBadRequest() {
        User otherAdmin = User.builder().id(3L).email("other@test.com").role(Role.ADMIN).build();
        when(userRepository.findById(3L)).thenReturn(Optional.of(otherAdmin));
        when(userRepository.countByRole(Role.ADMIN)).thenReturn(1L);

        assertThatThrownBy(() -> adminStudentService.updateStudentRole(3L, Role.STUDENT))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Cannot demote the last administrator");
    }

    @Test
    @DisplayName("enrollStudentManually should save enrollment and log audit")
    void enrollStudentManually_ShouldSaveAndLog() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(studentUser));
        when(courseRepository.findById(10L)).thenReturn(Optional.of(course));
        when(enrollmentRepository.findByUserIdAndCourseId(2L, 10L)).thenReturn(Optional.empty());

        Enrollment enrollment = Enrollment.builder().id(50L).user(studentUser).course(course).enrolledAt(Instant.now()).build();
        when(enrollmentRepository.save(any(Enrollment.class))).thenReturn(enrollment);

        EnrollmentDto result = adminStudentService.enrollStudentManually(2L, 10L);

        assertThat(result).isNotNull();
        assertThat(result.getCourseId()).isEqualTo(10L);
        assertThat(result.getUserId()).isEqualTo(2L);
        verify(auditService).logAction(eq(1L), eq("ADMIN_MANUAL_ENROLL"), eq("Course"), eq(10L), anyString(), isNull());
    }

    @Test
    @DisplayName("unenrollStudentManually should delete enrollment and log audit")
    void unenrollStudentManually_ShouldDeleteAndLog() {
        Enrollment enrollment = Enrollment.builder().id(50L).user(studentUser).course(course).build();
        when(enrollmentRepository.findByUserIdAndCourseId(2L, 10L)).thenReturn(Optional.of(enrollment));

        adminStudentService.unenrollStudentManually(2L, 10L);

        verify(enrollmentRepository).delete(enrollment);
        verify(auditService).logAction(eq(1L), eq("ADMIN_MANUAL_UNENROLL"), eq("Course"), eq(10L), anyString(), isNull());
    }

    @Test
    @DisplayName("unenrollStudentManually when enrollment not found should throw ResourceNotFoundException")
    void unenrollStudentManually_NotFound_ShouldThrow() {
        when(enrollmentRepository.findByUserIdAndCourseId(2L, 10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminStudentService.unenrollStudentManually(2L, 10L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("getStudentProgress should return comprehensive student progress details")
    void getStudentProgress_ShouldReturnFullDetails() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(studentUser));

        CourseProgressDto courseProgress = CourseProgressDto.builder()
                .courseId(10L)
                .courseTitle("Java Pro Course")
                .totalLessons(30)
                .completedCount(5)
                .progressPercentage(16.7)
                .build();
        when(progressService.getAllProgressForUser(2L)).thenReturn(List.of(courseProgress));

        LessonProgress lp = LessonProgress.builder()
                .id(1L)
                .user(studentUser)
                .lesson(lesson)
                .completedAt(Instant.now())
                .build();
        when(lessonProgressRepository.findAllByUserIdWithLessonAndCourseOrderByCompletedAtDesc(2L))
                .thenReturn(List.of(lp));

        Quiz quiz = Quiz.builder().id(5L).title("Quiz 1").lesson(lesson).build();
        QuizSubmission qs = QuizSubmission.builder()
                .id(1001L)
                .quiz(quiz)
                .user(studentUser)
                .scorePercentage(90)
                .passed(true)
                .startedAt(Instant.now())
                .completedAt(Instant.now())
                .build();
        when(quizSubmissionRepository.findByUserIdOrderByStartedAtDesc(2L))
                .thenReturn(List.of(qs));

        HomeworkSubmission hw = HomeworkSubmission.builder()
                .id(2001L)
                .lessonId(100L)
                .courseId(10L)
                .userId(2L)
                .codeSnippet("System.out.println(\"Hello\");")
                .status(SubmissionStatus.PASSED)
                .score(95)
                .passedTestsCount(5)
                .totalTestsCount(5)
                .createdAt(Instant.now())
                .build();
        when(homeworkSubmissionRepository.findByUserIdOrderByCreatedAtDesc(2L))
                .thenReturn(List.of(hw));
        when(lessonRepository.findAllById(any())).thenReturn(List.of(lesson));

        StudentProgressDetailDto result = adminStudentService.getStudentProgress(2L);

        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(2L);
        assertThat(result.getEmail()).isEqualTo("student@test.com");
        assertThat(result.getEnrolledCourses()).hasSize(1);
        assertThat(result.getCompletedLessons()).hasSize(1);
        assertThat(result.getQuizScores()).hasSize(1);
        assertThat(result.getQuizScores().get(0).getScorePercentage()).isEqualTo(90);
        assertThat(result.getHomeworkSubmissions()).hasSize(1);
        assertThat(result.getHomeworkSubmissions().get(0).getScore()).isEqualTo(95);
    }
}
