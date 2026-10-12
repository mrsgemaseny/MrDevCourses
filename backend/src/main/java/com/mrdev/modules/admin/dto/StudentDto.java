package com.mrdev.modules.admin.dto;

import com.mrdev.modules.auth.model.Role;
import com.mrdev.modules.course.dto.EnrollmentDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentDto {
    private Long id;
    private String email;
    private String name;
    private String avatarUrl;
    private Role role;
    private Instant createdAt;
    private LocalDate lastActiveDate;
    private String telegramUsername;
    private String githubUsername;
    private List<EnrollmentDto> enrollments;
    private String currentLessonTitle;
    private Instant estimatedFinishDate;
}
