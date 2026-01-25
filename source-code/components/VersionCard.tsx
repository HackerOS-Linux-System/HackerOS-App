import React from 'react';
import { Terminal, Calendar, Activity, ArrowRight } from 'lucide-react';
import { ReleaseInfo } from '../types';

interface VersionCardProps {
  release: ReleaseInfo;
  isLatest: boolean;
}

export const VersionCard: React.FC<VersionCardProps> = ({ release, isLatest }) => {
  return (
    <div className={`
      relative p-5 rounded-2xl border backdrop-blur-sm transition-all duration-300
      ${isLatest
        ? 'bg-card/40 border-primary/30 shadow-[0_0_30px_-15px_rgb(var(--color-primary))]'
        : 'bg-card/20 border-white/5 hover:bg-card/30'
      }
      `}>
      {isLatest && (
        <div className="absolute -top-3 right-4 flex items-center gap-1.5 bg-primary text-background text-[10px] font-bold px-3 py-1 rounded-full shadow-lg font-mono tracking-wide">
        <span className="relative flex h-2 w-2">
        <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-white opacity-75"></span>
        <span className="relative inline-flex rounded-full h-2 w-2 bg-white"></span>
        </span>
        LATEST_BUILD
        </div>
      )}

      <div className="flex items-center gap-3 mb-4">
      <div className={`p-2.5 rounded-xl ${isLatest ? 'bg-primary/20 text-primary' : 'bg-white/5 text-muted'}`}>
      <Terminal size={22} />
      </div>
      <div>
      <h3 className="text-xl font-mono font-bold text-text">
      {release.version}
      </h3>
      <p className="text-xs text-muted font-mono flex items-center gap-1">
      Build: RELEASE_CANDIDATE
      </p>
      </div>
      </div>

      <div className="flex items-center gap-2 mb-4 text-xs text-muted/80 bg-background/50 border border-white/5 p-3 rounded-lg">
      <Calendar size={14} className="shrink-0 text-primary" />
      <p className="font-mono leading-relaxed truncate">{release.editions}</p>
      </div>

      <div className="space-y-2">
      <div className="flex items-center gap-2 text-xs font-bold uppercase tracking-widest text-muted/50">
      <Activity size={12} />
      <span>System Log</span>
      </div>
      <div className="text-sm text-text/90 leading-relaxed border-l-2 border-primary/20 pl-3">
      {release.news
        ? release.news.split('\n').map((line, i) => (
          <p key={i} className="mb-1 last:mb-0 flex items-start gap-2">
          <span className="text-primary mt-1.5 text-[6px] opacity-60">●</span>
          {line}
          </p>
        ))
        : <span className="text-muted italic">No data available.</span>
      }
      </div>
      </div>
      </div>
  );
};
