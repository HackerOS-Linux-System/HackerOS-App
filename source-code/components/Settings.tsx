import React, { useState, useEffect } from 'react';
import { Smartphone, Github, ShieldCheck, Bell, ChevronRight, Palette, Layers, Info, ExternalLink, Globe, MessageSquare, Youtube, Languages } from 'lucide-react';
import { ThemeId, Theme, Language } from '../types';
import { THEMES } from '../utils/theme';
import { TRANSLATIONS } from '../utils/translations';

interface SettingsProps {
  currentTheme: ThemeId;
  setTheme: (id: ThemeId) => void;
  language: Language;
  setLanguage: (lang: Language) => void;
}

const SocialLink = ({ href, label, icon: Icon, subLabel }: { href: string, label: string, icon: any, subLabel?: string }) => (
  <a 
    href={href}
    target="_blank"
    rel="noreferrer"
    className="p-4 flex items-center justify-between hover:bg-white/5 transition-colors"
  >
    <div className="flex items-center gap-3">
      <div className="p-2 rounded-lg bg-background text-muted group-hover:text-primary transition-colors">
        <Icon size={18} />
      </div>
      <div>
        <p className="text-sm font-medium text-text">{label}</p>
        {subLabel && <p className="text-xs text-muted">{subLabel}</p>}
      </div>
    </div>
    <ExternalLink size={14} className="text-muted/50" />
  </a>
);

