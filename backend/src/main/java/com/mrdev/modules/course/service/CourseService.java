package com.mrdev.modules.course.service;

import com.mrdev.common.exception.ApiException;
import com.mrdev.common.exception.ResourceNotFoundException;
import com.mrdev.common.util.SecurityUtils;
import com.mrdev.modules.audit.service.AuditService;
import com.mrdev.modules.auth.model.Role;
import com.mrdev.modules.auth.model.User;
import com.mrdev.modules.auth.repository.UserRepository;
import com.mrdev.modules.course.dto.CourseDetailDto;
import com.mrdev.modules.course.dto.CourseDto;
import com.mrdev.modules.course.dto.CourseModuleDto;
import com.mrdev.modules.course.dto.EnrollmentDto;
import com.mrdev.modules.course.model.Course;
import com.mrdev.modules.course.model.CourseModule;
import com.mrdev.modules.course.model.Enrollment;
import com.mrdev.modules.course.repository.CourseModuleRepository;
import com.mrdev.modules.course.repository.CourseRepository;
import com.mrdev.modules.course.repository.EnrollmentRepository;
import com.mrdev.modules.lesson.dto.LessonSummaryDto;
import com.mrdev.modules.lesson.model.Lesson;
import com.mrdev.modules.lesson.model.LessonProgress;
import com.mrdev.modules.lesson.repository.LessonProgressRepository;
import com.mrdev.modules.lesson.repository.LessonRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CourseService {

    private final CourseRepository courseRepository;
    private final CourseModuleRepository courseModuleRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final LessonRepository lessonRepository;
    private final LessonProgressRepository lessonProgressRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<CourseDto> getActiveCourses(Optional<Long> currentUserId) {
        List<Course> courses = courseRepository.findByActiveTrueOrderByCreatedAtDesc();
        if (courses.isEmpty()) {
            return List.of();
        }

        List<Long> courseIds = courses.stream().map(Course::getId).toList();

        Map<Long, Long> lessonCountMap = lessonRepository.countLessonsByCourseIds(courseIds).stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (Long) row[1]
                ));

        Map<Long, Instant> userEnrollmentMap;
        if (currentUserId.isPresent()) {
            userEnrollmentMap = enrollmentRepository.findAllByUserIdAndCourseIdIn(currentUserId.get(), courseIds).stream()
                    .collect(Collectors.toMap(
                            e -> e.getCourse().getId(),
                            Enrollment::getEnrolledAt
                    ));
        } else {
            userEnrollmentMap = Map.of();
        }

        return courses.stream()
                .map(course -> {
                    boolean enrolled = userEnrollmentMap.containsKey(course.getId());
                    Instant enrolledAt = userEnrollmentMap.get(course.getId());
                    long totalLessons = lessonCountMap.getOrDefault(course.getId(), 0L);

                    return CourseDto.builder()
                            .id(course.getId())
                            .title(course.getTitle())
                            .description(course.getDescription())
                            .slug(course.getSlug())
                            .active(course.isActive())
                            .createdAt(course.getCreatedAt())
                            .enrolled(enrolled)
                            .enrolledAt(enrolledAt)
                            .totalLessons(totalLessons)
                            .build();
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public CourseDetailDto getCourseBySlug(String slug, Optional<Long> currentUserId) {
        Course course = courseRepository.findBySlugAndActiveTrue(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Course", "slug", slug));
        return toDetailDto(course, currentUserId);
    }

    @Transactional
    public EnrollmentDto enroll(Long courseId, Long userId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course", "id", courseId));

        if (!course.isActive()) {
            throw new ApiException("Cannot enroll in an inactive course", HttpStatus.BAD_REQUEST);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        if (user.getRole() == Role.ADMIN) {
            throw new ApiException("Администратор уже имеет полный доступ ко всем курсам и не должен регистрироваться как студент", HttpStatus.BAD_REQUEST);
        }

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
        log.info("User {} enrolled in course {} at {}", userId, courseId, saved.getEnrolledAt());

        auditService.logAction(userId, "COURSE_ENROLL", "Course", courseId, "Enrolled into: " + course.getTitle(), null);

        return toEnrollmentDto(saved);
    }

    private CourseDetailDto toDetailDto(Course course, Optional<Long> currentUserId) {
        boolean isAdmin = SecurityUtils.isAdmin();
        boolean enrolled = isAdmin;
        Instant enrolledAt = isAdmin ? Instant.now().minus(Duration.ofDays(365)) : null;
        Map<Long, LessonProgress> progressMap = Map.of();

        if (currentUserId.isPresent()) {
            Optional<Enrollment> enrollment = enrollmentRepository.findByUserIdAndCourseId(currentUserId.get(), course.getId());
            if (enrollment.isPresent()) {
                enrolled = true;
                enrolledAt = enrollment.get().getEnrolledAt();
                progressMap = lessonProgressRepository.findAllByUserIdAndCourseId(currentUserId.get(), course.getId()).stream()
                        .collect(Collectors.toMap(lp -> lp.getLesson().getId(), lp -> lp));
            }
        }

        List<CourseModule> modules = courseModuleRepository.findAllByCourseIdWithLessons(course.getId());
        List<Lesson> allLessonsOrdered = modules.stream()
                .flatMap(m -> m.getLessons().stream())
                .sorted(Comparator.comparingInt(Lesson::getSortOrder).thenComparingInt(Lesson::getDayNumber))
                .toList();

        Instant now = Instant.now();
        Instant finalEnrolledAt = enrolledAt;
        Map<Long, LessonProgress> finalProgressMap = progressMap;

        List<CourseModuleDto> moduleDtos = modules.stream().map(module -> {
            List<LessonSummaryDto> lessonDtos = module.getLessons().stream().map(lesson -> {
                Instant opensAt = finalEnrolledAt != null ? calculateUnlockTime(finalEnrolledAt, lesson.getDayNumber()) : Instant.now();
                boolean isAccessible = isAdmin
                        || Boolean.TRUE.equals(lesson.getIsFreePreview())
                        || isLessonSequentiallyUnlocked(lesson, allLessonsOrdered, finalProgressMap, finalEnrolledAt, now);
                LessonProgress progress = finalProgressMap.get(lesson.getId());
                boolean isCompleted = progress != null;
                Instant completedAt = isCompleted ? progress.getCompletedAt() : null;

                return LessonSummaryDto.builder()
                        .id(lesson.getId())
                        .courseId(course.getId())
                        .moduleId(module.getId())
                        .title(lesson.getTitle())
                        .lessonType(lesson.getLessonType())
                        .durationMinutes(lesson.getDurationMinutes())
                        .isFreePreview(lesson.getIsFreePreview())
                        .dayNumber(lesson.getDayNumber())
                        .sortOrder(lesson.getSortOrder())
                        .accessible(isAccessible)
                        .opensAt(opensAt)
                        .completed(isCompleted)
                        .completedAt(completedAt)
                        .build();
            }).collect(Collectors.toList());

            int completedCount = (int) lessonDtos.stream().filter(LessonSummaryDto::isCompleted).count();

            return CourseModuleDto.builder()
                    .id(module.getId())
                    .courseId(course.getId())
                    .title(module.getTitle())
                    .description(module.getDescription())
                    .sortOrder(module.getSortOrder())
                    .isFreePreview(module.getIsFreePreview())
                    .lessonsCount(lessonDtos.size())
                    .completedLessonsCount(completedCount)
                    .lessons(lessonDtos)
                    .build();
        }).collect(Collectors.toList());

        long totalLessons = lessonRepository.countByCourseId(course.getId());

        return CourseDetailDto.builder()
                .id(course.getId())
                .title(course.getTitle())
                .description(course.getDescription())
                .slug(course.getSlug())
                .active(course.isActive())
                .createdAt(course.getCreatedAt())
                .enrolled(enrolled)
                .enrolledAt(enrolledAt)
                .totalLessons(totalLessons)
                .modules(moduleDtos)
                .build();
    }

    private Instant calculateUnlockTime(Instant enrolledAt, int dayNumber) {
        if (dayNumber <= 1) {
            return enrolledAt;
        }
        return enrolledAt.plus(Duration.ofDays(dayNumber - 1L));
    }

    private boolean isLessonSequentiallyUnlocked(
            Lesson targetLesson,
            List<Lesson> courseLessons,
            Map<Long, LessonProgress> progressMap,
            Instant enrolledAt,
            Instant now
    ) {
        if (enrolledAt == null) {
            return false;
        }
        Instant opensAt = calculateUnlockTime(enrolledAt, targetLesson.getDayNumber());
        if (!now.isBefore(opensAt)) {
            return true;
        }
        if (progressMap == null || progressMap.isEmpty() || courseLessons == null || courseLessons.isEmpty()) {
            return false;
        }
        if (progressMap.containsKey(targetLesson.getId())) {
            return true;
        }
        for (int i = 0; i < courseLessons.size(); i++) {
            if (courseLessons.get(i).getId().equals(targetLesson.getId())) {
                if (i == 0) {
                    return true;
                }
                Lesson prevLesson = courseLessons.get(i - 1);
                return progressMap.containsKey(prevLesson.getId());
            }
        }
        return false;
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
