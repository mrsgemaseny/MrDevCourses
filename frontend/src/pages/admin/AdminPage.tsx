import React, { useState, useEffect } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { adminApi, CreateCoursePayload, CreateLessonPayload } from '@/entities/admin/api/adminApi';
import { AdminAnalyticsDashboard } from '@/features/admin-analytics';
import { Plus, Trash2, X, AlertTriangle, UserPlus } from 'lucide-react';

export const AdminPage: React.FC = () => {
  const queryClient = useQueryClient();
  const [activeTab, setActiveTab] = useState<'courses' | 'lessons' | 'students' | 'analytics'>('courses');
  const [selectedCourseId, setSelectedCourseId] = useState<number | null>(null);

  // Forms State
  const [showCourseModal, setShowCourseModal] = useState(false);
  const [courseForm, setCourseForm] = useState<CreateCoursePayload>({
    title: '',
    description: '',
    slug: '',
    active: true,
  });

  const [showLessonModal, setShowLessonModal] = useState(false);
  const [lessonForm, setLessonForm] = useState<CreateLessonPayload>({
    title: '',
    content: '',
    youtubeUrl: '',
    dayNumber: 1,
    sortOrder: 1,
  });

  // Custom UI Modals (replacing confirm / prompt)
  const [deleteConfirm, setDeleteConfirm] = useState<{
    isOpen: boolean;
    type: 'course' | 'lesson';
    id: number;
    title: string;
  } | null>(null);

  const [enrollModal, setEnrollModal] = useState<{
    isOpen: boolean;
    studentId: number;
    studentEmail: string;
    courseId: number;
  } | null>(null);

  // Close modals on Escape key
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        setShowCourseModal(false);
        setShowLessonModal(false);
        setDeleteConfirm(null);
        setEnrollModal(null);
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, []);

  // Queries
  const { data: courses = [], isLoading: coursesLoading } = useQuery({
    queryKey: ['admin', 'courses'],
    queryFn: adminApi.getCourses,
  });

  const effectiveCourseId = selectedCourseId || courses[0]?.id || null;

  const { data: lessons = [], isLoading: lessonsLoading } = useQuery({
    queryKey: ['admin', 'lessons', effectiveCourseId],
    queryFn: () => adminApi.getLessons(effectiveCourseId!),
    enabled: !!effectiveCourseId,
  });

  const { data: students = [], isLoading: studentsLoading } = useQuery({
    queryKey: ['admin', 'students'],
    queryFn: adminApi.getStudents,
  });

  // Mutations
  const createCourseMutation = useMutation({
    mutationFn: (payload: CreateCoursePayload) => adminApi.createCourse(payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'courses'] });
      queryClient.invalidateQueries({ queryKey: ['courses'] });
      setShowCourseModal(false);
      setCourseForm({ title: '', description: '', slug: '', active: true });
    },
  });

  const deleteCourseMutation = useMutation({
    mutationFn: (id: number) => adminApi.deleteCourse(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'courses'] });
      queryClient.invalidateQueries({ queryKey: ['courses'] });
      setDeleteConfirm(null);
    },
  });

  const createLessonMutation = useMutation({
    mutationFn: (payload: CreateLessonPayload) => adminApi.createLesson(effectiveCourseId!, payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'lessons', effectiveCourseId] });
      setShowLessonModal(false);
      setLessonForm({ title: '', content: '', youtubeUrl: '', dayNumber: (lessons.length || 0) + 1, sortOrder: (lessons.length || 0) + 1 });
    },
  });

  const deleteLessonMutation = useMutation({
    mutationFn: (id: number) => adminApi.deleteLesson(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'lessons', effectiveCourseId] });
      setDeleteConfirm(null);
    },
  });

  const enrollStudentMutation = useMutation({
    mutationFn: ({ userId, courseId }: { userId: number; courseId: number }) =>
      adminApi.enrollStudent(userId, courseId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'students'] });
      setEnrollModal(null);
    },
  });

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-10">
      {/* Header */}
      <div className="flex items-center justify-between mb-8 pb-6 border-b border-white/5">
        <div>
          <h1 className="text-2xl font-bold text-white tracking-tight">Управление MrDeveloper</h1>
        </div>

        {/* Tab Buttons */}
        <div className="flex items-center gap-1.5 p-1 bg-zinc-900 border border-white/5 rounded-sm">
          <button
            onClick={() => setActiveTab('courses')}
            className={`px-3 py-1.5 rounded-md text-xs font-medium transition-colors cursor-pointer ${
              activeTab === 'courses' ? 'bg-[#fafafa] text-[#09090b] shadow-sm' : 'text-zinc-400 hover:text-white'
            }`}
          >
            Курсы
          </button>
          <button
            onClick={() => setActiveTab('lessons')}
            className={`px-3 py-1.5 rounded-md text-xs font-medium transition-colors cursor-pointer ${
              activeTab === 'lessons' ? 'bg-[#fafafa] text-[#09090b] shadow-sm' : 'text-zinc-400 hover:text-white'
            }`}
          >
            Уроки
          </button>
          <button
            onClick={() => setActiveTab('students')}
            className={`px-3 py-1.5 rounded-md text-xs font-medium transition-colors cursor-pointer ${
              activeTab === 'students' ? 'bg-[#fafafa] text-[#09090b] shadow-sm' : 'text-zinc-400 hover:text-white'
            }`}
          >
            Студенты
          </button>
          <button
            onClick={() => setActiveTab('analytics')}
            className={`px-3 py-1.5 rounded-md text-xs font-medium transition-colors cursor-pointer ${
              activeTab === 'analytics' ? 'bg-[#fafafa] text-[#09090b] shadow-sm' : 'text-zinc-400 hover:text-white'
            }`}
          >
            Аналитика
          </button>
        </div>
      </div>

      {/* TAB 1: COURSES */}
      {activeTab === 'courses' && (
        <div className="space-y-6">
          <div className="flex items-center justify-end">
            <button
              onClick={() => setShowCourseModal(true)}
              className="px-4 py-2 bg-[#fafafa] hover:bg-white text-[#09090b] text-xs font-semibold rounded-md flex items-center gap-1.5 transition-all shadow-[0_0_15px_rgba(255,255,255,0.05)] cursor-pointer"
            >
              <Plus className="w-4 h-4" />
              <span>Создать курс</span>
            </button>
          </div>

          {coursesLoading ? (
            <div className="text-center py-12 text-zinc-500 text-xs">Загрузка курсов...</div>
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              {courses.map((course) => (
                <div
                  key={course.id}
                  className="p-5 rounded-sm bg-[#0e0e11] border border-white/5 flex flex-col justify-between"
                >
                  <div>
                    <div className="flex items-center justify-between mb-2">
                      <span className="text-[10px] font-mono text-zinc-400">/{course.slug}</span>
                    </div>

                    <h3 className="text-sm font-bold text-white mb-1.5">{course.title}</h3>
                    <p className="text-xs text-zinc-400 line-clamp-2 mb-4">{course.description}</p>
                  </div>

                  <div className="pt-3 border-t border-white/5 flex items-center justify-between text-xs">
                    <span className="text-zinc-500 font-mono">{course.totalLessons || 0} уроков</span>
                  </div>
                </div>
              ))}
            </div>
          )}

          {/* Modal Create Course */}
          {showCourseModal && (
            <div
              role="dialog"
              aria-modal="true"
              aria-labelledby="create-course-title"
              className="fixed inset-0 z-50 flex items-center justify-center bg-black/80 p-4"
            >
              <div className="w-full max-w-lg p-6 bg-[#0a0a0c] border border-white/5 rounded-sm shadow-[inset_0_1px_0_rgba(255,255,255,0.04)] relative">
                <div className="flex items-center justify-between mb-4 pb-2 border-b border-white/5">
                  <h3 id="create-course-title" className="text-sm font-bold text-white">Создать новый курс</h3>
                  <button
                    onClick={() => setShowCourseModal(false)}
                    aria-label="Закрыть"
                    className="p-1 text-zinc-400 hover:text-white"
                  >
                    <X className="w-4 h-4" />
                  </button>
                </div>
                <form
                  onSubmit={(e) => {
                    e.preventDefault();
                    createCourseMutation.mutate(courseForm);
                  }}
                  className="space-y-4"
                >
                  <div>
                    <label className="block text-xs text-zinc-400 mb-1">Название курса</label>
                    <input
                      type="text"
                      required
                      value={courseForm.title}
                      onChange={(e) => {
                        const title = e.target.value;
                        const slug = title
                          .toLowerCase()
                          .replace(/[^a-z0-9]+/g, '-')
                          .replace(/(^-|-$)+/g, '');
                        setCourseForm({ ...courseForm, title, slug });
                      }}
                      className="w-full px-3 py-2 bg-zinc-900 border border-white/5 rounded-md text-sm text-white focus:outline-none focus:border-zinc-500"
                      placeholder="Вайбкодинг с нуля"
                    />
                  </div>

                  <div>
                    <label className="block text-xs text-zinc-400 mb-1">Slug (URL)</label>
                    <input
                      type="text"
                      required
                      value={courseForm.slug}
                      onChange={(e) => setCourseForm({ ...courseForm, slug: e.target.value })}
                      className="w-full px-3 py-2 bg-zinc-900 border border-white/5 rounded-md text-sm text-white font-mono focus:outline-none focus:border-zinc-500"
                      placeholder="vibecoding-zero"
                    />
                  </div>

                  <div>
                    <label className="block text-xs text-zinc-400 mb-1">Описание</label>
                    <textarea
                      rows={3}
                      value={courseForm.description || ''}
                      onChange={(e) => setCourseForm({ ...courseForm, description: e.target.value })}
                      className="w-full px-3 py-2 bg-zinc-900 border border-white/5 rounded-md text-sm text-white focus:outline-none focus:border-zinc-500"
                      placeholder="Краткое описание целей и программы курса"
                    />
                  </div>

                  <div className="flex items-center gap-2">
                    <input
                      type="checkbox"
                      id="activeCheckbox"
                      checked={courseForm.active}
                      onChange={(e) => setCourseForm({ ...courseForm, active: e.target.checked })}
                      className="rounded border-white/5 text-white focus:ring-0"
                    />
                    <label htmlFor="activeCheckbox" className="text-xs text-zinc-300">
                      Опубликовать (активен)
                    </label>
                  </div>

                  <div className="flex items-center justify-end gap-3 pt-4 border-t border-white/5">
                    <button
                      type="button"
                      onClick={() => setShowCourseModal(false)}
                      className="px-4 py-2 text-xs text-zinc-400 hover:text-white cursor-pointer"
                    >
                      Отмена
                    </button>
                    <button
                      type="submit"
                      disabled={createCourseMutation.isPending}
                      className="px-4 py-2 bg-[#fafafa] hover:bg-white text-[#09090b] text-xs font-semibold rounded-md cursor-pointer"
                    >
                      {createCourseMutation.isPending ? 'Сохранение...' : 'Создать'}
                    </button>
                  </div>
                </form>
              </div>
            </div>
          )}
        </div>
      )}

      {/* TAB 2: LESSONS */}
      {activeTab === 'lessons' && (
        <div className="space-y-6">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
            {/* Course Selector */}
            <div className="flex items-center gap-3">
              <label htmlFor="courseSelect" className="text-xs text-zinc-400">Выберите курс:</label>
              <select
                id="courseSelect"
                value={effectiveCourseId || ''}
                onChange={(e) => setSelectedCourseId(Number(e.target.value))}
                className="px-3 py-1.5 bg-zinc-900 border border-white/5 rounded-md text-xs text-white focus:outline-none"
              >
                {courses.map((c) => (
                  <option key={c.id} value={c.id}>
                    {c.title}
                  </option>
                ))}
              </select>
            </div>

            {effectiveCourseId && (
              <button
                onClick={() => {
                  setLessonForm({
                    title: '',
                    content: '',
                    youtubeUrl: '',
                    dayNumber: (lessons.length || 0) + 1,
                    sortOrder: (lessons.length || 0) + 1,
                  });
                  setShowLessonModal(true);
                }}
                className="px-4 py-2 bg-[#fafafa] hover:bg-white text-[#09090b] text-xs font-semibold rounded-md flex items-center gap-1.5 transition-all shadow-[0_0_15px_rgba(255,255,255,0.05)] cursor-pointer self-start sm:self-auto"
              >
                <Plus className="w-4 h-4" />
                <span>Добавить урок</span>
              </button>
            )}
          </div>

          {lessonsLoading ? (
            <div className="text-center py-12 text-zinc-500 text-xs">Загрузка уроков...</div>
          ) : lessons.length === 0 ? (
            <div className="p-8 text-center bg-zinc-900/60 border border-white/5 rounded-sm text-zinc-400 text-xs">
              В этом курсе пока нет уроков.
            </div>
          ) : (
            <div className="space-y-3">
              {lessons.map((lesson) => (
                <div
                  key={lesson.id}
                  className="p-4 rounded-sm bg-[#0e0e11] border border-white/5 flex items-center justify-between"
                >
                  <div className="flex items-center gap-3">
                    <div className="w-8 h-8 rounded-md bg-zinc-800 border border-white/5 flex items-center justify-center text-xs font-mono font-bold text-zinc-300">
                      Д{lesson.dayNumber}
                    </div>
                    <div>
                      <h3 className="text-sm font-semibold text-white">{lesson.title}</h3>
                      <p className="text-xs text-zinc-500 truncate max-w-md">
                        {lesson.youtubeUrl || 'Без видео'}
                      </p>
                    </div>
                  </div>

                  <div className="flex items-center gap-2">
                    <button
                      onClick={() => {
                        setDeleteConfirm({
                          isOpen: true,
                          type: 'lesson',
                          id: lesson.id,
                          title: lesson.title,
                        });
                      }}
                      className="p-1.5 text-zinc-500 hover:text-red-400 transition-colors cursor-pointer"
                      title="Удалить"
                      aria-label={`Удалить урок ${lesson.title}`}
                    >
                      <Trash2 className="w-4 h-4" />
                    </button>
                  </div>
                </div>
              ))}
            </div>
          )}

          {/* Modal Create Lesson */}
          {showLessonModal && (
            <div
              role="dialog"
              aria-modal="true"
              aria-labelledby="create-lesson-title"
              className="fixed inset-0 z-50 flex items-center justify-center bg-black/80 p-4"
            >
              <div className="w-full max-w-lg p-6 bg-[#0a0a0c] border border-white/5 rounded-sm shadow-[inset_0_1px_0_rgba(255,255,255,0.04)] relative">
                <div className="flex items-center justify-between mb-4 pb-2 border-b border-white/5">
                  <h3 id="create-lesson-title" className="text-sm font-bold text-white">Добавить урок в курс</h3>
                  <button
                    onClick={() => setShowLessonModal(false)}
                    aria-label="Закрыть"
                    className="p-1 text-zinc-400 hover:text-white"
                  >
                    <X className="w-4 h-4" />
                  </button>
                </div>
                <form
                  onSubmit={(e) => {
                    e.preventDefault();
                    createLessonMutation.mutate(lessonForm);
                  }}
                  className="space-y-4"
                >
                  <div>
                    <label className="block text-xs text-zinc-400 mb-1">Название урока</label>
                    <input
                      type="text"
                      required
                      value={lessonForm.title}
                      onChange={(e) => setLessonForm({ ...lessonForm, title: e.target.value })}
                      className="w-full px-3 py-2 bg-zinc-900 border border-white/5 rounded-md text-xs text-white focus:outline-none focus:border-zinc-500"
                      placeholder="Урок 1: Введение и основы"
                    />
                  </div>

                  <div className="grid grid-cols-2 gap-3">
                    <div>
                      <label className="block text-xs text-zinc-400 mb-1">Порядковый номер урока (Sort Order)</label>
                      <input
                        type="number"
                        min={1}
                        required
                        value={lessonForm.dayNumber}

                        onChange={(e) =>
                          setLessonForm({
                            ...lessonForm,
                            dayNumber: Number(e.target.value),
                            sortOrder: Number(e.target.value),
                          })
                        }
                        className="w-full px-3 py-2 bg-zinc-900 border border-white/5 rounded-md text-xs text-white font-mono focus:outline-none focus:border-zinc-500"
                      />
                    </div>

                    <div>
                      <label className="block text-xs text-zinc-400 mb-1">YouTube URL</label>
                      <input
                        type="url"
                        value={lessonForm.youtubeUrl || ''}
                        onChange={(e) => setLessonForm({ ...lessonForm, youtubeUrl: e.target.value })}
                        className="w-full px-3 py-2 bg-zinc-900 border border-white/5 rounded-md text-xs text-white font-mono focus:outline-none focus:border-zinc-500"
                        placeholder="https://youtube.com/watch?v=..."
                      />
                    </div>
                  </div>

                  <div>
                    <label className="block text-xs text-zinc-400 mb-1">Конспект / Материалы урока</label>
                    <textarea
                      rows={5}
                      value={lessonForm.content || ''}
                      onChange={(e) => setLessonForm({ ...lessonForm, content: e.target.value })}
                      className="w-full px-3 py-2 bg-zinc-900 border border-white/5 rounded-md text-xs text-white focus:outline-none focus:border-zinc-500 font-mono"
                      placeholder="Markdown конспект урока..."
                    />
                  </div>

                  <div className="flex items-center justify-end gap-3 pt-4 border-t border-white/5">
                    <button
                      type="button"
                      onClick={() => setShowLessonModal(false)}
                      className="px-4 py-2 text-xs text-zinc-400 hover:text-white cursor-pointer"
                    >
                      Отмена
                    </button>
                    <button
                      type="submit"
                      disabled={createLessonMutation.isPending}
                      className="px-4 py-2 bg-[#fafafa] hover:bg-white text-[#09090b] text-xs font-semibold rounded-md cursor-pointer"
                    >
                      {createLessonMutation.isPending ? 'Сохранение...' : 'Добавить урок'}
                    </button>
                  </div>
                </form>
              </div>
            </div>
          )}
        </div>
      )}

      {/* TAB 3: STUDENTS */}
      {activeTab === 'students' && (
        <div className="space-y-6">
          <h2 className="text-sm font-bold text-white">Список зарегистрированных пользователей ({students.length})</h2>

          {studentsLoading ? (
            <div className="text-center py-12 text-zinc-500 text-xs">Загрузка пользователей...</div>
          ) : (
            <div className="rounded-sm border border-white/5 bg-[#0e0e11] overflow-hidden">
              <div className="overflow-x-auto">
                <table className="w-full text-left text-xs">
                  <thead className="bg-zinc-900/80 border-b border-white/5 text-zinc-400 font-mono">
                    <tr>
                      <th className="p-3.5">Пользователь</th>
                      <th className="p-3.5">Email</th>
                      <th className="p-3.5">Роль</th>
                      <th className="p-3.5">Записи на курсы</th>
                      <th className="p-3.5">Действие</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-[#27272a]">
                    {students.map((student) => (
                      <tr key={student.id} className="hover:bg-zinc-800/30 transition-colors">
                        <td className="p-3.5 font-medium text-white">
                          <div className="flex items-center gap-2">
                            {student.avatarUrl ? (
                              <img src={student.avatarUrl} alt="" className="w-6 h-6 rounded-full" />
                            ) : (
                              <div className="w-6 h-6 rounded-full bg-zinc-800 text-zinc-300 flex items-center justify-center font-bold">
                                {student.name?.charAt(0) || 'U'}
                              </div>
                            )}
                            <span>{student.name || 'Без имени'}</span>
                          </div>
                        </td>
                        <td className="p-3.5 text-zinc-400 font-mono">{student.email}</td>
                        <td className="p-3.5">
                          <span
                            className={`px-2 py-0.5 rounded text-[10px] font-mono ${
                              student.role === 'ADMIN'
                                ? 'bg-white/15 text-white border border-white/20'
                                : 'bg-zinc-800 text-zinc-300'
                            }`}
                          >
                            {student.role}
                          </span>
                        </td>
                        <td className="p-3.5 text-zinc-300">
                          {student.enrollments && student.enrollments.length > 0 ? (
                            <div className="flex flex-wrap gap-1">
                              {student.enrollments.map((enr) => (
                                <span
                                  key={enr.id}
                                  className="px-2 py-0.5 bg-zinc-900 border border-white/5 rounded text-[10px] text-zinc-300"
                                >
                                  {enr.courseTitle}
                                </span>
                              ))}
                            </div>
                          ) : (
                            <span className="text-zinc-600">Нет записей</span>
                          )}
                        </td>
                        <td className="p-3.5">
                          {courses.length > 0 && (
                            <button
                              onClick={() => {
                                setEnrollModal({
                                  isOpen: true,
                                  studentId: student.id,
                                  studentEmail: student.email,
                                  courseId: courses[0].id,
                                });
                              }}
                              className="px-2.5 py-1 bg-zinc-800 hover:bg-zinc-700 text-zinc-200 rounded text-xs transition-colors cursor-pointer flex items-center gap-1"
                            >
                              <UserPlus className="w-3 h-3" />
                              <span>Записать на курс</span>
                            </button>
                          )}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          )}
        </div>
      )}

      {/* TAB 4: ANALYTICS */}
      {activeTab === 'analytics' && (
        <AdminAnalyticsDashboard />
      )}

      {/* Confirmation Modal for Deletions */}
      {deleteConfirm && deleteConfirm.isOpen && (
        <div
          role="dialog"
          aria-modal="true"
          aria-labelledby="delete-confirm-title"
          className="fixed inset-0 z-50 flex items-center justify-center bg-black/80 p-4"
        >
          <div className="w-full max-w-md p-6 bg-[#0a0a0c] border border-white/5 rounded-sm shadow-[inset_0_1px_0_rgba(255,255,255,0.04)]">
            <div className="flex items-start gap-3 mb-4">
              <div className="w-9 h-9 rounded-full bg-red-950/80 border border-red-800/80 flex items-center justify-center text-red-400 shrink-0">
                <AlertTriangle className="w-5 h-5" />
              </div>
              <div>
                <h3 id="delete-confirm-title" className="text-sm font-bold text-white mb-1">
                  Подтверждение удаления
                </h3>
                <p className="text-xs text-zinc-400">
                  Вы действительно хотите удалить {deleteConfirm.type === 'course' ? 'курс' : 'урок'} «
                  <span className="text-zinc-200 font-semibold">{deleteConfirm.title}</span>»? Это действие необратимо.
                </p>
              </div>
            </div>

            <div className="flex items-center justify-end gap-2.5 pt-4 border-t border-white/5">
              <button
                type="button"
                onClick={() => setDeleteConfirm(null)}
                className="px-3.5 py-1.5 rounded-md text-xs text-zinc-400 hover:text-white cursor-pointer"
              >
                Отмена
              </button>
              <button
                type="button"
                onClick={() => {
                  if (deleteConfirm.type === 'course') {
                    deleteCourseMutation.mutate(deleteConfirm.id);
                  } else {
                    deleteLessonMutation.mutate(deleteConfirm.id);
                  }
                }}
                disabled={deleteCourseMutation.isPending || deleteLessonMutation.isPending}
                className="px-3.5 py-1.5 bg-red-600 hover:bg-red-500 text-white text-xs font-semibold rounded-md transition-colors cursor-pointer"
              >
                Удалить
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Manual Student Enrollment Modal */}
      {enrollModal && enrollModal.isOpen && (
        <div
          role="dialog"
          aria-modal="true"
          aria-labelledby="enroll-modal-title"
          className="fixed inset-0 z-50 flex items-center justify-center bg-black/80 p-4"
        >
          <div className="w-full max-w-md p-6 bg-[#0a0a0c] border border-white/5 rounded-sm shadow-[inset_0_1px_0_rgba(255,255,255,0.04)]">
            <div className="flex items-center justify-between mb-4 pb-2 border-b border-white/5">
              <h3 id="enroll-modal-title" className="text-sm font-bold text-white">
                Зачислить студента на курс
              </h3>
              <button
                onClick={() => setEnrollModal(null)}
                aria-label="Закрыть"
                className="p-1 text-zinc-400 hover:text-white"
              >
                <X className="w-4 h-4" />
              </button>
            </div>

            <div className="space-y-4">
              <div>
                <span className="block text-xs text-zinc-400 mb-1">Студент</span>
                <div className="px-3 py-2 bg-zinc-900 border border-white/5 rounded-md text-xs text-zinc-200 font-mono">
                  {enrollModal.studentEmail}
                </div>
              </div>

              <div>
                <label htmlFor="enrollCourseSelect" className="block text-xs text-zinc-400 mb-1">
                  Выберите курс для зачисления
                </label>
                <select
                  id="enrollCourseSelect"
                  value={enrollModal.courseId}
                  onChange={(e) =>
                    setEnrollModal({
                      ...enrollModal,
                      courseId: Number(e.target.value),
                    })
                  }
                  className="w-full px-3 py-2 bg-zinc-900 border border-white/5 rounded-md text-xs text-white focus:outline-none focus:border-zinc-500"
                >
                  {courses.map((c) => (
                    <option key={c.id} value={c.id}>
                      {c.title} (/{c.slug})
                    </option>
                  ))}
                </select>
              </div>

              <div className="flex items-center justify-end gap-2.5 pt-4 border-t border-white/5">
                <button
                  type="button"
                  onClick={() => setEnrollModal(null)}
                  className="px-3.5 py-1.5 rounded-md text-xs text-zinc-400 hover:text-white cursor-pointer"
                >
                  Отмена
                </button>
                <button
                  type="button"
                  onClick={() => {
                    enrollStudentMutation.mutate({
                      userId: enrollModal.studentId,
                      courseId: enrollModal.courseId,
                    });
                  }}
                  disabled={enrollStudentMutation.isPending}
                  className="px-3.5 py-1.5 bg-[#fafafa] hover:bg-white text-[#09090b] text-xs font-semibold rounded-md transition-colors cursor-pointer"
                >
                  {enrollStudentMutation.isPending ? 'Зачисление...' : 'Зачислить'}
                </button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
