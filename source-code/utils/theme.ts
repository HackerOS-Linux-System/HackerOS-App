import { Theme, ThemeId } from '../types';

export const THEMES: Record<ThemeId, Theme> = {
  hacker: {
    id: 'hacker',
    name: 'HackerOS Original',
    colors: {
      primary: '16 185 129',   // #10b981 (Emerald)
      background: '10 10 12',  // Very dark
      card: '23 23 28'         // Dark grey
    }
  },
  cyberpunk: {
    id: 'cyberpunk',
    name: 'Night City',
    colors: {
      primary: '236 72 153',   // #ec4899 (Pink-500)
      background: '28 8 20',   // Deep purple/black
      card: '46 16 34'         // Dark pinkish grey
    }
  },
  ocean: {
    id: 'ocean',
    name: 'Deep Sea',
    colors: {
      primary: '6 182 212',    // #06b6d4 (Cyan-500)
      background: '8 20 32',   // Deep blue
      card: '15 35 55'         // Lighter blue
    }
  },
  sunset: {
    id: 'sunset',
    name: 'Solar Flare',
    colors: {
      primary: '249 115 22',   // #f97316 (Orange-500)
      background: '28 10 5',   // Dark brown/red
      card: '50 20 10'         // Lighter brown
    }
  },
  matrix: {
    id: 'matrix',
    name: 'The Construct',
    colors: {
      primary: '34 197 94',    // #22c55e (Green-500)
      background: '0 0 0',     // Pure black
      card: '20 20 20'         // Dark grey
    }
  },
  crimson: {
    id: 'crimson',
    name: 'Red Alert',
    colors: {
      primary: '220 38 38',    // #dc2626 (Red-600)
      background: '20 5 5',    // Very dark red
      card: '40 10 10'         // Dark red
    }
  },
  royal: {
    id: 'royal',
    name: 'Luxury Gold',
    colors: {
      primary: '234 179 8',    // #eab308 (Yellow-500)
      background: '15 15 15',  // Dark grey
      card: '30 30 30'         // Lighter grey
    }
  }
};

export const applyTheme = (themeId: ThemeId) => {
  const theme = THEMES[themeId];
  const root = document.documentElement;
  
  root.style.setProperty('--color-primary', theme.colors.primary);
  root.style.setProperty('--color-background', theme.colors.background);
  root.style.setProperty('--color-card', theme.colors.card);
};
