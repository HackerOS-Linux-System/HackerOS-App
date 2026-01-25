import React, { useState } from 'react';
import { Download, X, Maximize2 } from 'lucide-react';
import { WALLPAPERS } from '../constants';
import { Wallpaper } from '../types';

export const WallpaperGrid: React.FC = () => {
  const [selectedWallpaper, setSelectedWallpaper] = useState<Wallpaper | null>(null);

  const handleDownload = (url: string, name: string) => {
    const link = document.createElement('a');
    link.href = url;
    link.target = '_blank';
    link.download = `${name.replace(/\s+/g, '-')}.png`;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  };

  return (
    <div className="pb-24 pt-2">
    <div className="px-6 mb-6">
    <h2 className="text-3xl font-mono font-bold text-white mb-1">WALLPAPERS</h2>
    <p className="text-muted text-sm">Customize your device</p>
    </div>

    <div className="grid grid-cols-2 gap-4 px-4">
    {WALLPAPERS.map((wp) => (
      <button
      key={wp.id}
      onClick={() => setSelectedWallpaper(wp)}
      className="group relative aspect-[9/16] rounded-2xl overflow-hidden shadow-lg border border-white/5 bg-card/30"
      >
      <img
      src={wp.thumbnail}
      alt={wp.name}
      className="w-full h-full object-cover transition-transform duration-700 group-hover:scale-105"
      loading="lazy"
      />

      {/* Overlay Gradient */}
      <div className="absolute inset-0 bg-gradient-to-t from-background via-transparent to-transparent opacity-80" />

      <div className="absolute bottom-0 left-0 right-0 p-4 text-left">
      <span className="text-white text-sm font-bold block mb-1 drop-shadow-md">{wp.name}</span>
      <div className="flex items-center gap-1 text-[10px] text-primary font-mono bg-black/40 backdrop-blur-sm px-2 py-0.5 rounded w-fit">
      <Maximize2 size={10} />
      <span>HD_ASSET</span>
      </div>
      </div>
      </button>
    ))}
    </div>

    {/* Fullscreen Preview Modal */}
    {selectedWallpaper && (
      <div className="fixed inset-0 z-[100] flex items-center justify-center bg-black/95 backdrop-blur-xl p-0 animate-in fade-in duration-200">
      <button
      onClick={() => setSelectedWallpaper(null)}
      className="absolute top-8 right-6 p-3 text-white/70 hover:text-white rounded-full bg-white/10 z-10 backdrop-blur-md"
      >
      <X size={24} />
      </button>

      <div className="relative w-full h-full flex flex-col items-center justify-center">
      <div className="relative w-full max-w-[85vw] aspect-[9/16] rounded-2xl overflow-hidden shadow-[0_0_50px_-12px_rgba(var(--color-primary),0.3)] border border-white/10">
      <img
      src={selectedWallpaper.url}
      alt={selectedWallpaper.name}
      className="w-full h-full object-cover"
      />
      </div>

      <div className="absolute bottom-12 w-full px-8">
      <button
      onClick={(e) => {
        e.stopPropagation();
        handleDownload(selectedWallpaper.url, selectedWallpaper.name);
      }}
      className="w-full flex items-center justify-center gap-3 bg-primary hover:bg-primary/90 text-background px-6 py-4 rounded-xl font-bold text-lg shadow-[0_0_20px_-5px_rgb(var(--color-primary))] transition-transform active:scale-95"
      >
      <Download size={24} />
      <span>INSTALL_WALLPAPER</span>
      </button>
      </div>
      </div>
      </div>
    )}
    </div>
  );
};
