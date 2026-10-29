'use client';

import React, { useState } from 'react';
import { useApp } from '@/context/AppContext';
import { DownloadAppModal } from '@/components/common/DownloadAppModal';
import {
  ShoppingBag,
  Bike,
  ShieldCheck,
  Box,
  Navigation,
  Sparkles,
  RefreshCw,
  CheckCircle2,
  ArrowLeft,
  LayoutGrid,
} from 'lucide-react';

export const RoleSwitcher: React.FC = () => {
  const {
    role,
    setRole,
    activeOrder,
    setIsTrackingOpen,
    setIsAIOpen,
    refreshCatalog,
    isLoadingCatalog,
    setIsRolePortalOpen,
  } = useApp();

  const [isDownloadOpen, setIsDownloadOpen] = useState(false);
  const [isUpdating, setIsUpdating] = useState(false);
  const [updateToast, setUpdateToast] = useState<string | null>(null);

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

  // 1. Operator View (When active role is Rider, Store, or Admin)
  if (role !== 'customer') {
    const roleLabels = {
      delivery: { label: 'Rider Partner App', icon: <Bike size={14} />, badge: 'Duty Active' },
      store: { label: 'Dark Store #04 Ops', icon: <Box size={14} />, badge: 'SG Highway Hub' },
      admin: { label: 'Super Admin Control Tower', icon: <ShieldCheck size={14} />, badge: 'Surveillance' },
    };

    const currentRoleInfo = roleLabels[role] || { label: 'Staff Portal', icon: <Box size={14} />, badge: 'Operator' };

    return (
      <>
        <div className="bg-[#0f462d] text-white border-b border-[#0a3320] text-xs py-1.5 px-3 sticky top-0 z-50 shadow-sm transition-all">
          <div className="max-w-7xl mx-auto flex items-center justify-between gap-2">
            {/* Left: Back to Customer Storefront Button */}
            <div className="flex items-center gap-2">
              <button
                onClick={() => setRole('customer')}
                className="flex items-center gap-1.5 px-3 py-1 bg-brand-accent text-brand-text font-black rounded-full text-xs hover:brightness-105 transition-all shadow-xs active:scale-95"
                title="Return to Customer Storefront"
              >
                <ArrowLeft size={13} strokeWidth={3} />
                <span>← Customer Store (ગ્રાહક સ્ટોર)</span>
              </button>

              <div className="hidden sm:flex items-center gap-1.5 text-white/90 text-xs font-bold pl-2 border-l border-white/20">
                {currentRoleInfo.icon}
                <span>{currentRoleInfo.label}</span>
                <span className="text-[10px] bg-white/15 px-1.5 py-0.2 rounded-full font-bold">
                  {currentRoleInfo.badge}
                </span>
              </div>
            </div>

            {/* Right: Switch Portal Modal & Sync */}
            <div className="flex items-center gap-2">
              <button
                onClick={() => setIsRolePortalOpen(true)}
                className="flex items-center gap-1 px-2.5 py-1 bg-white/15 hover:bg-white/25 text-white rounded-full text-xs font-bold transition-all"
                title="Switch Staff Role / Portal"
              >
                <LayoutGrid size={12} />
                <span>Switch Portal (પોર્ટલ બદલો)</span>
              </button>

              <button
                onClick={handleLiveUpdate}
                disabled={isUpdating || isLoadingCatalog}
                className="flex items-center gap-1 px-2.5 py-1 bg-white/10 hover:bg-white/20 text-white rounded-full text-xs font-bold transition-all"
                title="Live Sync"
              >
                <RefreshCw size={11} className={isUpdating || isLoadingCatalog ? 'animate-spin text-brand-accent' : ''} />
                <span className="hidden sm:inline">Sync</span>
              </button>
            </div>
          </div>
        </div>

        {/* Update Feedback Toast */}
        {updateToast && (
          <div className="fixed top-12 left-1/2 -translate-x-1/2 z-50 bg-brand-text text-white px-4 py-2 rounded-full shadow-2xl border border-brand-accent text-xs font-bold flex items-center gap-2 animate-in fade-in slide-in-from-top-2 duration-200">
            <CheckCircle2 size={15} className="text-brand-accent shrink-0" />
            <span>{updateToast}</span>
          </div>
        )}
      </>
    );
  }

  // 2. Customer View (Clean, Blinkit-Style Top Bar without Rider/Admin Clutter)
  return (
    <>
      <div className="bg-[#125638] text-white border-b border-[#0f462d] text-xs py-1 px-2.5 sm:px-3 sticky top-0 z-50 shadow-sm transition-all">
        <div className="max-w-7xl mx-auto flex items-center justify-between gap-1.5 sm:gap-2">
          {/* Left: Rapid Delivery Promise */}
          <div className="flex items-center gap-2">
            <span className="flex items-center gap-1.5 font-bold text-[11px] text-white/95">
              <span className="w-2 h-2 rounded-full bg-brand-accent animate-ping" />
              <span>⚡ 10-Minute Rapid Delivery in Ahmedabad</span>
            </span>
          </div>

          {/* Right: Clean Customer Utilities + Staff Portal Trigger */}
          <div className="flex items-center gap-1.5 sm:gap-2 shrink-0">
            {/* Download App Button */}
            <button
              onClick={() => setIsDownloadOpen(true)}
              className="flex items-center gap-1.5 px-2 sm:px-2.5 py-0.5 sm:py-1 bg-white text-brand-primary hover:bg-brand-accent hover:text-brand-text font-black rounded-full text-[11px] shadow-sm transition-all active:scale-95 shrink-0"
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
              className={`flex items-center gap-1 px-2 sm:px-2.5 py-0.5 sm:py-1 bg-white/15 hover:bg-white/25 text-white rounded-full text-[11px] font-bold border border-white/25 transition-all active:scale-95 shrink-0 ${
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
                className="flex items-center gap-1 px-2 py-0.5 sm:py-1 bg-brand-accent text-brand-text font-black rounded-full text-[11px] hover:brightness-105 animate-pulse shrink-0"
              >
                <Navigation size={11} />
                <span className="hidden sm:inline">Track ({activeOrder.id})</span>
              </button>
            )}

            {/* Nexora AI Assistant Button */}
            <button
              onClick={() => setIsAIOpen(true)}
              className="flex items-center gap-1 px-2 py-0.5 sm:py-1 bg-white/10 hover:bg-white/20 text-brand-accent rounded-full text-[11px] font-bold border border-brand-accent/30 transition-all shrink-0"
            >
              <Sparkles size={11} />
              <span className="hidden md:inline">Nexora AI</span>
            </button>

            {/* Staff & Partner Portal Entry (Discreet Nagrik Seva AI style selector) */}
            <button
              onClick={() => setIsRolePortalOpen(true)}
              className="flex items-center gap-1 px-2 sm:px-2.5 py-0.5 sm:py-1 bg-black/20 hover:bg-black/30 text-white/90 hover:text-white rounded-full text-[10px] sm:text-[11px] font-semibold border border-white/20 transition-all shrink-0"
              title="Staff, Rider & Store Manager Portal (પોર્ટલ પસંદ કરો)"
            >
              <LayoutGrid size={11} />
              <span className="hidden sm:inline">Partner Portal</span>
              <span className="sm:hidden text-[10px]">Portal</span>
            </button>
          </div>
        </div>
      </div>

      {/* Download App Modal */}
      <DownloadAppModal
        isOpen={isDownloadOpen}
        onClose={() => setIsDownloadOpen(false)}
      />

      {/* Floating Live Update Feedback Toast */}
      {updateToast && (
        <div className="fixed top-12 left-1/2 -translate-x-1/2 z-50 bg-brand-text text-white px-4 py-2 rounded-full shadow-2xl border border-brand-accent text-xs font-bold flex items-center gap-2 animate-in fade-in slide-in-from-top-2 duration-200">
          <CheckCircle2 size={15} className="text-brand-accent shrink-0" />
          <span>{updateToast}</span>
        </div>
      )}
    </>
  );
};
