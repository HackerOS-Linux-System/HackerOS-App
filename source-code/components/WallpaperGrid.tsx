import React, { useState } from 'react';
import { Download, X, ZoomIn } from 'lucide-react';
import { WALLPAPERS } from '../constants';
import { Wallpaper } from '../types';

export const WallpaperGrid: React.FC = () => {
  const [selectedWallpaper, setSelectedWallpaper] = useState<Wallpaper | null>(null);

  const handleDownload = (url: string, name: string) => {
    // In a browser context, we can try to force download or open in new tab
    const link = document.createElement('a');
    link.href = url;
    link.target = '_blank';
    link.download = `${name.replace(/\s+/g, '-')}.png`;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  };

  return (
    <div className="pb-20">
      <h2 className="text-2xl font-bold mb-6 px-4 pt-4 dark:text-white">Wallpapers</h2>
      
      <div className="grid grid-cols-2 gap-4 px-4">
        {WALLPAPERS.map((wp) => (
          <button
            key={wp.id}
            onClick={() => setSelectedWallpaper(wp)}
            className="group relative aspect-[9/16] rounded-xl overflow-hidden shadow-sm hover:shadow-lg transition-all"
          >
            <img 
              src={wp.thumbnail} 
              alt={wp.name} 
              className="w-full h-full object-cover transition-transform duration-500 group-hover:scale-110"
              loading="lazy"
            />
            <div className="absolute inset-0 bg-black/0 group-hover:bg-black/20 transition-colors" />
            <div className="absolute bottom-0 left-0 right-0 p-3 bg-gradient-to-t from-black/80 to-transparent opacity-100">
              <span className="text-white text-sm font-medium truncate block">{wp.name}</span>
            </div>
          </button>
        ))}
      </div>

      {/* Fullscreen Preview Modal */}
      {selectedWallpaper && (
        <div className="fixed inset-0 z-[60] flex items-center justify-center bg-black/95 p-4 backdrop-blur-sm">
          <button 
            onClick={() => setSelectedWallpaper(null)}
            className="absolute top-4 right-4 p-2 text-white/70 hover:text-white rounded-full bg-white/10"
          >
            <X size={24} />
          </button>

          <div className="relative w-full max-w-sm aspect-[9/16] rounded-lg overflow-hidden shadow-2xl">
            <img 
              src={selectedWallpaper.url} 
              alt={selectedWallpaper.name} 
              className="w-full h-full object-cover"
            />
          </div>

          <div className="absolute bottom-8 left-0 right-0 flex justify-center">
             <button
              onClick={(e) => {
                e.stopPropagation();
                handleDownload(selectedWallpaper.url, selectedWallpaper.name);
              }}
              className="flex items-center gap-2 bg-primary hover:bg-primary-dark text-white px-6 py-3 rounded-full font-bold shadow-lg transition-transform active:scale-95"
            >
              <Download size={20} />
              Download Wallpaper
            </button>
          </div>
        </div>
      )}
    </div>
  );
};
