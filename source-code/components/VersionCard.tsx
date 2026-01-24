import React from 'react';
import { Terminal, Calendar, Activity } from 'lucide-react';
import { ReleaseInfo } from '../types';

interface VersionCardProps {
  release: ReleaseInfo;
  isLatest: boolean;
}

export const VersionCard: React.FC<VersionCardProps> = ({ release, isLatest }) => {
  return (
    <div className={`relative p-6 mb-4 rounded-2xl shadow-sm border transition-all duration-300 hover:shadow-md 
      ${isLatest 
        ? 'bg-white dark:bg-cardbg border-primary/50 ring-1 ring-primary/20' 
        : 'bg-white dark:bg-cardbg border-gray-100 dark:border-gray-700'
      }`}
    >
      {isLatest && (
        <span className="absolute -top-3 right-4 bg-primary text-white text-xs font-bold px-3 py-1 rounded-full shadow-sm">
          LATEST
        </span>
      )}

      <div className="flex items-center gap-2 mb-3">
        <div className="p-2 rounded-lg bg-gray-100 dark:bg-gray-700 text-gray-700 dark:text-gray-300">
          <Terminal size={20} />
        </div>
        <h3 className="text-xl font-bold text-gray-900 dark:text-white">
          {release.version}
        </h3>
      </div>

      <div className="flex items-start gap-2 mb-4 text-sm text-gray-600 dark:text-gray-400 bg-gray-50 dark:bg-gray-800/50 p-3 rounded-lg">
        <Calendar size={16} className="mt-0.5 shrink-0" />
        <p className="font-mono text-xs leading-relaxed">{release.editions}</p>
      </div>

      <div className="flex items-start gap-2">
        <Activity size={16} className="mt-1 text-primary shrink-0" />
        <div>
          <h4 className="text-xs font-semibold uppercase tracking-wider text-gray-500 dark:text-gray-500 mb-1">
            Changelog
          </h4>
          <p className="text-sm text-gray-700 dark:text-gray-300 leading-relaxed whitespace-pre-line">
            {release.news || "No changelog details provided."}
          </p>
        </div>
      </div>
    </div>
  );
};
