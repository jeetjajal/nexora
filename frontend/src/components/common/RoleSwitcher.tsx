'use client';

import React from 'react';
import Link from 'next/link';
import { useApp, AppRole } from '@/context/AppContext';
import { ShoppingBag, Bike, ShieldCheck, Box, Navigation, Sparkles, Layers, PackageCheck } from 'lucide-react';

export const RoleSwitcher: React.FC = () => {
  const { role, setRole, activeOrder, setIsTrackingOpen, setIsAIOpen } = useApp();

  const roles: { id: AppRole; label: string; icon: React.ReactNode; badge?: string }[] = [
    { id: 'customer', label: 'Customer Store', icon: <ShoppingBag size={14} /> },
    { id: 'delivery', label: 'Delivery Partner', icon: <Bike size={14} />, badge: 'Rider' },
    { id: 'admin', label: 'Control Tower', icon: <ShieldCheck size={14} />, badge: 'Super Admin' },
    { id: 'store', label: 'Store Operations', icon: <Box size={14} />, badge: 'Picker' },
  ];

  return (
    <div className="bg-[#125638] text-white border-b border-[#0f462d] text-xs py-1.5 px-3 sticky top-0 z-50 shadow-sm">
      <div className="max-w-7xl mx-auto flex flex-wrap items-center justify-between gap-2">
        {/* Left: Role Switcher Buttons */}
        <div className="flex items-center gap-1 overflow-x-auto py-0.5">
          <span className="text-white/60 font-semibold mr-1.5 hidden sm:inline-flex items-center gap-1 text-[11px]">
            <span className="w-2 h-2 rounded-full bg-brand-accent animate-ping" />
            NEXORA UNIFIED:
          </span>
          {roles.map(r => {
            const isActive = role === r.id;
            return (
              <button
                key={r.id}
                onClick={() => setRole(r.id)}
                className={`flex items-center gap-1.5 px-2.5 py-1 rounded-full font-medium transition-all text-[11px] whitespace-nowrap ${
                  isActive
                    ? 'bg-brand-accent text-brand-text font-bold shadow-sm scale-102'
                    : 'text-white/80 hover:text-white hover:bg-white/10'
                }`}
              >
                {r.icon}
                <span>{r.label}</span>
                {r.badge && (
                  <span
                    className={`text-[9px] px-1 py-0.2 rounded font-extrabold ${
                      isActive ? 'bg-brand-primary text-white' : 'bg-white/15 text-white/90'
                    }`}
                  >
                    {r.badge}
                  </span>
                )}
              </button>
            );
          })}

          {/* Quick Dedicated Route Links */}
          <div className="hidden lg:flex items-center gap-1 ml-2 pl-2 border-l border-white/20">
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

        {/* Right: Quick Actions */}
        <div className="flex items-center gap-2">
          {activeOrder && (
            <button
              onClick={() => setIsTrackingOpen(true)}
              className="flex items-center gap-1.5 px-2.5 py-1 bg-brand-accent text-brand-text font-bold rounded-full text-[11px] hover:brightness-105 animate-pulse"
            >
              <Navigation size={12} />
              <span>Track Order ({activeOrder.id})</span>
            </button>
          )}

          <button
            onClick={() => setIsAIOpen(true)}
            className="flex items-center gap-1 px-2.5 py-1 bg-white/10 hover:bg-white/20 text-brand-accent rounded-full text-[11px] font-semibold border border-brand-accent/30 transition-all"
          >
            <Sparkles size={12} />
            <span className="hidden sm:inline">Nexora AI</span>
          </button>

          <span className="hidden md:inline-flex items-center gap-1.5 text-white/70 text-[10px] pl-2 border-l border-white/20">
            <span className="w-1.5 h-1.5 rounded-full bg-emerald-400" />
            Dark Store: SG Highway #04
          </span>
        </div>
      </div>
    </div>
  );
};

export default RoleSwitcher;
