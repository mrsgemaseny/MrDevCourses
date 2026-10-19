# Current Project Context — MrDevCourses

## Current Operational Focus
- **Current Operational Focus**: Knowledge Base / Docs Page & Clean Monochrome Experience.
- **Recent Accomplishments**: 
  - Removed all streaks and gamification noise across the application (Header, ProgressView, LessonContextPanel, QuickNavDrawer).
  - Built full-featured Documentation & Hashtag Knowledge Base page (`DocsPage.tsx`, `/docs`, `/glossary`) with search, tag filtering, code copy, and lesson navigation.
  - Enforced strict `BLACK & WHITE ONLY` design rule in `AGENTS.md` and refactored `LessonActionCard`, `StudentHelpModal`, and `LessonPage` removing all green, yellow, and amber accents.
  - Replaced text button «Застрял?» with a clean monochrome `?` action button.
  - Aligned `LessonPage.tsx` layout to exact 75% left-aligned column matching `CourseDetailPage.tsx` with clean right breathing room.
  - Replaced YouTube video URL across frontend, DataSeeder, and created Flyway migrations `V26` and `V68__update_default_youtube_url.sql` for `https://youtu.be/WSVQ4Qqh7uo?si=LbMmX-OpnDsZ4bUZ`.
  - Configured GitHub Actions Pages workflow (`.github/workflows/deploy-pages.yml`) with automated Vitest suites and production build.
  - Implemented dynamic Vite base path (`base: process.env.VITE_BASE_PATH || '/'`) and React Router `basename` integration for seamless GitHub Pages subpath routing.
  - Added SPA 404 fallback mechanism (`dist/404.html`) ensuring direct URL navigation without 404 errors.
  - Added `frontend/src/vite-env.d.ts` for strict client TypeScript environment variable resolution.
  - Implemented OpenTelemetry & Micrometer Tracing integration with W3C/B3 propagation and `1.0` sampling probability.
  - Implemented `CorrelationIdFilter` with highest precedence, populating MDC with `requestId`, `traceId`, `spanId`, and `clientIp`, and returning `X-Request-ID` in HTTP response headers.
  - Configured `logback-spring.xml` with colorized developer console output and structured single-line Logstash/Grafana Loki JSON logging.
  - Updated frontend `base.ts` to automatically generate `X-Request-ID` and bind `requestId` to `ApiError`.
  - Implemented comprehensive `StudentSuiteE2ETest.java` covering 5 tiers: Registration, Catalog, Drip intervals, Homework review triage loop, Anti-Cheat quiz grading, SOS help tickets, and Public certificate verification.
  - Implemented `StudentSuiteE2E.test.tsx` on frontend validating complete user navigation and API integrations.
  - Migrated Frontend from Axios to Native Fetch Interceptor (`ADR-005`, `knowledge/frontend-native-fetch-interceptor.md`) with 100% backward compatibility for all 17 API clients.
  - Implemented PostgreSQL Append-Only Audit Triggers (`V25__audit_triggers_security.sql`, `knowledge/db-trigger-audit-logs.md`) preventing UPDATE/DELETE on `audit_logs` and auto-auditing role/enrollment transitions.
  - Fixed Java Deserialization RCE in `CookieUtils` with HMAC-SHA256 constant-time verification.
  - Eliminated OAuth Account Preemption/Takeover in `CustomOAuth2UserService` by neutralizing unverified password hashes on Google account linking.
  - Added JWT revocation on logout via `jti` (UUID claim) and `JwtBlacklistService` with TTL eviction.
  - Guarded `DataSeeder` with `@Profile("!prod")` and set `JWT_COOKIE_SECURE: true` default.
  - Security & Legal Audit Remediation: Eliminated hardcoded JWT fallback in `application.yml`, introduced `.env.example`, isolated test JWT secret in `application-test.yml`, fortified `.gitignore`.
  - Legal & UX Compliance: Created `RefundPolicyPage.tsx` (`/refund`, 14-day policy per KZ consumer protection law), updated `PrivacyPage.tsx` with KZ Law No 94-V and explicit Cookies Policy, added business credentials and location to `Footer.tsx`.
  - Implemented Google Identity Services (ID Token Flow) based on JF-1C pattern with `com.google.api-client:google-api-client:2.2.0`, `GoogleAuthService`, `GoogleAuthRequest`, and `POST /v1/auth/google`, fully resolving `authorization_request_not_found` and cross-domain redirect issues.
  - Resolved `HTTP Error 403` on `POST /api/v1/auth/google` by updating `OriginValidationFilter.java` and `SecurityConfig.java` to permit requests from `courses.mrsgemaseny.com`, `*.mrsgemaseny.com`, and `*.vercel.app`.
  - Added `Cross-Origin-Opener-Policy: same-origin-allow-popups` to `SecurityHeadersFilter.java` and `vercel.json` to prevent browser COOP blocking on Google Identity popup communication.
  - Enhanced `GoogleLoginButton.tsx` with dynamic `window.google` detection while maintaining full compatibility with isolated unit tests.
  - Implemented Gallery Page (`/gallery`, `GalleryPage.tsx`) with full folder browser (`galleryData.ts`), supporting original Brand & Ryo 111-file archive (`gallery/full/`), stickers, YouTube covers, animations (GIF/MP4), search, pagination, and Lightbox viewer. Removed `hero-preview.png`.
  - Populated Public Graduation Projects Showcase Wall (`/projects`, `courses.mrsgemaseny.com/projects`) with 5 real deployed student projects: Tandamen.kz (Commercial live startup by Usman Sulaimanov), QazaqMarket (Book marketplace by Ratmir Mekenov), KitapAll (Apparel/Book marketplace by Usman), Global Coffee (Coffee shop landing by Usman), and Digital Projects Hub (Spotify & MindCheck by Ratmir).
  - Created Flyway migration `V69__seed_student_projects.sql` ensuring student accounts and showcases persist in PostgreSQL, and `V70__remove_featured_and_likes_from_showcases.sql` resetting featured/like flags.
  - Added frontend `defaultProjects.ts` fallback ensuring immediate zero-latency rendering even during cold starts.
  - Completely removed all gamification, likes, TOP/featured badges, and filter tabs from `ProjectsPage.tsx` in favor of a clean, egalitarian, minimalist layout.
  - Eliminated API debt by removing deprecated `adminApi.getStudents` and unifying on canonical `adminStudentApi.getStudents` across `AdminPage` and `AdminSuiteE2E.test.tsx`.
  - Scaled `AdminStudentService.searchStudents` to zero in-memory overhead: replaced JVM `findAll()` with dynamic Spring Data JPA `Specification<User>` and database `Pageable` (`WHERE ... ORDER BY created_at DESC LIMIT/OFFSET`).
  - Implemented Help Ticket Vectorization & RAG Dataset (`V71__add_embedding_to_student_help_requests.sql`): automatic 1536-dim embedding generation in `StudentHelpService` on ticket creation and mentor resolution (`problemText + mentorSolution`).
  - Integrated RAG AI-Core with Mentor Help Knowledge: implemented `HybridSearchService.searchResolvedHelpTickets` (cosine similarity ranking) and injected verified mentor solutions directly into `AiTutorService` system prompt.
  - Implemented Course Invite Links System (`V72__create_course_invites.sql`): full manual payment access flow without mocks on PostgreSQL. Backend (`CourseInviteService`, `AdminInviteController`, `CourseInviteController`) + Frontend (`inviteApi.ts`, `InviteRedeemPage.tsx`, `InviteManagerModal.tsx` in `AdminStudentsPage.tsx`).
  - Fixed `403 Forbidden` on `POST /api/v1/courses/{courseId}/lessons/{lessonId}/complete` (`LessonService`, `CourseService`): unified `isFreePreview` and sequential lesson unlock rules so completing Lesson 1 immediately unlocks Lesson 2 for viewing and completion without a 24h lock wall.
  - Fixed Telegram Bot (`TelegramBotCommandService`, `TelegramBotPollingRunner`, `TelegramLinkController`, `TelegramNotificationService`): added `@Transactional` and `findAllByUserIdWithCourse` (`JOIN FETCH`) to eliminate `LazyInitializationException`, added automatic Telegram Webhook registration (`setWebhook` to `/api/v1/telegram/webhook`) when `TELEGRAM_POLLING_ENABLED=false` on Render, enabled `@Async` non-blocking Telegram notifications, and added 10-minute keep-alive self-ping to prevent Render container cold-start spin-downs.
  - Refactored Graduation Certificate PDF template (`certificate.html`) and public verification page (`CertificateVerifyPage.tsx`) to strict `BLACK & WHITE ONLY` monochrome design (`#0a0a0c` / `#0e0e11` / `#141418` / `#fafafa`), removing legacy gold `#e2b340` borders.
  - Added automatic chunk-load recovery (`vite:preloadError` listener in `main.tsx`, `lazyWithRetry` wrapper and monochrome `RouteErrorFallback` in `router/index.tsx`) to eliminate `Failed to fetch dynamically imported module` errors across deployments.
