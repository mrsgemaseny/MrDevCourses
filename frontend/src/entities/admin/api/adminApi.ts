import { apiClient } from '@/shared/api/base';
import { ApiResponse, Course, CourseModule, Enrollment, LessonDetail, LessonMaterial, LessonType, MaterialType, QuestionType, Quiz, Student } from '@/shared/types';

export interface CreateCoursePayload {
  title: string;
  description?: string;
  slug: string;
  active: boolean;
}

export interface UpdateCoursePayload {
  title: string;
  description?: string;
  slug: string;
  active: boolean;
}

export interface CreateModulePayload {
  title: string;
  description?: string;
  sortOrder?: number;
  isFreePreview?: boolean;
}

export interface UpdateModulePayload {
  title: string;
  description?: string;
  sortOrder?: number;
  isFreePreview?: boolean;
}

export interface ReorderItemPayload {
  id: number;
  sortOrder?: number;
  moduleId?: number;
}

export interface CreateLessonPayload {
  title: string;
  content?: string;
  checklist?: string;
  youtubeUrl?: string;
  dayNumber: number;
  sortOrder?: number;
  moduleId?: number;
  lessonType?: LessonType;
  durationMinutes?: number;
  isFreePreview?: boolean;
  isPublished?: boolean;
}

export interface UpdateLessonPayload {
  title: string;
  content?: string;
  checklist?: string;
  youtubeUrl?: string;
  dayNumber: number;
  sortOrder?: number;
  moduleId?: number;
  lessonType?: LessonType;
  durationMinutes?: number;
  isFreePreview?: boolean;
  isPublished?: boolean;
}

export interface CreateMaterialPayload {
  title: string;
  materialType: MaterialType;
  url: string;
  fileSizeBytes?: number;
  sortOrder?: number;
}

export interface CreateQuizOptionPayload {
  id?: number;
  optionText: string;
  isCorrect?: boolean;
  sortOrder?: number;
}

export interface CreateQuizQuestionPayload {
  id?: number;
  questionText: string;
  questionType?: QuestionType;
  explanation?: string;
  points?: number;
  sortOrder?: number;
  options?: CreateQuizOptionPayload[];
}

export interface CreateQuizPayload {
  title: string;
  description?: string;
  passingScorePercentage?: number;
  maxAttempts?: number;
  timeLimitSeconds?: number;
  questions?: CreateQuizQuestionPayload[];
}

export const adminApi = {
  // Course Endpoints
  getCourses: async (): Promise<Course[]> => {
    const res = await apiClient.get<ApiResponse<Course[]>>('/v1/admin/courses');
    return res.data.data;
  },

  createCourse: async (data: CreateCoursePayload): Promise<Course> => {
    const res = await apiClient.post<ApiResponse<Course>>('/v1/admin/courses', data);
    return res.data.data;
  },

  updateCourse: async (courseId: number, data: UpdateCoursePayload): Promise<Course> => {
    const res = await apiClient.put<ApiResponse<Course>>(`/v1/admin/courses/${courseId}`, data);
    return res.data.data;
  },

  deleteCourse: async (courseId: number): Promise<void> => {
    await apiClient.delete<ApiResponse<void>>(`/v1/admin/courses/${courseId}`);
  },

  // Module Endpoints
  getModules: async (courseId: number): Promise<CourseModule[]> => {
    const res = await apiClient.get<ApiResponse<CourseModule[]>>(`/v1/admin/courses/${courseId}/modules`);
    return res.data.data;
  },

  createModule: async (courseId: number, data: CreateModulePayload): Promise<CourseModule> => {
    const res = await apiClient.post<ApiResponse<CourseModule>>(`/v1/admin/courses/${courseId}/modules`, data);
    return res.data.data;
  },

  updateModule: async (moduleId: number, data: UpdateModulePayload): Promise<CourseModule> => {
    const res = await apiClient.put<ApiResponse<CourseModule>>(`/v1/admin/modules/${moduleId}`, data);
    return res.data.data;
  },

  deleteModule: async (moduleId: number): Promise<void> => {
    await apiClient.delete<ApiResponse<void>>(`/v1/admin/modules/${moduleId}`);
  },

  reorderModules: async (courseId: number, items: ReorderItemPayload[]): Promise<CourseModule[]> => {
    const res = await apiClient.put<ApiResponse<CourseModule[]>>(`/v1/admin/courses/${courseId}/modules/reorder`, items);
    return res.data.data;
  },

  // Lesson Endpoints
  getLessons: async (courseId: number): Promise<LessonDetail[]> => {
    const res = await apiClient.get<ApiResponse<LessonDetail[]>>(`/v1/admin/courses/${courseId}/lessons`);
    return res.data.data;
  },

  createLesson: async (courseId: number, data: CreateLessonPayload): Promise<LessonDetail> => {
    const res = await apiClient.post<ApiResponse<LessonDetail>>(`/v1/admin/courses/${courseId}/lessons`, data);
    return res.data.data;
  },

  updateLesson: async (lessonId: number, data: UpdateLessonPayload): Promise<LessonDetail> => {
    const res = await apiClient.put<ApiResponse<LessonDetail>>(`/v1/admin/lessons/${lessonId}`, data);
    return res.data.data;
  },

  deleteLesson: async (lessonId: number): Promise<void> => {
    await apiClient.delete<ApiResponse<void>>(`/v1/admin/lessons/${lessonId}`);
  },

  reorderLessons: async (courseId: number, items: ReorderItemPayload[]): Promise<LessonDetail[]> => {
    const res = await apiClient.put<ApiResponse<LessonDetail[]>>(`/v1/admin/courses/${courseId}/lessons/reorder`, items);
    return res.data.data;
  },

  // Material Endpoints
  addMaterial: async (lessonId: number, data: CreateMaterialPayload): Promise<LessonMaterial> => {
    const res = await apiClient.post<ApiResponse<LessonMaterial>>(`/v1/admin/lessons/${lessonId}/materials`, data);
    return res.data.data;
  },

  deleteMaterial: async (materialId: number): Promise<void> => {
    await apiClient.delete<ApiResponse<void>>(`/v1/admin/materials/${materialId}`);
  },

  // Quiz Endpoints
  getQuiz: async (lessonId: number): Promise<Quiz | null> => {
    try {
      const res = await apiClient.get<ApiResponse<Quiz>>(`/v1/admin/lessons/${lessonId}/quiz`);
      return res.data.data;
    } catch {
      return null;
    }
  },

  saveQuiz: async (lessonId: number, data: CreateQuizPayload): Promise<Quiz> => {
    const res = await apiClient.post<ApiResponse<Quiz>>(`/v1/admin/lessons/${lessonId}/quiz`, data);
    return res.data.data;
  },

  deleteQuiz: async (quizId: number): Promise<void> => {
    await apiClient.delete<ApiResponse<void>>(`/v1/admin/quizzes/${quizId}`);
  },

  // Student Endpoints
  getStudents: async (): Promise<Student[]> => {
    const res = await apiClient.get<ApiResponse<PageResponse<Student>>>('/v1/admin/students');
    return res.data.data?.content ?? [];
  },

  enrollStudent: async (userId: number, courseId: number): Promise<Enrollment> => {
    const res = await apiClient.post<ApiResponse<Enrollment>>(`/v1/admin/students/${userId}/enroll/${courseId}`);
    return res.data.data;
  },

  // Audit Endpoints
  getAuditLogs: async (params?: { page?: number; size?: number; userId?: number; action?: string }): Promise<PageResponse<AuditLog>> => {
    const res = await apiClient.get<ApiResponse<PageResponse<AuditLog>>>('/v1/admin/audit-logs', { params });
    return res.data.data;
  },

  // System Health & Monitoring Endpoints
  getSystemHealth: async (): Promise<SystemHealth> => {
    const res = await apiClient.get<ApiResponse<SystemHealth>>('/v1/admin/system/health');
    return res.data.data;
  },

  getRateLimits: async (): Promise<RateLimitTelemetry> => {
    const res = await apiClient.get<ApiResponse<RateLimitTelemetry>>('/v1/admin/system/rate-limits');
    return res.data.data;
  },
};

