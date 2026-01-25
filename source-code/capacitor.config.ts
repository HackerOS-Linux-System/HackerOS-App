import { CapacitorConfig } from '@capacitor/cli';

const config: CapacitorConfig = {
    appId: 'com.hackeros.app',
    appName: 'HackerOS App',
    webDir: 'dist',
    server: {
        androidScheme: 'https'
    }
};

export default config;
