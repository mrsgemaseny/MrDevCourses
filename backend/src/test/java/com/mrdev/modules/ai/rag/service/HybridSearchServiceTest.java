package com.mrdev.modules.ai.rag.service;

import com.mrdev.modules.ai.rag.dto.ResolvedTicketMatchDto;
import com.mrdev.modules.ai.rag.dto.SearchResultDto;
import com.mrdev.modules.ai.rag.model.ChunkType;
import com.mrdev.modules.ai.rag.model.LessonChunk;
import com.mrdev.modules.ai.rag.repository.LessonChunkRepository;
import com.mrdev.modules.help.model.HelpRequestStatus;
import com.mrdev.modules.help.model.StudentHelpRequest;
import com.mrdev.modules.help.repository.StudentHelpRequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HybridSearchServiceTest {

    @Mock
    private LessonChunkRepository lessonChunkRepository;

    @Mock
    private StudentHelpRequestRepository studentHelpRequestRepository;

    @Spy
    private EmbeddingService embeddingService = new EmbeddingService();

    @InjectMocks
    private HybridSearchService hybridSearchService;

    private LessonChunk chunk1;
    private LessonChunk chunk2;

    @BeforeEach
    void setUp() {
        float[] vec1 = embeddingService.generateEmbedding("JWT Authentication and Spring Security filters");
        float[] vec2 = embeddingService.generateEmbedding("Database indexing and query performance in PostgreSQL");

        chunk1 = LessonChunk.builder()
                .id(1L)
                .lessonId(10L)
                .courseId(100L)
                .chunkIndex(0)
                .chunkType(ChunkType.THEORY)
                .header("JWT Authentication")
                .content("In this section we configure JwtAuthenticationFilter and SecurityFilterChain.")
                .embedding(embeddingService.vectorToString(vec1))
                .build();

        chunk2 = LessonChunk.builder()
                .id(2L)
                .lessonId(10L)
                .courseId(100L)
                .chunkIndex(1)
                .chunkType(ChunkType.CODE)
                .header("PostgreSQL Indexing")
                .content("PostgreSQL B-Tree and HNSW indexes enhance search query latency.")
                .embedding(embeddingService.vectorToString(vec2))
                .build();
    }

    @Test
    @DisplayName("searchLesson with RRF returns most relevant chunk at the top")
    void searchLesson_ReturnsRankedResults() {
        when(lessonChunkRepository.findByLessonIdOrderByChunkIndexAsc(10L))
                .thenReturn(List.of(chunk1, chunk2));

        List<SearchResultDto> results = hybridSearchService.searchLesson(10L, "Security JWT Filter", 2);

        assertThat(results).isNotEmpty();
        assertThat(results.get(0).getChunkId()).isEqualTo(1L);
        assertThat(results.get(0).getHeader()).isEqualTo("JWT Authentication");
        assertThat(results.get(0).getMatchType()).isEqualTo("HYBRID_RRF");
    }

    @Test
    @DisplayName("searchLesson with blank query returns empty list")
    void searchLesson_WhenBlank_ReturnsEmpty() {
        List<SearchResultDto> results = hybridSearchService.searchLesson(10L, "  ", 5);
        assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("searchResolvedHelpTickets should find and rank resolved help requests by cosine similarity")
    void searchResolvedHelpTickets_RanksRelevantTickets() {
        float[] ticketVec = embeddingService.generateEmbedding("Docker daemon error while starting postgres container");
        StudentHelpRequest ticket = StudentHelpRequest.builder()
                .id(42L)
                .lessonId(10L)
                .courseId(100L)
                .stepTitle("Docker Compose Setup")
                .problemText("Docker daemon is not running")
                .mentorSolution("Start Docker Desktop and enable WSL2 integration")
                .status(HelpRequestStatus.RESOLVED)
                .embedding(embeddingService.vectorToString(ticketVec))
                .build();

        when(studentHelpRequestRepository.findByLessonIdAndStatus(10L, HelpRequestStatus.RESOLVED))
                .thenReturn(List.of(ticket));

        List<ResolvedTicketMatchDto> results = hybridSearchService.searchResolvedHelpTickets(10L, "docker daemon error", 3);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getTicketId()).isEqualTo(42L);
        assertThat(results.get(0).getMentorSolution()).contains("Docker Desktop");
        assertThat(results.get(0).getSimilarity()).isGreaterThan(0.25);
    }
}
