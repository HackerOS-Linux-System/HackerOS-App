import React from 'react';
import { Moon, Sun, Smartphone, Github, ShieldCheck } from 'lucide-react';

interface SettingsProps {
  darkMode: boolean;
  toggleTheme: () => void;
}

export const Settings: React.FC<SettingsProps> = ({ darkMode, toggleTheme }) => {
  return (
    <div className="px-4 pt-4 pb-20 max-w-md mx-auto">
      <h2 className="text-2xl font-bold mb-6 dark:text-white">Settings</h2>

      {/* Appearance Section */}
      <div className="bg-white dark:bg-cardbg rounded-2xl p-4 shadow-sm border border-gray-100 dark:border-gray-700 mb-6">
        <h3 className="text-sm font-semibold text-gray-500 uppercase tracking-wider mb-4 ml-1">Appearance</h3>
        
        <div className="flex items-center justify-between py-2">
          <div className="flex items-center gap-3">
            <div className={`p-2 rounded-lg ${darkMode ? 'bg-indigo-900/50 text-indigo-300' : 'bg-orange-100 text-orange-500'}`}>
              {darkMode ? <Moon size={20} /> : <Sun size={20} />}
            </div>
            <div>
              <p className="font-medium text-gray-900 dark:text-white">Dark Mode</p>
              <p className="text-xs text-gray-500">Adjust the app appearance</p>
            </div>
          </div>
          
          <button 
            onClick={toggleTheme}
            className={`relative inline-flex h-6 w-11 items-center rounded-full transition-colors focus:outline-none focus:ring-2 focus:ring-primary focus:ring-offset-2 ${
              darkMode ? 'bg-primary' : 'bg-gray-200'
            }`}
          >
            <span
              className={`inline-block h-4 w-4 transform rounded-full bg-white transition-transform ${
                darkMode ? 'translate-x-6' : 'translate-x-1'
              }`}
            />
          </button>
        </div>
      </div>

      {/* About Section */}
      <div className="bg-white dark:bg-cardbg rounded-2xl p-4 shadow-sm border border-gray-100 dark:border-gray-700">
        <h3 className="text-sm font-semibold text-gray-500 uppercase tracking-wider mb-4 ml-1">About</h3>
        
        <div className="space-y-4">
          <div className="flex items-center gap-3">
            <div className="p-2 rounded-lg bg-gray-100 dark:bg-gray-700 text-gray-700 dark:text-gray-300">
              <ShieldCheck size={20} />
            </div>
            <div>
              <p className="font-medium text-gray-900 dark:text-white">HackerOS App</p>
              <p className="text-xs text-gray-500">Version 1.0.0 (Beta)</p>
            </div>
          </div>

           <a 
            href="https://github.com/HackerOS-Linux-System" 
            target="_blank" 
            rel="noreferrer"
            className="flex items-center gap-3 group cursor-pointer"
          >
            <div className="p-2 rounded-lg bg-gray-100 dark:bg-gray-700 text-gray-700 dark:text-gray-300 group-hover:text-primary dark:group-hover:text-primary transition-colors">
              <Github size={20} />
            </div>
            <div>
              <p className="font-medium text-gray-900 dark:text-white group-hover:text-primary transition-colors">GitHub Repository</p>
              <p className="text-xs text-gray-500">View source code</p>
            </div>
          </a>

          <div className="flex items-center gap-3">
            <div className="p-2 rounded-lg bg-gray-100 dark:bg-gray-700 text-gray-700 dark:text-gray-300">
              <Smartphone size={20} />
            </div>
            <div>
              <p className="font-medium text-gray-900 dark:text-white">Build</p>
              <p className="text-xs text-gray-500">React + Capacitor</p>
            </div>
          </div>
        </div>
      </div>

      <div className="mt-8 text-center">
        <p className="text-xs text-gray-400">Made with &lt;3 for HackerOS Community</p>
      </div>
    </div>
  );
};
