package com.mrdev.modules.lesson.service;

import com.mrdev.common.exception.ApiException;
import com.mrdev.common.exception.LessonLockedException;
import com.mrdev.common.exception.ResourceNotFoundException;
import com.mrdev.modules.audit.service.AuditService;
import com.mrdev.modules.auth.model.Role;
import com.mrdev.modules.auth.model.User;
import com.mrdev.modules.auth.repository.UserRepository;
import com.mrdev.modules.course.model.Course;
import com.mrdev.modules.course.model.Enrollment;
import com.mrdev.modules.course.repository.CourseRepository;
import com.mrdev.modules.course.repository.EnrollmentRepository;
import com.mrdev.modules.lesson.dto.LessonDetailDto;
import com.mrdev.modules.lesson.dto.LessonMaterialDto;
import com.mrdev.modules.lesson.dto.LessonSummaryDto;
import com.mrdev.modules.lesson.model.Lesson;
import com.mrdev.modules.lesson.model.LessonProgress;
import com.mrdev.modules.lesson.repository.LessonMaterialRepository;
import com.mrdev.modules.lesson.repository.LessonProgressRepository;
import com.mrdev.modules.lesson.repository.LessonRepository;
import com.mrdev.modules.quiz.repository.QuizRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LessonService {

    private final LessonRepository lessonRepository;
    private final LessonProgressRepository lessonProgressRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;
    private final LessonMaterialRepository lessonMaterialRepository;
    private final QuizRepository quizRepository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<LessonSummaryDto> getLessonsForCourse(Long courseId, Long userId, Role userRole) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course", "id", courseId));

        List<Lesson> lessons = lessonRepository.findByCourseIdOrderBySortOrderAscDayNumberAsc(courseId);

        if (userRole == Role.ADMIN) {
            return lessons.stream()
                    .map(lesson -> LessonSummaryDto.builder()
                            .id(lesson.getId())
                            .courseId(course.getId())
                            .moduleId(lesson.getModule() != null ? lesson.getModule().getId() : null)
                            .title(lesson.getTitle())
                            .lessonType(lesson.getLessonType())
                            .durationMinutes(lesson.getDurationMinutes())
                            .isFreePreview(lesson.getIsFreePreview())
                            .dayNumber(lesson.getDayNumber())
                            .sortOrder(lesson.getSortOrder())
                            .accessible(true)
                            .opensAt(Instant.now())
                            .completed(false)
                            .completedAt(null)
                            .build())
                    .toList();
        }

        Enrollment enrollment = enrollmentRepository.findByUserIdAndCourseId(userId, courseId)
                .orElseThrow(() -> new ApiException("Вы не записаны на этот курс", HttpStatus.FORBIDDEN));

        Map<Long, LessonProgress> progressMap = lessonProgressRepository.findAllByUserIdAndCourseId(userId, courseId)
                .stream()
                .collect(Collectors.toMap(lp -> lp.getLesson().getId(), lp -> lp));

        Instant now = Instant.now();
        Instant enrolledAt = enrollment.getEnrolledAt();

        return lessons.stream()
                .map(lesson -> {
                    Instant opensAt = calculateUnlockTime(enrolledAt, lesson.getDayNumber());
                    boolean isAccessible = isLessonUnlocked(lesson, lessons, progressMap, enrolledAt, now);
                    LessonProgress progress = progressMap.get(lesson.getId());
                    boolean isCompleted = progress != null;
                    Instant completedAt = isCompleted ? progress.getCompletedAt() : null;

                    return LessonSummaryDto.builder()
                            .id(lesson.getId())
                            .courseId(course.getId())
                            .moduleId(lesson.getModule() != null ? lesson.getModule().getId() : null)
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
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public LessonDetailDto getLessonDetail(Long courseId, Long lessonId, Long userId, Role userRole) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course", "id", courseId));

        Lesson lesson = lessonRepository.findByIdAndCourseId(lessonId, courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Lesson", "id", lessonId));

        List<Lesson> allCourseLessons = lessonRepository.findByCourseIdOrderBySortOrderAscDayNumberAsc(courseId);
        Long prevLessonId = null;
        Long nextLessonId = null;
        for (int i = 0; i < allCourseLessons.size(); i++) {
            if (allCourseLessons.get(i).getId().equals(lessonId)) {
                if (i > 0) {
                    prevLessonId = allCourseLessons.get(i - 1).getId();
                }
                if (i < allCourseLessons.size() - 1) {
                    nextLessonId = allCourseLessons.get(i + 1).getId();
                }
                break;
            }
        }

        List<LessonMaterialDto> materials = lessonMaterialRepository.findByLessonIdOrderBySortOrderAsc(lessonId).stream()
                .map(m -> LessonMaterialDto.builder()
                        .id(m.getId())
                        .title(m.getTitle())
                        .materialType(m.getMaterialType())
                        .url(m.getUrl())
                        .fileSizeBytes(m.getFileSizeBytes())
                        .sortOrder(m.getSortOrder())
                        .build())
                .toList();

        boolean hasQuiz = quizRepository.findByLessonId(lessonId).isPresent();
        Long moduleId = lesson.getModule() != null ? lesson.getModule().getId() : null;
        String moduleTitle = lesson.getModule() != null ? lesson.getModule().getTitle() : null;

        if (userRole == Role.ADMIN) {
            return LessonDetailDto.builder()
                    .id(lesson.getId())
                    .courseId(course.getId())
                    .courseTitle(course.getTitle())
                    .courseSlug(course.getSlug())
                    .moduleId(moduleId)
                    .moduleTitle(moduleTitle)
                    .title(lesson.getTitle())
                    .lessonType(lesson.getLessonType())
                    .durationMinutes(lesson.getDurationMinutes())
                    .isFreePreview(lesson.getIsFreePreview())
                    .content(lesson.getContent())
                    .checklist(lesson.getChecklist())
                    .youtubeUrl(lesson.getYoutubeUrl())
                    .dayNumber(lesson.getDayNumber())
                    .sortOrder(lesson.getSortOrder())
                    .accessible(true)
                    .opensAt(Instant.now())
                    .completed(false)
                    .completedAt(null)
                    .hasQuiz(hasQuiz)
                    .materials(materials)
                    .prevLessonId(prevLessonId)
                    .nextLessonId(nextLessonId)
                    .build();
        }

        Optional<Enrollment> enrollmentOpt = enrollmentRepository.findByUserIdAndCourseId(userId, courseId);
        if (enrollmentOpt.isEmpty() && !Boolean.TRUE.equals(lesson.getIsFreePreview())) {
            throw new ApiException("Вы не записаны на этот курс", HttpStatus.FORBIDDEN);
        }

        Instant now = Instant.now();
        Instant enrolledAt = enrollmentOpt.map(Enrollment::getEnrolledAt).orElse(null);
        Instant opensAt = enrolledAt != null ? calculateUnlockTime(enrolledAt, lesson.getDayNumber()) : now;

        Map<Long, LessonProgress> progressMap = lessonProgressRepository.findAllByUserIdAndCourseId(userId, courseId)
                .stream()
                .collect(Collectors.toMap(lp -> lp.getLesson().getId(), lp -> lp));

        if (!isLessonUnlocked(lesson, allCourseLessons, progressMap, enrolledAt, now)) {
            throw new LessonLockedException("Урок заблокирован. Он станет доступен: " + opensAt.toString(), opensAt);
        }

        LessonProgress progress = progressMap.get(lessonId);
        boolean isCompleted = progress != null || lessonProgressRepository.findByUserIdAndLessonId(userId, lessonId).isPresent();
        Instant completedAt = progress != null ? progress.getCompletedAt()
                : lessonProgressRepository.findByUserIdAndLessonId(userId, lessonId).map(LessonProgress::getCompletedAt).orElse(null);

        return LessonDetailDto.builder()
                .id(lesson.getId())
                .courseId(course.getId())
                .courseTitle(course.getTitle())
                .courseSlug(course.getSlug())
                .moduleId(moduleId)
                .moduleTitle(moduleTitle)
                .title(lesson.getTitle())
                .lessonType(lesson.getLessonType())
                .durationMinutes(lesson.getDurationMinutes())
                .isFreePreview(lesson.getIsFreePreview())
                .content(lesson.getContent())
                .checklist(lesson.getChecklist())
                .youtubeUrl(lesson.getYoutubeUrl())
                .dayNumber(lesson.getDayNumber())
                .sortOrder(lesson.getSortOrder())
                .accessible(true)
                .opensAt(opensAt)
                .completed(isCompleted)
                .completedAt(completedAt)
                .hasQuiz(hasQuiz)
                .materials(materials)
                .prevLessonId(prevLessonId)
                .nextLessonId(nextLessonId)
                .build();
    }

    @Transactional
    public LessonSummaryDto completeLesson(Long courseId, Long lessonId, Long userId, Role userRole) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course", "id", courseId));

        Lesson lesson = lessonRepository.findByIdAndCourseId(lessonId, courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Lesson", "id", lessonId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        if (userRole != Role.ADMIN) {
            Optional<Enrollment> enrollmentOpt = enrollmentRepository.findByUserIdAndCourseId(userId, courseId);
            if (enrollmentOpt.isEmpty() && !Boolean.TRUE.equals(lesson.getIsFreePreview())) {
                throw new ApiException("Вы не записаны на этот курс", HttpStatus.FORBIDDEN);
            }

            Instant now = Instant.now();
            Instant enrolledAt = enrollmentOpt.map(Enrollment::getEnrolledAt).orElse(null);
            Instant opensAt = enrolledAt != null ? calculateUnlockTime(enrolledAt, lesson.getDayNumber()) : now;

            List<Lesson> allCourseLessons = lessonRepository.findByCourseIdOrderBySortOrderAscDayNumberAsc(courseId);
            Map<Long, LessonProgress> progressMap = lessonProgressRepository.findAllByUserIdAndCourseId(userId, courseId)
                    .stream()
                    .collect(Collectors.toMap(lp -> lp.getLesson().getId(), lp -> lp));

            if (!isLessonUnlocked(lesson, allCourseLessons, progressMap, enrolledAt, now)) {
                throw new LessonLockedException("Нельзя завершить заблокированный урок", opensAt);
            }
        }

        Optional<LessonProgress> existingProgress = lessonProgressRepository.findByUserIdAndLessonId(userId, lessonId);
        LessonProgress progress;
        if (existingProgress.isPresent()) {
            progress = existingProgress.get();
        } else {
            progress = LessonProgress.builder()
                    .user(user)
                    .lesson(lesson)
                    .completedAt(Instant.now())
                    .build();
            progress = lessonProgressRepository.save(progress);
            log.info("User {} marked lesson {} ({}) as complete", userId, lessonId, lesson.getTitle());

            updateUserLastActive(user);
            auditService.logAction(userId, "LESSON_COMPLETE", "Lesson", lessonId, "Completed lesson: " + lesson.getTitle(), null);
        }

        Instant opensAt = Instant.now();
        return LessonSummaryDto.builder()
                .id(lesson.getId())
                .courseId(course.getId())
                .moduleId(lesson.getModule() != null ? lesson.getModule().getId() : null)
                .title(lesson.getTitle())
                .lessonType(lesson.getLessonType())
                .durationMinutes(lesson.getDurationMinutes())
                .isFreePreview(lesson.getIsFreePreview())
                .dayNumber(lesson.getDayNumber())
                .sortOrder(lesson.getSortOrder())
                .accessible(true)
                .opensAt(opensAt)
                .completed(true)
                .completedAt(progress.getCompletedAt())
                .build();
    }

    @Transactional
    public void completeLesson(Long courseId, Long lessonId) {
        completeLesson(courseId, lessonId, com.mrdev.common.util.SecurityUtils.getCurrentUserId(), Role.STUDENT);
    }

    public Instant calculateUnlockTime(Instant enrolledAt, int dayNumber) {
        if (dayNumber <= 1) {
            return enrolledAt;
        }
        return enrolledAt.plus(Duration.ofDays(dayNumber - 1L));
    }

    public boolean isLessonUnlocked(
            Lesson targetLesson,
            List<Lesson> courseLessons,
            Map<Long, LessonProgress> progressMap,
            Instant enrolledAt,
            Instant now
    ) {
        if (Boolean.TRUE.equals(targetLesson.getIsFreePreview())) {
            return true;
        }
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

    private void updateUserLastActive(User user) {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        user.setLastActiveDate(today);
        userRepository.save(user);
    }
}
