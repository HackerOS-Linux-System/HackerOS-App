import React from 'react';
import { LayoutList, Image, Settings, Aperture, Users } from 'lucide-react';
import { AppSection, Language } from '../types';
import { TRANSLATIONS } from '../utils/translations';

interface NavbarProps {
  currentSection: AppSection;
  onSectionChange: (section: AppSection) => void;
  language: Language;
}

export const Navbar: React.FC<NavbarProps> = ({ currentSection, onSectionChange, language }) => {
  const t = TRANSLATIONS[language];

  const navItems = [
    { id: AppSection.RELEASES, icon: LayoutList, label: t.nav_releases },
    { id: AppSection.WALLPAPERS, icon: Image, label: t.nav_wallpapers },
    { id: AppSection.GALLERY, icon: Aperture, label: t.nav_gallery },
    { id: AppSection.TEAM, icon: Users, label: t.nav_team },
    { id: AppSection.SETTINGS, icon: Settings, label: t.nav_config },
  ];

  return (
    <nav className="fixed bottom-0 left-0 w-full z-50">
      {/* Glass gradient background */}
      <div className="absolute inset-0 bg-background/80 backdrop-blur-lg border-t border-white/5" />
      
      <div className="relative flex justify-around items-center h-20 max-w-lg mx-auto pb-4 safe-area-bottom px-2">
        {navItems.map((item) => {
          const isActive = currentSection === item.id;
          const Icon = item.icon;
          
          return (
            <button
              key={item.id}
              onClick={() => onSectionChange(item.id)}
              className="relative flex flex-col items-center justify-center w-full h-full group"
            >
              <div className={`
                p-1.5 rounded-xl transition-all duration-300 ease-out mb-1
                ${isActive ? 'bg-primary/20 text-primary translate-y-[-2px]' : 'text-muted group-hover:text-text'}
              `}>
                <Icon size={20} strokeWidth={isActive ? 2.5 : 2} />
              </div>
              
              <span className={`
                text-[9px] font-bold tracking-wider transition-colors duration-300 uppercase
                ${isActive ? 'text-primary' : 'text-muted/60'}
              `}>
                {item.label}
              </span>

              {/* Active Indicator Dot */}
              {isActive && (
                <span className="absolute bottom-2 w-1 h-1 bg-primary rounded-full shadow-[0_0_8px_rgb(var(--color-primary))]" />
              )}
            </button>
          );
        })}
      </div>
    </nav>
  );
};
