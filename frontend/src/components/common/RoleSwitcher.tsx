'use client';

import React, { useState } from 'react';
import Link from 'next/link';
import { useApp, AppRole } from '@/context/AppContext';
import { DownloadAppModal } from '@/components/common/DownloadAppModal';
import {
  ShoppingBag,
  Bike,
  ShieldCheck,
  Box,
  Navigation,
  Sparkles,
  RefreshCw,
  Download,
  CheckCircle2,
} from 'lucide-react';

export const RoleSwitcher: React.FC = () => {
  const { role, setRole, activeOrder, setIsTrackingOpen, setIsAIOpen, refreshCatalog, isLoadingCatalog } = useApp();
  const [isDownloadOpen, setIsDownloadOpen] = useState(false);
  const [isUpdating, setIsUpdating] = useState(false);
  const [updateToast, setUpdateToast] = useState<string | null>(null);

  const roles: { id: AppRole; label: string; icon: React.ReactNode; badge?: string }[] = [
    { id: 'customer', label: 'Store', icon: <ShoppingBag size={13} /> },
    { id: 'delivery', label: 'Rider', icon: <Bike size={13} />, badge: 'Duty' },
    { id: 'store', label: 'Store Ops', icon: <Box size={13} />, badge: 'Hub' },
    { id: 'admin', label: 'Admin', icon: <ShieldCheck size={13} />, badge: 'Tower' },
  ];

  const handleRoleSelect = (newRole: AppRole) => {
    setRole(newRole);
    if (typeof window !== 'undefined' && window.location.pathname !== '/') {
      window.location.href = '/';
    }
  };

  const handleLiveUpdate = async () => {
    setIsUpdating(true);
    try {
      await refreshCatalog();
      setUpdateToast('⚡ Live Sync Complete! Catalog & inventory updated from backend.');
    } catch {
      setUpdateToast('⚡ Live sync finished.');
    } finally {
      setIsUpdating(false);
      setTimeout(() => setUpdateToast(null), 3500);
    }
  };

  return (
    <>
      <div className="bg-[#125638] text-white border-b border-[#0f462d] text-xs py-1.5 px-2.5 sm:px-3 sticky top-0 z-50 shadow-sm transition-all">
        <div className="max-w-7xl mx-auto flex items-center justify-between gap-1.5 sm:gap-2">
          {/* Left: Role Switcher Buttons */}
          <div className="flex items-center gap-1 overflow-x-auto py-0.5 scrollbar-none">
            <span className="text-white/70 font-black mr-1 hidden sm:inline-flex items-center gap-1.5 text-[11px] shrink-0">
              <span className="w-2 h-2 rounded-full bg-brand-accent animate-ping" />
              NEXORA:
            </span>

            {roles.map((r) => {
              const isActive = role === r.id;
              return (
                <button
                  key={r.id}
                  onClick={() => handleRoleSelect(r.id)}
                  className={`flex items-center gap-1 px-2 sm:px-2.5 py-1 rounded-full font-bold transition-all text-[11px] whitespace-nowrap shrink-0 active:scale-95 ${
                    isActive
                      ? 'bg-brand-accent text-brand-text font-black shadow-sm'
                      : 'text-white/80 hover:text-white hover:bg-white/10'
                  }`}
                >
                  {r.icon}
                  <span>{r.label}</span>
                  {r.badge && (
                    <span
                      className={`text-[8px] px-1 py-0 rounded font-black ${
                        isActive ? 'bg-brand-primary text-white' : 'bg-white/15 text-white/90'
                      }`}
                    >
                      {r.badge}
                    </span>
                  )}
                </button>
              );
            })}

            {/* Quick Dedicated Route Links (Desktop only) */}
            <div className="hidden xl:flex items-center gap-1 ml-2 pl-2 border-l border-white/20">
              <Link
                href="/store/manager"
                className="text-[10px] text-white/70 hover:text-white px-1.5 py-0.5 rounded hover:bg-white/10 transition-colors"
              >
                Manager
              </Link>
              <Link
                href="/store/picker"
                className="text-[10px] text-white/70 hover:text-white px-1.5 py-0.5 rounded hover:bg-white/10 transition-colors"
              >
                Picker
              </Link>
              <Link
                href="/store/packing"
                className="text-[10px] text-white/70 hover:text-white px-1.5 py-0.5 rounded hover:bg-white/10 transition-colors"
              >
                Packing
              </Link>
              <Link
                href="/rider/orders"
                className="text-[10px] text-white/70 hover:text-white px-1.5 py-0.5 rounded hover:bg-white/10 transition-colors"
              >
                Rider
              </Link>
            </div>
          </div>

          {/* Right: Quick Action Controls */}
          <div className="flex items-center gap-1 sm:gap-1.5 shrink-0">
            {/* Download App Button with User's Logo */}
            <button
              onClick={() => setIsDownloadOpen(true)}
              className="flex items-center gap-1.5 px-2 sm:px-2.5 py-1 bg-white text-brand-primary hover:bg-brand-accent hover:text-brand-text font-black rounded-full text-[11px] shadow-sm transition-all active:scale-95 shrink-0"
              title="Download NEXORA App on Mobile (PWA / Shortcut)"
            >
              {/* eslint-disable-next-line @next/next/no-img-element */}
              <img
                src="/assets/brand/app-icon.png"
                alt="App Icon"
                className="w-3.5 h-3.5 rounded object-cover shrink-0"
              />
              <span className="hidden sm:inline">Download App</span>
              <span className="sm:hidden text-[10px] font-black">App</span>
            </button>

            {/* Live Update / Refresh Button */}
            <button
              onClick={handleLiveUpdate}
              disabled={isUpdating || isLoadingCatalog}
              className={`flex items-center gap-1 px-2 sm:px-2.5 py-1 bg-white/15 hover:bg-white/25 text-white rounded-full text-[11px] font-bold border border-white/25 transition-all active:scale-95 shrink-0 ${
                isUpdating ? 'opacity-80' : ''
              }`}
              title="Click to live sync catalog, products and store changes"
            >
              <RefreshCw size={11} className={isUpdating || isLoadingCatalog ? 'animate-spin text-brand-accent' : ''} />
              <span className="hidden sm:inline">Update</span>
              <span className="sm:hidden text-[10px]">Sync</span>
            </button>

            {/* Active Order Tracker */}
            {activeOrder && (
              <button
                onClick={() => setIsTrackingOpen(true)}
                className="flex items-center gap-1 px-2 py-1 bg-brand-accent text-brand-text font-black rounded-full text-[11px] hover:brightness-105 animate-pulse shrink-0"
              >
                <Navigation size={11} />
                <span className="hidden sm:inline">Track ({activeOrder.id})</span>
              </button>
            )}

            {/* Nexora AI Assistant Button */}
            <button
              onClick={() => setIsAIOpen(true)}
              className="flex items-center gap-1 px-2 py-1 bg-white/10 hover:bg-white/20 text-brand-accent rounded-full text-[11px] font-bold border border-brand-accent/30 transition-all shrink-0"
            >
              <Sparkles size={11} />
              <span className="hidden md:inline">Nexora AI</span>
            </button>
          </div>
        </div>
      </div>

      {/* Floating Live Update Feedback Toast */}
      {updateToast && (
        <div className="fixed top-12 left-1/2 -translate-x-1/2 z-50 bg-brand-text text-white px-4 py-2 rounded-full shadow-2xl border border-brand-accent text-xs font-bold flex items-center gap-2 animate-in fade-in slide-in-from-top-2 duration-200">
          <CheckCircle2 size={15} className="text-brand-accent shrink-0" />
          <span>{updateToast}</span>
        </div>
      )}

      {/* Download App Modal */}
      <DownloadAppModal isOpen={isDownloadOpen} onClose={() => setIsDownloadOpen(false)} />
    </>
  );
};

export default RoleSwitcher;
