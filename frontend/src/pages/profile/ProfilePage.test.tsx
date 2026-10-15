import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { ProfilePage } from './ProfilePage';
import { AuthContext } from '@/features/auth/model/authContext';
import { userApi } from '@/entities/user/api/userApi';

vi.mock('@/entities/user/api/userApi', () => ({
  userApi: {
    getProfile: vi.fn().mockResolvedValue({
      id: 1,
      email: 'student@mrdev.com',
      name: 'Azamat Student',
      avatarUrl: 'https://avatars.githubusercontent.com/u/100',
      role: 'STUDENT',
      telegramUsername: 'azamat_tg',
      githubUsername: 'azamat-gh',
      bio: 'Junior Developer',
      goal: 'Запустить свой первый AI SaaS продукт',
      enrolledCoursesCount: 1,
      completedLessonsCount: 4,
      certificatesCount: 1,
      createdAt: '2026-08-01T00:00:00Z',
    }),
    updateProfile: vi.fn().mockResolvedValue({
      id: 1,
      email: 'student@mrdev.com',
      name: 'Azamat Senior',
      avatarUrl: 'https://avatars.githubusercontent.com/u/100',
      role: 'STUDENT',
      telegramUsername: 'azamat_senior',
      githubUsername: 'azamat-gh',
      bio: 'Full-Stack Developer',
      goal: 'Запустить свой первый AI SaaS продукт',
      enrolledCoursesCount: 1,
      completedLessonsCount: 4,
      certificatesCount: 1,
      createdAt: '2026-08-01T00:00:00Z',
    }),
  },
}));

describe('ProfilePage Component', () => {
  let queryClient: QueryClient;

  beforeEach(() => {
    vi.clearAllMocks();
    queryClient = new QueryClient({
      defaultOptions: { queries: { retry: false } },
    });
  });

  it('renders student profile header, stats, and populated form', async () => {
    render(
      <MemoryRouter>
        <QueryClientProvider client={queryClient}>
          <AuthContext.Provider
            value={{
              user: { id: 1, name: 'Azamat Student', email: 'student@mrdev.com', role: 'STUDENT' as any, createdAt: '2026-08-01T00:00:00Z' },
              isAuthenticated: true,
              isAdmin: false,
              isLoading: false,
              loginWithGoogle: vi.fn(),
              loginWithEmail: vi.fn(),
              register: vi.fn(),
              logout: vi.fn(),
              checkAuth: vi.fn(),
            }}
          >
            <ProfilePage />
          </AuthContext.Provider>
        </QueryClientProvider>
      </MemoryRouter>
    );

    expect(await screen.findByText('Профиль и настройки аккаунта')).toBeInTheDocument();
    expect(screen.getByText('student@mrdev.com')).toBeInTheDocument();
    expect(screen.getByText('Время обучения')).toBeInTheDocument();
    expect(screen.getByText('Сделано проектов')).toBeInTheDocument();
    expect(screen.getByText('Пройдено уроков')).toBeInTheDocument();
    expect(await screen.findByDisplayValue('Azamat Student')).toBeInTheDocument();
    expect(screen.getByDisplayValue('azamat_tg')).toBeInTheDocument();
    expect(screen.getByDisplayValue('azamat-gh')).toBeInTheDocument();
  });

  it('updates form fields and dispatches updateProfile mutation', async () => {
    const checkAuthMock = vi.fn();

    render(
      <MemoryRouter>
        <QueryClientProvider client={queryClient}>
          <AuthContext.Provider
            value={{
              user: { id: 1, name: 'Azamat Student', email: 'student@mrdev.com', role: 'STUDENT' as any, createdAt: '2026-08-01T00:00:00Z' },
              isAuthenticated: true,
              isAdmin: false,
              isLoading: false,
              loginWithGoogle: vi.fn(),
              loginWithEmail: vi.fn(),
              register: vi.fn(),
              logout: vi.fn(),
              checkAuth: checkAuthMock,
            }}
          >
            <ProfilePage />
          </AuthContext.Provider>
        </QueryClientProvider>
      </MemoryRouter>
    );

    const nameInput = await screen.findByDisplayValue('Azamat Student');
    fireEvent.change(nameInput, { target: { value: 'Azamat Senior' } });

    const submitBtn = screen.getByRole('button', { name: /Сохранить изменения/i });
    fireEvent.click(submitBtn);

    await waitFor(() => {
      expect(userApi.updateProfile).toHaveBeenCalledWith(
        expect.objectContaining({
          name: 'Azamat Senior',
        })
      );
    });
  });
});