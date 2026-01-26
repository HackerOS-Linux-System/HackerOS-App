import React, { useState, useEffect } from 'react';
import { Aperture, Loader2, WifiOff, X, Maximize2, Download } from 'lucide-react';
import { Language } from '../types';
import { TRANSLATIONS } from '../utils/translations';
import { GALLERY_API_URL } from '../constants';
import { Filesystem, Directory } from '@capacitor/filesystem';
import { Toast } from '@capacitor/toast';

interface GalleryProps {
  language: Language;
}

interface GalleryImage {
  name: string;
  path: string;
  sha: string;
  size: number;
  url: string;
  html_url: string;
  git_url: string;
  download_url: string;
  type: string;
}

export const Gallery: React.FC<GalleryProps> = ({ language }) => {
  const t = TRANSLATIONS[language];
  const [images, setImages] = useState<GalleryImage[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(false);
  const [selectedImage, setSelectedImage] = useState<GalleryImage | null>(null);
  const [downloading, setDownloading] = useState(false);

  useEffect(() => {
    const fetchImages = async () => {
      try {
        setLoading(true);
        // This hits https://api.github.com/repos/HackerOS-Linux-System/HackerOS-App/contents/gallery
        // which corresponds to /tree/main/gallery in the UI
        const response = await fetch(GALLERY_API_URL);
        
        if (!response.ok) throw new Error("Failed to fetch gallery");

        const data = await response.json();
        
        // Filter only images
        if (Array.isArray(data)) {
          const imageFiles = data.filter((item: any) => 
            item.type === 'file' && 
            /\.(jpg|jpeg|png|gif|webp)$/i.test(item.name)
          );
          setImages(imageFiles);
        } else {
            setImages([]);
        }
      } catch (err) {
        console.error(err);
        setError(true);
      } finally {
        setLoading(false);
      }
    };

    fetchImages();
  }, []);

  const handleDownload = async (url: string, name: string) => {
    setDownloading(true);
    try {
      const response = await fetch(url);
      const blob = await response.blob();
      const reader = new FileReader();
      
      reader.readAsDataURL(blob);
      reader.onloadend = async () => {
        const base64data = reader.result as string;
        try {
          await Filesystem.writeFile({
            path: `HackerOS_Gallery_${name}`,
            data: base64data,
            directory: Directory.Documents
          });
          await Toast.show({ text: 'Saved to Documents', duration: 'short' });
        } catch (e) {
            // Fallback for browser
            const link = document.createElement('a');
            link.href = base64data;
            link.download = name;
            document.body.appendChild(link);
            link.click();
            document.body.removeChild(link);
        }
        setDownloading(false);
      };
    } catch (e) {
      setDownloading(false);
      await Toast.show({ text: 'Download failed', duration: 'short' });
    }
  };

  return (
    <div className="pb-24 pt-2 h-screen flex flex-col">
      <div className="px-6 mb-6">
        <h2 className="text-3xl font-mono font-bold text-white mb-1">{t.header_gallery}</h2>
        <p className="text-muted text-sm">{t.sub_gallery}</p>
      </div>

      <div className="flex-1 px-4 overflow-y-auto">
        {loading ? (
           <div className="flex flex-col items-center justify-center h-64 space-y-4">
             <div className="relative">
                <div className="absolute inset-0 bg-primary/20 blur-xl rounded-full"></div>
                <Loader2 className="animate-spin relative z-10 text-primary" size={48} />
             </div>
             <p className="font-mono text-xs animate-pulse text-muted">{t.gallery_loading}</p>
           </div>
        ) : error ? (
           <div className="flex flex-col items-center justify-center h-64 text-red-400 text-center px-6">
             <div className="bg-red-500/10 p-4 rounded-full mb-4 ring-1 ring-red-500/20">
               <WifiOff size={32} />
             </div>
             <p className="font-bold mb-2">{t.error_signal}</p>
             <button 
               onClick={() => window.location.reload()} 
               className="mt-4 px-6 py-2 bg-red-500 hover:bg-red-600 text-white rounded-lg text-sm font-bold"
             >
               {t.retry}
             </button>
           </div>
        ) : images.length === 0 ? (
            <div className="flex flex-col items-center justify-center h-64 text-muted">
                <Aperture size={48} className="opacity-20 mb-4" />
                <p>{t.gallery_empty}</p>
            </div>
        ) : (
          <div className="grid grid-cols-2 md:grid-cols-3 gap-3 pb-20">
             {images.map((img) => (
                <button
                  key={img.sha}
                  onClick={() => setSelectedImage(img)}
                  className="group relative aspect-square rounded-xl overflow-hidden bg-card/40 border border-white/5 hover:border-primary/50 transition-all duration-300"
                >
                  <img 
                    src={img.download_url} 
                    alt={img.name}
                    className="w-full h-full object-cover transition-transform duration-500 group-hover:scale-110 opacity-80 group-hover:opacity-100"
                    loading="lazy"
                  />
                  <div className="absolute inset-0 bg-gradient-to-t from-black/60 to-transparent opacity-0 group-hover:opacity-100 transition-opacity flex items-end p-3">
                     <span className="text-[10px] font-mono text-white truncate w-full text-left">
                        {img.name}
                     </span>
                  </div>
                </button>
             ))}
          </div>
        )}
      </div>

       {/* Fullscreen Modal */}
       {selectedImage && (
        <div className="fixed inset-0 z-[100] flex items-center justify-center bg-black/95 backdrop-blur-xl p-0 animate-in fade-in duration-200">
          <button 
            onClick={() => setSelectedImage(null)}
            className="absolute top-8 right-6 p-3 text-white/70 hover:text-white rounded-full bg-white/10 z-10 backdrop-blur-md"
          >
            <X size={24} />
          </button>

          <div className="relative w-full h-full flex flex-col items-center justify-center">
            <div className="relative w-full max-w-[90vw] max-h-[70vh] rounded-lg overflow-hidden shadow-[0_0_50px_-12px_rgba(var(--color-primary),0.3)] border border-white/10">
              <img 
                src={selectedImage.download_url} 
                alt={selectedImage.name} 
                className="w-full h-full object-contain bg-black"
              />
            </div>

            <div className="absolute bottom-12 w-full px-8">
               <button
                disabled={downloading}
                onClick={(e) => {
                  e.stopPropagation();
                  handleDownload(selectedImage.download_url, selectedImage.name);
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