export interface AuditLog {
  id: number;
  userId?: number;
  userEmail?: string;
  userName?: string;
  action: string;
  entityType?: string;
  entityName?: string;
  entityId?: number;
  details?: string;
  ipAddress?: string;
  createdAt: string;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}

export interface HikariPoolStats {
  poolName: string;
  activeConnections: number;
  idleConnections: number;
  totalConnections: number;
  threadsAwaitingConnection: number;
  maxPoolSize: number;
  minIdle: number;
}

export interface DatabaseStats {
  status: string;
  databaseName: string;
  databaseVersion: string;
  uptimeSeconds?: number;
  postmasterStartTime?: string;
  responseTimeMs: number;
}

export interface FlywayStats {
  currentVersion: string;
  currentDescription: string;
  state: string;
  totalMigrations: number;
  installedOn: string;
}

export interface OutboxStats {
  pendingCount: number;
  processingCount: number;
  completedCount: number;
  failedCount: number;
  totalCount: number;
}

export interface JvmStats {
  totalMemoryBytes: number;
  freeMemoryBytes: number;
  usedMemoryBytes: number;
  maxMemoryBytes: number;
  availableProcessors: number;
  activeThreads: number;
  uptimeMs: number;
}

export interface SystemHealth {
  status: string;
  databasePool?: HikariPoolStats;
  database?: DatabaseStats;
  flyway?: FlywayStats;
  outboxQueue?: OutboxStats;
  jvm?: JvmStats;
  timestamp?: string;

  // Flattened helpers
  dbStatus?: string;
  uptimeSeconds?: number;
  jvmFreeMemoryMb?: number;
  jvmTotalMemoryMb?: number;
  jvmMaxMemoryMb?: number;
  activeThreads?: number;
}

export interface RateLimitTier {
  name: string;
  capacity: number;
  refillTokens: number;
  refillPeriod: string;
  activeBucketsCount: number;
  throttledCount: number;
}

export interface RateLimitTelemetry {
  totalActiveBuckets: number;
  totalThrottledRequests: number;
  tiers?: Record<string, RateLimitTier>;
  recentThrottles?: Array<{
    ip: string;
    tier: string;
    path: string;
    throttledAt: string;
  }>;
  timestamp?: string;

  // Flattened helpers
  authLimitRemaining?: number;
  aiLimitRemaining?: number;
  generalLimitRemaining?: number;
  totalRejectedRequests?: number;
}