- **Verification**: Backend 258/258 JUnit Green | Frontend 94/94 Vitest Green (36 suites) | Build 0 errors (1757 modules, tsc 0 errors).

## Roadmap & Product Philosophy (Первоисточник)
- **Master Roadmap File**: `C:\Users\murat\Downloads\mrdevcourses_roadmap.md` (копия во Втором Мозге: `Brain's protocol - second brain/projects/mrdevcourses/mrdevcourses_roadmap.md`).
- **Линейка курсов платформы по уровням сложности (`mr-developer-curriculum.md`)**:
  - **Курсы Уровня 1 (Базовый уровень)**: Инструменты, AI-ассистенты, промпты, Git, FSD архитектура, Лендинг стартапа и Клиентский Маркетплейс.
  - **Курс Уровня 2 (ОСНОВНОЙ КУРС / Флагман)**: Системная архитектура, Full-Stack разработка (Spring Boot + React + PostgreSQL), RBAC, OAuth 2.0, Three.js 3D (Трекер денег), CRM Kanban + Telegram Bot (вебхуки/алерты) + CI/CD (Render/Vercel).
  - **Курсы Уровня 3 (Продвинутый AI SaaS)**: Мультимодальный AI Core, LLM Streaming SSE, RAG-система, реактивный WebClient, PII-маскирование данных, Google SMTP / Gmail API (Pensee).
