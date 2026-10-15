import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { InviteManagerModal } from './InviteManagerModal';
import { inviteApi } from '@/entities/invite/api/inviteApi';
import { adminStudentApi } from '@/entities/adminStudentApi';
import { Course, CourseInvite } from '@/shared/types';

vi.mock('@/entities/invite/api/inviteApi', () => ({
  inviteApi: {
    getAdminInvites: vi.fn(),
    createInvite: vi.fn(),
    deleteInvite: vi.fn(),
  },
}));

vi.mock('@/entities/adminStudentApi', () => ({
  adminStudentApi: {
    getAllCohorts: vi.fn(),
  },
}));

const mockCourses: Course[] = [
  {
    id: 1,
    title: 'Vibe-Coding Course',
    slug: 'vibe-coding',
    description: 'Learn modern engineering',
    active: true,
    totalLessons: 10,
    modules: [],
    createdAt: '2026-01-01T00:00:00Z',
  },
];

const mockInvites: CourseInvite[] = [
  {
    id: 10,
    code: 'VIBE-AZA-2026',
    courseId: 1,
    courseTitle: 'Vibe-Coding Course',
    courseSlug: 'vibe-coding',
    maxUses: 1,
    usedCount: 0,
    note: 'Kaspi Azamat',
    valid: true,
    expired: false,
    exhausted: false,
    createdAt: '2026-10-15T12:00:00Z',
  },
];

describe('InviteManagerModal', () => {
  let queryClient: QueryClient;

  beforeEach(() => {
    vi.clearAllMocks();
    queryClient = new QueryClient({
      defaultOptions: {
        queries: { retry: false },
      },
    });
    vi.mocked(adminStudentApi.getAllCohorts).mockResolvedValue([]);
    vi.mocked(inviteApi.getAdminInvites).mockResolvedValue(mockInvites);
  });

  const renderComponent = (isOpen = true, onClose = vi.fn()) => {
    return render(
      <QueryClientProvider client={queryClient}>
        <InviteManagerModal courses={mockCourses} isOpen={isOpen} onClose={onClose} />
      </QueryClientProvider>
    );
  };

  it('renders modal when isOpen is true', () => {
    renderComponent();
    expect(screen.getByText('Инвайт-ссылки для зачисления')).toBeInTheDocument();
    expect(screen.getByText('Создать инвайт')).toBeInTheDocument();
    expect(screen.getByText('Все инвайты')).toBeInTheDocument();
  });

  it('does not render when isOpen is false', () => {
    const { container } = renderComponent(false);
    expect(container.firstChild).toBeNull();
  });

  it('submits invite creation form and renders generated code banner', async () => {
    const createdInvite: CourseInvite = {
      id: 11,
      code: 'VIBE-TEST-CODE',
      courseId: 1,
      courseTitle: 'Vibe-Coding Course',
      courseSlug: 'vibe-coding',
      maxUses: 1,
      usedCount: 0,
      valid: true,
      expired: false,
      exhausted: false,
      createdAt: '2026-10-15T12:00:00Z',
    };
    vi.mocked(inviteApi.createInvite).mockResolvedValue(createdInvite);

    renderComponent();

    const selects = screen.getAllByRole('combobox');
    fireEvent.change(selects[0], { target: { value: '1' } });

    const submitBtn = screen.getByRole('button', { name: /Сгенерировать ссылку/i });
    fireEvent.click(submitBtn);

    await waitFor(() => {
      expect(inviteApi.createInvite).toHaveBeenCalledWith(
        expect.objectContaining({
          courseId: 1,
          maxUses: 1,
        })
      );
      expect(screen.getByText(/Инвайт-ссылка сгенерирована/i)).toBeInTheDocument();
      expect(screen.getByDisplayValue(/VIBE-TEST-CODE/)).toBeInTheDocument();
    });
  });

  it('switches to list tab and renders existing invites', async () => {
    renderComponent();

    fireEvent.click(screen.getByText('Все инвайты'));

    await waitFor(() => {
      expect(screen.getByText('VIBE-AZA-2026')).toBeInTheDocument();
      expect(screen.getByText('Активен')).toBeInTheDocument();
      expect(screen.getByText(/Kaspi Azamat/i)).toBeInTheDocument();
    });
  });
});
