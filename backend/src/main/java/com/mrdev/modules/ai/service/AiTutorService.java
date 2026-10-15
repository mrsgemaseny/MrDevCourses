package com.mrdev.modules.ai.service;

import com.mrdev.common.exception.ApiException;
import com.mrdev.common.exception.ResourceNotFoundException;
import com.mrdev.modules.ai.dto.AiCitationDto;
import com.mrdev.modules.ai.dto.AiTutorRequest;
import com.mrdev.modules.ai.dto.AiTutorResponse;
import com.mrdev.modules.ai.rag.dto.ResolvedTicketMatchDto;
import com.mrdev.modules.ai.rag.dto.SearchResultDto;
import com.mrdev.modules.ai.rag.service.HybridSearchService;
import com.mrdev.modules.audit.service.AuditService;
import com.mrdev.modules.auth.model.Role;
import com.mrdev.modules.course.repository.EnrollmentRepository;
import com.mrdev.modules.lesson.model.Lesson;
import com.mrdev.modules.lesson.repository.LessonRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiTutorService {

    private final LessonRepository lessonRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final GroqClient groqClient;
    private final PromptSanitizer promptSanitizer;
    private final AuditService auditService;
    private final HybridSearchService hybridSearchService;

    @Transactional(readOnly = true)
    public AiTutorResponse askTutor(AiTutorRequest request, Long userId, Role userRole) {
        Lesson lesson = lessonRepository.findByIdAndCourseId(request.getLessonId(), request.getCourseId())
                .orElseThrow(() -> new ResourceNotFoundException("Lesson", "id", request.getLessonId()));

        if (userRole != Role.ADMIN) {
            if (!enrollmentRepository.existsByUserIdAndCourseId(userId, request.getCourseId())) {
                throw new ApiException("Вы не записаны на этот курс", HttpStatus.FORBIDDEN);
            }
        }

        String cleanQuestion = promptSanitizer.sanitizeInput(request.getQuestion());

        // 1. Retrieve RAG chunks via Dense + Sparse Hybrid Search
        List<SearchResultDto> relevantChunks = hybridSearchService.searchLesson(lesson.getId(), cleanQuestion, 3);
        List<ResolvedTicketMatchDto> relevantTickets = hybridSearchService.searchResolvedHelpTickets(lesson.getId(), cleanQuestion, 2);

        StringBuilder contextBuilder = new StringBuilder();
        List<AiCitationDto> citations = new ArrayList<>();

        if (!relevantChunks.isEmpty()) {
            contextBuilder.append("НАЙДЕННЫЕ СЕМАНТИЧЕСКИЕ ФРАГМЕНТЫ УРОКА (RAG):\n");
            for (SearchResultDto chunk : relevantChunks) {
                contextBuilder.append("### [Блок: ").append(chunk.getHeader()).append("]\n")
                        .append(chunk.getContent()).append("\n\n");

                String snippet = chunk.getContent().length() > 140
                        ? chunk.getContent().substring(0, 140) + "..."
                        : chunk.getContent();

                citations.add(AiCitationDto.builder()
                        .chunkId(chunk.getChunkId())
                        .header(chunk.getHeader())
                        .snippet(snippet)
                        .relevanceScore(Math.round(chunk.getScore() * 1000.0) / 10.0)
                        .build());
            }
        } else {
            // Fallback to full lesson text if chunking has not completed yet
            contextBuilder.append("ОСНОВНОЙ МАТЕРИАЛ УРОКА:\n")
                    .append(lesson.getContent() != null ? lesson.getContent() : "");
        }

        if (!relevantTickets.isEmpty()) {
            contextBuilder.append("\nБАЗА ЗНАНИЙ РЕШЕННЫХ ЗАТЫКОВ И ОТВЕТОВ МЕНТОРА:\n");
            for (ResolvedTicketMatchDto ticket : relevantTickets) {
                contextBuilder.append("• [Шаг: ").append(ticket.getStepTitle()).append("]\n")
                        .append("  Проблема: ").append(ticket.getProblemText()).append("\n")
                        .append("  Решение ментора: ").append(ticket.getMentorSolution()).append("\n\n");
            }
        }

        String systemPrompt = """
                Ты — профессиональный Senior AI-наставник образовательной платформы MrDeveloper.
                Твоя задача — помогать студенту глубоко понимать материал текущего урока на основе точных технических фрагментов.
                
                Правила:
                1. Отвечай кратко, ёмко, точно по существу и без воды.
                2. Опирайся в первую очередь на контекст урока и семантические фрагменты, предоставленные ниже.
                3. Если в контексте присутствуют решения ментора по похожим затыкам, используй этот проверенный практический опыт.
                4. Используй чистый Markdown для оформления кода и терминов.
                5. Если студент спрашивает о вещах, не связанных с программированием/уроком, вежливо направь его обратно к теме.
                
                КОНТЕКСТ УРОКА:
                Название: %s (День %d)
                %s
                """.formatted(lesson.getTitle(), lesson.getDayNumber(), contextBuilder.toString());

        String rawAnswer = groqClient.generateAnswer(systemPrompt, cleanQuestion);
        boolean isFallback = false;

        if (rawAnswer == null || rawAnswer.isBlank()) {
            isFallback = true;
            rawAnswer = """
                    В уроке **"%s"** рассматриваются ключевые концепции дня №%d.
                    
                    Для успешного освоения материала:
                    1. Внимательно изучите видеоматериалы и примеры кода из описания.
                    2. Обратите внимание на термины в глоссарии справа.
                    3. Выполните практическое задание перед переходом к следующему дню.
                    
                    *(Режим локального ассистента: для генерации индивидуальных ответов задайте API-ключ GROQ в настройках).*
                    """.formatted(lesson.getTitle(), lesson.getDayNumber());
        }

        auditService.logAction(userId, "AI_TUTOR_QUERY", "Lesson", lesson.getId(),
                "Asked AI tutor on lesson: " + lesson.getTitle(), null);

        List<String> suggestedFollowUps = List.of(
                "Как применить это на практике?",
                "Какие частые ошибки бывают в этой теме?",
                "Поясни подробнее ключевой термин из урока"
        );

        return AiTutorResponse.builder()
                .answer(rawAnswer)
                .lessonTitle(lesson.getTitle())
                .suggestedFollowUps(suggestedFollowUps)
                .citations(citations)
                .fallbackMode(isFallback)
                .build();
    }
}
