import React from 'react';
import { Smartphone, Github, ShieldCheck, Bell, ChevronRight, Palette, Layers, Info } from 'lucide-react';
import { ThemeId, Theme } from '../types';
import { THEMES } from '../utils/theme';

interface SettingsProps {
  currentTheme: ThemeId;
  setTheme: (id: ThemeId) => void;
}

export const Settings: React.FC<SettingsProps> = ({ currentTheme, setTheme }) => {
  return (
    <div className="space-y-6 pb-20">
    <div className="px-6 pt-2">
    <h2 className="text-3xl font-mono font-bold text-white mb-1">SYSTEM_CONFIG</h2>
    <p className="text-muted text-sm">Personalize your experience</p>
    </div>

    {/* Theme Section */}
    <section className="px-4">
    <div className="bg-card/50 backdrop-blur-md border border-white/5 rounded-2xl p-5 shadow-xl">
    <div className="flex items-center gap-3 mb-4">
    <Palette className="text-primary" size={20} />
    <h3 className="font-bold text-sm uppercase tracking-widest text-muted">Theme Engine</h3>
    </div>

    <div className="grid grid-cols-2 gap-3">
    {Object.values(THEMES).map((theme: Theme) => (
      <button
      key={theme.id}
      onClick={() => setTheme(theme.id)}
      className={`
        relative overflow-hidden rounded-xl p-3 border text-left transition-all duration-200
        ${currentTheme === theme.id
          ? 'border-primary bg-primary/10 shadow-[0_0_15px_-5px_rgb(var(--color-primary))]'
    : 'border-white/5 bg-background/50 hover:bg-white/5'}
    `}
    >
    <div className="flex items-center justify-between mb-2">
    <div
    className="w-8 h-8 rounded-lg shadow-lg"
    style={{ backgroundColor: `rgb(${theme.colors.primary})` }}
    />
    {currentTheme === theme.id && (
      <div className="w-2 h-2 rounded-full bg-primary animate-pulse" />
    )}
    </div>
    <p className={`text-xs font-bold ${currentTheme === theme.id ? 'text-white' : 'text-muted'}`}>
    {theme.name}
    </p>
    </button>
    ))}
    </div>
    </div>
    </section>

    {/* Preferences Section */}
    <section className="px-4">
    <div className="bg-card/50 backdrop-blur-md border border-white/5 rounded-2xl overflow-hidden shadow-xl">
    <div className="p-4 border-b border-white/5 flex items-center gap-3">
    <Layers className="text-primary" size={20} />
    <h3 className="font-bold text-sm uppercase tracking-widest text-muted">Preferences</h3>
    </div>

    <div className="divide-y divide-white/5">
    <div className="p-4 flex items-center justify-between group cursor-pointer hover:bg-white/5 transition-colors">
    <div className="flex items-center gap-3">
    <div className="p-2 rounded-lg bg-background text-muted">
    <Bell size={18} />
    </div>
    <div>
    <p className="text-sm font-medium text-text">Release Notifications</p>
    <p className="text-xs text-muted">Get alerts for new HackerOS versions</p>
    </div>
    </div>
    <div className="relative inline-flex h-6 w-11 items-center rounded-full bg-primary">
    <span className="inline-block h-4 w-4 transform translate-x-6 rounded-full bg-white transition-transform"/>
    </div>
    </div>

    <div className="p-4 flex items-center justify-between group cursor-pointer hover:bg-white/5 transition-colors">
    <div className="flex items-center gap-3">
    <div className="p-2 rounded-lg bg-background text-muted">
    <Smartphone size={18} />
    </div>
    <div>
    <p className="text-sm font-medium text-text">Cache Management</p>
    <p className="text-xs text-muted">0.4 MB used</p>
    </div>
    </div>
    <ChevronRight size={16} className="text-muted" />
    </div>
    </div>
    </div>
    </section>

    {/* About Section */}
    <section className="px-4">
    <div className="bg-card/50 backdrop-blur-md border border-white/5 rounded-2xl overflow-hidden shadow-xl">
    <div className="p-4 border-b border-white/5 flex items-center gap-3">
    <Info className="text-primary" size={20} />
    <h3 className="font-bold text-sm uppercase tracking-widest text-muted">Information</h3>
    </div>

    <div className="divide-y divide-white/5">
    <a
    href="https://github.com/HackerOS-Linux-System"
    target="_blank"
    rel="noreferrer"
    className="p-4 flex items-center justify-between hover:bg-white/5 transition-colors"
    >
    <div className="flex items-center gap-3">
    <div className="p-2 rounded-lg bg-background text-muted">
    <Github size={18} />
    </div>
    <div>
    <p className="text-sm font-medium text-text">Source Code</p>
    <p className="text-xs text-muted">github.com/HackerOS-Linux-System</p>
    </div>
    </div>
    <ChevronRight size={16} className="text-muted" />
    </a>

    <div className="p-4 flex items-center justify-between">
    <div className="flex items-center gap-3">
    <div className="p-2 rounded-lg bg-background text-muted">
    <ShieldCheck size={18} />
    </div>
    <div>
    <p className="text-sm font-medium text-text">Build Version</p>
    <p className="text-xs text-muted">v1.0.2-stable (Android)</p>
    </div>
    </div>
    </div>
    </div>
    </div>
    </section>

    <div className="text-center pt-4">
    <p className="text-[10px] font-mono text-muted/40 uppercase tracking-[0.2em]">
    Designed for Hackers
    </p>
    </div>
    </div>
  );
};
