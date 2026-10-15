import React, { useState, useEffect } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { useAuth } from '@/features/auth';
import { inviteApi } from '@/entities/invite/api/inviteApi';
import { CourseInvite, RedeemInviteResult } from '@/shared/types';
import { Check, AlertCircle, ArrowRight, BookOpen, Clock, Users, ShieldCheck } from 'lucide-react';

export const InviteRedeemPage: React.FC = () => {
  const { code } = useParams<{ code: string }>();
  const navigate = useNavigate();
  const { isAuthenticated, isLoading: authLoading, user } = useAuth();

  const [invite, setInvite] = useState<CourseInvite | null>(null);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  const [redeeming, setRedeeming] = useState<boolean>(false);
  const [redeemResult, setRedeemResult] = useState<RedeemInviteResult | null>(null);
  const [redeemError, setRedeemError] = useState<string | null>(null);

  useEffect(() => {
    if (!code) {
      setError('Код инвайта не указан');
      setLoading(false);
      return;
    }

    let isMounted = true;
    setLoading(true);
    setError(null);

    inviteApi.getInviteByCode(code.trim().toUpperCase())
      .then((data) => {
        if (isMounted) {
          setInvite(data);
          setLoading(false);
        }
      })
      .catch((err: unknown) => {
        if (isMounted) {
          const message = err instanceof Error ? err.message : 'Инвайт-ссылка недействительна или не существует';
          setError(message);
          setLoading(false);
        }
      });

    return () => {
      isMounted = false;
    };
  }, [code]);

  const handleRedeem = async () => {
    if (!code) return;
    setRedeeming(true);
    setRedeemError(null);

    try {
      const result = await inviteApi.redeemInvite(code.trim().toUpperCase());
      setRedeemResult(result);
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : 'Ошибка активации инвайта';
      setRedeemError(message);
    } finally {
      setRedeeming(false);
    }
  };

  if (loading || authLoading) {
    return (
      <div className="flex flex-col items-center justify-center min-h-[70vh] px-4">
        <div className="w-8 h-8 border-2 border-white/10 border-t-white rounded-full animate-spin mb-4" />
        <span className="text-xs font-mono text-zinc-400">Проверка инвайт-ссылки...</span>
      </div>
    );
  }

  if (error || !invite) {
    return (
      <div className="flex flex-col items-center justify-center min-h-[70vh] px-4">
        <div className="w-full max-w-md p-8 rounded-sm bg-[#0e0e11] border border-white/10 text-center">
          <div className="w-12 h-12 mx-auto mb-4 rounded-full bg-white/5 border border-white/10 flex items-center justify-center text-zinc-400">
            <AlertCircle className="w-6 h-6" />
          </div>
          <h1 className="text-xl font-bold text-white mb-2">Ссылка недействительна</h1>
          <p className="text-sm text-zinc-400 mb-6">
            {error || 'Инвайт-код не найден или срок его действия истек.'}
          </p>
          <Link
            to="/courses"
            className="inline-flex items-center justify-center px-5 py-2.5 rounded-sm bg-white text-black font-semibold text-sm hover:bg-zinc-200 transition-colors"
          >
            Каталог курсов
          </Link>
        </div>
      </div>
    );
  }

  // Success state after redeeming
  if (redeemResult) {
    return (
      <div className="flex flex-col items-center justify-center min-h-[70vh] px-4">
        <div className="w-full max-w-md p-8 rounded-sm bg-[#0e0e11] border border-white/10 text-center shadow-2xl">
          <div className="w-12 h-12 mx-auto mb-4 rounded-full bg-white/10 border border-white/20 flex items-center justify-center text-white">
            <Check className="w-6 h-6" />
          </div>
          <h1 className="text-xl font-bold text-white mb-2">
            {redeemResult.alreadyEnrolled ? 'Вы уже зачислены' : 'Доступ активирован'}
          </h1>
          <p className="text-sm text-zinc-400 mb-6">
            {redeemResult.message}
          </p>

          <div className="p-4 mb-6 rounded-sm bg-[#141418] border border-white/5 text-left space-y-2">
            <div className="text-xs font-mono text-zinc-500 uppercase tracking-wider">Курс</div>
            <div className="text-base font-semibold text-white">{redeemResult.courseTitle}</div>
            {redeemResult.cohortName && (
              <div className="text-xs text-zinc-400">Когорта: {redeemResult.cohortName}</div>
            )}
          </div>

          <button
            type="button"
            onClick={() => navigate(`/courses/${redeemResult.courseSlug}`)}
            className="w-full inline-flex items-center justify-center gap-2 px-5 py-2.5 rounded-sm bg-white text-black font-semibold text-sm hover:bg-zinc-200 transition-colors cursor-pointer"
          >
            <span>Перейти к обучению</span>
            <ArrowRight className="w-4 h-4" />
          </button>
        </div>
      </div>
    );
  }

  const isInvalid = !invite.valid;

  return (
    <div className="flex flex-col items-center justify-center min-h-[70vh] px-4 py-12">
      <div className="w-full max-w-md p-8 rounded-sm bg-[#0e0e11] border border-white/10 shadow-2xl">
        {/* Header */}
        <div className="flex items-center gap-3 pb-6 border-b border-white/10">
          <div className="w-10 h-10 rounded-sm bg-white/5 border border-white/10 flex items-center justify-center text-white shrink-0">
            <ShieldCheck className="w-5 h-5" />
          </div>
          <div>
            <div className="text-xs font-mono uppercase tracking-wider text-zinc-400">Персональный инвайт</div>
            <h1 className="text-lg font-bold text-white tracking-tight">Доступ к платформе</h1>
          </div>
        </div>

        {/* Course Card */}
        <div className="my-6 p-4 rounded-sm bg-[#141418] border border-white/5 space-y-3">
          <div>
            <div className="text-xs font-mono text-zinc-500 uppercase tracking-wider mb-1 flex items-center gap-1.5">
              <BookOpen className="w-3.5 h-3.5" />
              <span>Программа курса</span>
            </div>
            <div className="text-base font-semibold text-white">{invite.courseTitle}</div>
          </div>

          {invite.cohortName && (
            <div className="flex items-center gap-2 text-xs text-zinc-400 pt-1 border-t border-white/5">
              <Users className="w-3.5 h-3.5 text-zinc-500" />
              <span>Когорта: <strong className="text-zinc-200">{invite.cohortName}</strong></span>
            </div>
          )}

          <div className="flex items-center justify-between text-xs text-zinc-400 pt-2 border-t border-white/5 font-mono">
            <span>Код: {invite.code}</span>
            {invite.expiresAt && (
              <span className="flex items-center gap-1 text-zinc-500">
                <Clock className="w-3 h-3" />
                <span>до {new Date(invite.expiresAt).toLocaleDateString()}</span>
              </span>
            )}
          </div>
        </div>

        {/* Error notification if redeem failed */}
        {redeemError && (
          <div className="mb-6 p-3 rounded-sm bg-white/5 border border-white/20 text-xs text-zinc-300">
            {redeemError}
          </div>
        )}

        {/* Status: Exhausted or Expired */}
        {isInvalid ? (
          <div className="space-y-4">
            <div className="p-3.5 rounded-sm bg-white/5 border border-white/10 text-xs text-zinc-400 text-center">
              {invite.expired
                ? 'Срок действия этого инвайт-кода истек.'
                : 'Лимит активаций этого инвайт-кода исчерпан.'}
            </div>
            <Link
              to="/courses"
              className="w-full inline-flex items-center justify-center px-4 py-2.5 rounded-sm bg-white/5 border border-white/10 text-white font-medium text-xs hover:bg-white/10 transition-colors"
            >
              Вернуться в каталог
            </Link>
          </div>
        ) : !isAuthenticated ? (
          /* Guest state: Prompt to log in / register */
          <div className="space-y-4">
            <div className="p-3 rounded-sm bg-white/5 border border-white/10 text-xs text-zinc-300">
              Для активации инвайта войдите в существующий аккаунт или зарегистрируйтесь.
            </div>
            <Link
              to={`/login?redirect=${encodeURIComponent(`/invite/${invite.code}`)}`}
              className="w-full inline-flex items-center justify-center gap-2 px-5 py-2.5 rounded-sm bg-white text-black font-semibold text-sm hover:bg-zinc-200 transition-colors"
            >
              <span>Войти и активировать</span>
              <ArrowRight className="w-4 h-4" />
            </Link>
          </div>
        ) : (
          /* Authenticated state: Prompt to redeem */
          <div className="space-y-4">
            <div className="text-xs text-zinc-400">
              Вы авторизованы как <strong className="text-white">{user?.email}</strong>. Нажмите кнопку ниже для зачисления.
            </div>
            <button
              type="button"
              disabled={redeeming}
              onClick={handleRedeem}
              className="w-full inline-flex items-center justify-center gap-2 px-5 py-2.5 rounded-sm bg-white text-black font-semibold text-sm hover:bg-zinc-200 disabled:opacity-50 transition-colors cursor-pointer"
            >
              {redeeming ? (
                <>
                  <div className="w-4 h-4 border-2 border-black/20 border-t-black rounded-full animate-spin" />
                  <span>Активация доступа...</span>
                </>
              ) : (
                <>
                  <span>Активировать доступ</span>
                  <ArrowRight className="w-4 h-4" />
                </>
              )}
            </button>
          </div>
        )}
      </div>
    </div>
  );
};
