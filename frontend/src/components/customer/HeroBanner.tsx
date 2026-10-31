'use client';

import React from 'react';
import { Sparkles, Zap, ShieldCheck, Tag, ArrowRight } from 'lucide-react';
import { useApp } from '@/context/AppContext';

export const HeroBanner: React.FC = () => {
  const { applyCoupon, setIsCartOpen } = useApp();

  return (
    <div className="py-4">
      {/* Primary Highlights Row */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        {/* Banner 1: Rapid 10-Min Delivery */}
        <div className="relative overflow-hidden rounded-card bg-gradient-to-br from-brand-primary via-[#176B45] to-[#0f462d] text-white p-5 shadow-card flex flex-col justify-between min-h-[175px] group">
          <div className="z-10 max-w-[65%] sm:max-w-[68%]">
            <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full bg-brand-accent text-brand-text font-black text-[10px] uppercase tracking-wider mb-2 shadow-sm">
              <Zap size={11} fill="currentColor" /> Instant Rapid
            </span>
            <h2 className="text-lg sm:text-xl font-black leading-snug text-white">
              Groceries Delivered in <br />
              <span className="text-brand-accent underline decoration-wavy decoration-1">10 Minutes Flat</span>
            </h2>
            <p className="text-[11px] sm:text-xs text-white/85 mt-1 font-medium line-clamp-2">
              Over 5,000+ items picked directly from our nearest dark store.
            </p>
          </div>

          <div className="z-10 flex items-center gap-2 mt-3">
            <span className="text-[10px] sm:text-[11px] font-bold text-white/95 bg-white/20 backdrop-blur-sm px-2.5 py-1 rounded-full flex items-center gap-1 border border-white/10">
              <ShieldCheck size={12} className="text-brand-accent" /> 100% Quality Checked
            </span>
          </div>

          {/* Real Photo: Rapid Grocery Bag & Fresh Produce */}
          <div className="absolute -right-2 -bottom-2 w-28 h-28 sm:w-32 sm:h-32 md:w-28 md:h-28 lg:w-32 lg:h-32 pointer-events-none select-none z-0">
            <div className="relative w-full h-full transform rotate-[-4deg] group-hover:rotate-0 group-hover:scale-105 transition-all duration-300">
              <img
                src="/assets/banners/banner-delivery.jpg"
                alt="Fresh Groceries in Rapid Delivery Bag"
                className="w-full h-full object-cover rounded-2xl shadow-2xl border-2 border-white/30"
                loading="eager"
              />
              <span className="absolute -top-1.5 -left-1.5 bg-brand-accent text-brand-text font-black text-[9px] px-1.5 py-0.5 rounded-full shadow-md flex items-center gap-0.5 border border-white/50">
                <Zap size={9} fill="currentColor" /> 10m
              </span>
            </div>
          </div>
        </div>

        {/* Banner 2: Fresh Farm Produce (Fruits & Vegetables) */}
        <div className="relative overflow-hidden rounded-card bg-gradient-to-br from-[#f0f9f3] via-[#e5f5eb] to-[#d6edd9] border border-[#c3e4cb] text-brand-text p-5 shadow-card flex flex-col justify-between min-h-[175px] group">
          <div className="z-10 max-w-[65%] sm:max-w-[68%]">
            <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full bg-brand-primary text-white font-black text-[10px] uppercase tracking-wider mb-2 shadow-sm">
              <Sparkles size={11} /> Farm to Kitchen
            </span>
            <h3 className="text-lg sm:text-xl font-black leading-snug text-brand-text">
              Crisp Veggies & <br />
              <span className="text-brand-primary">Luscious Fresh Fruits</span>
            </h3>
            <p className="text-[11px] sm:text-xs text-brand-muted mt-1 font-medium line-clamp-2">
              Sourced daily at 4 AM from certified regional organic farmers.
            </p>
          </div>

          <div className="z-10 flex items-center gap-2 mt-3">
            <span className="text-[10px] sm:text-[11px] font-black text-brand-primary bg-brand-primary/10 px-2.5 py-1 rounded-full border border-brand-primary/20">
              Up to 35% OFF Today
            </span>
          </div>

          {/* Real Photo: Fresh Farm Fruits and Veggies */}
          <div className="absolute -right-2 -bottom-2 w-28 h-28 sm:w-32 sm:h-32 md:w-28 md:h-28 lg:w-32 lg:h-32 pointer-events-none select-none z-0">
            <div className="relative w-full h-full transform rotate-[3deg] group-hover:rotate-0 group-hover:scale-105 transition-all duration-300">
              <img
                src="/assets/banners/banner-fresh-produce.jpg"
                alt="Farm Fresh Crisp Vegetables and Luscious Fruits"
                className="w-full h-full object-cover rounded-2xl shadow-xl border-2 border-white"
                loading="eager"
              />
              <span className="absolute -top-1.5 -right-1 bg-brand-primary text-white font-black text-[9px] px-1.5 py-0.5 rounded-full shadow-md border border-white">
                Fresh
              </span>
            </div>
          </div>
        </div>

        {/* Banner 3: Welcome Offer / Gift Hamper */}
        <div className="relative overflow-hidden rounded-card bg-gradient-to-br from-brand-accent via-[#d5f062] to-[#c7e54f] text-brand-text p-5 shadow-card flex flex-col justify-between min-h-[175px] border border-[#c3df4b] group">
          <div className="z-10 max-w-[65%] sm:max-w-[68%]">
            <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full bg-brand-text text-brand-accent font-black text-[10px] uppercase tracking-wider mb-2 shadow-sm">
              <Tag size={11} /> Welcome Offer
            </span>
            <h3 className="text-lg sm:text-xl font-black leading-snug text-brand-text">
              Flat 50% OFF on <br />
              Your First 3 Orders
            </h3>
            <p className="text-[11px] sm:text-xs text-brand-text/85 mt-1 font-semibold">
              Use code <span className="bg-brand-text text-brand-accent px-1.5 py-0.5 rounded font-mono font-bold text-[10px]">NEXORA50</span> at checkout
            </p>
          </div>

          <div className="z-10 mt-3">
            <button
              onClick={() => {
                applyCoupon('NEXORA50');
                setIsCartOpen(true);
              }}
              className="text-[11px] sm:text-xs font-black bg-brand-primary hover:bg-brand-primaryHover text-white px-3.5 py-1.5 rounded-button shadow-md transition-all active:scale-95 flex items-center gap-1"
            >
              <span>Apply Code Now</span>
              <ArrowRight size={12} />
            </button>
          </div>

          {/* Real Photo: Luxury Curated Gift Hamper Basket with Satin Ribbon */}
          <div className="absolute -right-2 -bottom-2 w-28 h-28 sm:w-32 sm:h-32 md:w-28 md:h-28 lg:w-32 lg:h-32 pointer-events-none select-none z-0">
            <div className="relative w-full h-full transform rotate-[-3deg] group-hover:rotate-0 group-hover:scale-105 transition-all duration-300">
              <img
                src="/assets/banners/banner-gift-hamper.jpg"
                alt="Luxury Curated Gift Hamper Basket"
                className="w-full h-full object-cover rounded-2xl shadow-xl border-2 border-white/90"
                loading="eager"
              />
              <span className="absolute -top-1.5 -left-1.5 bg-brand-text text-brand-accent font-black text-[9px] px-1.5 py-0.5 rounded-full shadow-md border border-brand-accent/50">
                Hamper
              </span>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default HeroBanner;
