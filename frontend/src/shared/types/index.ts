export type UserRole = 'STUDENT' | 'ADMIN';

export interface User {
  id: number;
  email: string;
  name?: string;
  avatarUrl?: string;
  role: UserRole;
  lastActiveDate?: string;
  createdAt: string;
}

export type LessonType = 'VIDEO' | 'ARTICLE' | 'PRACTICE' | 'QUIZ';

export type MaterialType = 'CHEAT_SHEET' | 'SOURCE_CODE' | 'REPO_LINK' | 'DOCUMENTATION' | 'PDF';

export interface LessonMaterial {
  id: number;
  title: string;
  materialType: MaterialType;
  url: string;
  fileSizeBytes?: number;
  sortOrder: number;
}

export interface LessonPitfall {
  id: number;
  lessonId: number;
  title: string;
  errorSymptom?: string;
  solutionMarkdown: string;
  orderIndex: number;
  createdAt: string;
}

export interface LessonSummary {
  id: number;
  courseId: number;
  moduleId?: number;
  title: string;
  lessonType?: LessonType;
  durationMinutes?: number;
  isFreePreview?: boolean;
  isPublished?: boolean;
  content?: string;
  checklist?: string;
  youtubeUrl?: string;
  dayNumber: number;
  sortOrder: number;
  accessible: boolean;
  opensAt: string;
  completed: boolean;
  completedAt?: string;
}

export interface CourseModule {
  id: number;
  courseId: number;
  title: string;
  description?: string;
  sortOrder: number;
  isFreePreview: boolean;
  lessonsCount: number;
  completedLessonsCount: number;
  lessons: LessonSummary[];
}

export interface Course {
  id: number;
  title: string;
  description?: string;
  slug: string;
  active: boolean;
  level?: string;
  createdAt: string;
  enrolled?: boolean;
  enrolledAt?: string;
  totalLessons?: number;
  modules?: CourseModule[];
}

export interface LessonDetail {
  id: number;
  courseId: number;
  courseTitle: string;
  courseSlug: string;
  moduleId?: number;
  moduleTitle?: string;
  title: string;
  lessonType?: LessonType;
  durationMinutes?: number;
  isFreePreview?: boolean;
  isPublished?: boolean;
  content?: string;
  checklist?: string;
  youtubeUrl?: string;
  dayNumber: number;
  sortOrder: number;
  accessible: boolean;
  opensAt: string;
  completed: boolean;
  completedAt?: string;
  hasQuiz?: boolean;
  materials?: LessonMaterial[];
  prevLessonId?: number;
  nextLessonId?: number;
}

export interface Enrollment {
  id: number;
  userId: number;
  userEmail: string;
  userName?: string;
  courseId: number;
  courseTitle: string;
  courseSlug: string;
  enrolledAt: string;
}

export interface CourseProgress {
  courseId: number;
  courseTitle: string;
  courseDescription?: string;
  courseSlug: string;
  enrolledAt: string;
  currentDay: number;
  completedCount: number;
  totalUnlocked: number;
  totalLessons: number;
  progressPercentage: number;
  nextUnlockAt?: string;
}

export interface Student {
  id: number;
  email: string;
  name?: string;
  avatarUrl?: string;
  role: UserRole;
  lastActiveDate?: string;
  telegramUsername?: string;
  githubUsername?: string;
  createdAt: string;
  enrollments: Enrollment[];
  currentLessonTitle?: string;
  estimatedFinishDate?: string;
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}

export interface CompletedLesson {
  lessonId: number;
  lessonTitle: string;
  dayNumber: number;
  courseId: number;
  courseTitle: string;
  completedAt: string;
}

export interface StudentQuizScore {
  submissionId: number;
  quizId?: number;
  quizTitle?: string;
  lessonId?: number;
  lessonTitle?: string;
  scorePercentage: number;
  passed: boolean;
  startedAt: string;
  completedAt?: string;
}

export interface StudentHomeworkStatus {
  submissionId: number;
  lessonId: number;
  lessonTitle: string;
  courseId?: number;
  courseTitle?: string;
  codeSnippet: string;
  repositoryUrl?: string;
  status: SubmissionStatus;
  score: number;
  aiFeedback?: string;
  passedTestsCount: number;
  totalTestsCount: number;
  reviewedAt?: string;
  createdAt: string;
}

export interface StudentProgressDetail {
  userId: number;
  email: string;
  name?: string;
  avatarUrl?: string;
  role: UserRole;
  lastActiveDate?: string;
  createdAt: string;
  enrolledCourses: CourseProgress[];
  completedLessons: CompletedLesson[];
  quizScores: StudentQuizScore[];
  homeworkSubmissions: StudentHomeworkStatus[];
}

export interface Cohort {
  id: number;
  courseId: number;
  courseTitle: string;
  courseSlug?: string;
  name: string;
  startDate: string;
  endDate?: string;
  maxStudents: number;
  currentStudentsCount: number;
  isActive: boolean;
  createdAt: string;
}

export interface CreateCohortPayload {
  courseId?: number;
  name: string;
  startDate: string;
  endDate?: string;
  maxStudents?: number;
  isActive?: boolean;
}

export interface UpdateCohortPayload {
  name: string;
  startDate: string;
  endDate?: string;
  maxStudents?: number;
  isActive?: boolean;
}

export interface ApiResponse<T> {
  success: boolean;
  message?: string;
  data: T;
  timestamp: string;
}

export interface ApiError {
  status: number;
  error: string;
  message: string;
  path: string;
  timestamp: string;
  validationErrors?: Record<string, string>;
}

