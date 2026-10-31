import type { CapacitorConfig } from '@capacitor/cli';

const config: CapacitorConfig = {
  appId: 'com.nexora.app',
  appName: 'Nexora Quick Commerce',
  webDir: 'out',
  bundledWebRuntime: false,
  server: {
    // In production, the native shell loads the fast Next.js live production build or local bundle
    url: process.env.CAPACITOR_LIVE_URL || 'https://nexora-mu-eosin.vercel.app',
    cleartext: false,
    allowNavigation: [
      'nexora-mu-eosin.vercel.app',
      'nexora-backend-fw14.onrender.com',
      '*.razorpay.com',
      '*.tile.openstreetmap.org',
    ],
  },
  plugins: {
    SplashScreen: {
      launchShowDuration: 2000,
      launchAutoHide: true,
      backgroundColor: '#176B45',
      androidSplashResourceName: 'splash',
      androidScaleType: 'CENTER_CROP',
      showSpinner: true,
      spinnerColor: '#D8F36A',
    },
    PushNotifications: {
      presentationOptions: ['badge', 'sound', 'alert'],
    },
    Keyboard: {
      resize: 'body',
      style: 'dark',
      resizeOnFullScreen: true,
    },
  },
};

export default config;
