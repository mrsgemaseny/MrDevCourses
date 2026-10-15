package com.mrdev.modules.course.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateCourseInviteRequest {

    @NotNull(message = "Course ID is required")
    private Long courseId;

    private Long cohortId;

    @Min(value = 1, message = "Max uses must be at least 1")
    @Builder.Default
    private Integer maxUses = 1;

    private Integer expiresInDays;

    private String customCode;

    private String note;
}
