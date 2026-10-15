package com.mrdev.modules.help.repository;

import com.mrdev.modules.help.model.HelpRequestStatus;
import com.mrdev.modules.help.model.StudentHelpRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StudentHelpRequestRepository extends JpaRepository<StudentHelpRequest, Long> {
    List<StudentHelpRequest> findByLessonIdOrderByCreatedAtDesc(Long lessonId);
    List<StudentHelpRequest> findByUserIdAndLessonIdOrderByCreatedAtDesc(Long userId, Long lessonId);
    List<StudentHelpRequest> findByStatusOrderByCreatedAtDesc(HelpRequestStatus status);
    List<StudentHelpRequest> findByLessonIdAndStatus(Long lessonId, HelpRequestStatus status);
    List<StudentHelpRequest> findByCourseIdAndStatus(Long courseId, HelpRequestStatus status);
    List<StudentHelpRequest> findAllByOrderByCreatedAtDesc();
}