export const Settings: React.FC<SettingsProps> = ({ currentTheme, setTheme, language, setLanguage }) => {
  const [notificationsEnabled, setNotificationsEnabled] = useState(false);
  const t = TRANSLATIONS[language];

  useEffect(() => {
    const saved = localStorage.getItem('hackeros_notifications');
    setNotificationsEnabled(saved === 'true');
  }, []);

  const toggleNotifications = () => {
    const newState = !notificationsEnabled;
    setNotificationsEnabled(newState);
    localStorage.setItem('hackeros_notifications', String(newState));
  };

  const languages: { code: Language; label: string; flag: string }[] = [
    { code: 'pl', label: 'Polski', flag: '🇵🇱' },
    { code: 'en', label: 'English', flag: '🇺🇸' },
    { code: 'de', label: 'Deutsch', flag: '🇩🇪' },
    { code: 'es', label: 'Español', flag: '🇪🇸' },
    { code: 'fr', label: 'Français', flag: '🇫🇷' },
  ];

  return (
    <div className="space-y-6 pb-24">
      <div className="px-6 pt-2">
        <h2 className="text-3xl font-mono font-bold text-white mb-1">{t.header_config}</h2>
        <p className="text-muted text-sm">{t.sub_config}</p>
      </div>

       {/* Language Section */}
       <section className="px-4">
        <div className="bg-card/50 backdrop-blur-md border border-white/5 rounded-2xl p-5 shadow-xl">
          <div className="flex items-center gap-3 mb-4">
            <Languages className="text-primary" size={20} />
            <h3 className="font-bold text-sm uppercase tracking-widest text-muted">{t.settings_lang}</h3>
          </div>
          
          <div className="grid grid-cols-2 gap-2">
            {languages.map((lang) => (
              <button
                key={lang.code}
                onClick={() => setLanguage(lang.code)}
                className={`
                  flex items-center gap-3 px-4 py-3 rounded-xl border transition-all duration-200
                  ${language === lang.code 
                    ? 'border-primary bg-primary/10 text-white' 
                    : 'border-white/5 bg-background/50 text-muted hover:bg-white/5'}
                `}
              >
                <span className="text-xl">{lang.flag}</span>
                <span className="text-sm font-bold">{lang.label}</span>
              </button>
            ))}
          </div>
        </div>
      </section>

      {/* Theme Section */}
      <section className="px-4">
        <div className="bg-card/50 backdrop-blur-md border border-white/5 rounded-2xl p-5 shadow-xl">
          <div className="flex items-center gap-3 mb-4">
            <Palette className="text-primary" size={20} />
            <h3 className="font-bold text-sm uppercase tracking-widest text-muted">{t.settings_theme}</h3>
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

      {/* Links / Socials */}
      <section className="px-4">
        <div className="bg-card/50 backdrop-blur-md border border-white/5 rounded-2xl overflow-hidden shadow-xl">
           <div className="p-4 border-b border-white/5 flex items-center gap-3">
             <Globe className="text-primary" size={20} />
             <h3 className="font-bold text-sm uppercase tracking-widest text-muted">{t.settings_social}</h3>
           </div>
           
           <div className="divide-y divide-white/5">
             <SocialLink 
               href="https://discord.com/invite/8yHNcBaEKy" 
               label="Discord Community" 
               icon={MessageSquare}
               subLabel="Join the server"
             />
             <SocialLink 
               href="https://x.com/hackeros_linux" 
               label="X / Twitter" 
               icon={ExternalLink} 
               subLabel="@hackeros_linux"
             />
             <SocialLink 
               href="https://linuxiarze.pl/distro-hackeros/" 
               label="Linuxiarze.pl" 
               icon={Globe} 
             />
             <SocialLink 
               href="https://distrowatch.com/table.php?distribution=hackeros" 
               label="DistroWatch" 
               icon={Globe} 
             />
             <SocialLink 
               href="https://www.reddit.com/r/HackerOS_/" 
               label="Reddit" 
               icon={ExternalLink} 
               subLabel="r/HackerOS_"
             />
             <SocialLink 
               href="https://www.youtube.com/channel/UCB_b48f2diMH2JByN2OmgGw" 
               label="YouTube" 
               icon={Youtube} 
               subLabel="Official Channel"
             />
           </div>
        </div>
      </section>

      {/* Preferences Section */}
      <section className="px-4">
        <div className="bg-card/50 backdrop-blur-md border border-white/5 rounded-2xl overflow-hidden shadow-xl">
           <div className="p-4 border-b border-white/5 flex items-center gap-3">
             <Layers className="text-primary" size={20} />
             <h3 className="font-bold text-sm uppercase tracking-widest text-muted">{t.settings_pref}</h3>
           </div>
           
           <div className="divide-y divide-white/5">
             <div 
                className="p-4 flex items-center justify-between group cursor-pointer hover:bg-white/5 transition-colors"
                onClick={toggleNotifications}
             >
               <div className="flex items-center gap-3">
                 <div className="p-2 rounded-lg bg-background text-muted">
                   <Bell size={18} />
                 </div>
                 <div>
                   <p className="text-sm font-medium text-text">{t.pref_notifications}</p>
                   <p className="text-xs text-muted">{t.pref_notifications_desc}</p>
                 </div>
               </div>
               <div className={`relative inline-flex h-6 w-11 items-center rounded-full transition-colors ${notificationsEnabled ? 'bg-primary' : 'bg-gray-700'}`}>
                  <span className={`inline-block h-4 w-4 transform rounded-full bg-white transition-transform ${notificationsEnabled ? 'translate-x-6' : 'translate-x-1'}`}/>
               </div>
             </div>

             <div className="p-4 flex items-center justify-between group cursor-pointer hover:bg-white/5 transition-colors">
               <div className="flex items-center gap-3">
                 <div className="p-2 rounded-lg bg-background text-muted">
                   <Smartphone size={18} />
                 </div>
                 <div>
                   <p className="text-sm font-medium text-text">{t.pref_cache}</p>
                   <p className="text-xs text-muted">0.5 MB used</p>
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
             <h3 className="font-bold text-sm uppercase tracking-widest text-muted">{t.settings_info}</h3>
           </div>
           
           <div className="divide-y divide-white/5">
             <a 
              href="https://github.com/HackerOS-Linux-System/HackerOS-App" 
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
                   <p className="text-xs text-muted">HackerOS-App</p>
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
                   <p className="text-sm font-medium text-text">App Version</p>
                   <p className="text-xs text-muted">v0.1-beta</p>
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
