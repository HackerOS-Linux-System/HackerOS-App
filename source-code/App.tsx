import React, { useState, useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { Navbar } from './components/Navbar';
import { VersionCard } from './components/VersionCard';
import { WallpaperGrid } from './components/WallpaperGrid';
import { Settings } from './components/Settings';
import { Gallery } from './components/Gallery';
import { Team } from './components/Team';
import { AppSection, ReleaseInfo, ThemeId, Language } from './types';
import { RELEASE_INFO_URL } from './constants';
import { parseHackerReleaseFile } from './utils/parser';
import { Loader2, WifiOff, Terminal } from 'lucide-react';
import { applyTheme } from './utils/theme';
import { TRANSLATIONS } from './utils/translations';

export default function App() {
  const [currentSection, setCurrentSection] = useState<AppSection>(AppSection.RELEASES);
  const [releases, setReleases] = useState<ReleaseInfo[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [currentTheme, setCurrentTheme] = useState<ThemeId>('hacker');
  const [currentLanguage, setCurrentLanguage] = useState<Language>('pl');

  // Load saved settings on boot
  useEffect(() => {
    // Theme
    const savedTheme = localStorage.getItem('hackeros_theme') as ThemeId;
    if (savedTheme) {
      setCurrentTheme(savedTheme);
      applyTheme(savedTheme);
    } else {
      applyTheme('hacker');
    }

    // Language
    const savedLang = localStorage.getItem('hackeros_lang') as Language;
    if (savedLang) {
      setCurrentLanguage(savedLang);
    }
  }, []);

  const handleThemeChange = (id: ThemeId) => {
    setCurrentTheme(id);
    applyTheme(id);
    localStorage.setItem('hackeros_theme', id);
  };

  const handleLanguageChange = (lang: Language) => {
    setCurrentLanguage(lang);
    localStorage.setItem('hackeros_lang', lang);
  };

  const t = TRANSLATIONS[currentLanguage];

  useEffect(() => {
    const fetchData = async () => {
      try {
        setLoading(true);
        // Add cache busting
        const response = await fetch(`${RELEASE_INFO_URL}?t=${Date.now()}`);
        
        if (!response.ok) throw new Error('Network error');

        const text = await response.text();
        const parsedData = parseHackerReleaseFile(text);
        setReleases(parsedData);
      } catch (err) {
        console.error(err);
        setError('Connection failed. Check your uplink.');
      } finally {
        setLoading(false);
      }
    };

    fetchData();
  }, []);

  const renderContent = () => {
    switch (currentSection) {
      case AppSection.RELEASES:
        return (
          <div className="px-4 pb-24 pt-2 max-w-lg mx-auto">
            <header className="px-2 mb-6 flex flex-col justify-center">
              <h1 className="text-3xl font-mono font-bold text-white flex items-center gap-3">
                HackerOS 
                <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-primary text-background border border-primary/50">
                  {t.sub_releases}
                </span>
              </h1>
            </header>

            {loading ? (
              <div className="flex flex-col items-center justify-center h-[50vh] text-muted space-y-4">
                <div className="relative">
                   <div className="absolute inset-0 bg-primary/20 blur-xl rounded-full"></div>
                   <Loader2 className="animate-spin relative z-10 text-primary" size={48} />
                </div>
                <p className="font-mono text-xs animate-pulse">{t.decrypting}</p>
              </div>
            ) : error ? (
              <div className="flex flex-col items-center justify-center h-[50vh] text-red-400 text-center px-6">
                <div className="bg-red-500/10 p-4 rounded-full mb-4 ring-1 ring-red-500/20">
                  <WifiOff size={40} />
                </div>
                <p className="font-bold mb-2">{t.error_signal}</p>
                <p className="text-xs text-muted mb-6">{t.error_network}</p>
                <button 
                  onClick={() => window.location.reload()} 
                  className="px-6 py-2 bg-red-500 hover:bg-red-600 text-white rounded-lg text-sm font-bold shadow-lg transition-colors"
                >
                  {t.retry}
                </button>
              </div>
            ) : (
              <div className="space-y-4">
                {releases.map((release, index) => (
                  <motion.div
                    key={index}
                    initial={{ opacity: 0, y: 20 }}
                    animate={{ opacity: 1, y: 0 }}
                    transition={{ delay: index * 0.1 }}
                  >
                    <VersionCard 
                      release={release} 
                      isLatest={index === 0}
                      language={currentLanguage}
                    />
                  </motion.div>
                ))}
                
                <div className="text-center py-8 opacity-50">
                  <Terminal size={24} className="mx-auto mb-2 text-muted" />
                  <p className="text-[10px] font-mono text-muted uppercase">{t.end_of_log}</p>
                </div>
              </div>
            )}
          </div>
        );
      case AppSection.WALLPAPERS:
        return <WallpaperGrid language={currentLanguage} />;
      case AppSection.GALLERY:
        return <Gallery language={currentLanguage} />;
      case AppSection.TEAM:
        return <Team language={currentLanguage} />;
      case AppSection.SETTINGS:
        return <Settings 
          currentTheme={currentTheme} 
          setTheme={handleThemeChange} 
          language={currentLanguage}
          setLanguage={handleLanguageChange}
        />;
      default:
        return null;
    }
  };

  return (
    <div className="min-h-screen bg-background text-text selection:bg-primary/30 safe-area-top">
      {/* Background ambient glow */}
      <div className="fixed top-0 left-0 w-full h-96 bg-primary/5 blur-[100px] pointer-events-none" />
      
      <AnimatePresence mode="wait">
        <motion.main
          key={currentSection}
          initial={{ opacity: 0, x: 10 }}
          animate={{ opacity: 1, x: 0 }}
          exit={{ opacity: 0, x: -10 }}
          transition={{ duration: 0.2 }}
          className="relative z-10"
        >
          {renderContent()}
        </motion.main>
      </AnimatePresence>
      
      <Navbar 
        currentSection={currentSection} 
        onSectionChange={setCurrentSection} 
        language={currentLanguage}
      />
    </div>
  );
}
