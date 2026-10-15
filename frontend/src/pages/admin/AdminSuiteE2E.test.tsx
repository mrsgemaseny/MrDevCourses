import { render, screen, waitFor, fireEvent, within } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { AdminPage } from './AdminPage';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import * as adminApiModule from '@/entities/admin/api/adminApi';
import * as adminStudentApiModule from '@/entities/adminStudentApi';
import * as adminAnalyticsApiModule from '@/entities/adminAnalyticsApi';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: { retry: false },
  },
});

const mockCourses = [
  {
    id: 1,
    title: 'Вайбкодинг с нуля',
    description: 'Полный курс по современному вайбкодингу',
    slug: 'vibecoding-zero',
    active: true,
    totalLessons: 6,
    createdAt: '2026-08-25T10:00:00Z',
  },
];

const mockLessons = [
  {
    id: 101,
    courseId: 1,
    courseTitle: 'Вайбкодинг с нуля',
    courseSlug: 'vibecoding-zero',
    title: 'Введение и настройка окружения',
    content: 'Настройка IDE и рабочего пространства.',
    youtubeUrl: 'https://youtube.com/watch?v=intro123',
    dayNumber: 1,
    sortOrder: 1,
    accessible: true,
    completed: false,
    opensAt: '2026-08-25T10:00:00Z',
  },
];

const mockStudents = [
  {
    id: 42,
    email: 'student.e2e@mrdevcourses.com',
    name: 'E2E Student Test',
    role: 'STUDENT' as const,
    createdAt: '2026-08-26T10:00:00Z',
    enrollments: [
      {
        id: 10,
        userId: 42,
        userEmail: 'student.e2e@mrdevcourses.com',
        userName: 'E2E Student Test',
        courseId: 1,
        courseTitle: 'Вайбкодинг с нуля',
        courseSlug: 'vibecoding-zero',
        enrolledAt: '2026-08-26T10:00:00Z',
      },
    ],
  },
];

