package com.mrdev.modules.help.service;

import com.mrdev.modules.audit.service.AuditService;
import com.mrdev.modules.auth.model.Role;
import com.mrdev.modules.auth.model.User;
import com.mrdev.modules.auth.repository.UserRepository;
import com.mrdev.modules.course.model.Course;
import com.mrdev.modules.course.repository.CourseRepository;
import com.mrdev.modules.course.repository.EnrollmentRepository;
import com.mrdev.modules.help.dto.CreateHelpRequest;
import com.mrdev.modules.help.dto.HelpRequestDto;
import com.mrdev.modules.help.dto.ResolveHelpRequest;
import com.mrdev.modules.help.model.HelpRequestStatus;
import com.mrdev.modules.help.model.StudentHelpRequest;
import com.mrdev.modules.help.repository.StudentHelpRequestRepository;
import com.mrdev.modules.lesson.model.Lesson;
import com.mrdev.modules.lesson.repository.LessonRepository;
import com.mrdev.modules.ai.rag.service.EmbeddingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StudentHelpServiceTest {

    @Mock
    private StudentHelpRequestRepository helpRequestRepository;

    @Mock
    private LessonRepository lessonRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private TelegramNotificationService telegramNotificationService;

    @Mock
    private com.mrdev.modules.automation.service.EmailNotificationService emailNotificationService;

    @Mock
    private AuditService auditService;

    @Mock
    private EmbeddingService embeddingService;

    @InjectMocks
    private StudentHelpService studentHelpService;

    private User user;
    private Course course;
    private Lesson lesson;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .name("Azamat")
                .email("azamat@test.com")
                .role(Role.STUDENT)
                .build();

        course = Course.builder()
                .id(10L)
                .title("Вайбкодинг: Твой первый сайт")
                .build();

        lesson = Lesson.builder()
                .id(101L)
                .title("Установка и настройка окружения")
                .dayNumber(1)
                .course(course)
                .build();

        lenient().when(embeddingService.generateEmbedding(any())).thenReturn(new float[1536]);
        lenient().when(embeddingService.vectorToString(any())).thenReturn("[]");
    }

    @Test
    @DisplayName("createHelpRequest should persist request, dispatch Telegram alert and log audit")
    void createHelpRequest_Success() {
        CreateHelpRequest request = CreateHelpRequest.builder()
                .stepIdentifier("STEP_2_GIT")
                .stepTitle("Шаг 2: Установка Git и генерация SSH-ключа")
                .problemText("Команда ssh -T git@github.com выдает Permission denied")
                .errorLogs("git@github.com: Permission denied (publickey).")
                .build();

        when(lessonRepository.findByIdAndCourseId(101L, 10L)).thenReturn(Optional.of(lesson));
        when(courseRepository.findById(10L)).thenReturn(Optional.of(course));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(enrollmentRepository.existsByUserIdAndCourseId(1L, 10L)).thenReturn(true);
        when(helpRequestRepository.save(any(StudentHelpRequest.class))).thenAnswer(inv -> {
            StudentHelpRequest r = inv.getArgument(0);
            r.setId(500L);
            return r;
        });

        HelpRequestDto result = studentHelpService.createHelpRequest(10L, 101L, 1L, Role.STUDENT, request);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(500L);
        assertThat(result.getStatus()).isEqualTo(HelpRequestStatus.OPEN);
        assertThat(result.getStepTitle()).isEqualTo("Шаг 2: Установка Git и генерация SSH-ключа");
        assertThat(result.getStudentName()).isEqualTo("Azamat");

        verify(helpRequestRepository).save(any(StudentHelpRequest.class));
        verify(telegramNotificationService).sendHelpAlert(
                eq("Azamat"),
                eq("azamat@test.com"),
                eq("Вайбкодинг: Твой первый сайт"),
                eq("Установка и настройка окружения"),
                eq(1),
                eq("Шаг 2: Установка Git и генерация SSH-ключа"),
                eq("Команда ssh -T git@github.com выдает Permission denied"),
                eq("git@github.com: Permission denied (publickey).")
        );
        verify(auditService).logAction(eq(1L), eq("STUDENT_HELP_REQUESTED"), eq("Lesson"), eq(101L), any(), any());
    }

    @Test
    @DisplayName("resolveHelpRequest should update status and mentor solution")
    void resolveHelpRequest_Success() {
        StudentHelpRequest req = StudentHelpRequest.builder()
                .id(500L)
                .userId(1L)
                .courseId(10L)
                .lessonId(101L)
                .stepIdentifier("STEP_2_GIT")
                .status(HelpRequestStatus.OPEN)
                .build();

        when(helpRequestRepository.findById(500L)).thenReturn(Optional.of(req));
        when(helpRequestRepository.save(any(StudentHelpRequest.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(courseRepository.findById(10L)).thenReturn(Optional.of(course));
        when(lessonRepository.findById(101L)).thenReturn(Optional.of(lesson));

        ResolveHelpRequest resolve = ResolveHelpRequest.builder()
                .status(HelpRequestStatus.RESOLVED)
                .mentorSolution("Добавили открытый ключ id_ed25519.pub в настройки GitHub. Всё заработало.")
                .build();

        HelpRequestDto result = studentHelpService.resolveHelpRequest(500L, 999L, resolve);

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(HelpRequestStatus.RESOLVED);
        assertThat(result.getMentorSolution()).contains("id_ed25519.pub");
        assertThat(result.getResolvedBy()).isEqualTo(999L);
        assertThat(result.getResolvedAt()).isNotNull();

        verify(auditService).logAction(eq(999L), eq("HELP_REQUEST_RESOLVED"), eq("StudentHelpRequest"), eq(500L), any(), any());
    }
}
