import { CapacitorConfig } from '@capacitor/cli';

const config: CapacitorConfig = {
  appId: 'com.hackeros.app',
  appName: 'HackerOS App',
  webDir: 'dist',
  server: {
    androidScheme: 'https'
  },
  // Removed SplashScreen plugin configuration as requested
  plugins: {},
};

export default config;
