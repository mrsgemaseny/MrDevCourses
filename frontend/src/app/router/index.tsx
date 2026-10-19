import React, { Suspense } from 'react';
import { createBrowserRouter, Navigate } from 'react-router-dom';
import { App } from '../App';
import { ProtectedRoute } from './ProtectedRoute';
import { AdminLayout } from '../layout/AdminLayout';

import { AuthLayout } from '../layout/AuthLayout';

const PageLoader: React.FC = () => (
  <div className="flex items-center justify-center min-h-[60vh]" data-testid="page-loader">
    <div className="flex flex-col items-center gap-3">
      <div className="w-8 h-8 border-2 border-white/5 border-t-[#fafafa] rounded-full animate-spin" />
      <span className="text-xs text-zinc-500 font-mono">Загрузка...</span>
    </div>
  </div>
);

const RouteErrorFallback: React.FC = () => (
  <div className="flex items-center justify-center min-h-screen bg-[#0a0a0c] text-zinc-100 px-4">
    <div className="max-w-md w-full p-8 bg-[#0e0e11] border border-white/10 text-center space-y-4 font-mono">
      <div className="text-xs uppercase tracking-widest text-zinc-400">Обновление версии сайта</div>
      <h1 className="text-lg font-bold text-white">Доступна новая сборка платформы</h1>
      <p className="text-xs text-zinc-500 leading-relaxed">
        Кэш браузера обращается к старой версии модуля после обновления. Перезагрузите страницу для загрузки актуальной версии.
      </p>
      <button
        type="button"
        onClick={() => window.location.reload()}
        className="px-5 py-2.5 bg-white hover:bg-zinc-200 text-black text-xs font-semibold transition-colors cursor-pointer"
      >
        Обновить страницу
      </button>
    </div>
  </div>
);

function lazyWithRetry(
  importer: () => Promise<{ default: React.FC }>
): React.LazyExoticComponent<React.FC> {
  return React.lazy(async () => {
    try {
      return await importer();
    } catch (error) {
      const storageKey = 'mrdev_chunk_reload';
      const hasReloaded = typeof window !== 'undefined' && window.sessionStorage?.getItem(storageKey);
      if (typeof window !== 'undefined' && !hasReloaded) {
        window.sessionStorage?.setItem(storageKey, '1');
        window.location.reload();
      }
      throw error;
    }
  });
}

// Lazy Loaded Pages
const LandingPage = lazyWithRetry(() => import('@/pages/landing/LandingPage').then((m) => ({ default: m.LandingPage })));
const CoursesPage = lazyWithRetry(() => import('@/pages/courses/CoursesPage').then((m) => ({ default: m.CoursesPage })));
const CourseDetailPage = lazyWithRetry(() => import('@/pages/course/CourseDetailPage').then((m) => ({ default: m.CourseDetailPage })));
const LessonPage = lazyWithRetry(() => import('@/pages/lesson/LessonPage').then((m) => ({ default: m.LessonPage })));
const DashboardPage = lazyWithRetry(() => import('@/pages/dashboard/DashboardPage').then((m) => ({ default: m.DashboardPage })));
const AdminPage = lazyWithRetry(() => import('@/pages/admin/AdminPage').then((m) => ({ default: m.AdminPage })));
const AdminCurriculumPage = lazyWithRetry(() => import('@/pages/admin/AdminCurriculumPage').then((m) => ({ default: m.AdminCurriculumPage })));
const AdminStudentsPage = lazyWithRetry(() => import('@/pages/admin/AdminStudentsPage').then((m) => ({ default: m.AdminStudentsPage })));
const AdminHomeworksPage = lazyWithRetry(() => import('@/pages/admin/AdminHomeworksPage').then((m) => ({ default: m.AdminHomeworksPage })));
const AdminAnalyticsPage = lazyWithRetry(() => import('@/pages/admin/AdminAnalyticsPage').then((m) => ({ default: m.AdminAnalyticsPage })));
const AdminAuditPage = lazyWithRetry(() => import('@/pages/admin/AdminAuditPage').then((m) => ({ default: m.AdminAuditPage })));
const AdminSystemPage = lazyWithRetry(() => import('@/pages/admin/AdminSystemPage').then((m) => ({ default: m.AdminSystemPage })));
const LoginPage = lazyWithRetry(() => import('@/pages/LoginPage').then((m) => ({ default: m.LoginPage })));
const AuthCallbackPage = lazyWithRetry(() => import('@/pages/AuthCallbackPage').then((m) => ({ default: m.AuthCallbackPage })));
const CertificateVerifyPage = lazyWithRetry(() => import('@/pages/certificate/CertificateVerifyPage').then((m) => ({ default: m.CertificateVerifyPage })));
const ProjectsPage = lazyWithRetry(() => import('@/pages/projects/ProjectsPage').then((m) => ({ default: m.ProjectsPage })));
const ProfilePage = lazyWithRetry(() => import('@/pages/profile/ProfilePage').then((m) => ({ default: m.ProfilePage })));
const DocsPage = lazyWithRetry(() => import('@/pages/docs/DocsPage').then((m) => ({ default: m.DocsPage })));
const GalleryPage = lazyWithRetry(() => import('@/pages/gallery/GalleryPage').then((m) => ({ default: m.GalleryPage })));
const PrivacyPage = lazyWithRetry(() => import('@/pages/legal/PrivacyPage').then((m) => ({ default: m.PrivacyPage })));
const TermsPage = lazyWithRetry(() => import('@/pages/legal/TermsPage').then((m) => ({ default: m.TermsPage })));
const RefundPolicyPage = lazyWithRetry(() => import('@/pages/legal/RefundPolicyPage').then((m) => ({ default: m.RefundPolicyPage })));
const InviteRedeemPage = lazyWithRetry(() => import('@/pages/invite/InviteRedeemPage').then((m) => ({ default: m.InviteRedeemPage })));

