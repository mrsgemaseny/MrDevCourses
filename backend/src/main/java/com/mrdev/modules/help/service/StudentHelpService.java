package com.mrdev.modules.help.service;

import com.mrdev.common.exception.ApiException;
import com.mrdev.common.exception.ResourceNotFoundException;
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
import com.mrdev.modules.automation.service.EmailNotificationService;
import com.mrdev.modules.lesson.model.Lesson;
import com.mrdev.modules.lesson.repository.LessonRepository;
import com.mrdev.modules.ai.rag.service.EmbeddingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StudentHelpService {

    private final StudentHelpRequestRepository helpRequestRepository;
    private final LessonRepository lessonRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final TelegramNotificationService telegramNotificationService;
    private final EmailNotificationService emailNotificationService;
    private final AuditService auditService;
    private final EmbeddingService embeddingService;

    @Transactional
    public HelpRequestDto createHelpRequest(Long courseId, Long lessonId, Long userId, Role role, CreateHelpRequest request) {
        Lesson lesson = lessonRepository.findByIdAndCourseId(lessonId, courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Lesson", "id", lessonId));

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course", "id", courseId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        if (role != Role.ADMIN && !enrollmentRepository.existsByUserIdAndCourseId(userId, courseId)) {
            throw new ApiException("Вы не записаны на этот курс", HttpStatus.FORBIDDEN);
        }

        float[] initialVec = embeddingService.generateEmbedding(request.getProblemText());

        StudentHelpRequest helpRequest = StudentHelpRequest.builder()
                .userId(userId)
                .courseId(courseId)
                .lessonId(lessonId)
                .stepIdentifier(request.getStepIdentifier())
                .stepTitle(request.getStepTitle() != null ? request.getStepTitle() : request.getStepIdentifier())
                .problemText(request.getProblemText())
                .errorLogs(request.getErrorLogs())
                .status(HelpRequestStatus.OPEN)
                .embedding(embeddingService.vectorToString(initialVec))
                .build();

        helpRequest = helpRequestRepository.save(helpRequest);

        // Dual notification: Telegram and Email to mentor
        telegramNotificationService.sendHelpAlert(
                user.getName(),
                user.getEmail(),
                course.getTitle(),
                lesson.getTitle(),
                lesson.getDayNumber(),
                helpRequest.getStepTitle(),
                helpRequest.getProblemText(),
                helpRequest.getErrorLogs()
        );

        emailNotificationService.sendSosMentorAlertEmail(
                user.getName(),
                user.getEmail(),
                course.getTitle(),
                lesson.getTitle(),
                helpRequest.getProblemText(),
                helpRequest.getErrorLogs()
        );

        auditService.logAction(userId, "STUDENT_HELP_REQUESTED", "Lesson", lessonId,
                "SOS request on step: " + helpRequest.getStepTitle() + " - " + helpRequest.getProblemText(), null);

        return mapToDto(helpRequest, user, course, lesson);
    }

    @Transactional(readOnly = true)
    public List<HelpRequestDto> getUserLessonHelpRequests(Long courseId, Long lessonId, Long userId) {
        List<StudentHelpRequest> list = helpRequestRepository.findByUserIdAndLessonIdOrderByCreatedAtDesc(userId, lessonId);
        return enrichList(list);
    }

    @Transactional(readOnly = true)
    public List<HelpRequestDto> getAllHelpRequests(HelpRequestStatus status) {
        List<StudentHelpRequest> list = (status != null)
                ? helpRequestRepository.findByStatusOrderByCreatedAtDesc(status)
                : helpRequestRepository.findAllByOrderByCreatedAtDesc();
        return enrichList(list);
    }

    @Transactional
    public HelpRequestDto resolveHelpRequest(Long requestId, Long adminId, ResolveHelpRequest request) {
        StudentHelpRequest helpRequest = helpRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("StudentHelpRequest", "id", requestId));

        helpRequest.setStatus(request.getStatus());
        helpRequest.setMentorSolution(request.getMentorSolution());
        helpRequest.setResolvedBy(adminId);
        helpRequest.setResolvedAt(Instant.now());

        if (request.getStatus() == HelpRequestStatus.RESOLVED && request.getMentorSolution() != null && !request.getMentorSolution().isBlank()) {
            String combined = helpRequest.getProblemText() + " " + request.getMentorSolution();
            float[] resolvedVec = embeddingService.generateEmbedding(combined);
            helpRequest.setEmbedding(embeddingService.vectorToString(resolvedVec));
        }

        helpRequest = helpRequestRepository.save(helpRequest);

        auditService.logAction(adminId, "HELP_REQUEST_RESOLVED", "StudentHelpRequest", requestId,
                "Resolved help request with status=" + request.getStatus(), null);

        return mapSingle(helpRequest);
    }

    private List<HelpRequestDto> enrichList(List<StudentHelpRequest> list) {
        if (list.isEmpty()) return List.of();

        List<Long> userIds = list.stream().map(StudentHelpRequest::getUserId).distinct().toList();
        List<Long> lessonIds = list.stream().map(StudentHelpRequest::getLessonId).distinct().toList();
        List<Long> courseIds = list.stream().map(StudentHelpRequest::getCourseId).distinct().toList();

        Map<Long, User> userMap = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));
        Map<Long, Lesson> lessonMap = lessonRepository.findAllById(lessonIds).stream()
                .collect(Collectors.toMap(Lesson::getId, l -> l));
        Map<Long, Course> courseMap = courseRepository.findAllById(courseIds).stream()
                .collect(Collectors.toMap(Course::getId, c -> c));

        return list.stream().map(req -> mapToDto(
                req,
                userMap.get(req.getUserId()),
                courseMap.get(req.getCourseId()),
                lessonMap.get(req.getLessonId())
        )).toList();
    }

    private HelpRequestDto mapSingle(StudentHelpRequest req) {
        User user = userRepository.findById(req.getUserId()).orElse(null);
        Course course = courseRepository.findById(req.getCourseId()).orElse(null);
        Lesson lesson = lessonRepository.findById(req.getLessonId()).orElse(null);
        return mapToDto(req, user, course, lesson);
    }

    private HelpRequestDto mapToDto(StudentHelpRequest req, User user, Course course, Lesson lesson) {
        return HelpRequestDto.builder()
                .id(req.getId())
                .userId(req.getUserId())
                .studentName(user != null ? user.getName() : "Unknown")
                .studentEmail(user != null ? user.getEmail() : "")
                .courseId(req.getCourseId())
                .courseTitle(course != null ? course.getTitle() : "")
                .lessonId(req.getLessonId())
                .lessonTitle(lesson != null ? lesson.getTitle() : "")
                .lessonDayNumber(lesson != null ? lesson.getDayNumber() : null)
                .stepIdentifier(req.getStepIdentifier())
                .stepTitle(req.getStepTitle())
                .problemText(req.getProblemText())
                .errorLogs(req.getErrorLogs())
                .status(req.getStatus())
                .mentorSolution(req.getMentorSolution())
                .resolvedBy(req.getResolvedBy())
                .resolvedAt(req.getResolvedAt())
                .createdAt(req.getCreatedAt())
                .build();
    }
}
