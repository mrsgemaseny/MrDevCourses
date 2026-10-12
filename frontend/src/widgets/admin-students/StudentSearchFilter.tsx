import React from 'react';
import { Search, RotateCcw } from 'lucide-react';
import { Course, UserRole } from '@/shared/types';

interface StudentSearchFilterProps {
  searchQuery: string;
  onSearchChange: (value: string) => void;
  selectedCourseId: number | 'ALL';
  onCourseChange: (courseId: number | 'ALL') => void;
  selectedRole?: UserRole | 'ALL';
  onRoleFilterChange?: (role: UserRole | 'ALL') => void;
  courses: Course[];
  onReset: () => void;
  isLoading?: boolean;
}

export const StudentSearchFilter: React.FC<StudentSearchFilterProps> = ({
  searchQuery,
  onSearchChange,
  selectedCourseId,
  onCourseChange,
  selectedRole = 'ALL',
  onRoleFilterChange,
  courses,
  onReset,
  isLoading,
}) => {
  const isFiltered = searchQuery.trim() !== '' || selectedCourseId !== 'ALL' || selectedRole !== 'ALL';

  return (
    <div className="bg-[#18181b] border border-white/5 rounded-xl p-4 mb-6 shadow-sm">
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
        {/* Search Input */}
        <div className="relative flex-1">
          <Search className="absolute left-3.5 top-1/2 -translate-y-1/2 w-4 h-4 text-zinc-500" />
          <input
            type="text"
            value={searchQuery}
            onChange={(e) => onSearchChange(e.target.value)}
            placeholder="Поиск по имени или email, Telegram, GitHub..."
            className="w-full pl-10 pr-4 py-2 bg-[#0a0a0c] border border-white/10 rounded-lg text-xs text-zinc-100 placeholder-zinc-500 focus:outline-none focus:border-zinc-500 transition-colors"
          />
        </div>

        {/* Filters Group */}
        <div className="flex flex-wrap items-center gap-3">
          {/* Role Filter */}
          {onRoleFilterChange && (
            <div className="flex items-center gap-1.5 bg-[#0a0a0c] border border-white/10 rounded-lg px-2.5 py-1.5">
              <select
                value={selectedRole}
                onChange={(e) => onRoleFilterChange(e.target.value as UserRole | 'ALL')}
                className="bg-transparent text-xs text-zinc-300 focus:outline-none cursor-pointer"
              >
                <option value="ALL" className="bg-[#18181b] text-zinc-100">
                  Все роли
                </option>
                <option value="STUDENT" className="bg-[#18181b] text-zinc-100">
                  Студенты
                </option>
                <option value="ADMIN" className="bg-[#18181b] text-zinc-100">
                  Администраторы
                </option>
              </select>
            </div>
          )}

          {/* Course Filter */}
          <div className="flex items-center gap-1.5 bg-[#0a0a0c] border border-white/10 rounded-lg px-2.5 py-1.5">
            <select
              value={selectedCourseId}
              onChange={(e) =>
                onCourseChange(e.target.value === 'ALL' ? 'ALL' : Number(e.target.value))
              }
              className="bg-transparent text-xs text-zinc-300 focus:outline-none cursor-pointer max-w-[200px] truncate"
            >
              <option value="ALL" className="bg-[#18181b] text-zinc-100">
                Все курсы
              </option>
              {courses.map((course) => (
                <option key={course.id} value={course.id} className="bg-[#18181b] text-zinc-100">
                  {course.title}
                </option>
              ))}
            </select>
          </div>

          {/* Reset Button */}
          {isFiltered && (
            <button
              onClick={onReset}
              disabled={isLoading}
              className="flex items-center gap-1.5 px-3 py-1.5 bg-zinc-800/80 hover:bg-zinc-800 text-zinc-300 hover:text-zinc-100 border border-white/10 rounded-lg text-xs font-medium transition-colors"
              title="Сбросить фильтры"
            >
              <RotateCcw className="w-3.5 h-3.5" />
              <span>Сброс</span>
            </button>
          )}
        </div>
      </div>
    </div>
  );
};
