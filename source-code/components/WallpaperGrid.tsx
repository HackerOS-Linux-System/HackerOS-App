import React, { useState } from 'react';
import { Download, X, Maximize2, Loader2, CheckCircle, AlertCircle } from 'lucide-react';
import { WALLPAPERS } from '../constants';
import { Wallpaper, Language } from '../types';
import { Filesystem, Directory } from '@capacitor/filesystem';
import { Toast } from '@capacitor/toast';
import { TRANSLATIONS } from '../utils/translations';

interface WallpaperGridProps {
  language: Language;
}

export const WallpaperGrid: React.FC<WallpaperGridProps> = ({ language }) => {
  const [selectedWallpaper, setSelectedWallpaper] = useState<Wallpaper | null>(null);
  const [downloading, setDownloading] = useState(false);
  const t = TRANSLATIONS[language];

  const handleDownload = async (url: string, name: string) => {
    setDownloading(true);
    try {
      // 1. Fetch the blob
      const response = await fetch(url);
      const blob = await response.blob();

      // 2. Convert to base64
      const reader = new FileReader();
      reader.readAsDataURL(blob);
      reader.onloadend = async () => {
        const base64data = reader.result as string;
        
        try {
          // 3. Try Capacitor Filesystem
          const fileName = `HackerOS_${name.replace(/\s+/g, '_')}_${Date.now()}.png`;
          
          await Filesystem.writeFile({
            path: fileName,
            data: base64data,
            directory: Directory.Documents
          });

          await Toast.show({
            text: `Wallpaper saved to Documents/${fileName}`,
            duration: 'long'
          });
          
        } catch (fsError) {
          console.error("Filesystem write failed, falling back to browser download", fsError);
          
          // Fallback: Create generic anchor click (standard browser behavior)
          const link = document.createElement('a');
          link.href = base64data;
          link.download = `${name.replace(/\s+/g, '-')}.png`;
          document.body.appendChild(link);
          link.click();
          document.body.removeChild(link);

          await Toast.show({
            text: `Download started...`,
            duration: 'short'
          });
        }
        setDownloading(false);
      };

    } catch (error) {
      console.error('Download failed:', error);
      await Toast.show({
        text: 'Download failed. Check internet connection.',
        duration: 'short'
      });
      setDownloading(false);
    }
  };

  return (
    <div className="pb-24 pt-2">
      <div className="px-6 mb-6">
        <h2 className="text-3xl font-mono font-bold text-white mb-1">{t.header_wallpapers}</h2>
        <p className="text-muted text-sm">{t.sub_wallpapers}</p>
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
                <span>{t.hd_asset}</span>
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
                disabled={downloading}
                onClick={(e) => {
                  e.stopPropagation();
                  handleDownload(selectedWallpaper.url, selectedWallpaper.name);
                }}
                className={`w-full flex items-center justify-center gap-3 px-6 py-4 rounded-xl font-bold text-lg shadow-[0_0_20px_-5px_rgb(var(--color-primary))] transition-all active:scale-95
                  ${downloading ? 'bg-primary/50 cursor-wait' : 'bg-primary hover:bg-primary/90 text-background'}
                `}
              >
                {downloading ? (
                   <>
                     <Loader2 size={24} className="animate-spin" />
                     <span>{t.downloading}</span>
                   </>
                ) : (
                   <>
                     <Download size={24} />
                     <span>{t.download_save}</span>
                   </>
                )}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
