package com.mrdev.modules.course.dto;

import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RedeemInviteResultDto {
    private Long courseId;
    private String courseTitle;
    private String courseSlug;
    private Long cohortId;
    private String cohortName;
    private Instant enrolledAt;
    private boolean alreadyEnrolled;
    private String message;
}
