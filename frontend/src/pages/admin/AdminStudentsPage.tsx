import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  Users,
  RefreshCw,
} from 'lucide-react';
import { adminStudentApi, StudentFilterParams } from '@/entities/adminStudentApi';
import { adminApi } from '@/entities/admin/api/adminApi';
import { useAuth } from '@/features/auth';
import {
  Student,
  Course,
  PageResponse,
  UserRole,
} from '@/shared/types';
import {
  StudentSearchFilter,
  StudentTable,
  StudentProgressDrawer,
  ManualEnrollModal,
  CohortManagerModal,
} from '@/widgets/admin-students';

export const AdminStudentsPage: React.FC = () => {
  const queryClient = useQueryClient();
  const { user: currentUser } = useAuth();

  // Filter & Pagination state
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedCourseId, setSelectedCourseId] = useState<number | 'ALL'>('ALL');
  const [selectedRole, setSelectedRole] = useState<UserRole | 'ALL'>('ALL');
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(20);

  // Selected student for modals / drawer
  const [progressUserId, setProgressUserId] = useState<number | null>(null);
  const [isProgressDrawerOpen, setIsProgressDrawerOpen] = useState(false);
  const [enrollStudent, setEnrollStudent] = useState<Student | null>(null);
  const [isEnrollModalOpen, setIsEnrollModalOpen] = useState(false);
  const [isCohortModalOpen, setIsCohortModalOpen] = useState(false);

  // Toast / feedback message
  const [feedback, setFeedback] = useState<{ type: 'success' | 'error'; message: string } | null>(null);

  const showFeedback = (type: 'success' | 'error', message: string) => {
    setFeedback({ type, message });
    setTimeout(() => setFeedback(null), 4000);
  };

  // Queries
  const { data: courses = [] } = useQuery<Course[]>({
    queryKey: ['admin', 'courses'],
    queryFn: adminApi.getCourses,
  });

  const filterParams: StudentFilterParams = {
    q: searchQuery.trim() || undefined,
    role: selectedRole === 'ALL' ? undefined : selectedRole,
    courseId: selectedCourseId === 'ALL' ? undefined : selectedCourseId,
    page,
    size: pageSize,
  };

  const {
    data: pageData,
    isLoading: isStudentsLoading,
    isFetching: isStudentsFetching,
    refetch: refetchStudents,
  } = useQuery<PageResponse<Student>>({
    queryKey: ['admin', 'students', filterParams],
    queryFn: () => adminStudentApi.getStudents(filterParams),
  });

  // Mutations
  const enrollMutation = useMutation({
    mutationFn: ({ userId, courseId }: { userId: number; courseId: number }) =>
      adminStudentApi.enrollStudent(userId, courseId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'students'] });
      if (enrollStudent) {
        // Refetch or update local modal student enrollments
        refetchStudents();
      }
      showFeedback('success', 'Студент успешно зачислен на курс');
    },
    onError: (err: any) => {
      const msg = err?.response?.data?.message || err?.message || 'Ошибка зачисления на курс';
      showFeedback('error', msg);
    },
  });

  const unenrollMutation = useMutation({
    mutationFn: ({ userId, courseId }: { userId: number; courseId: number }) =>
      adminStudentApi.unenrollStudent(userId, courseId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'students'] });
      refetchStudents();
      showFeedback('success', 'Студент отчислен с курса');
    },
    onError: (err: any) => {
      const msg = err?.response?.data?.message || err?.message || 'Ошибка отчисления с курса';
      showFeedback('error', msg);
    },
  });

  const roleMutation = useMutation({
    mutationFn: ({ userId, role }: { userId: number; role: UserRole }) =>
      adminStudentApi.updateStudentRole(userId, role),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'students'] });
      refetchStudents();
      showFeedback('success', 'Роль пользователя успешно обновлена');
    },
    onError: (err: any) => {
      const msg = err?.response?.data?.message || err?.message || 'Ошибка смены роли';
      showFeedback('error', msg);
    },
  });

  const handleInspectProgress = (student: Student) => {
    setProgressUserId(student.id);
    setIsProgressDrawerOpen(true);
  };

  const handleManageEnrollments = (student: Student) => {
    // Find latest student data from pageData if available
    const freshStudent = pageData?.content.find((s) => s.id === student.id) || student;
    setEnrollStudent(freshStudent);
    setIsEnrollModalOpen(true);
  };

  const handleResetFilters = () => {
    setSearchQuery('');
    setSelectedCourseId('ALL');
    setSelectedRole('ALL');
    setPage(0);
  };

  return (
    <div className="space-y-6 animate-in fade-in duration-200">
      {/* Feedback Banner */}
      {feedback && (
        <div
          className={`p-3.5 rounded-sm border text-xs flex items-center justify-between font-mono animate-in slide-in-from-top duration-150 ${
            feedback.type === 'success'
              ? 'bg-white/10 border-white/20 text-white'
              : 'bg-zinc-900 border-white/20 text-zinc-200'
          }`}
        >
          <span>{feedback.message}</span>
          <button
            onClick={() => setFeedback(null)}
            className="text-zinc-400 hover:text-zinc-200 text-xs ml-4 cursor-pointer"
          >
            Закрыть
          </button>
        </div>
      )}

      {/* Page Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-5 border-b border-white/5">
        <div>
          <div className="flex items-center gap-2.5">
            <h1 className="text-2xl font-bold text-zinc-100 tracking-tight">
              Студенты и когорты
            </h1>
          </div>
        </div>

        {/* Quick Actions */}
        <div className="flex items-center gap-2.5">
          <button
            type="button"
            onClick={() => setIsCohortModalOpen(true)}
            className="inline-flex items-center gap-1.5 px-3.5 py-2 rounded-xl bg-zinc-800 hover:bg-zinc-700 text-zinc-200 border border-white/10 text-xs font-semibold transition-colors cursor-pointer"
          >
            <Users className="w-4 h-4 text-zinc-400" />
            <span>Управление когортами</span>
          </button>

          <button
            type="button"
            onClick={() => refetchStudents()}
            disabled={isStudentsFetching}
            className="inline-flex items-center gap-1.5 px-3.5 py-2 rounded-xl bg-zinc-800 hover:bg-zinc-700 text-zinc-200 border border-white/10 text-xs font-medium transition-colors cursor-pointer disabled:opacity-50"
            title="Обновить список"
          >
            <RefreshCw className={`w-3.5 h-3.5 ${isStudentsFetching ? 'animate-spin' : ''}`} />
            <span>Обновить</span>
          </button>
        </div>
      </div>

      {/* Search & Filter Toolbar */}
      <StudentSearchFilter
        searchQuery={searchQuery}
        onSearchChange={(q) => {
          setSearchQuery(q);
          setPage(0);
        }}
        selectedCourseId={selectedCourseId}
        onCourseChange={(c) => {
          setSelectedCourseId(c);
          setPage(0);
        }}
        selectedRole={selectedRole}
        onRoleFilterChange={(r) => {
          setSelectedRole(r);
          setPage(0);
        }}
        courses={courses}
        onReset={handleResetFilters}
        isLoading={isStudentsLoading || isStudentsFetching}
      />

      {/* Main Student Table */}
      <StudentTable
        students={pageData?.content || []}
        totalElements={pageData?.totalElements || 0}
        totalPages={pageData?.totalPages || 0}
        currentPage={page}
        pageSize={pageSize}
        onPageChange={(newPage) => setPage(newPage)}
        onPageSizeChange={(newSize) => {
          setPageSize(newSize);
          setPage(0);
        }}
        currentUserId={currentUser?.id}
        onInspectProgress={handleInspectProgress}
        onManageEnrollments={handleManageEnrollments}
        onRoleChange={async (userId, newRole) => {
          await roleMutation.mutateAsync({ userId, role: newRole });
        }}
        isLoading={isStudentsLoading}
      />

      {/* Slide-over Progress Drawer */}
      <StudentProgressDrawer
        userId={progressUserId}
        isOpen={isProgressDrawerOpen}
        onClose={() => {
          setIsProgressDrawerOpen(false);
          setProgressUserId(null);
        }}
      />

      {/* Manual Enroll Modal */}
      <ManualEnrollModal
        student={enrollStudent}
        courses={courses}
        isOpen={isEnrollModalOpen}
        onClose={() => {
          setIsEnrollModalOpen(false);
          setEnrollStudent(null);
        }}
        onEnroll={async (uId, cId) => {
          await enrollMutation.mutateAsync({ userId: uId, courseId: cId });
        }}
        onUnenroll={async (uId, cId) => {
          await unenrollMutation.mutateAsync({ userId: uId, courseId: cId });
        }}
      />

      {/* Cohort Manager Modal */}
      <CohortManagerModal
        courses={courses}
        isOpen={isCohortModalOpen}
        onClose={() => setIsCohortModalOpen(false)}
      />
    </div>
  );
};
