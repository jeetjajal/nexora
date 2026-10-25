'use client';

import React from 'react';
import { Sparkles, Zap, ShieldCheck, Tag } from 'lucide-react';
import { useApp } from '@/context/AppContext';

export const HeroBanner: React.FC = () => {
  const { applyCoupon, setIsCartOpen } = useApp();

  return (
    <div className="py-4">
      {/* Primary Highlights Row */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        {/* Banner 1: Rapid 10-Min Delivery */}
        <div className="relative overflow-hidden rounded-card bg-gradient-to-r from-brand-primary to-[#1f7c52] text-white p-5 shadow-card flex flex-col justify-between min-h-[160px]">
          <div className="z-10">
            <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full bg-brand-accent text-brand-text font-black text-[10px] uppercase tracking-wider mb-2">
              <Zap size={11} fill="currentColor" /> Instant Rapid
            </span>
            <h2 className="text-xl font-black leading-tight text-white">
              Groceries Delivered in <br />
              <span className="text-brand-accent underline decoration-wavy decoration-1">10 Minutes Flat</span>
            </h2>
            <p className="text-xs text-white/80 mt-1 font-medium">
              Over 5,000+ items picked directly from our nearest dark store.
            </p>
          </div>

          <div className="z-10 flex items-center gap-2 mt-3">
            <span className="text-[11px] font-bold text-white/90 bg-white/15 px-2.5 py-1 rounded-full flex items-center gap-1">
              <ShieldCheck size={13} className="text-brand-accent" /> 100% Quality Checked
            </span>
          </div>

          {/* Decorative geometric speed stripes */}
          <div className="absolute -right-6 -bottom-6 w-32 h-32 rounded-full bg-white/5 pointer-events-none" />
          <div className="absolute right-4 top-4 text-5xl opacity-20 pointer-events-none">⚡</div>
        </div>

        {/* Banner 2: Fresh Farm Produce */}
        <div className="relative overflow-hidden rounded-card bg-[#eef7ee] border border-[#d6ebd6] text-brand-text p-5 shadow-card flex flex-col justify-between min-h-[160px]">
          <div className="z-10">
            <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full bg-brand-primary text-white font-black text-[10px] uppercase tracking-wider mb-2">
              <Sparkles size={11} /> Farm to Kitchen
            </span>
            <h3 className="text-xl font-black leading-tight text-brand-text">
              Crisp Veggies & <br />
              <span className="text-brand-primary">Luscious Fresh Fruits</span>
            </h3>
            <p className="text-xs text-brand-muted mt-1 font-medium">
              Sourced daily at 4 AM from certified regional organic farmers.
            </p>
          </div>

          <div className="z-10 flex items-center gap-2 mt-3">
            <span className="text-[11px] font-bold text-brand-primary bg-brand-primary/10 px-2.5 py-1 rounded-full">
              Up to 35% OFF Today
            </span>
          </div>

          <div className="absolute right-4 bottom-2 text-5xl opacity-80 pointer-events-none">🥦🍎</div>
        </div>

        {/* Banner 3: Coupon Code NEXORA50 */}
        <div className="relative overflow-hidden rounded-card bg-brand-accent text-brand-text p-5 shadow-card flex flex-col justify-between min-h-[160px] border border-[#cbe65c]">
          <div className="z-10">
            <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full bg-brand-text text-brand-accent font-black text-[10px] uppercase tracking-wider mb-2">
              <Tag size={11} /> Welcome Offer
            </span>
            <h3 className="text-xl font-black leading-tight text-brand-text">
              Flat 50% OFF on <br />
              Your First 3 Orders
            </h3>
            <p className="text-xs text-brand-text/80 mt-1 font-semibold">
              Use code <span className="bg-brand-text text-brand-accent px-1.5 py-0.5 rounded font-mono font-bold">NEXORA50</span> at checkout
            </p>
          </div>

          <div className="z-10 mt-3">
            <button
              onClick={() => {
                applyCoupon('NEXORA50');
                setIsCartOpen(true);
              }}
              className="text-xs font-black bg-brand-primary hover:bg-brand-primaryHover text-white px-3.5 py-1.5 rounded-button shadow-sm transition-transform active:scale-95"
            >
              Apply Code Now &rarr;
            </button>
          </div>

          <div className="absolute right-3 bottom-2 text-5xl opacity-40 pointer-events-none">🎁</div>
        </div>
      </div>
    </div>
  );
};

export default HeroBanner;
