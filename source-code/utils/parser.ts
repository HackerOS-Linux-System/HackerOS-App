import { ReleaseInfo } from '../types';

export const parseHackerReleaseFile = (text: string): ReleaseInfo[] => {
  // 1. Clean up outer brackets (start and end)
  const cleanText = text.replace(/^\s*\[/, '').replace(/\]\s*$/, '').trim();
  
  // 2. Split into lines and filter empty ones
  const lines = cleanText.split('\n').map(l => l.trim()).filter(l => l.length > 0);

  const releases: ReleaseInfo[] = [];
  
  // State variables
  let currentVersion: string | null = null;
  let currentSection: 'new' | 'release' | 'description' | null = null;
  
  // Buffers for current release data
  let tempNews: string[] = [];
  let tempEditions: string[] = [];
  let tempDescription: string[] = [];

  // Helper to push the collected data to releases array
  const saveCurrentRelease = () => {
    if (currentVersion) {
      releases.push({
        version: currentVersion,
        description: tempDescription.length > 0 ? tempDescription.join(' ') : '',
        news: tempNews.length > 0 ? tempNews.join('\n') : '',
        editions: tempEditions.length > 0 ? tempEditions.join('\n') : ''
      });
    }
  };

  for (const line of lines) {
    // Detect start of a new version (e.g., "V4.2", "V3.0")
    if (/^V\d/.test(line)) {
      // If we were already parsing a version, save it before starting a new one
      saveCurrentRelease();

      // Reset state for new version
      currentVersion = line;
      currentSection = null;
      tempNews = [];
      tempEditions = [];
      tempDescription = [];
      continue;
    }

    // Detect Section Headers (case insensitive, handle potential trailing colons or spaces)
    const lowerLine = line.toLowerCase();
    
    if (lowerLine.startsWith('new:')) {
      currentSection = 'new';
      continue;
    } 
    
    if (lowerLine.startsWith('release:')) {
      currentSection = 'release';
      continue;
    }

    if (lowerLine.startsWith('description:')) {
      currentSection = 'description';
      continue;
    }

    // Detect Content Items (starting with '=')
    if (line.startsWith('=')) {
      const content = line.substring(1).trim(); // Remove '=' and whitespace
      
      if (currentSection === 'new') {
        tempNews.push(content);
      } else if (currentSection === 'release') {
        tempEditions.push(content);
      } else if (currentSection === 'description') {
        tempDescription.push(content);
      }
    }
  }

  // Don't forget to save the very last release after the loop finishes
  saveCurrentRelease();

  return releases;
};
