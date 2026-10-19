import React, { useEffect, useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import { certificateApi } from '@/entities/certificate/api/certificateApi';
import type { Certificate } from '@/shared/types';

export const CertificateVerifyPage: React.FC = () => {
  const { code } = useParams<{ code: string }>();
  const [certificate, setCertificate] = useState<Certificate | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!code) {
      setError('Код сертификата не указан');
      setIsLoading(false);
      return;
    }

    certificateApi.verifyCertificate(code)
      .then((data) => {
        setCertificate(data);
        setError(null);
      })
      .catch(() => {
        setError('Сертификат с указанным кодом не найден или недействителен.');
      })
      .finally(() => {
        setIsLoading(false);
      });
  }, [code]);

  if (isLoading) {
    return (
      <div className="flex items-center justify-center min-h-[60vh]">
        <div className="w-8 h-8 border-2 border-white border-t-transparent rounded-full animate-spin" />
      </div>
    );
  }

  return (
    <div className="w-full max-w-[1440px] mx-auto px-4 sm:px-6 lg:px-8 py-10">
      <div className="w-full lg:w-[82%] max-w-[1120px] space-y-8">
        {error ? (
          <div className="p-8 rounded-sm bg-[#0e0e11] border border-white/5 shadow-[inset_0_1px_0_rgba(255,255,255,0.04)] text-center">
            <div className="w-12 h-12 mx-auto mb-4 rounded-full bg-white/10 border border-white/20 flex items-center justify-center text-white">
              <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <circle cx="12" cy="12" r="10" />
                <line x1="15" y1="9" x2="9" y2="15" />
                <line x1="9" y1="9" x2="15" y2="15" />
              </svg>
            </div>
            <h1 className="text-2xl font-bold text-white mb-2">Ошибка верификации</h1>
            <p className="text-xs text-zinc-500 mb-6 font-mono">{error}</p>
            <Link
              to="/courses"
              className="inline-block px-5 py-2.5 rounded-sm bg-white hover:bg-zinc-200 text-xs text-black font-semibold transition-colors font-mono"
            >
              К каталогу курсов
            </Link>
          </div>
        ) : certificate ? (
          <div className="space-y-6">
            {/* Top Status Strip */}
            <div className="flex flex-wrap items-center justify-between gap-4 p-4 rounded-sm bg-[#0e0e11] border border-white/10 font-mono">
              <div className="flex items-center gap-3">
                <div className="w-8 h-8 rounded-full bg-white/10 border border-white/20 flex items-center justify-center text-white">
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                    <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14" />
                    <polyline points="22 4 12 14.01 9 11.01" />
                  </svg>
                </div>
                <div>
                  <div className="text-xs font-bold text-white uppercase tracking-wider">
                    ОФИЦИАЛЬНО ПОДТВЕРЖДЕНО — СЕРТИФИКАТ ПОДЛИННЫЙ
                  </div>
                  <div className="text-[11px] text-zinc-400">
                    Реестр верификации выпускников MrDevCourses • ID: {certificate.certificateCode}
                  </div>
                </div>
              </div>
              <div className="flex items-center gap-3">
                <a
                  href={certificateApi.getPdfDownloadUrl(certificate.certificateCode)}
                  target="_blank"
                  rel="noreferrer"
                  className="px-4 py-2 rounded-sm bg-white hover:bg-zinc-200 text-xs font-semibold text-black transition-colors"
                >
                  Скачать официальный PDF
                </a>
                <Link
                  to="/courses"
                  className="px-4 py-2 rounded-sm bg-[#141418] hover:bg-zinc-800 text-xs text-white transition-colors border border-white/10"
                >
                  Все курсы MrDeveloper
                </Link>
              </div>
            </div>

            {/* Visual Monochrome A4 Certificate Preview */}
            <div className="p-5 sm:p-8 bg-[#0e0e11] border-2 border-white/90 shadow-2xl">
              <div className="p-6 sm:p-12 bg-[#141418] border border-white/15 text-center space-y-8">
                <div className="space-y-2">
                  <div className="text-xs sm:text-sm font-bold tracking-[0.3em] text-white uppercase font-mono">
                    MRDEV COURSES
                  </div>
                  <div className="text-[10px] tracking-[0.25em] text-zinc-500 uppercase font-mono">
                    ENGINEERING &amp; ARCHITECTURE ACADEMY
                  </div>
                  <h1 className="text-2xl sm:text-4xl font-extrabold tracking-[0.2em] text-white uppercase pt-3">
                    СЕРТИФИКАТ
                  </h1>
                  <p className="text-[10px] sm:text-xs tracking-[0.15em] text-zinc-400 uppercase font-mono">
                    НАСТОЯЩИЙ ДОКУМЕНТ ПОДТВЕРЖДАЕТ УСПЕШНОЕ ОКОНЧАНИЕ ОБУЧЕНИЯ
                  </p>
                </div>

                <div className="py-4 space-y-4">
                  <div className="inline-block border-b-2 border-white px-8 pb-2 text-2xl sm:text-3xl font-bold text-white tracking-wide">
                    {certificate.userName}
                  </div>
                  <p className="text-xs sm:text-sm text-zinc-400 max-w-xl mx-auto">
                    успешно освоил(а) инженерную программу и защитил(а) все практические работы курса
                  </p>
                  <div className="text-lg sm:text-2xl font-bold text-white tracking-tight">
                    {certificate.courseTitle}
                  </div>
                  <div className="pt-2">
                    <span className="inline-block px-4 py-1.5 bg-[#0a0a0c] border border-white text-[10px] font-bold tracking-[0.2em] text-white uppercase font-mono">
                      100% ВЫПОЛНЕНИЕ ПРОГРАММЫ
                    </span>
                  </div>
                </div>

                <div className="pt-8 border-t border-white/10 grid grid-cols-1 sm:grid-cols-3 gap-6 items-end text-xs font-mono text-zinc-400">
                  <div className="text-left space-y-1">
                    <div>
                      Дата выдачи:{' '}
                      <span className="text-white font-semibold">
                        {new Date(certificate.issuedAt).toLocaleDateString('ru-RU', {
                          day: 'numeric',
                          month: 'long',
                          year: 'numeric',
                        })}
                      </span>
                    </div>
                    <div>
                      ID Сертификата: <span className="text-white font-semibold">{certificate.certificateCode}</span>
                    </div>
                    <div>
                      Статус: <span className="text-white font-semibold">100% Завершено</span>
                    </div>
                  </div>

                  <div className="text-center space-y-1">
                    <div className="inline-block border-t border-white/30 pt-1.5 px-6 text-white font-bold tracking-widest">
                      MR DEVELOPER
                    </div>
                    <div className="text-[10px] text-zinc-500 uppercase tracking-wider">
                      Lead Architect &amp; Mentor
                    </div>
                  </div>

                  <div className="text-left sm:text-right space-y-1">
                    <div className="text-zinc-500">Реестр подлинности:</div>
                    <div className="text-white break-all text-[11px]">
                      {certificate.verificationUrl || `https://courses.mrsgemaseny.com/certificates/verify/${certificate.certificateCode}`}
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        ) : null}
      </div>
    </div>
  );
};
