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

// Lazy Loaded Pages
const LandingPage = React.lazy(() => import('@/pages/landing/LandingPage').then((m) => ({ default: m.LandingPage })));
const CoursesPage = React.lazy(() => import('@/pages/courses/CoursesPage').then((m) => ({ default: m.CoursesPage })));
const CourseDetailPage = React.lazy(() => import('@/pages/course/CourseDetailPage').then((m) => ({ default: m.CourseDetailPage })));
const LessonPage = React.lazy(() => import('@/pages/lesson/LessonPage').then((m) => ({ default: m.LessonPage })));
const DashboardPage = React.lazy(() => import('@/pages/dashboard/DashboardPage').then((m) => ({ default: m.DashboardPage })));
const AdminPage = React.lazy(() => import('@/pages/admin/AdminPage').then((m) => ({ default: m.AdminPage })));
const AdminCurriculumPage = React.lazy(() => import('@/pages/admin/AdminCurriculumPage').then((m) => ({ default: m.AdminCurriculumPage })));
const AdminStudentsPage = React.lazy(() => import('@/pages/admin/AdminStudentsPage').then((m) => ({ default: m.AdminStudentsPage })));
const AdminHomeworksPage = React.lazy(() => import('@/pages/admin/AdminHomeworksPage').then((m) => ({ default: m.AdminHomeworksPage })));
const AdminAnalyticsPage = React.lazy(() => import('@/pages/admin/AdminAnalyticsPage').then((m) => ({ default: m.AdminAnalyticsPage })));
const AdminAuditPage = React.lazy(() => import('@/pages/admin/AdminAuditPage').then((m) => ({ default: m.AdminAuditPage })));
const AdminSystemPage = React.lazy(() => import('@/pages/admin/AdminSystemPage').then((m) => ({ default: m.AdminSystemPage })));
const LoginPage = React.lazy(() => import('@/pages/LoginPage').then((m) => ({ default: m.LoginPage })));
const AuthCallbackPage = React.lazy(() => import('@/pages/AuthCallbackPage').then((m) => ({ default: m.AuthCallbackPage })));
const CertificateVerifyPage = React.lazy(() => import('@/pages/certificate/CertificateVerifyPage').then((m) => ({ default: m.CertificateVerifyPage })));
const ProjectsPage = React.lazy(() => import('@/pages/projects/ProjectsPage').then((m) => ({ default: m.ProjectsPage })));
const ProfilePage = React.lazy(() => import('@/pages/profile/ProfilePage').then((m) => ({ default: m.ProfilePage })));
const DocsPage = React.lazy(() => import('@/pages/docs/DocsPage').then((m) => ({ default: m.DocsPage })));
const GalleryPage = React.lazy(() => import('@/pages/gallery/GalleryPage').then((m) => ({ default: m.GalleryPage })));
const PrivacyPage = React.lazy(() => import('@/pages/legal/PrivacyPage').then((m) => ({ default: m.PrivacyPage })));
const TermsPage = React.lazy(() => import('@/pages/legal/TermsPage').then((m) => ({ default: m.TermsPage })));
const RefundPolicyPage = React.lazy(() => import('@/pages/legal/RefundPolicyPage').then((m) => ({ default: m.RefundPolicyPage })));
const InviteRedeemPage = React.lazy(() => import('@/pages/invite/InviteRedeemPage').then((m) => ({ default: m.InviteRedeemPage })));

const wrap = (element: React.ReactNode) => (
  <Suspense fallback={<PageLoader />}>{element}</Suspense>
);

export const router = createBrowserRouter([
  // ── Auth Pages (Isolated Minimal Layout, No Header/Footer) ─────
  {
    element: <AuthLayout />,
    children: [
      { path: 'auth', element: wrap(<LoginPage />) },
      { path: 'login', element: wrap(<LoginPage />) },
      { path: 'auth/callback', element: wrap(<AuthCallbackPage />) },
    ],
  },
  {
    path: '/',
    element: <App />,
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
