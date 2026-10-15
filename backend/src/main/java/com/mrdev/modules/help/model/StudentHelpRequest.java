package com.mrdev.modules.help.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "student_help_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentHelpRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "course_id", nullable = false)
    private Long courseId;

    @Column(name = "lesson_id", nullable = false)
    private Long lessonId;

    @Column(name = "step_identifier", nullable = false, length = 128)
    private String stepIdentifier;

    @Column(name = "step_title", length = 256)
    private String stepTitle;

    @Column(name = "problem_text", nullable = false, columnDefinition = "TEXT")
    private String problemText;

    @Column(name = "error_logs", columnDefinition = "TEXT")
    private String errorLogs;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    @Builder.Default
    private HelpRequestStatus status = HelpRequestStatus.OPEN;

    @Column(name = "mentor_solution", columnDefinition = "TEXT")
    private String mentorSolution;

    @Column(name = "embedding", columnDefinition = "TEXT")
    private String embedding;

    @Column(name = "resolved_by")
    private Long resolvedBy;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
