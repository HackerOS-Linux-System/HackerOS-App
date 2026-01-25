export interface ReleaseInfo {
  version: string;
  editions: string;
  news: string;
}

export enum AppSection {
  RELEASES = 'releases',
  WALLPAPERS = 'wallpapers',
  SETTINGS = 'settings'
}

export interface Wallpaper {
  id: number;
  url: string;
  thumbnail: string;
  name: string;
}

export type ThemeId = 'hacker' | 'cyberpunk' | 'ocean' | 'sunset';

export interface Theme {
  id: ThemeId;
  name: string;
  colors: {
    primary: string; // RGB values like "16 185 129"
    background: string;
    card: string;
  };
}
