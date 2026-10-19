package com.mrdev.modules.certificate.service;

import com.mrdev.common.exception.ApiException;
import com.mrdev.modules.audit.service.AuditService;
import com.mrdev.modules.auth.model.Role;
import com.mrdev.modules.auth.model.User;
import com.mrdev.modules.auth.repository.UserRepository;
import com.mrdev.modules.certificate.dto.CertificateDto;
import com.mrdev.modules.certificate.model.Certificate;
import com.mrdev.modules.certificate.repository.CertificateRepository;
import com.mrdev.modules.course.model.Course;
import com.mrdev.modules.course.repository.CourseRepository;
import com.mrdev.modules.course.repository.EnrollmentRepository;
import com.mrdev.modules.lesson.repository.LessonProgressRepository;
import com.mrdev.modules.lesson.repository.LessonRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CertificateServiceTest {

    @Mock
    private CertificateRepository certificateRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private LessonRepository lessonRepository;

    @Mock
    private LessonProgressRepository lessonProgressRepository;

    @Mock
    private CertificatePdfGenerator pdfGenerator;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private CertificateService certificateService;

    private User student;
    private Course course;
    private Certificate certificate;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(certificateService, "frontendUrl", "http://localhost:5173");

        student = User.builder()
                .id(1L)
                .email("student@mrdev.com")
                .name("Murat Student")
                .role(Role.STUDENT)
                .build();

        course = Course.builder()
                .id(10L)
                .title("Full-Stack Architecture")
                .slug("fullstack-arch")
                .active(true)
                .build();

        certificate = Certificate.builder()
                .id(100L)
                .certificateCode("MRDEV-TEST-CERT-10")
                .user(student)
                .course(course)
                .issuedAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("issueCertificateIfEligible should issue certificate when all lessons are completed")
    void issueCertificate_WhenEligible_IssuesSuccessfully() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(student));
        when(courseRepository.findById(10L)).thenReturn(Optional.of(course));
        when(enrollmentRepository.existsByUserIdAndCourseId(1L, 10L)).thenReturn(true);
        when(certificateRepository.findByUserIdAndCourseId(1L, 10L)).thenReturn(Optional.empty());

        when(lessonRepository.countByCourseId(10L)).thenReturn(5L);
        when(lessonProgressRepository.countCompletedLessonsByUserIdAndCourseId(1L, 10L)).thenReturn(5L);

        when(certificateRepository.save(any(Certificate.class))).thenReturn(certificate);

        CertificateDto dto = certificateService.issueCertificateIfEligible(1L, 10L);

        assertThat(dto).isNotNull();
        assertThat(dto.getCertificateCode()).isEqualTo("MRDEV-TEST-CERT-10");
        assertThat(dto.getUserName()).isEqualTo("Murat Student");
        assertThat(dto.getCourseTitle()).isEqualTo("Full-Stack Architecture");
        assertThat(dto.getVerificationUrl()).contains("/certificates/verify/MRDEV-TEST-CERT-10");

        verify(auditService).logAction(eq(1L), eq("CERTIFICATE_ISSUED"), eq("Certificate"), eq(100L), any(), any());
    }

    @Test
    @DisplayName("issueCertificateIfEligible should throw ApiException when course is not 100% completed")
    void issueCertificate_WhenIncomplete_ThrowsException() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(student));
        when(courseRepository.findById(10L)).thenReturn(Optional.of(course));
        when(enrollmentRepository.existsByUserIdAndCourseId(1L, 10L)).thenReturn(true);
        when(certificateRepository.findByUserIdAndCourseId(1L, 10L)).thenReturn(Optional.empty());

        when(lessonRepository.countByCourseId(10L)).thenReturn(5L);
        when(lessonProgressRepository.countCompletedLessonsByUserIdAndCourseId(1L, 10L)).thenReturn(3L);

        assertThatThrownBy(() -> certificateService.issueCertificateIfEligible(1L, 10L))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Курс еще не завершен");

        verify(certificateRepository, never()).save(any());
    }

    @Test
    @DisplayName("verifyCertificate should return valid CertificateDto by code")
    void verifyCertificate_ReturnsValidDto() {
        when(certificateRepository.findByCertificateCode("MRDEV-TEST-CERT-10"))
                .thenReturn(Optional.of(certificate));

        CertificateDto dto = certificateService.verifyCertificate("MRDEV-TEST-CERT-10");

        assertThat(dto).isNotNull();
        assertThat(dto.getCertificateCode()).isEqualTo("MRDEV-TEST-CERT-10");
        assertThat(dto.getCourseTitle()).isEqualTo("Full-Stack Architecture");
    }

    @Test
    @DisplayName("generatePdfForCertificate should return PDF byte stream")
    void generatePdfForCertificate_ReturnsBytes() {
        when(certificateRepository.findByCertificateCode("MRDEV-TEST-CERT-10"))
                .thenReturn(Optional.of(certificate));

        byte[] mockPdf = new byte[]{1, 2, 3, 4, 5};
        when(pdfGenerator.generatePdf(eq("certificate"), any())).thenReturn(mockPdf);

        byte[] result = certificateService.generatePdfForCertificate("MRDEV-TEST-CERT-10");

        assertThat(result).isEqualTo(mockPdf);
    }

    @Test
    @DisplayName("Certificate template should render strict monochrome HTML without gold accents")
    void renderRealMonochromeCertificatePdfAndPng() throws Exception {
        try (java.io.InputStream is = getClass().getClassLoader().getResourceAsStream("templates/certificate.html")) {
            assertThat(is).isNotNull();
            String html = new String(is.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            assertThat(html).contains("MRDEV COURSES");
            assertThat(html).contains("Lead Architect");
            assertThat(html).doesNotContain("#e2b340");
        }
    }
}
