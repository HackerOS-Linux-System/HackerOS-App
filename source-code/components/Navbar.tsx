import React from 'react';
import { LayoutList, Image, Settings } from 'lucide-react';
import { AppSection } from '../types';

interface NavbarProps {
  currentSection: AppSection;
  onSectionChange: (section: AppSection) => void;
}

export const Navbar: React.FC<NavbarProps> = ({ currentSection, onSectionChange }) => {
  const getButtonClass = (section: AppSection) => {
    const base = "flex flex-col items-center justify-center w-full h-full transition-colors duration-200";
    const active = "text-primary dark:text-primary-light";
    const inactive = "text-gray-500 hover:text-gray-900 dark:text-gray-400 dark:hover:text-gray-200";
    return `${base} ${currentSection === section ? active : inactive}`;
  };

  return (
    <nav className="fixed bottom-0 left-0 w-full h-16 bg-white dark:bg-cardbg border-t border-gray-200 dark:border-gray-700 shadow-lg z-50">
      <div className="flex justify-around items-center h-full max-w-md mx-auto">
        <button 
          onClick={() => onSectionChange(AppSection.RELEASES)}
          className={getButtonClass(AppSection.RELEASES)}
        >
          <LayoutList size={24} />
          <span className="text-xs mt-1 font-medium">Releases</span>
        </button>
        
        <button 
          onClick={() => onSectionChange(AppSection.WALLPAPERS)}
          className={getButtonClass(AppSection.WALLPAPERS)}
        >
          <Image size={24} />
          <span className="text-xs mt-1 font-medium">Wallpapers</span>
        </button>
        
        <button 
          onClick={() => onSectionChange(AppSection.SETTINGS)}
          className={getButtonClass(AppSection.SETTINGS)}
        >
          <Settings size={24} />
          <span className="text-xs mt-1 font-medium">Settings</span>
        </button>
      </div>
    </nav>
  );
};
