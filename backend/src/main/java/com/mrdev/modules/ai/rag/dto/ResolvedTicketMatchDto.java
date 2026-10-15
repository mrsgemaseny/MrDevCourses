package com.mrdev.modules.ai.rag.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ResolvedTicketMatchDto {
    private final Long ticketId;
    private final Long lessonId;
    private final String stepTitle;
    private final String problemText;
    private final String mentorSolution;
    private final double similarity;
}
