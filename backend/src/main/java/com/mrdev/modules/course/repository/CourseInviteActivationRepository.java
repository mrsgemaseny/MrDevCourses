package com.mrdev.modules.course.repository;

import com.mrdev.modules.course.model.CourseInviteActivation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CourseInviteActivationRepository extends JpaRepository<CourseInviteActivation, Long> {
    boolean existsByInviteIdAndUserId(Long inviteId, Long userId);
    List<CourseInviteActivation> findByInviteId(Long inviteId);
    List<CourseInviteActivation> findByUserId(Long userId);
}
