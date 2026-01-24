import React, { useState, useEffect } from 'react';
import { Navbar } from './components/Navbar';
import { VersionCard } from './components/VersionCard';
import { WallpaperGrid } from './components/WallpaperGrid';
import { Settings } from './components/Settings';
import { AppSection, ReleaseInfo } from './types';
import { RELEASE_INFO_URL } from './constants';
import { parseHackerReleaseFile } from './utils/parser';
import { Loader2, WifiOff } from 'lucide-react';

export default function App() {
  const [currentSection, setCurrentSection] = useState<AppSection>(AppSection.RELEASES);
  const [releases, setReleases] = useState<ReleaseInfo[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  
  // Theme State
  const [darkMode, setDarkMode] = useState<boolean>(() => {
    // Check local storage or system preference
    if (typeof window !== 'undefined') {
        const saved = localStorage.getItem('theme');
        if (saved) return saved === 'dark';
        return window.matchMedia('(prefers-color-scheme: dark)').matches;
    }
    return true; // Default to dark for HackerOS vibe
  });

  // Apply Theme
  useEffect(() => {
    if (darkMode) {
      document.documentElement.classList.add('dark');
      localStorage.setItem('theme', 'dark');
    } else {
      document.documentElement.classList.remove('dark');
      localStorage.setItem('theme', 'light');
    }
  }, [darkMode]);

  const toggleTheme = () => setDarkMode(!darkMode);

  // Fetch Release Data
  useEffect(() => {
    const fetchData = async () => {
      try {
        setLoading(true);
        const response = await fetch(RELEASE_INFO_URL);
        
        if (!response.ok) {
          throw new Error('Failed to connect to update server.');
        }

        const text = await response.text();
        const parsedData = parseHackerReleaseFile(text);
        setReleases(parsedData);
      } catch (err) {
        console.error(err);
        setError('Could not load release info. Please check your internet connection.');
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
          <div className="px-4 pb-20 pt-4 max-w-md mx-auto">
            <header className="mb-6 flex items-center justify-between">
              <h1 className="text-2xl font-bold dark:text-white flex items-center gap-2">
                HackerOS <span className="text-primary text-sm font-mono border border-primary px-2 rounded-md">RELEASE_LOG</span>
              </h1>
            </header>

            {loading ? (
              <div className="flex flex-col items-center justify-center h-64 text-gray-400">
                <Loader2 className="animate-spin mb-4" size={32} />
                <p>Establishing secure connection...</p>
              </div>
            ) : error ? (
              <div className="flex flex-col items-center justify-center h-64 text-red-500 text-center">
                <WifiOff size={48} className="mb-4" />
                <p>{error}</p>
                <button 
                  onClick={() => window.location.reload()} 
                  className="mt-4 px-4 py-2 bg-red-100 dark:bg-red-900/30 rounded-lg text-sm font-medium"
                >
                  Retry
                </button>
              </div>
            ) : (
              <div className="space-y-4">
                {releases.map((release, index) => (
                  <VersionCard 
                    key={index} 
                    release={release} 
                    isLatest={index === 0}
                  />
                ))}
              </div>
            )}
          </div>
        );
      case AppSection.WALLPAPERS:
        return <WallpaperGrid />;
      case AppSection.SETTINGS:
        return <Settings darkMode={darkMode} toggleTheme={toggleTheme} />;
      default:
        return null;
    }
  };

  return (
    <div className="min-h-screen bg-gray-50 dark:bg-darkbg text-slate-900 dark:text-slate-100">
      {renderContent()}
      <Navbar currentSection={currentSection} onSectionChange={setCurrentSection} />
    </div>
  );
}
