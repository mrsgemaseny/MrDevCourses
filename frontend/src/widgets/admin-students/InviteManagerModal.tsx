import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  X,
  Link as LinkIcon,
  Plus,
  Trash2,
  Copy,
  Check,
  AlertCircle,
  ExternalLink,
} from 'lucide-react';
import { inviteApi } from '@/entities/invite/api/inviteApi';
import { adminStudentApi } from '@/entities/adminStudentApi';
import { Course, Cohort, CourseInvite, CreateCourseInvitePayload } from '@/shared/types';

interface InviteManagerModalProps {
  courses: Course[];
  isOpen: boolean;
  onClose: () => void;
}

export const InviteManagerModal: React.FC<InviteManagerModalProps> = ({
  courses,
  isOpen,
  onClose,
}) => {
  const queryClient = useQueryClient();

  const [activeTab, setActiveTab] = useState<'create' | 'list'>('create');
  const [errorMsg, setErrorMsg] = useState<string | null>(null);
  const [copiedCode, setCopiedCode] = useState<string | null>(null);

  // Form State
  const [courseId, setCourseId] = useState<number | ''>(courses[0]?.id || '');
  const [cohortId, setCohortId] = useState<number | ''>('');
  const [maxUses, setMaxUses] = useState<number>(1);
  const [expiresInDays, setExpiresInDays] = useState<number>(14);
  const [customCode, setCustomCode] = useState<string>('');
  const [note, setNote] = useState<string>('');
  const [createdInvite, setCreatedInvite] = useState<CourseInvite | null>(null);

  // Fetch all cohorts for dropdown
  const { data: cohorts = [] } = useQuery<Cohort[]>({
    queryKey: ['admin', 'cohorts'],
    queryFn: adminStudentApi.getAllCohorts,
    enabled: isOpen,
  });

  // Filter cohorts for selected course
  const courseCohorts = cohorts.filter((c) => c.courseId === Number(courseId));

  // Fetch invites
  const { data: invites = [], isLoading: isInvitesLoading } = useQuery<CourseInvite[]>({
    queryKey: ['admin', 'invites'],
    queryFn: () => inviteApi.getAdminInvites(),
    enabled: isOpen,
  });

  // Create Invite Mutation
  const createMutation = useMutation({
    mutationFn: (payload: CreateCourseInvitePayload) => inviteApi.createInvite(payload),
    onSuccess: (newInvite) => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'invites'] });
      setCreatedInvite(newInvite);
      setErrorMsg(null);
      setCustomCode('');
      setNote('');
    },
    onError: (err: unknown) => {
      const msg = err instanceof Error ? err.message : 'Ошибка при создании инвайта';
      setErrorMsg(msg);
    },
  });

  // Delete Invite Mutation
  const deleteMutation = useMutation({
    mutationFn: (id: number) => inviteApi.deleteInvite(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'invites'] });
      if (createdInvite) {
        setCreatedInvite(null);
      }
    },
    onError: (err: unknown) => {
      const msg = err instanceof Error ? err.message : 'Ошибка при удалении инвайта';
      setErrorMsg(msg);
    },
  });

  const handleCreate = (e?: React.FormEvent) => {
    if (e) {
      e.preventDefault();
    }
    if (!courseId) {
      setErrorMsg('Выберите курс');
      return;
    }

    createMutation.mutate({
      courseId: Number(courseId),
      cohortId: cohortId ? Number(cohortId) : undefined,
      maxUses: Number(maxUses) || 1,
      expiresInDays: expiresInDays > 0 ? Number(expiresInDays) : undefined,
      customCode: customCode.trim() || undefined,
      note: note.trim() || undefined,
    });
  };

  const getInviteUrl = (code: string) => {
    return `${window.location.origin}/invite/${code}`;
  };

  const copyToClipboard = (text: string, code: string) => {
    navigator.clipboard.writeText(text);
    setCopiedCode(code);
    setTimeout(() => setCopiedCode(null), 2500);
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-xs animate-in fade-in duration-200">
      <div className="relative w-full max-w-2xl bg-[#0e0e11] border border-white/10 rounded-sm shadow-2xl flex flex-col max-h-[90vh] overflow-hidden">
        {/* Header */}
        <div className="flex items-center justify-between px-6 py-4 border-b border-white/10 bg-[#141418]">
          <div className="flex items-center gap-2.5">
            <LinkIcon className="w-5 h-5 text-white" />
            <h2 className="text-base font-bold text-white tracking-tight">
              Инвайт-ссылки для зачисления
            </h2>
          </div>
          <button
            type="button"
            onClick={onClose}
            className="p-1 rounded-sm text-zinc-400 hover:text-white hover:bg-white/5 transition-colors cursor-pointer"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Tab switcher */}
        <div className="flex border-b border-white/10 bg-[#0a0a0c] px-6">
          <button
            type="button"
            onClick={() => setActiveTab('create')}
            className={`py-3 px-4 text-xs font-semibold border-b-2 transition-colors cursor-pointer ${
              activeTab === 'create'
                ? 'border-white text-white'
                : 'border-transparent text-zinc-400 hover:text-zinc-200'
            }`}
          >
            Создать инвайт
          </button>
          <button
            type="button"
            onClick={() => setActiveTab('list')}
            className={`py-3 px-4 text-xs font-semibold border-b-2 transition-colors cursor-pointer flex items-center gap-2 ${
              activeTab === 'list'
                ? 'border-white text-white'
                : 'border-transparent text-zinc-400 hover:text-zinc-200'
            }`}
          >
            <span>Все инвайты</span>
            <span className="px-1.5 py-0.5 rounded-xs text-[10px] font-mono bg-white/10 text-zinc-300">
              {invites.length}
            </span>
          </button>
        </div>

        {/* Body */}
        <div className="p-6 overflow-y-auto space-y-6 flex-1">
          {errorMsg && (
            <div className="p-3 rounded-sm bg-white/5 border border-white/20 text-xs text-zinc-200 flex items-center gap-2">
              <AlertCircle className="w-4 h-4 text-zinc-400 shrink-0" />
              <span>{errorMsg}</span>
            </div>
          )}

          {activeTab === 'create' ? (
            <div className="space-y-6">
              {/* Success Result Banner */}
              {createdInvite && (
                <div className="p-4 rounded-sm bg-[#141418] border border-white/20 space-y-3">
                  <div className="flex items-center justify-between">
                    <span className="text-xs font-mono uppercase tracking-wider text-white flex items-center gap-1.5">
                      <Check className="w-3.5 h-3.5" />
                      Инвайт-ссылка сгенерирована
                    </span>
                    <span className="text-[11px] font-mono text-zinc-400">
                      Код: {createdInvite.code}
                    </span>
                  </div>

                  <div className="flex items-center gap-2">
                    <input
                      type="text"
                      readOnly
                      value={getInviteUrl(createdInvite.code)}
                      className="flex-1 px-3 py-2 text-xs font-mono bg-[#0a0a0c] border border-white/10 rounded-sm text-zinc-200 select-all"
                    />
                    <button
                      type="button"
                      onClick={() => copyToClipboard(getInviteUrl(createdInvite.code), createdInvite.code)}
                      className="px-3 py-2 bg-white text-black font-semibold text-xs rounded-sm hover:bg-zinc-200 transition-colors cursor-pointer flex items-center gap-1.5 shrink-0"
                    >
                      {copiedCode === createdInvite.code ? (
                        <>
                          <Check className="w-3.5 h-3.5" />
                          <span>Скопировано</span>
                        </>
                      ) : (
                        <>
                          <Copy className="w-3.5 h-3.5" />
                          <span>Копировать</span>
                        </>
                      )}
                    </button>
                  </div>

                  <div className="text-[11px] text-zinc-400">
                    Отправьте эту ссылку студенту после получения перевода. При переходе он сможет мгновенно активировать доступ.
                  </div>
                </div>
              )}

              {/* Creation Form */}
              <form onSubmit={handleCreate} className="space-y-4">
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                  {/* Course */}
                  <div className="space-y-1.5">
                    <label className="text-xs font-mono text-zinc-400 uppercase tracking-wider">
                      Курс *
                    </label>
                    <select
                      value={courseId ? String(courseId) : ''}
                      onChange={(e) => {
                        setCourseId(e.target.value ? Number(e.target.value) : '');
                        setCohortId('');
                      }}
                      className="w-full px-3 py-2 text-xs bg-[#0a0a0c] border border-white/10 rounded-sm text-white focus:border-white/40 focus:outline-hidden"
                      required
                    >
                      <option value="" disabled>Выберите курс</option>
                      {courses.map((c) => (
                        <option key={c.id} value={c.id}>
                          {c.title}
                        </option>
                      ))}
                    </select>
                  </div>

                  {/* Cohort */}
                  <div className="space-y-1.5">
                    <label className="text-xs font-mono text-zinc-400 uppercase tracking-wider">
                      Когорта (опционально)
                    </label>
                    <select
                      value={cohortId}
                      onChange={(e) => setCohortId(e.target.value ? Number(e.target.value) : '')}
                      className="w-full px-3 py-2 text-xs bg-[#0a0a0c] border border-white/10 rounded-sm text-white focus:border-white/40 focus:outline-hidden"
                    >
                      <option value="">Без привязки к когорте</option>
                      {courseCohorts.map((cohort) => (
                        <option key={cohort.id} value={cohort.id}>
                          {cohort.name}
                        </option>
                      ))}
                    </select>
                  </div>
                </div>

                <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                  {/* Max Uses */}
                  <div className="space-y-1.5">
                    <label className="text-xs font-mono text-zinc-400 uppercase tracking-wider">
                      Количество активаций
                    </label>
                    <input
                      type="number"
                      min={1}
                      max={100}
                      value={maxUses}
                      onChange={(e) => setMaxUses(Math.max(1, parseInt(e.target.value, 10) || 1))}
                      className="w-full px-3 py-2 text-xs bg-[#0a0a0c] border border-white/10 rounded-sm text-white focus:border-white/40 focus:outline-hidden font-mono"
                    />
                    <div className="text-[10px] text-zinc-500 font-mono">
                      1 — персональный инвайт для одного студента
                    </div>
                  </div>

                  {/* Expires In Days */}
                  <div className="space-y-1.5">
                    <label className="text-xs font-mono text-zinc-400 uppercase tracking-wider">
                      Срок действия (дней)
                    </label>
                    <select
                      value={expiresInDays}
                      onChange={(e) => setExpiresInDays(Number(e.target.value))}
                      className="w-full px-3 py-2 text-xs bg-[#0a0a0c] border border-white/10 rounded-sm text-white focus:border-white/40 focus:outline-hidden"
                    >
                      <option value={3}>3 дня</option>
                      <option value={7}>7 дней</option>
                      <option value={14}>14 дней (2 недели)</option>
                      <option value={30}>30 дней (1 месяц)</option>
                      <option value={90}>90 дней (3 месяца)</option>
                      <option value={0}>Бессрочно</option>
                    </select>
                  </div>
                </div>

                <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                  {/* Custom Code */}
                  <div className="space-y-1.5">
                    <label className="text-xs font-mono text-zinc-400 uppercase tracking-wider">
                      Свой код (опционально)
                    </label>
                    <input
                      type="text"
                      placeholder="Автогенерация (VIBE-XXXX)"
                      value={customCode}
                      onChange={(e) => setCustomCode(e.target.value.toUpperCase())}
                      className="w-full px-3 py-2 text-xs bg-[#0a0a0c] border border-white/10 rounded-sm text-white uppercase placeholder-zinc-600 focus:border-white/40 focus:outline-hidden font-mono"
                    />
                  </div>

                  {/* Note */}
                  <div className="space-y-1.5">
                    <label className="text-xs font-mono text-zinc-400 uppercase tracking-wider">
                      Заметка (кому выдано)
                    </label>
                    <input
                      type="text"
                      placeholder="Оплата Kaspi / имя студента"
                      value={note}
                      onChange={(e) => setNote(e.target.value)}
                      className="w-full px-3 py-2 text-xs bg-[#0a0a0c] border border-white/10 rounded-sm text-white placeholder-zinc-600 focus:border-white/40 focus:outline-hidden"
                    />
                  </div>
                </div>

                <div className="pt-2">
                  <button
                    type="button"
                    onClick={() => handleCreate()}
                    disabled={createMutation.isPending}
                    className="w-full sm:w-auto inline-flex items-center justify-center gap-2 px-5 py-2.5 rounded-sm bg-white text-black font-semibold text-xs hover:bg-zinc-200 disabled:opacity-50 transition-colors cursor-pointer"
                  >
                    <Plus className="w-4 h-4" />
                    <span>{createMutation.isPending ? 'Генерация...' : 'Сгенерировать ссылку'}</span>
                  </button>
                </div>
              </form>
            </div>
          ) : (
            /* Invites List */
            <div className="space-y-3">
              {isInvitesLoading ? (
                <div className="py-12 text-center text-xs font-mono text-zinc-500">
                  Загрузка списка инвайтов...
                </div>
              ) : invites.length === 0 ? (
                <div className="py-12 text-center text-xs text-zinc-400">
                  Активных инвайт-ссылок пока нет.
                </div>
              ) : (
                <div className="divide-y divide-white/5 border border-white/10 rounded-sm overflow-hidden bg-[#0a0a0c]">
                  {invites.map((inv) => {
                    const isExpired = inv.expired;
                    const isActiveInvite = inv.valid;

                    return (
                      <div
                        key={inv.id}
                        className="p-4 flex flex-col sm:flex-row sm:items-center justify-between gap-3 hover:bg-white/[0.02] transition-colors"
                      >
                        <div className="space-y-1 min-w-0">
                          <div className="flex items-center gap-2">
                            <span className="font-mono font-bold text-xs text-white">
                              {inv.code}
                            </span>
                            <span
                              className={`px-1.5 py-0.5 rounded-xs text-[10px] font-mono border ${
                                isActiveInvite
                                  ? 'bg-white/10 text-white border-white/20'
                                  : 'bg-white/5 text-zinc-500 border-white/10'
                              }`}
                            >
                              {isActiveInvite ? 'Активен' : isExpired ? 'Истек' : 'Исчерпан'}
                            </span>
                          </div>

                          <div className="text-xs text-zinc-300 truncate">
                            {inv.courseTitle}
                            {inv.cohortName && (
                              <span className="text-zinc-500 ml-1.5">
                                ({inv.cohortName})
                              </span>
                            )}
                          </div>

                          <div className="flex items-center gap-3 text-[11px] font-mono text-zinc-500">
                            <span>Использовано: {inv.usedCount}/{inv.maxUses}</span>
                            {inv.expiresAt && (
                              <span>до {new Date(inv.expiresAt).toLocaleDateString()}</span>
                            )}
                            {inv.note && (
                              <span className="text-zinc-400 italic">«{inv.note}»</span>
                            )}
                          </div>
                        </div>

                        <div className="flex items-center gap-2 shrink-0">
                          <button
                            type="button"
                            onClick={() => copyToClipboard(getInviteUrl(inv.code), inv.code)}
                            className="p-2 rounded-sm bg-white/5 hover:bg-white/10 border border-white/10 text-zinc-300 hover:text-white transition-colors cursor-pointer"
                            title="Скопировать ссылку"
                          >
                            {copiedCode === inv.code ? (
                              <Check className="w-4 h-4 text-white" />
                            ) : (
                              <Copy className="w-4 h-4" />
                            )}
                          </button>

                          <a
                            href={`/invite/${inv.code}`}
                            target="_blank"
                            rel="noopener noreferrer"
                            className="p-2 rounded-sm bg-white/5 hover:bg-white/10 border border-white/10 text-zinc-300 hover:text-white transition-colors cursor-pointer"
                            title="Открыть страницу инвайта"
                          >
                            <ExternalLink className="w-4 h-4" />
                          </a>

                          <button
                            type="button"
                            disabled={deleteMutation.isPending}
                            onClick={() => {
                              if (window.confirm(`Удалить инвайт ${inv.code}?`)) {
                                deleteMutation.mutate(inv.id);
                              }
                            }}
                            className="p-2 rounded-sm bg-white/5 hover:bg-white/10 border border-white/10 text-zinc-500 hover:text-white transition-colors cursor-pointer"
                            title="Удалить инвайт"
                          >
                            <Trash2 className="w-4 h-4" />
                          </button>
                        </div>
                      </div>
                    );
                  })}
                </div>
              )}
            </div>
          )}
        </div>
      </div>
    </div>
  );
};
