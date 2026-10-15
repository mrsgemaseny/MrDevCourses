package com.mrdev.modules.ai.rag.service;

import com.mrdev.modules.ai.rag.dto.ResolvedTicketMatchDto;
import com.mrdev.modules.ai.rag.dto.SearchResultDto;
import com.mrdev.modules.ai.rag.model.LessonChunk;
import com.mrdev.modules.ai.rag.repository.LessonChunkRepository;
import com.mrdev.modules.help.model.HelpRequestStatus;
import com.mrdev.modules.help.model.StudentHelpRequest;
import com.mrdev.modules.help.repository.StudentHelpRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class HybridSearchService {

    private static final int RRF_K = 60;
    private static final double DENSE_WEIGHT = 0.6;
    private static final double SPARSE_WEIGHT = 0.4;

    private final LessonChunkRepository lessonChunkRepository;
    private final EmbeddingService embeddingService;
    private final StudentHelpRequestRepository studentHelpRequestRepository;

    @Transactional(readOnly = true)
    public List<SearchResultDto> searchCourse(Long courseId, String query, int topK) {
        if (query == null || query.isBlank()) {
            return List.of();
        }

        List<LessonChunk> allChunks = lessonChunkRepository.findByCourseId(courseId);
        if (allChunks.isEmpty()) {
            return List.of();
        }

        return executeHybridRrf(allChunks, query, topK);
    }

    @Transactional(readOnly = true)
    public List<SearchResultDto> searchLesson(Long lessonId, String query, int topK) {
        if (query == null || query.isBlank()) {
            return List.of();
        }

        List<LessonChunk> lessonChunks = lessonChunkRepository.findByLessonIdOrderByChunkIndexAsc(lessonId);
        if (lessonChunks.isEmpty()) {
            return List.of();
        }

        return executeHybridRrf(lessonChunks, query, topK);
    }

    private List<SearchResultDto> executeHybridRrf(List<LessonChunk> candidateChunks, String query, int topK) {
        float[] queryEmbedding = embeddingService.generateEmbedding(query);

        // 1. Dense Retrieval (Cosine Similarity)
        List<ScoredChunk> denseRanked = candidateChunks.stream()
                .map(chunk -> {
                    float[] chunkVec = embeddingService.stringToVector(chunk.getEmbedding());
                    double sim = embeddingService.calculateCosineSimilarity(queryEmbedding, chunkVec);
                    return new ScoredChunk(chunk, sim);
                })
                .sorted(Comparator.comparingDouble(ScoredChunk::score).reversed())
                .toList();

        // 2. Sparse Retrieval (Keyword / BM25-like matching)
        String[] queryTokens = query.toLowerCase().split("[\\s\\p{Punct}]+");
        List<ScoredChunk> sparseRanked = candidateChunks.stream()
                .map(chunk -> {
                    double score = calculateSparseScore(chunk, queryTokens);
                    return new ScoredChunk(chunk, score);
                })
                .filter(sc -> sc.score() > 0.0)
                .sorted(Comparator.comparingDouble(ScoredChunk::score).reversed())
                .toList();

        // 3. Reciprocal Rank Fusion (RRF)
        Map<Long, Double> rrfScores = new HashMap<>();
        Map<Long, LessonChunk> chunkMap = candidateChunks.stream()
                .collect(Collectors.toMap(LessonChunk::getId, c -> c));

        for (int rank = 0; rank < denseRanked.size(); rank++) {
            LessonChunk c = denseRanked.get(rank).chunk();
            double rrf = DENSE_WEIGHT / (RRF_K + rank + 1);
            rrfScores.merge(c.getId(), rrf, Double::sum);
        }

        for (int rank = 0; rank < sparseRanked.size(); rank++) {
            LessonChunk c = sparseRanked.get(rank).chunk();
            double rrf = SPARSE_WEIGHT / (RRF_K + rank + 1);
            rrfScores.merge(c.getId(), rrf, Double::sum);
        }

        return rrfScores.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(topK)
                .map(entry -> {
                    LessonChunk chunk = chunkMap.get(entry.getKey());
                    return SearchResultDto.builder()
                            .chunkId(chunk.getId())
                            .lessonId(chunk.getLessonId())
                            .courseId(chunk.getCourseId())
                            .header(chunk.getHeader())
                            .content(chunk.getContent())
                            .chunkType(chunk.getChunkType())
                            .score(entry.getValue())
                            .matchType("HYBRID_RRF")
                            .build();
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ResolvedTicketMatchDto> searchResolvedHelpTickets(Long lessonId, String query, int topK) {
        if (query == null || query.isBlank() || lessonId == null) {
            return List.of();
        }

        List<StudentHelpRequest> tickets = studentHelpRequestRepository.findByLessonIdAndStatus(lessonId, HelpRequestStatus.RESOLVED);
        if (tickets.isEmpty()) {
            return List.of();
        }

        float[] queryEmbedding = embeddingService.generateEmbedding(query);

        return tickets.stream()
                .filter(t -> t.getMentorSolution() != null && !t.getMentorSolution().isBlank())
                .map(ticket -> {
                    float[] ticketVec = embeddingService.stringToVector(ticket.getEmbedding());
                    double sim = embeddingService.calculateCosineSimilarity(queryEmbedding, ticketVec);
                    return ResolvedTicketMatchDto.builder()
                            .ticketId(ticket.getId())
                            .lessonId(ticket.getLessonId())
                            .stepTitle(ticket.getStepTitle())
                            .problemText(ticket.getProblemText())
                            .mentorSolution(ticket.getMentorSolution())
                            .similarity(sim)
                            .build();
                })
                .filter(dto -> dto.getSimilarity() > 0.25)
                .sorted(Comparator.comparingDouble(ResolvedTicketMatchDto::getSimilarity).reversed())
                .limit(topK)
                .toList();
    }

    private double calculateSparseScore(LessonChunk chunk, String[] queryTokens) {
        String content = chunk.getContent().toLowerCase();
        String header = chunk.getHeader() != null ? chunk.getHeader().toLowerCase() : "";

        double score = 0.0;
        for (String token : queryTokens) {
            if (token.isBlank() || token.length() < 2) continue;

            if (header.contains(token)) {
                score += 3.0; // Header match bonus
            }
            if (content.contains(token)) {
                score += 1.0;
            }
        }
        return score;
    }

    private record ScoredChunk(LessonChunk chunk, double score) {}
}
