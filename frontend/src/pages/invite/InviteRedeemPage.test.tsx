import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import { InviteRedeemPage } from './InviteRedeemPage';
import { inviteApi } from '@/entities/invite/api/inviteApi';
import * as authFeature from '@/features/auth';
import { CourseInvite, RedeemInviteResult } from '@/shared/types';

vi.mock('@/entities/invite/api/inviteApi', () => ({
  inviteApi: {
    getInviteByCode: vi.fn(),
    redeemInvite: vi.fn(),
  },
}));

vi.mock('@/features/auth', () => ({
  useAuth: vi.fn(),
}));

const mockInvite: CourseInvite = {
  id: 1,
  code: 'VIBE-DEMO-TEST',
  courseId: 10,
  courseTitle: 'Full-Stack Spring Boot & React',
  courseSlug: 'fullstack-spring-react',
  cohortId: 2,
  cohortName: 'Поток Весна 2026',
  maxUses: 1,
  usedCount: 0,
  expiresAt: '2026-12-31T23:59:59Z',
  note: 'Оплата Kaspi',
  valid: true,
  expired: false,
  exhausted: false,
  createdAt: '2026-10-15T10:00:00Z',
};

describe('InviteRedeemPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders loading state initially', () => {
    vi.mocked(authFeature.useAuth).mockReturnValue({
      user: null,
      isAuthenticated: false,
      isAdmin: false,
      isLoading: false,
      loginWithGoogle: vi.fn(),
      loginWithEmail: vi.fn(),
      register: vi.fn(),
      logout: vi.fn(),
      checkAuth: vi.fn(),
    });

    vi.mocked(inviteApi.getInviteByCode).mockImplementation(() => new Promise(() => {}));

    render(
      <MemoryRouter initialEntries={['/invite/VIBE-DEMO-TEST']}>
        <Routes>
          <Route path="/invite/:code" element={<InviteRedeemPage />} />
        </Routes>
      </MemoryRouter>
    );

    expect(screen.getByText(/Проверка инвайт-ссылки.../i)).toBeInTheDocument();
  });

  it('renders error state when invite is invalid or not found', async () => {
    vi.mocked(authFeature.useAuth).mockReturnValue({
      user: null,
      isAuthenticated: false,
      isAdmin: false,
      isLoading: false,
      loginWithGoogle: vi.fn(),
      loginWithEmail: vi.fn(),
      register: vi.fn(),
      logout: vi.fn(),
      checkAuth: vi.fn(),
    });

    vi.mocked(inviteApi.getInviteByCode).mockRejectedValue(new Error('Инвайт-код не найден'));

    render(
      <MemoryRouter initialEntries={['/invite/INVALID-CODE']}>
        <Routes>
          <Route path="/invite/:code" element={<InviteRedeemPage />} />
        </Routes>
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByText(/Ссылка недействительна/i)).toBeInTheDocument();
      expect(screen.getByText(/Инвайт-код не найден/i)).toBeInTheDocument();
    });
  });

  it('renders course information and login prompt for unauthenticated guest', async () => {
    vi.mocked(authFeature.useAuth).mockReturnValue({
      user: null,
      isAuthenticated: false,
      isAdmin: false,
      isLoading: false,
      loginWithGoogle: vi.fn(),
      loginWithEmail: vi.fn(),
      register: vi.fn(),
      logout: vi.fn(),
      checkAuth: vi.fn(),
    });

    vi.mocked(inviteApi.getInviteByCode).mockResolvedValue(mockInvite);

    render(
      <MemoryRouter initialEntries={['/invite/VIBE-DEMO-TEST']}>
        <Routes>
          <Route path="/invite/:code" element={<InviteRedeemPage />} />
        </Routes>
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByText('Full-Stack Spring Boot & React')).toBeInTheDocument();
      expect(screen.getByText(/Поток Весна 2026/i)).toBeInTheDocument();
      expect(screen.getByText(/Войти и активировать/i)).toBeInTheDocument();
    });
  });

  it('renders redeem button for authenticated student and completes redemption', async () => {
    vi.mocked(authFeature.useAuth).mockReturnValue({
      user: { id: 42, email: 'student@vibe.kz', role: 'STUDENT', createdAt: '2026-08-01T00:00:00Z' },
      isAuthenticated: true,
      isAdmin: false,
      isLoading: false,
      loginWithGoogle: vi.fn(),
      loginWithEmail: vi.fn(),
      register: vi.fn(),
      logout: vi.fn(),
      checkAuth: vi.fn(),
    });

    vi.mocked(inviteApi.getInviteByCode).mockResolvedValue(mockInvite);

    const mockResult: RedeemInviteResult = {
      courseId: 10,
      courseTitle: 'Full-Stack Spring Boot & React',
      courseSlug: 'fullstack-spring-react',
      cohortId: 2,
      cohortName: 'Поток Весна 2026',
      enrolledAt: '2026-10-15T12:00:00Z',
      alreadyEnrolled: false,
      message: 'Доступ к курсу успешно активирован',
    };
    vi.mocked(inviteApi.redeemInvite).mockResolvedValue(mockResult);

    render(
      <MemoryRouter initialEntries={['/invite/VIBE-DEMO-TEST']}>
        <Routes>
          <Route path="/invite/:code" element={<InviteRedeemPage />} />
        </Routes>
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /Активировать доступ/i })).toBeInTheDocument();
    });

    fireEvent.click(screen.getByRole('button', { name: /Активировать доступ/i }));

    await waitFor(() => {
      expect(inviteApi.redeemInvite).toHaveBeenCalledWith('VIBE-DEMO-TEST');
      expect(screen.getByText('Доступ активирован')).toBeInTheDocument();
      expect(screen.getByText(/Перейти к обучению/i)).toBeInTheDocument();
    });
  });
});
