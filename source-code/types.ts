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
