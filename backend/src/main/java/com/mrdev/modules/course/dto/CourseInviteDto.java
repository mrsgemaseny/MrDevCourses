package com.mrdev.modules.course.dto;

import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseInviteDto {
    private Long id;
    private String code;
    private Long courseId;
    private String courseTitle;
    private String courseSlug;
    private Long cohortId;
    private String cohortName;
    private Integer maxUses;
    private Integer usedCount;
    private Instant expiresAt;
    private String note;
    private boolean valid;
    private boolean expired;
    private boolean exhausted;
    private Instant createdAt;
}
