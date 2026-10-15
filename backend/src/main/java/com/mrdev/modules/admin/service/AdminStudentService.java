package com.mrdev.modules.admin.service;

import com.mrdev.common.dto.PageResponse;
import com.mrdev.common.exception.ApiException;
import com.mrdev.common.exception.ResourceNotFoundException;
import com.mrdev.common.util.SecurityUtils;
import com.mrdev.modules.admin.dto.CompletedLessonDto;
import com.mrdev.modules.admin.dto.StudentDto;
import com.mrdev.modules.admin.dto.StudentHomeworkStatusDto;
import com.mrdev.modules.admin.dto.StudentProgressDetailDto;
import com.mrdev.modules.admin.dto.StudentQuizScoreDto;
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
import com.mrdev.modules.homework.repository.HomeworkSubmissionRepository;
import com.mrdev.modules.lesson.model.Lesson;
import com.mrdev.modules.lesson.model.LessonProgress;
import com.mrdev.modules.lesson.repository.LessonProgressRepository;
import com.mrdev.modules.lesson.repository.LessonRepository;
import com.mrdev.modules.progress.dto.CourseProgressDto;
import com.mrdev.modules.progress.service.ProgressService;
import com.mrdev.modules.quiz.model.QuizSubmission;
import com.mrdev.modules.quiz.repository.QuizSubmissionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminStudentService {

    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final LessonRepository lessonRepository;
    private final LessonProgressRepository lessonProgressRepository;
    private final QuizSubmissionRepository quizSubmissionRepository;
    private final HomeworkSubmissionRepository homeworkSubmissionRepository;
    private final ProgressService progressService;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public PageResponse<StudentDto> searchStudents(String query, Role role, Long courseId, int page, int size) {
        int safePage = Math.max(0, page);
        int safeSize = size <= 0 ? 20 : size;
        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        Specification<User> spec = (root, cq, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (role != null) {
                predicates.add(cb.equal(root.get("role"), role));
            }

            if (courseId != null) {
                Subquery<Long> subquery = cq.subquery(Long.class);
                Root<Enrollment> enrollmentRoot = subquery.from(Enrollment.class);
                subquery.select(enrollmentRoot.get("user").get("id"))
                        .where(cb.equal(enrollmentRoot.get("course").get("id"), courseId));
                predicates.add(root.get("id").in(subquery));
            }

            if (query != null && !query.trim().isEmpty()) {
                String pattern = "%" + query.trim().toLowerCase() + "%";
                Predicate matchEmail = cb.like(cb.lower(root.get("email")), pattern);
                Predicate matchName = cb.like(cb.lower(root.get("name")), pattern);
                Predicate matchTg = cb.like(cb.lower(root.get("telegramUsername")), pattern);
                Predicate matchGh = cb.like(cb.lower(root.get("githubUsername")), pattern);
                predicates.add(cb.or(matchEmail, matchName, matchTg, matchGh));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<User> userPage = userRepository.findAll(spec, pageable);
        List<User> pageUsers = userPage.getContent();
        long totalElements = userPage.getTotalElements();

        if (pageUsers.isEmpty()) {
            return PageResponse.of(List.of(), safePage, safeSize, totalElements);
        }

        // Batch fetch enrollments and lesson progress for page users
        List<Long> pageUserIds = pageUsers.stream().map(User::getId).toList();
        List<Enrollment> enrollments = enrollmentRepository.findAllByUserIdsWithCourse(pageUserIds);

        Map<Long, List<Enrollment>> enrollmentsByUser = enrollments.stream()
                .collect(Collectors.groupingBy(e -> e.getUser().getId()));

        List<LessonProgress> lessonProgressList = lessonProgressRepository.findAllByUserIdsWithLesson(pageUserIds);

        Map<Long, List<LessonProgress>> progressByUser = lessonProgressList.stream()
                .collect(Collectors.groupingBy(lp -> lp.getUser().getId()));

        List<StudentDto> dtoList = pageUsers.stream()
                .map(user -> toStudentDto(
                        user,
                        enrollmentsByUser.getOrDefault(user.getId(), List.of()),
                        progressByUser.getOrDefault(user.getId(), List.of())
                ))
                .toList();

        return PageResponse.of(dtoList, safePage, safeSize, totalElements);
    }

    @Transactional
    public StudentDto updateStudentRole(Long targetUserId, Role newRole) {
        User user = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", targetUserId));

        Long currentAdminId = SecurityUtils.getCurrentUserIdOptional().orElse(null);

        // Self-demotion guard
        if (currentAdminId != null && currentAdminId.equals(targetUserId)
                && user.getRole() == Role.ADMIN && newRole == Role.STUDENT) {
            throw new ApiException("Cannot demote yourself from admin role", HttpStatus.FORBIDDEN);
        }

        // Last admin protection
        if (user.getRole() == Role.ADMIN && newRole != Role.ADMIN) {
            long adminCount = userRepository.countByRole(Role.ADMIN);
            if (adminCount <= 1) {
                throw new ApiException("Cannot demote the last administrator in the platform", HttpStatus.BAD_REQUEST);
            }
        }

        Role oldRole = user.getRole();
        if (oldRole != newRole) {
            user.setRole(newRole);
            User saved = userRepository.save(user);
            log.info("Admin ID {} updated user ID {} role from {} to {}", currentAdminId, targetUserId, oldRole, newRole);
            auditService.logAction(
                    currentAdminId,
                    "ADMIN_UPDATE_ROLE",
                    "User",
                    targetUserId,
                    "Role changed from " + oldRole + " to " + newRole,
                    null
            );
            List<Enrollment> userEnrollments = enrollmentRepository.findAllByUserIdWithCourse(targetUserId);
            return toStudentDto(saved, userEnrollments);
        }

        List<Enrollment> userEnrollments = enrollmentRepository.findAllByUserIdWithCourse(targetUserId);
        return toStudentDto(user, userEnrollments);
    }

    @Transactional
    public EnrollmentDto enrollStudentManually(Long userId, Long courseId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course", "id", courseId));

        Optional<Enrollment> existing = enrollmentRepository.findByUserIdAndCourseId(userId, courseId);
        if (existing.isPresent()) {
            return toEnrollmentDto(existing.get());
        }

        Enrollment enrollment = Enrollment.builder()
                .user(user)
                .course(course)
                .enrolledAt(Instant.now())
                .build();

        Enrollment saved = enrollmentRepository.save(enrollment);
        Long adminId = SecurityUtils.getCurrentUserIdOptional().orElse(userId);
        log.info("Admin ID {} manually enrolled user ID {} into course ID {}", adminId, userId, courseId);
        auditService.logAction(
                adminId,
                "ADMIN_MANUAL_ENROLL",
                "Course",
                courseId,
                "Enrolled user ID " + userId + " (" + user.getEmail() + ") into course: " + course.getTitle(),
                null
        );

        return toEnrollmentDto(saved);
    }

    @Transactional
    public void unenrollStudentManually(Long userId, Long courseId) {
        Enrollment enrollment = enrollmentRepository.findByUserIdAndCourseId(userId, courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment", "userId: " + userId + ", courseId", courseId));

        enrollmentRepository.delete(enrollment);
        Long adminId = SecurityUtils.getCurrentUserIdOptional().orElse(null);
        log.info("Admin ID {} unenrolled user ID {} from course ID {}", adminId, userId, courseId);
        auditService.logAction(
                adminId,
                "ADMIN_MANUAL_UNENROLL",
                "Course",
                courseId,
                "Unenrolled user ID " + userId + " from course ID " + courseId,
                null
        );
    }

    @Transactional(readOnly = true)
    public StudentProgressDetailDto getStudentProgress(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        // 1. Enrolled courses progress
        List<CourseProgressDto> courseProgressList = progressService.getAllProgressForUser(userId);

        // 2. Completed lessons
        List<LessonProgress> lessonProgressList = lessonProgressRepository
                .findAllByUserIdWithLessonAndCourseOrderByCompletedAtDesc(userId);

        List<CompletedLessonDto> completedLessonDtos = lessonProgressList.stream()
                .map(lp -> CompletedLessonDto.builder()
                        .lessonId(lp.getLesson().getId())
                        .lessonTitle(lp.getLesson().getTitle())
                        .dayNumber(lp.getLesson().getDayNumber())
                        .courseId(lp.getLesson().getCourse().getId())
                        .courseTitle(lp.getLesson().getCourse().getTitle())
                        .completedAt(lp.getCompletedAt())
                        .build())
                .toList();

        // 3. Quiz submissions
        List<QuizSubmission> quizSubmissions = quizSubmissionRepository.findByUserIdOrderByStartedAtDesc(userId);
        List<StudentQuizScoreDto> quizScoreDtos = quizSubmissions.stream()
                .map(qs -> StudentQuizScoreDto.builder()
                        .submissionId(qs.getId())
                        .quizId(qs.getQuiz() != null ? qs.getQuiz().getId() : null)
                        .quizTitle(qs.getQuiz() != null ? qs.getQuiz().getTitle() : "Quiz")
                        .lessonId(qs.getQuiz() != null && qs.getQuiz().getLesson() != null ? qs.getQuiz().getLesson().getId() : null)
                        .lessonTitle(qs.getQuiz() != null && qs.getQuiz().getLesson() != null ? qs.getQuiz().getLesson().getTitle() : null)
                        .scorePercentage(qs.getScorePercentage())
                        .passed(qs.getPassed())
                        .startedAt(qs.getStartedAt())
                        .completedAt(qs.getCompletedAt())
                        .build())
                .toList();

        // 4. Homework submissions
        List<HomeworkSubmission> homeworkSubmissions = homeworkSubmissionRepository.findByUserIdOrderByCreatedAtDesc(userId);

        // Batch fetch lessons and courses for homework
        Set<Long> hwLessonIds = homeworkSubmissions.stream().map(HomeworkSubmission::getLessonId).collect(Collectors.toSet());
        Map<Long, Lesson> lessonsMap = hwLessonIds.isEmpty()
                ? Map.of()
                : lessonRepository.findAllById(hwLessonIds).stream().collect(Collectors.toMap(Lesson::getId, l -> l));

        List<StudentHomeworkStatusDto> homeworkDtos = homeworkSubmissions.stream()
                .map(hw -> {
                    Lesson lesson = lessonsMap.get(hw.getLessonId());
                    String lessonTitle = lesson != null ? lesson.getTitle() : "Lesson #" + hw.getLessonId();
                    String courseTitle = lesson != null && lesson.getCourse() != null ? lesson.getCourse().getTitle() : null;

                    return StudentHomeworkStatusDto.builder()
                            .submissionId(hw.getId())
                            .lessonId(hw.getLessonId())
                            .lessonTitle(lessonTitle)
                            .courseId(hw.getCourseId())
                            .courseTitle(courseTitle)
                            .codeSnippet(hw.getCodeSnippet())
                            .repositoryUrl(hw.getRepositoryUrl())
                            .status(hw.getStatus())
                            .score(hw.getScore())
                            .aiFeedback(hw.getAiFeedback())
                            .passedTestsCount(hw.getPassedTestsCount())
                            .totalTestsCount(hw.getTotalTestsCount())
                            .reviewedAt(hw.getReviewedAt())
                            .createdAt(hw.getCreatedAt())
                            .build();
                })
                .toList();

        return StudentProgressDetailDto.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .avatarUrl(user.getAvatarUrl())
                .role(user.getRole())
                .lastActiveDate(user.getLastActiveDate())
                .createdAt(user.getCreatedAt())
                .enrolledCourses(courseProgressList)
                .completedLessons(completedLessonDtos)
                .quizScores(quizScoreDtos)
                .homeworkSubmissions(homeworkDtos)
                .build();
    }

    private StudentDto toStudentDto(User user, List<Enrollment> enrollments) {
        return toStudentDto(user, enrollments, List.of());
    }

    private StudentDto toStudentDto(User user, List<Enrollment> enrollments, List<LessonProgress> progressList) {
        List<EnrollmentDto> enrollmentDtos = enrollments.stream()
                .map(this::toEnrollmentDto)
                .toList();

        String currentLessonTitle = "Не начат";
        if (!progressList.isEmpty()) {
            Lesson latestLesson = progressList.get(0).getLesson();
            if (latestLesson != null) {
                currentLessonTitle = "Урок " + latestLesson.getDayNumber() + ": " + latestLesson.getTitle();
            }
        }

        Instant estimatedFinishDate = null;
        if (!enrollments.isEmpty()) {
            Instant earliestEnrollment = enrollments.stream()
                    .map(Enrollment::getEnrolledAt)
                    .filter(Objects::nonNull)
                    .min(Comparator.naturalOrder())
                    .orElse(user.getCreatedAt());

            if (earliestEnrollment != null) {
                estimatedFinishDate = earliestEnrollment.plus(14, ChronoUnit.DAYS);
            }
        }

        return StudentDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .avatarUrl(user.getAvatarUrl())
                .role(user.getRole())
                .createdAt(user.getCreatedAt())
                .lastActiveDate(user.getLastActiveDate())
                .telegramUsername(user.getTelegramUsername())
                .githubUsername(user.getGithubUsername())
                .enrollments(enrollmentDtos)
                .currentLessonTitle(currentLessonTitle)
                .estimatedFinishDate(estimatedFinishDate)
                .build();
    }

    private EnrollmentDto toEnrollmentDto(Enrollment enrollment) {
        return EnrollmentDto.builder()
                .id(enrollment.getId())
                .userId(enrollment.getUser().getId())
                .userEmail(enrollment.getUser().getEmail())
                .userName(enrollment.getUser().getName())
                .courseId(enrollment.getCourse().getId())
                .courseTitle(enrollment.getCourse().getTitle())
                .courseSlug(enrollment.getCourse().getSlug())
                .enrolledAt(enrollment.getEnrolledAt())
                .build();
    }
}