- **Ключевые принципы Mr Developer**:
  1. **Zero Friction Setup через сайт**: Все ссылки на софт, шаблоны, промпты и пошаговые чеклисты установки — прямо в карточке урока. Студент не ищет ничего в Discord.
  2. **Кнопка «Не получается / Сложно справляться» (Data-First RAG Dataset)**: SOS-сигнал на каждом шаге урока → **100% персист в базу данных (`student_help_requests`)** + мгновенный push ментору в Telegram с именем студента, уроком, шагом и описанием проблемы. Каждое нажатие — это сырые данные и живой датасет реальных затыков новичков для обучения будущего RAG.
  3. **Telegram Dashboard Ментора**: Уведомления о сданных ДЗ, SOS-тикеты, алерты о неактивности 3+ дней (`/stuck`), Telegram-команды `/hw`, `/status`, `/approve <id>`, `/reject <id> <комментарий>`.
  4. **Будущий AI с реальной RAG-системой (Vector Search по pgvector)**: Обучается и строит поиск не на синтетических FAQ, а на **накопленной базе реальных вопросов и решений ментора** из таблицы `student_help_requests`.
  5. **GitHub-Grade UX**: Максимально чистый, плотный, честный интерфейс (`#0d1117`, monochrome), фокус на реальном знании и первом задеплоенном онлайн веб-приложении.

## Status & Architecture
- **Project Stage**: Level 3 — Strong Educational MVP (Pre-Release Pilot with 2 live students).
- **Stack**: Java 17, Spring Boot 3.3.0, PostgreSQL 17 (pgvector, pg_trgm), Flyway (V1..V67 with rich mentor curriculum), React 19, TypeScript, Vite, FSD Architecture, Tailwind CSS v4, TanStack React Query v5, Bucket4j.
- **Core Modules & Capabilities**:
  - `homework-pipeline`: Human-centric submission (GitHub Repo URL, Live Demo URL, notes) + Admin Review Triage Queue (`/admin/homeworks`) with instant 1-click approve (auto-complete lesson + early drip unlock) or revision feedback.
  - `onboarding`: `WelcomeOnboardingModal` on course entry (outcome goal, Discord invite, tools checklist).
  - `domain-hierarchy`: Deep course structure (`Course -> CourseModule -> Lesson -> Materials/Quizzes`), Lesson Types (`VIDEO`, `ARTICLE`, `PRACTICE`, `QUIZ`), cohorts, free preview gates.
  - `quiz-engine`: Interactive assessment subsystem with anti-cheat option masking, auto-scoring, explanation feedback.
  - `materials`: Per-lesson downloadable resources (`CHEAT_SHEET`, `SOURCE_CODE`, `REPO_LINK`, `PDF`).
  - `auth`: Google OAuth2 + Email/Password, JWT stateless session in httpOnly cookie (`MrDev_token`), custom rate limiting.
  - `ratelimit`: Token Bucket (Bucket4j + Caffeine) with 3 tiers: Auth (10 req/15m/IP), AI (5 req/min/user), General (60 req/min/user/IP).
  - `admin-suite`: Complete management console (Curriculum Tree, Drip schedule, Module reordering, Markdown authoring, ChecklistEditorModal, Material manager, Quiz builder, Student triage & Role toggle, Cohort manager, Telemetry & Audit logs).
  - `b2c-discovery`: Modern B2C course catalog (`/courses`) with hover video preview; 2-column B2C landing (`/courses/:slug`) with Syllabus accordion, Sticky card, author badge, and FAQ.

## Test Verification & Quality Gates
- **Backend (JUnit)**: 250/250 tests PASSED (100% Green, clean `:jacocoTestReport` verified).
- **Frontend (Vitest)**: 85/85 tests PASSED across 34 test suites (100% Green).
- **Security & IDOR Coverage**: IDOR guards on Homework and Help modules, Anti-Cheat option masking on Quizzes, Drip SQL calculation bounds, and Admin RBAC gates thoroughly verified.
- **Production Build**: `tsc -b && vite build` SUCCESSFUL (1754 modules transformed, 0 errors).
- **Phase 0 Status**: 100% COMPLETE (Operational Lesson Card, SOS Ticket persisting + Telegram alerts, Telegram Mentor Bot commands `/hw`, `/approve`, `/reject`, `/status`, `/stuck`, Student Focus Dashboard).
- **Phase 1 Status**: 100% COMPLETE (Automated Stuck Detection Engine, Public Graduation Project Showcase Wall `/projects` with likes and GitHub/Demo previews, Welcome Onboarding flow).

