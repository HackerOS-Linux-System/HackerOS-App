import React from 'react';
import { Aperture, Construction } from 'lucide-react';
import { Language } from '../types';
import { TRANSLATIONS } from '../utils/translations';

interface GalleryProps {
  language: Language;
}

export const Gallery: React.FC<GalleryProps> = ({ language }) => {
  const t = TRANSLATIONS[language];

  return (
    <div className="pb-24 pt-2 h-screen flex flex-col">
      <div className="px-6 mb-6">
        <h2 className="text-3xl font-mono font-bold text-white mb-1">{t.header_gallery}</h2>
        <p className="text-muted text-sm">{t.sub_gallery}</p>
      </div>

      <div className="flex-1 flex flex-col items-center justify-center px-6 text-center space-y-6">
        <div className="relative">
          <div className="absolute inset-0 bg-primary/20 blur-2xl rounded-full" />
          <div className="relative bg-card/50 border border-white/5 p-8 rounded-full backdrop-blur-md">
            <Aperture size={48} className="text-primary animate-spin-slow" style={{ animationDuration: '10s' }} />
          </div>
        </div>

        <div className="space-y-2">
          <h3 className="text-xl font-bold text-white flex items-center justify-center gap-2">
            <Construction size={20} className="text-yellow-500" />
            {t.construction_title}
          </h3>
          <p className="text-muted text-sm max-w-xs mx-auto leading-relaxed">
            {t.construction_desc}
          </p>
        </div>

        <div className="pt-8">
          <span className="px-3 py-1 rounded-full bg-white/5 border border-white/10 text-[10px] font-mono text-muted uppercase tracking-widest">
            Module: 0x4_GALLERY
          </span>
        </div>
      </div>
    </div>
  );
};
