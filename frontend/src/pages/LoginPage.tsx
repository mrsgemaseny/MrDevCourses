import React, { useState } from 'react';
import { Navigate, useSearchParams, Link } from 'react-router-dom';
import { EmailAuthForm, useAuth, AuthMode } from '@/features/auth';

export const LoginPage: React.FC = () => {
  const { isAuthenticated, isLoading, user } = useAuth();
  const [searchParams] = useSearchParams();
  const errorParam = searchParams.get('error');

  const [mode, setMode] = useState<AuthMode>('login');

  if (!isLoading && isAuthenticated) {
    const redirectParam = searchParams.get('redirect');
    const target = redirectParam || (user?.role === 'ADMIN' ? '/admin' : '/courses');
    return <Navigate to={target} replace />;
  }

  const [avatarError, setAvatarError] = React.useState(false);

  return (
    <div className="flex flex-col items-center justify-center min-h-[75vh] px-4 py-12">
      <div className="w-full max-w-[340px] sm:max-w-[360px] space-y-4">
        {/* Brand Header */}
        <div className="flex flex-col items-center text-center space-y-3 mb-2">
          <Link to="/" className="w-20 h-20 rounded-full border border-white/20 overflow-hidden shrink-0 bg-[#0a0a0c] shadow-xl hover:border-white/40 transition-colors flex items-center justify-center">
            {!avatarError ? (
              <img
                src={`${import.meta.env.BASE_URL}author-avatar.png`}
                alt="MrDeveloper"
                onError={() => setAvatarError(true)}
                className="w-full h-full object-cover scale-[1.35]"
              />
            ) : (
              <span className="text-sm font-bold font-mono text-zinc-300">MD</span>
            )}
          </Link>
          <h1 className="text-xl sm:text-2xl font-bold text-white tracking-tight">
            {mode === 'login' ? 'Вход в MrDeveloper' : 'Регистрация в MrDeveloper'}
          </h1>
        </div>

        {/* URL Error notification */}
        {errorParam && (
          <div className="p-3 rounded-sm bg-red-500/10 border border-red-500/20 text-red-400 text-xs text-center">
            {errorParam}
          </div>
        )}

        {/* Main Form Box */}
        <div className="p-6 rounded-sm bg-[#0e0e11] border border-white/10 shadow-xl">
          <EmailAuthForm mode={mode} />
        </div>

        {/* Toggle Mode Box */}
        <div className="p-3.5 rounded-sm border border-white/10 bg-[#0e0e11] text-center text-xs text-zinc-400">
          {mode === 'login' ? (
            <span>
              Впервые в MrDeveloper?{' '}
              <button
                type="button"
                onClick={() => setMode('register')}
                className="text-white hover:underline font-medium transition-colors cursor-pointer"
              >
                Создать аккаунт
              </button>
            </span>
          ) : (
            <span>
              Уже есть аккаунт?{' '}
              <button
                type="button"
                onClick={() => setMode('login')}
                className="text-white hover:underline font-medium transition-colors cursor-pointer"
              >
                Войти
              </button>
            </span>
          )}
        </div>

        {/* Footer info */}
        <div className="text-center text-[11px] text-zinc-500 pt-2">
          Авторизуясь, вы соглашаетесь с правилами платформы
        </div>
      </div>
    </div>
  );
};
