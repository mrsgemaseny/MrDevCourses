package com.mrdev.modules.course.repository;

import com.mrdev.modules.course.model.CourseInvite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CourseInviteRepository extends JpaRepository<CourseInvite, Long> {

    Optional<CourseInvite> findByCode(String code);

    @Query("SELECT ci FROM CourseInvite ci JOIN FETCH ci.course LEFT JOIN FETCH ci.cohort WHERE ci.code = :code")
    Optional<CourseInvite> findByCodeWithCourseAndCohort(@Param("code") String code);

    @Query("SELECT ci FROM CourseInvite ci JOIN FETCH ci.course LEFT JOIN FETCH ci.cohort ORDER BY ci.createdAt DESC")
    List<CourseInvite> findAllWithCourseAndCohort();

    List<CourseInvite> findByCourseIdOrderByCreatedAtDesc(Long courseId);
}
