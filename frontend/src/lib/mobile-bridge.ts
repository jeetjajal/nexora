/**
 * Nexora 3.0 Mobile Bridge & Device Capabilities
 * Provides cross-platform native wrapper functionality for Capacitor Native (Android/iOS)
 * and graceful fallback for Progressive Web Apps (PWA) / Desktop Web browsers.
 */

export interface MobileDeviceRegistrationInfo {
  id?: number;
  userId?: number;
  deviceId: string;
  deviceToken: string;
  platform: 'ANDROID' | 'IOS' | 'WEB_PWA';
  appVersion: string;
  pushProvider: string;
  isActive: boolean;
  registeredAt?: string;
}

export interface PermissionStatusResult {
  granted: boolean;
  fineLocation?: boolean;
  backgroundLocation?: boolean;
  camera?: boolean;
  notifications?: boolean;
}

export class MobileBridge {
  private static readonly APP_VERSION = '3.0.0';
  private static readonly DEVICE_ID_KEY = 'nx_device_uuid';

  /**
   * Detects whether the app is running inside a Capacitor native container (Android or iOS).
   */
  static isNative(): boolean {
    if (typeof window === 'undefined') return false;
    const win = window as any;
    return !!(win.Capacitor && typeof win.Capacitor.isNativePlatform === 'function' && win.Capacitor.isNativePlatform());
  }

  /**
   * Returns current active platform: 'android', 'ios', or 'web'
   */
  static getPlatform(): 'android' | 'ios' | 'web' {
    if (typeof window === 'undefined') return 'web';
    const win = window as any;
    if (win.Capacitor && typeof win.Capacitor.getPlatform === 'function') {
      const p = win.Capacitor.getPlatform();
      if (p === 'android' || p === 'ios') return p;
    }
    const userAgent = navigator.userAgent || '';
    if (/android/i.test(userAgent)) return 'android';
    if (/iPad|iPhone|iPod/.test(userAgent)) return 'ios';
    return 'web';
  }

  /**
   * Retrieves persistent UUID for this device installation.
   */
  static getDeviceId(): string {
    if (typeof window === 'undefined') return 'server-rendered-id';
    let deviceId = localStorage.getItem(this.DEVICE_ID_KEY);
    if (!deviceId) {
      deviceId = 'nx-dev-' + Math.random().toString(36).substring(2, 10) + '-' + Date.now().toString(36);
      localStorage.setItem(this.DEVICE_ID_KEY, deviceId);
    }
    return deviceId;
  }

  /**
   * Requests High-Accuracy GPS permissions (Foreground and Background).
   * Critical for Rider GPS real-time telemetry tracking.
   */
  static async requestGpsPermissions(): Promise<PermissionStatusResult> {
    if (typeof window === 'undefined') {
      return { granted: false };
    }

    // 1. If running in Capacitor Native
    const win = window as any;
    if (this.isNative() && win.Capacitor?.Plugins?.Geolocation) {
      try {
        const status = await win.Capacitor.Plugins.Geolocation.requestPermissions();
        const granted = status.location === 'granted';
        return {
          granted,
          fineLocation: granted,
          backgroundLocation: granted,
        };
      } catch (err) {
        console.warn('Native GPS permission request error, falling back:', err);
      }
    }

    // 2. Web Browser standard fallback
    if (navigator.geolocation) {
      return new Promise((resolve) => {
        navigator.geolocation.getCurrentPosition(
          () => {
            resolve({ granted: true, fineLocation: true, backgroundLocation: false });
          },
          (err) => {
            console.warn('Browser geolocation denied or error:', err.message);
            resolve({ granted: false, fineLocation: false, backgroundLocation: false });
          },
          { enableHighAccuracy: true, timeout: 8000 }
        );
      });
    }

    return { granted: false };
  }

  /**
   * Requests Camera Permissions for Rider KYC Shift Selfie & barcode scanning.
   */
  static async requestCameraPermissions(): Promise<boolean> {
    if (typeof window === 'undefined') return false;

    const win = window as any;
    if (this.isNative() && win.Capacitor?.Plugins?.Camera) {
      try {
        const status = await win.Capacitor.Plugins.Camera.requestPermissions();
        return status.camera === 'granted';
      } catch (err) {
        console.warn('Native camera permission error:', err);
      }
    }

    if (navigator.mediaDevices && navigator.mediaDevices.getUserMedia) {
      try {
        const stream = await navigator.mediaDevices.getUserMedia({ video: true });
        stream.getTracks().forEach((track) => track.stop());
        return true;
      } catch {
        return false;
      }
    }

    return false;
  }

  /**
   * Triggers native tactile haptic feedback (or Web Vibration API fallback).
   */
  static async triggerHaptic(type: 'light' | 'medium' | 'heavy' | 'success' | 'warning' | 'error' = 'medium'): Promise<void> {
    if (typeof window === 'undefined') return;

    const win = window as any;
    if (this.isNative() && win.Capacitor?.Plugins?.Haptics) {
      try {
        const Haptics = win.Capacitor.Plugins.Haptics;
        if (type === 'light' || type === 'medium' || type === 'heavy') {
          await Haptics.impact({ style: type.toUpperCase() });
        } else if (type === 'success' || type === 'warning' || type === 'error') {
          await Haptics.notification({ type: type.toUpperCase() });
        }
        return;
      } catch (err) {
        console.warn('Native haptics error:', err);
      }
    }

    // Web Vibration Fallback
    if (typeof navigator !== 'undefined' && 'vibrate' in navigator) {
      try {
        switch (type) {
          case 'light':
            navigator.vibrate(15);
            break;
          case 'medium':
            navigator.vibrate(30);
            break;
          case 'heavy':
            navigator.vibrate(60);
            break;
          case 'success':
            navigator.vibrate([20, 40, 30]);
            break;
          case 'warning':
            navigator.vibrate([40, 30, 40]);
            break;
          case 'error':
            navigator.vibrate([80, 50, 80]);
            break;
        }
      } catch {
        // Ignored
      }
    }
  }

  /**
   * Registers this device with the Nexora backend push notification engine.
   */
  static async registerDeviceWithBackend(token?: string, authToken?: string): Promise<MobileDeviceRegistrationInfo> {
    const platform = this.isNative()
      ? (this.getPlatform().toUpperCase() as 'ANDROID' | 'IOS')
      : 'WEB_PWA';

    const payload = {
      deviceId: this.getDeviceId(),
      deviceToken: token || `token-${this.getDeviceId()}`,
      platform,
      appVersion: this.APP_VERSION,
      pushProvider: platform === 'IOS' ? 'APNS' : 'FCM',
    };

    const headers: Record<string, string> = { 'Content-Type': 'application/json' };
    if (authToken) headers['Authorization'] = `Bearer ${authToken}`;

    const res = await fetch('/api/mobile/device', {
      method: 'POST',
      headers,
      body: JSON.stringify(payload),
    });

    const json = await res.json();
    return json.data;
  }

  /**
   * Monitors live online/offline network connectivity.
   */
  static isOnline(): boolean {
    if (typeof window === 'undefined') return true;
    return navigator.onLine;
  }
}