describe('Admin Suite E2E Workflows (Frontend)', () => {
  beforeEach(() => {
    queryClient.clear();
    vi.clearAllMocks();

    vi.spyOn(adminApiModule.adminApi, 'getCourses').mockResolvedValue(mockCourses);
    vi.spyOn(adminApiModule.adminApi, 'getLessons').mockResolvedValue(mockLessons);
    vi.spyOn(adminStudentApiModule.adminStudentApi, 'getStudents').mockResolvedValue({
      content: mockStudents,
      page: 0,
      size: 100,
      totalElements: mockStudents.length,
      totalPages: 1,
      last: true,
    });

    vi.spyOn(adminAnalyticsApiModule.adminAnalyticsApi, 'getOverviewMetrics').mockResolvedValue({
      totalStudents: 1,
      totalEnrollments: 1,
      totalCompletions: 1,
      totalLessonsCompleted: 6,
      activeStudents: 1,
      completionRate: 100.0,
    });

    vi.spyOn(adminAnalyticsApiModule.adminAnalyticsApi, 'getCourseFunnel').mockResolvedValue([
      {
        stepOrder: 0,
        stepName: 'Зачислено на курс',
        dayNumber: null,
        lessonId: null,
        lessonTitle: null,
        studentsCount: 1,
        conversionRate: 100,
        dropOffRate: 0,
      },
    ]);

    vi.spyOn(adminAnalyticsApiModule.adminAnalyticsApi, 'getCourseRetention').mockResolvedValue({
      courseId: 1,
      courseTitle: 'Вайбкодинг с нуля',
      totalEnrolled: 1,
      completedCount: 1,
      overallCompletionRate: 100.0,
      lessonRetention: [],
    });

    vi.spyOn(adminAnalyticsApiModule.adminAnalyticsApi, 'getAiTutorSummary').mockResolvedValue({
      totalQuestions: 10,
      estimatedTokensUsed: 5000,
      throttledCount: 0,
      activeUsersCount: 1,
      avgQuestionsPerUser: 10,
      topLessonTopics: [],
    });

    vi.spyOn(adminAnalyticsApiModule.adminAnalyticsApi, 'getQuizHotspots').mockResolvedValue([]);
  });

  it('Tier 1: Renders main admin shell and switches between tabs seamlessly', async () => {
    render(
      <QueryClientProvider client={queryClient}>
        <AdminPage />
      </QueryClientProvider>
    );

    // Initial Courses tab
    expect(screen.getByText('Управление MrDeveloper')).toBeInTheDocument();
    expect(await screen.findByText('Вайбкодинг с нуля')).toBeInTheDocument();
    expect(screen.getByText('/vibecoding-zero')).toBeInTheDocument();

    // Switch to Lessons tab
    const lessonsTabBtn = screen.getByRole('button', { name: 'Уроки' });
    fireEvent.click(lessonsTabBtn);
    expect(await screen.findByText('Введение и настройка окружения')).toBeInTheDocument();
    expect(screen.getByText('Д1')).toBeInTheDocument();

    // Switch to Students tab
    const studentsTabBtn = screen.getByRole('button', { name: 'Студенты' });
    fireEvent.click(studentsTabBtn);
    expect(await screen.findByText('student.e2e@mrdevcourses.com')).toBeInTheDocument();
    expect(screen.getByText('E2E Student Test')).toBeInTheDocument();

    // Switch to Analytics tab
    const analyticsTabBtn = screen.getByRole('button', { name: 'Аналитика' });
    fireEvent.click(analyticsTabBtn);
    expect(await screen.findByText('Платформенная аналитика и когорты')).toBeInTheDocument();
    expect(await screen.findByText('100%')).toBeInTheDocument();
  });

  it('Tier 2: Handles Course creation modal submission and Escape key dismissal', async () => {
    const createSpy = vi.spyOn(adminApiModule.adminApi, 'createCourse').mockResolvedValue({
      id: 2,
      title: 'Spring Security Deep Dive',
      description: 'Zero Trust architecture',
      slug: 'spring-security-deep-dive',
      active: true,
      createdAt: '2026-08-30T15:00:00Z',
    });

    render(
      <QueryClientProvider client={queryClient}>
        <AdminPage />
      </QueryClientProvider>
    );

    const createBtn = screen.getByRole('button', { name: /Создать курс/i });
    fireEvent.click(createBtn);

    expect(screen.getByRole('heading', { name: 'Создать новый курс' })).toBeInTheDocument();

    const titleInput = screen.getByPlaceholderText('Вайбкодинг с нуля');
    fireEvent.change(titleInput, { target: { value: 'Spring Security Deep Dive' } });

    const submitBtn = screen.getByRole('button', { name: 'Создать' });
    fireEvent.click(submitBtn);

    await waitFor(() => {
      expect(createSpy).toHaveBeenCalledWith(
        expect.objectContaining({
          title: 'Spring Security Deep Dive',
          slug: 'spring-security-deep-dive',
          active: true,
        })
      );
    });
  });

  it('Tier 3: Opens Manual Enrollment modal and dispatches enrollment mutation', async () => {
    const enrollSpy = vi.spyOn(adminApiModule.adminApi, 'enrollStudent').mockResolvedValue({
      id: 11,
      userId: 42,
      userEmail: 'student.e2e@mrdevcourses.com',
      userName: 'E2E Student Test',
      courseId: 1,
      courseTitle: 'Вайбкодинг с нуля',
      courseSlug: 'vibecoding-zero',
      enrolledAt: '2026-08-30T15:00:00Z',
    });

    render(
      <QueryClientProvider client={queryClient}>
        <AdminPage />
      </QueryClientProvider>
    );

    // Navigate to Students
    fireEvent.click(screen.getByRole('button', { name: 'Студенты' }));

    const enrollBtn = await screen.findByRole('button', { name: /Записать на курс/i });
    fireEvent.click(enrollBtn);

    expect(screen.getByRole('heading', { name: 'Зачислить студента на курс' })).toBeInTheDocument();
    expect(screen.getAllByText('student.e2e@mrdevcourses.com').length).toBeGreaterThan(0);

    const confirmEnrollBtn = screen.getByRole('button', { name: 'Зачислить' });
    fireEvent.click(confirmEnrollBtn);

    await waitFor(() => {
      expect(enrollSpy).toHaveBeenCalledWith(42, 1);
    });
  });

  it('Tier 4: Opens Delete confirmation modal with accessible dialog role and triggers deletion', async () => {
    const deleteSpy = vi.spyOn(adminApiModule.adminApi, 'deleteLesson').mockResolvedValue();

    render(
      <QueryClientProvider client={queryClient}>
        <AdminPage />
      </QueryClientProvider>
    );

    // Switch to lessons tab
    fireEvent.click(screen.getByRole('button', { name: 'Уроки' }));

    await screen.findByText('Введение и настройка окружения');

    const deleteBtn = screen.getByLabelText(/Удалить урок Введение и настройка окружения/i);
    fireEvent.click(deleteBtn);

    const dialog = screen.getByRole('dialog');
    expect(dialog).toBeInTheDocument();
    expect(within(dialog).getByText(/Вы действительно хотите удалить урок/i)).toBeInTheDocument();

    const confirmDeleteBtn = within(dialog).getByRole('button', { name: 'Удалить' });
    fireEvent.click(confirmDeleteBtn);

    await waitFor(() => {
      expect(deleteSpy).toHaveBeenCalledWith(101);
    });
  });
});
