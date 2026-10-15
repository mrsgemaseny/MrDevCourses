package com.mrdev.modules.course.model;

import com.mrdev.modules.auth.model.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "course_invite_activations", uniqueConstraints = {
        @UniqueConstraint(name = "uk_course_invite_activation", columnNames = {"invite_id", "user_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseInviteActivation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invite_id", nullable = false)
    private CourseInvite invite;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @CreationTimestamp
    @Column(name = "activated_at", nullable = false, updatable = false)
    private Instant activatedAt;
}
