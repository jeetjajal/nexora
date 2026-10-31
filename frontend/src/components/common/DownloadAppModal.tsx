'use client';

import React, { useState, useEffect } from 'react';
import { Smartphone, Download, QrCode, CheckCircle2, X, Sparkles, Share2, PlusSquare, ShieldCheck, Radio, Bell } from 'lucide-react';
import { MobileBridge } from '@/lib/mobile-bridge';

interface DownloadAppModalProps {
  isOpen: boolean;
  onClose: () => void;
}

export const DownloadAppModal: React.FC<DownloadAppModalProps> = ({ isOpen, onClose }) => {
  const [deferredPrompt, setDeferredPrompt] = useState<any>(null);
  const [isInstalled, setIsInstalled] = useState(false);
  const [downloadSuccess, setDownloadSuccess] = useState(false);
  const [currentPlatform, setCurrentPlatform] = useState<'android' | 'ios' | 'web'>('web');
  const [isNativeShell, setIsNativeShell] = useState(false);

  useEffect(() => {
    setCurrentPlatform(MobileBridge.getPlatform());
    setIsNativeShell(MobileBridge.isNative());

    const handleBeforeInstall = (e: Event) => {
      e.preventDefault();
      setDeferredPrompt(e);
    };

    window.addEventListener('beforeinstallprompt', handleBeforeInstall);

    if (window.matchMedia('(display-mode: standalone)').matches || MobileBridge.isNative()) {
      setIsInstalled(true);
    }

    // Auto-register device token if opened
    if (isOpen) {
      MobileBridge.registerDeviceWithBackend().catch(() => {});
    }

    return () => {
      window.removeEventListener('beforeinstallprompt', handleBeforeInstall);
    };
  }, [isOpen]);

  if (!isOpen) return null;

  const handleInstallClick = async () => {
    await MobileBridge.triggerHaptic('medium');

    if (deferredPrompt) {
      deferredPrompt.prompt();
      const choiceResult = await deferredPrompt.userChoice;
      if (choiceResult.outcome === 'accepted') {
        setIsInstalled(true);
      }
      setDeferredPrompt(null);
    } else {
      // Trigger a direct download of the PWA install manifest shortcut
      const element = document.createElement('a');
      const file = new Blob([JSON.stringify({
        name: "NEXORA Quick Commerce 3.0",
        version: "3.0.0",
        platform: currentPlatform,
        appId: "com.nexora.app",
        url: window.location.origin,
        icon: window.location.origin + "/assets/brand/app-icon.png",
        installedAt: new Date().toISOString()
      }, null, 2)], { type: 'application/json' });
      element.href = URL.createObjectURL(file);
      element.download = "Nexora-3.0-Native-Package.json";
      document.body.appendChild(element);
      element.click();
      document.body.removeChild(element);
      setDownloadSuccess(true);
      setTimeout(() => setDownloadSuccess(false), 4000);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
      {/* Backdrop */}
      <div className="fixed inset-0 bg-brand-text/70 backdrop-blur-sm" onClick={onClose} />

      {/* Modal Dialog */}
      <div className="relative w-full max-w-md bg-white rounded-card shadow-2xl z-10 overflow-hidden border border-brand-border animate-in zoom-in-95 duration-200">
        {/* Top Accent Gradient */}
        <div className="h-2 bg-gradient-to-r from-brand-primary via-brand-accent to-brand-primary" />

        <button
          onClick={onClose}
          className="absolute top-4 right-4 p-1.5 rounded-full text-brand-muted hover:text-brand-text hover:bg-brand-bg transition-colors"
        >
          <X size={18} />
        </button>

        <div className="p-6 text-center">
          {/* App Logo */}
          <div className="relative mx-auto w-20 h-20 mb-3">
            {/* eslint-disable-next-line @next/next/no-img-element */}
            <img
              src="/assets/brand/app-icon.png"
              alt="NEXORA Logo"
              className="w-20 h-20 rounded-2xl shadow-md mx-auto object-contain border border-brand-border/60"
            />
            <span className="absolute -bottom-1 -right-1 bg-brand-accent text-[#176B45] text-[9px] font-black px-1.5 py-0.5 rounded-full shadow-xs border border-brand-primary/20">
              v3.0 Native
            </span>
          </div>

          <div className="flex items-center justify-center gap-1.5 mb-1">
            <h3 className="text-xl font-black text-brand-text">
              NEXORA Mobile App
            </h3>
            {isNativeShell && (
              <span className="text-[10px] bg-emerald-100 text-emerald-800 font-bold px-2 py-0.5 rounded-full">
                Native Shell
              </span>
            )}
          </div>
          <p className="text-xs text-brand-muted max-w-xs mx-auto mb-4">
            Capacitor Native Android &amp; iOS container with hardware GPS telemetry, facial KYC, and instant push alerts.
          </p>

          {/* Quick Features Grid */}
          <div className="grid grid-cols-3 gap-2 mb-4 text-left">
            <div className="bg-brand-bg/70 p-2 rounded-lg border border-brand-border text-center">
              <span className="text-base block mb-0.5">⚡</span>
              <span className="text-[11px] font-bold text-brand-text block">Sub-10 Min</span>
              <span className="text-[9px] text-brand-muted">Dark store dispatch</span>
            </div>
            <div className="bg-brand-bg/70 p-2 rounded-lg border border-brand-border text-center">
              <span className="text-base block mb-0.5">🛵</span>
              <span className="text-[11px] font-bold text-brand-text block">Live SSE GPS</span>
              <span className="text-[9px] text-brand-muted">Interpolated radar</span>
            </div>
            <div className="bg-brand-bg/70 p-2 rounded-lg border border-brand-border text-center">
              <span className="text-base block mb-0.5">🔔</span>
              <span className="text-[11px] font-bold text-brand-text block">FCM Alerts</span>
              <span className="text-[9px] text-brand-muted">Real-time status</span>
            </div>
          </div>

          {/* Native Capabilities Pills */}
          <div className="flex flex-wrap items-center justify-center gap-1.5 mb-4 text-[10px]">
            <span className="px-2 py-0.5 bg-emerald-50 text-emerald-800 border border-emerald-200 rounded-full font-bold flex items-center gap-1">
              <ShieldCheck size={11} /> Biometric KYC
            </span>
            <span className="px-2 py-0.5 bg-sky-50 text-sky-800 border border-sky-200 rounded-full font-bold flex items-center gap-1">
              <Radio size={11} /> Background GPS
            </span>
            <span className="px-2 py-0.5 bg-amber-50 text-amber-800 border border-amber-200 rounded-full font-bold flex items-center gap-1">
              <Sparkles size={11} /> Haptic Tactile
            </span>
          </div>

          {/* Main Action Button */}
          <div className="space-y-3">
            <button
              onClick={handleInstallClick}
              className="w-full py-3 px-4 bg-brand-primary hover:bg-[#125638] text-white font-black text-sm rounded-button transition-all shadow-md active:scale-98 flex items-center justify-center gap-2 group"
            >
              <Download size={18} className="group-hover:translate-y-0.5 transition-transform" />
              <span>{isInstalled ? 'App Ready on Device' : 'Install NEXORA 3.0 Container'}</span>
            </button>

            {downloadSuccess && (
              <div className="p-2 bg-emerald-50 text-emerald-800 text-xs font-semibold rounded-lg flex items-center justify-center gap-1.5 animate-in fade-in">
                <CheckCircle2 size={14} className="text-emerald-600" />
                <span>Nexora 3.0 Native Package ready! Device registered.</span>
              </div>
            )}
          </div>

          {/* Mobile Install Guide */}
          <div className="mt-4 pt-4 border-t border-brand-border text-left">
            <h4 className="text-xs font-bold text-brand-text mb-2 flex items-center gap-1.5">
              <Smartphone size={13} className="text-brand-primary" />
              <span>How to add on Mobile (Current: {currentPlatform.toUpperCase()}):</span>
            </h4>
            <div className="space-y-2 text-[11px] text-brand-muted">
              <div className="flex items-start gap-2 bg-brand-bg/50 p-2 rounded-lg">
                <PlusSquare size={14} className="text-brand-primary shrink-0 mt-0.5" />
                <div>
                  <strong className="text-brand-text">Android / APK:</strong> Tap the three dots (⋮) in Chrome and select <em>&quot;Install app&quot;</em> or build native APK via <code>npx cap run android</code>.
                </div>
              </div>
              <div className="flex items-start gap-2 bg-brand-bg/50 p-2 rounded-lg">
                <Share2 size={14} className="text-brand-primary shrink-0 mt-0.5" />
                <div>
                  <strong className="text-brand-text">iPhone Safari:</strong> Tap Share (<span className="font-bold">⎙</span>) and choose <em>&quot;Add to Home Screen&quot;</em> or build via Xcode.
                </div>
              </div>
            </div>
          </div>

          <div className="mt-3 pt-3 flex items-center justify-between text-[11px] text-brand-muted">
            <span className="flex items-center gap-1 text-emerald-700 font-semibold">
              <CheckCircle2 size={13} /> Capacitor 6 &bull; PWA Ready
            </span>
            <span>App ID: com.nexora.app</span>
          </div>
        </div>
      </div>
    </div>
  );
};

export default DownloadAppModal;
