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
    }
};

export const applyTheme = (themeId: ThemeId) => {
    const theme = THEMES[themeId];
    const root = document.documentElement;

    root.style.setProperty('--color-primary', theme.colors.primary);
    root.style.setProperty('--color-background', theme.colors.background);
    root.style.setProperty('--color-card', theme.colors.card);
};
