import React from 'react';
import {
  BookOpen,
  ChevronLeft,
  ChevronRight,
  TrendingUp,
  Loader2,
} from 'lucide-react';
import { Student, UserRole } from '@/shared/types';
import { RoleToggle } from './RoleToggle';

interface StudentTableProps {
  students: Student[];
  totalElements: number;
  totalPages: number;
  currentPage: number;
  pageSize: number;
  onPageChange: (page: number) => void;
  onPageSizeChange: (size: number) => void;
  currentUserId?: number;
  onInspectProgress: (student: Student) => void;
  onManageEnrollments: (student: Student) => void;
  onRoleChange?: (userId: number, newRole: UserRole) => Promise<void>;
  isLoading?: boolean;
}

export const StudentTable: React.FC<StudentTableProps> = ({
  students,
  totalElements,
  totalPages,
  currentPage,
  pageSize,
  onPageChange,
  onPageSizeChange,
  currentUserId,
  onInspectProgress,
  onManageEnrollments,
  onRoleChange,
  isLoading,
}) => {
  if (isLoading) {
    return (
      <div className="bg-[#18181b] border border-white/5 rounded-2xl p-12 text-center">
        <Loader2 className="w-6 h-6 animate-spin text-zinc-400 mx-auto mb-2" />
        <span className="text-xs text-zinc-500 font-mono">Загрузка списка студентов...</span>
      </div>
    );
  }

  if (!students || students.length === 0) {
    return (
      <div className="bg-[#18181b] border border-white/5 rounded-2xl p-12 text-center">
        <div className="w-12 h-12 rounded-xl bg-zinc-800 border border-white/10 flex items-center justify-center mx-auto mb-3 text-zinc-400">
          <BookOpen className="w-6 h-6" />
        </div>
        <h3 className="text-sm font-semibold text-zinc-200 mb-1">Студенты не найдены</h3>
        <p className="text-xs text-zinc-500">
          По вашему поисковому запросу или выбранным фильтрам ничего не найдено.
        </p>
      </div>
    );
  }

  return (
    <div className="bg-[#18181b] border border-white/5 rounded-2xl overflow-hidden shadow-sm flex flex-col">
      {/* Table responsive container */}
      <div className="overflow-x-auto">
        <table className="w-full text-left border-collapse">
          <thead>
            <tr className="border-b border-white/5 bg-[#121216] text-[10px] font-bold text-zinc-400 uppercase tracking-wider">
              <th className="py-3.5 px-4">Пользователь</th>
              <th className="py-3.5 px-4">Роль</th>
              <th className="py-3.5 px-4">Зачислен на курсы</th>
              <th className="py-3.5 px-4">На каком уроке</th>
              <th className="py-3.5 px-4">Дата регистрации</th>
              <th className="py-3.5 px-4">Примерное окончание</th>
              <th className="py-3.5 px-4 text-right">Действия</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-white/5 text-xs text-zinc-200">
            {students.map((student) => {
              const isSelf = currentUserId !== undefined && currentUserId === student.id;
              const enrollCount = student.enrollments?.length || 0;

              return (
                <tr
                  key={student.id}
                  className="hover:bg-white/[0.02] transition-colors group"
                >
                  {/* Student profile */}
                  <td className="py-3.5 px-4">
                    <div className="flex items-center gap-3">
                      <div className="w-8 h-8 rounded-lg bg-zinc-800 border border-white/10 flex items-center justify-center font-bold text-xs text-zinc-200 uppercase overflow-hidden shrink-0">
                        {student.avatarUrl ? (
                          <img
                            src={student.avatarUrl}
                            alt=""
                            className="w-full h-full object-cover"
                          />
                        ) : (
                          <span>{(student.name || student.email).charAt(0)}</span>
                        )}
                      </div>
                      <div className="min-w-0">
                        <div className="font-semibold text-zinc-100 truncate flex items-center gap-1.5">
                          <span>{student.name || 'Без имени'}</span>
                          {isSelf && (
                            <span className="px-1.5 py-0.2 rounded bg-zinc-800 text-zinc-400 text-[9px] font-mono">
                              ВЫ
                            </span>
                          )}
                        </div>
                        <div className="text-[11px] text-zinc-400 font-mono truncate">
                          {student.email}
                        </div>
                        {(student.telegramUsername || student.githubUsername) && (
                          <div className="flex items-center gap-2 mt-0.5">
                            {student.telegramUsername && (
                              <span className="text-[9px] text-zinc-400 font-mono">
                                TG: @{student.telegramUsername}
                              </span>
                            )}
                            {student.githubUsername && (
                              <span className="text-[9px] text-zinc-400 font-mono">
                                GH: {student.githubUsername}
                              </span>
                            )}
                          </div>
                        )}
                      </div>
                    </div>
                  </td>

                  {/* Role */}
                  <td className="py-3.5 px-4">
                    {onRoleChange ? (
                      <RoleToggle
                        userId={student.id}
                        userEmail={student.email}
                        currentRole={student.role}
                        isCurrentAdmin={isSelf}
                        onRoleChange={(newRole) => onRoleChange(student.id, newRole)}
                      />
                    ) : (
                      <span className="px-2 py-0.5 rounded text-[10px] font-mono bg-zinc-800 text-zinc-300 border border-white/5">
                        {student.role}
                      </span>
                    )}
                  </td>

                  {/* Enrolled Courses */}
                  <td className="py-3.5 px-4 text-[11px] text-zinc-300 font-mono">
                    {enrollCount > 0 ? (
                      <div className="flex items-center gap-1.5 max-w-xs truncate" title={student.enrollments.map((e) => e.courseTitle).join(', ')}>
                        <span className="truncate">
                          {student.enrollments.slice(0, 2).map((e) => e.courseTitle).join(', ')}
                        </span>
                        {enrollCount > 2 && (
                          <span className="text-[10px] text-zinc-500 font-mono shrink-0">
                            +{enrollCount - 2}
                          </span>
                        )}
                      </div>
                    ) : (
                      <span className="text-[11px] text-zinc-500 font-mono">—</span>
                    )}
                  </td>

                  {/* Current Lesson */}
                  <td className="py-3.5 px-4 text-[11px] text-zinc-300 font-mono">
                    <span className="block max-w-[220px] truncate" title={student.currentLessonTitle || 'Не начат'}>
                      {student.currentLessonTitle || 'Не начат'}
                    </span>
                  </td>

                  {/* Registration Date & Activity */}
                  <td className="py-3.5 px-4 text-[11px] text-zinc-300 font-mono">
                    <div>
                      {student.createdAt
                        ? new Date(student.createdAt).toLocaleString('ru-RU', {
                            day: '2-digit',
                            month: '2-digit',
                            year: 'numeric',
                            hour: '2-digit',
                            minute: '2-digit',
                          })
                        : '—'}
                    </div>
                    {student.lastActiveDate && (
                      <div className="text-[10px] text-zinc-500 mt-0.5">
                        Был: {new Date(student.lastActiveDate).toLocaleDateString('ru-RU')}
                      </div>
                    )}
                  </td>

                  {/* Estimated Finish Date */}
                  <td className="py-3.5 px-4 text-[11px] text-zinc-400 font-mono">
                    {student.estimatedFinishDate
                      ? new Date(student.estimatedFinishDate).toLocaleDateString('ru-RU')
                      : '—'}
                  </td>

                  {/* Actions */}
                  <td className="py-3.5 px-4 text-right">
                    <div className="flex items-center justify-end gap-1.5">
                      <button
                        type="button"
                        onClick={() => onManageEnrollments(student)}
                        className="px-2.5 py-1 rounded-lg bg-zinc-800 hover:bg-zinc-700 text-zinc-200 text-[11px] font-medium border border-white/5 transition-colors cursor-pointer"
                        title="Управление зачислениями"
                      >
                        Зачисления
                      </button>

                      <button
                        type="button"
                        onClick={() => onInspectProgress(student)}
                        className="inline-flex items-center gap-1 px-2.5 py-1 rounded-lg bg-zinc-100 hover:bg-white text-zinc-950 text-[11px] font-semibold transition-colors cursor-pointer"
                        title="Инспекция детального прогресса"
                      >
                        <TrendingUp className="w-3 h-3" />
                        <span>Прогресс</span>
                      </button>
                    </div>
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>

      {/* Pagination Footer */}
      <div className="p-4 border-t border-white/5 bg-[#121216] flex flex-col sm:flex-row items-center justify-between gap-3 text-xs">
        <div className="text-zinc-400 text-[11px] font-mono">
          Показано{' '}
          <span className="font-semibold text-zinc-200">
            {students.length > 0 ? currentPage * pageSize + 1 : 0}
          </span>{' '}
          –{' '}
          <span className="font-semibold text-zinc-200">
            {Math.min((currentPage + 1) * pageSize, totalElements)}
          </span>{' '}
          из <span className="font-semibold text-zinc-200">{totalElements}</span> студентов
        </div>

        <div className="flex items-center gap-3">
          {/* Page size selector */}
          <div className="flex items-center gap-1.5 text-[11px] text-zinc-400">
            <span>На странице:</span>
            <select
              value={pageSize}
              onChange={(e) => onPageSizeChange(Number(e.target.value))}
              className="bg-[#0a0a0c] border border-white/10 rounded px-2 py-1 text-zinc-200 focus:outline-none cursor-pointer"
            >
              <option value={10}>10</option>
              <option value={20}>20</option>
              <option value={50}>50</option>
            </select>
          </div>

          {/* Navigation buttons */}
          <div className="flex items-center gap-1">
            <button
              type="button"
              disabled={currentPage <= 0}
              onClick={() => onPageChange(currentPage - 1)}
              className="p-1.5 rounded bg-zinc-800 hover:bg-zinc-700 text-zinc-200 disabled:opacity-30 disabled:cursor-not-allowed transition-colors cursor-pointer"
              title="Предыдущая страница"
            >
              <ChevronLeft className="w-4 h-4" />
            </button>

            <span className="px-2.5 py-1 text-[11px] font-mono text-zinc-300">
              {currentPage + 1} / {Math.max(1, totalPages)}
            </span>

            <button
              type="button"
              disabled={currentPage >= totalPages - 1}
              onClick={() => onPageChange(currentPage + 1)}
              className="p-1.5 rounded bg-zinc-800 hover:bg-zinc-700 text-zinc-200 disabled:opacity-30 disabled:cursor-not-allowed transition-colors cursor-pointer"
              title="Следующая страница"
            >
              <ChevronRight className="w-4 h-4" />
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};