export type GlossaryCategory = 'core' | 'backend' | 'frontend' | 'security' | 'ai' | 'devops';

export interface GlossaryTerm {
  id: string;
  term: string;
  category: GlossaryCategory;
  shortDefinition: string;
  fullExplanation: string;
  codeSnippet?: string;
  relatedDayNumbers?: number[];
  tags: string[];
}

export interface Certificate {
  id: number;
  certificateCode: string;
  userId: number;
  userName: string;
  userEmail: string;
  courseId: number;
  courseTitle: string;
  courseSlug: string;
  issuedAt: string;
  verificationUrl: string;
}

export interface AiCitation {
  chunkId: number;
  header: string;
  snippet: string;
  relevanceScore: number;
}

export interface AiTutorRequest {
  courseId: number;
  lessonId: number;
  question: string;
}

export interface AiTutorResponse {
  answer: string;
  lessonTitle: string;
  suggestedFollowUps: string[];
  citations?: AiCitation[];
  fallbackMode: boolean;
}

export type SubmissionStatus = 'PENDING' | 'EVALUATING' | 'PASSED' | 'NEEDS_IMPROVEMENT' | 'FAILED';

export interface HomeworkSubmission {
  id: number;
  lessonId: number;
  userId: number;
  courseId: number;
  codeSnippet: string;
  repositoryUrl?: string;
  liveDemoUrl?: string;
  status: SubmissionStatus;
  score: number;
  aiFeedback?: string;
  mentorFeedback?: string;
  reviewedBy?: number;
  passedTestsCount: number;
  totalTestsCount: number;
  securityFlags?: string;
  studentName?: string;
  studentEmail?: string;
  lessonTitle?: string;
  courseTitle?: string;
  reviewedAt?: string;
  createdAt: string;
}

export interface HomeworkSubmitRequest {
  codeSnippet?: string;
  repositoryUrl?: string;
  liveDemoUrl?: string;
}

export interface AdminReviewHomeworkRequest {
  status: SubmissionStatus;
  mentorFeedback?: string;
}

export type QuestionType = 'SINGLE_CHOICE' | 'MULTIPLE_CHOICE' | 'TEXT_INPUT';

export interface QuizOption {
  id: number;
  optionText: string;
  sortOrder: number;
}

export interface QuizQuestion {
  id: number;
  questionText: string;
  questionType: QuestionType;
  points: number;
  sortOrder: number;
  options: QuizOption[];
}

export interface Quiz {
  id: number;
  lessonId: number;
  title: string;
  description?: string;
  passingScorePercentage: number;
  maxAttempts: number;
  timeLimitSeconds: number;
  questionsCount: number;
  questions: QuizQuestion[];
}

export interface QuizSubmitRequest {
  quizId: number;
  selectedOptionIds: Record<number, number[]>;
}

export interface QuizResult {
  submissionId: number;
  quizId: number;
  scorePercentage: number;
  passed: boolean;
  correctCount: number;
  totalCount: number;
  passingScorePercentage: number;
  questionResults: Record<number, boolean>;
  questionExplanations?: Record<number, string>;
}

export interface OutboxMetrics {
  pendingCount: number;
  processingCount: number;
  completedCount: number;
  failedCount: number;
}

export interface StudentRisk {
  userId: number;
  userEmail: string;
  userName: string;
  courseId: number;
  courseTitle: string;
  currentDay: number;
  daysInactive: number;
  lastActiveDate: string;
  riskLevel: 'LOW' | 'MEDIUM' | 'HIGH';
  recommendedNudge: string;
}

export interface SemanticLink {
  term: string;
  category?: string;
  definition: string;
  similarityScore: number;
  previewSnippet?: string;
}

export interface LessonFunnelItem {
  lessonId: number;
  dayNumber: number;
  title: string;
  completedCount: number;
  conversionRate: number;
  dropOffRate: number;
  hwSubmissionsCount?: number;
  hwRejectionsCount?: number;
  isBottleneck?: boolean;
}

export interface CourseFunnelStep {
  stepOrder: number;
  stepName: string;
  dayNumber?: number;
  lessonId?: number;
  lessonTitle?: string;
  studentsCount: number;
  conversionRate: number;
  dropOffRate: number;
  hwSubmissionsCount?: number;
  hwRejectionsCount?: number;
  isBottleneck?: boolean;
}

export interface AdminAnalytics {
  totalStudents: number;
  totalEnrollments: number;
  totalCompletions: number;
  totalCertificates: number;
  overallCompletionRate: number;
  funnel: LessonFunnelItem[];
}

export type HelpRequestStatus = 'OPEN' | 'IN_PROGRESS' | 'RESOLVED' | 'RESOLVED_WITH_FAQ';

export interface HelpRequest {
  id: number;
  userId: number;
  studentName: string;
  studentEmail: string;
  courseId: number;
  courseTitle: string;
  lessonId: number;
  lessonTitle: string;
  lessonDayNumber?: number;
  stepIdentifier: string;
  stepTitle?: string;
  problemText: string;
  errorLogs?: string;
  status: HelpRequestStatus;
  mentorSolution?: string;
  resolvedBy?: number;
  resolvedAt?: string;
  createdAt: string;
}

export interface CreateHelpRequestPayload {
  stepIdentifier: string;
  stepTitle?: string;
  problemText: string;
  errorLogs?: string;
}

export interface ResolveHelpRequestPayload {
  status: HelpRequestStatus;
  mentorSolution?: string;
}

