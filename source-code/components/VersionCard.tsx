import React from 'react';
import { Terminal, Calendar, Activity, Info, Clock, CheckCircle2 } from 'lucide-react';
import { ReleaseInfo, Language } from '../types';
import { TRANSLATIONS } from '../utils/translations';

interface VersionCardProps {
  release: ReleaseInfo;
  isLatest: boolean;
  language: Language;
}

export const VersionCard: React.FC<VersionCardProps> = ({ release, isLatest, language }) => {
  const t = TRANSLATIONS[language];

  // Helper to parse the edition string (e.g. "HackerOS Official: 18.01.2026")
  const parseEditionLine = (line: string) => {
    const parts = line.split(':');
    if (parts.length >= 2) {
      return {
        name: parts[0].trim(),
        date: parts.slice(1).join(':').trim()
      };
    }
    return { name: line, date: '' };
  };

  const editionsList = release.editions 
    ? release.editions.split('\n').map(parseEditionLine)
    : [];

  return (
    <div className={`
      relative overflow-hidden rounded-2xl border backdrop-blur-md transition-all duration-300 mb-6
      ${isLatest 
        ? 'bg-card/60 border-primary/30 shadow-[0_0_25px_-10px_rgb(var(--color-primary))]' 
        : 'bg-card/30 border-white/5 hover:bg-card/40'
      }
    `}>
      {/* Header Background Accent */}
      {isLatest && (
        <div className="absolute top-0 right-0 w-32 h-32 bg-primary/10 blur-[50px] rounded-full pointer-events-none" />
      )}

      {/* Top Banner for Latest */}
      {isLatest && (
        <div className="bg-primary/10 border-b border-primary/10 px-4 py-1.5 flex items-center justify-between">
          <span className="text-[10px] font-bold text-primary tracking-widest uppercase flex items-center gap-2">
            <span className="relative flex h-2 w-2">
              <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-primary opacity-75"></span>
              <span className="relative inline-flex rounded-full h-2 w-2 bg-primary"></span>
            </span>
            {t.latest_build}
          </span>
        </div>
      )}

      <div className="p-5">
        {/* Version Header */}
        <div className="flex items-start gap-4 mb-4">
          <div className={`p-3 rounded-xl shrink-0 ${isLatest ? 'bg-primary text-background' : 'bg-white/5 text-muted'}`}>
            <Terminal size={24} strokeWidth={2.5} />
          </div>
          <div className="flex-1">
            <h3 className="text-2xl font-mono font-bold text-text tracking-tight">
              {release.version}
            </h3>
            {release.description && (
              <div className="mt-1.5 flex items-start gap-2 text-sm text-muted/90 leading-snug">
                <Info size={14} className="mt-0.5 shrink-0 opacity-70" />
                <p>{release.description}</p>
              </div>
            )}
          </div>
        </div>

        {/* Release Roadmap / Editions Grid */}
        {editionsList.length > 0 && (
          <div className="mb-5 bg-background/40 rounded-xl border border-white/5 overflow-hidden">
             <div className="px-3 py-2 border-b border-white/5 bg-white/5 flex items-center gap-2">
               <Calendar size={12} className="text-primary" />
               <span className="text-[10px] font-bold uppercase tracking-wider text-muted">{t.roadmap}</span>
             </div>
             <div className="divide-y divide-white/5">
               {editionsList.map((edition, idx) => (
                 <div key={idx} className="flex items-center justify-between p-3 hover:bg-white/5 transition-colors">
                   <div className="flex items-center gap-2">
                     <div className="w-1.5 h-1.5 rounded-full bg-primary/50" />
                     <span className="text-xs font-medium text-text">{edition.name}</span>
                   </div>
                   {edition.date && (
                     <div className="flex items-center gap-1.5 bg-background/60 px-2 py-1 rounded text-[10px] font-mono text-primary border border-primary/10">
                       <Clock size={10} />
                       {edition.date}
                     </div>
                   )}
                 </div>
               ))}
             </div>
          </div>
        )}

        {/* Changelog / News */}
        <div className="space-y-3">
          <div className="flex items-center gap-2 text-xs font-bold uppercase tracking-widest text-muted/50 pl-1">
            <Activity size={12} />
            <span>{t.changelog}</span>
          </div>
          <div className="relative">
            <div className="absolute top-0 bottom-0 left-1.5 w-px bg-white/10" />
            <div className="space-y-3 pl-5">
              {release.news 
                ? release.news.split('\n').map((line, i) => (
                    <div key={i} className="group relative">
                      <div className="absolute -left-[19px] top-1.5 w-2 h-2 rounded-full border border-card bg-muted/20 group-hover:bg-primary transition-colors" />
                      <p className="text-xs text-text/80 leading-relaxed font-mono">
                        {line}
                      </p>
                    </div>
                  ))
                : <span className="text-muted italic text-xs">{t.no_changes}</span>
              }
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};