const wrap = (element: React.ReactNode) => (
  <Suspense fallback={<PageLoader />}>{element}</Suspense>
);

export const router = createBrowserRouter([
  // ── Auth Pages (Isolated Minimal Layout, No Header/Footer) ─────
  {
    element: <AuthLayout />,
    errorElement: <RouteErrorFallback />,
    children: [
      { path: 'auth', element: wrap(<LoginPage />) },
      { path: 'login', element: wrap(<LoginPage />) },
      { path: 'auth/callback', element: wrap(<AuthCallbackPage />) },
    ],
  },
  {
    path: '/',
    element: <App />,
    errorElement: <RouteErrorFallback />,
    children: [
      // ── Public Pages ───────────────────────────────────────────────
      { index: true, element: wrap(<LandingPage />) },
      { path: 'projects', element: wrap(<ProjectsPage />) },
      { path: 'gallery', element: wrap(<GalleryPage />) },
      { path: 'docs', element: wrap(<DocsPage />) },
      { path: 'glossary', element: wrap(<DocsPage />) },
      { path: 'certificates/verify/:code', element: wrap(<CertificateVerifyPage />) },
      { path: 'invite/:code', element: wrap(<InviteRedeemPage />) },
      { path: 'privacy', element: wrap(<PrivacyPage />) },
      { path: 'terms', element: wrap(<TermsPage />) },
      { path: 'refund', element: wrap(<RefundPolicyPage />) },

      // ── Protected Student/Core App Pages (Auth Required) ───────────
      {
        path: 'courses',
        element: (
          <ProtectedRoute>
            {wrap(<CoursesPage />)}
          </ProtectedRoute>
        ),
      },
      {
        path: 'courses/:slug',
        element: (
          <ProtectedRoute>
            {wrap(<CourseDetailPage />)}
          </ProtectedRoute>
        ),
      },
      {
        path: 'courses/:courseId/lessons/:lessonId',
        element: (
          <ProtectedRoute>
            {wrap(<LessonPage />)}
          </ProtectedRoute>
        ),
      },
      {
        path: 'dashboard',
        element: (
          <ProtectedRoute>
            {wrap(<DashboardPage />)}
          </ProtectedRoute>
        ),
      },
      {
        path: 'profile',
        element: (
          <ProtectedRoute>
            {wrap(<ProfilePage />)}
          </ProtectedRoute>
        ),
      },
    ],
  },

  // ── Protected Admin Suite (Dedicated Admin Shell with Sidebar) ────
  {
    path: 'admin',
    element: (
      <ProtectedRoute adminOnly>
        <AdminLayout />
      </ProtectedRoute>
    ),
    children: [
      { index: true, element: wrap(<AdminPage />) },
      { path: 'curriculum', element: wrap(<AdminCurriculumPage />) },
      { path: 'courses', element: wrap(<AdminCurriculumPage />) },
      { path: 'students', element: wrap(<AdminStudentsPage />) },
      { path: 'homeworks', element: wrap(<AdminHomeworksPage />) },
      { path: 'analytics', element: wrap(<AdminAnalyticsPage />) },
      { path: 'audit', element: wrap(<AdminAuditPage />) },
      { path: 'system', element: wrap(<AdminSystemPage />) },
    ],
  },

  // ── Fallback ──────────────────────────────────────────────────────
  { path: '*', element: <Navigate to="/" replace /> },
], {
  basename: import.meta.env.BASE_URL,
});
