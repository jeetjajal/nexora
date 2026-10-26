'use client';

import React from 'react';
import { useApp } from '@/context/AppContext';
import { NexoraLogo } from '@/components/brand/Logo';
import { Search, MapPin, ShoppingCart, User, ChevronDown, Clock, ShieldCheck } from 'lucide-react';

export const Header: React.FC = () => {
  const {
    currentUser,
    savedAddresses,
    setIsAuthOpen,
    setIsAccountOpen,
    getCartCount,
    getCartTotal,
    setIsCartOpen,
    searchQuery,
    setSearchQuery,
  } = useApp();

  const cartCount = getCartCount();
  const cartTotal = getCartTotal();

  const activeAddr = savedAddresses.find(a => a.isDefault) || savedAddresses[0];

  return (
    <header className="bg-white border-b border-brand-border sticky top-[33px] z-40 shadow-sm transition-all">
      <div className="max-w-7xl mx-auto px-4 py-3 flex items-center justify-between gap-4">
        {/* Brand Logo & Tagline */}
        <div className="flex items-center gap-6 shrink-0">
          <NexoraLogo size="md" showSubtitle={true} />

          {/* Location Delivery Selector (Blinkit Style) */}
          <div
            onClick={() => currentUser ? setIsAccountOpen(true) : setIsAuthOpen(true)}
            className="hidden lg:flex flex-col text-left cursor-pointer group"
          >
            <div className="flex items-center gap-1 text-[13px] font-black text-brand-text">
              <Clock size={14} className="text-brand-primary" />
              <span>Delivery in {activeAddr?.eta || '9 MINS'}</span>
              <ChevronDown size={14} className="text-brand-muted group-hover:text-brand-primary transition-colors" />
            </div>
            <div className="flex items-center gap-1 text-[11px] text-brand-muted font-medium truncate max-w-[220px]">
              <MapPin size={11} className="shrink-0 text-brand-primary" />
              <span className="truncate">
                {activeAddr
                  ? `${activeAddr.addressLine1}, ${activeAddr.city}`
                  : 'B-402, Shivalik Residency, Ahmedabad'}
              </span>
            </div>
          </div>
        </div>

        {/* Global Search Bar */}
        <div className="flex-1 max-w-xl mx-2">
          <div className="relative">
            <Search className="absolute left-3.5 top-1/2 -translate-y-1/2 text-brand-muted" size={18} />
            <input
              type="text"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              placeholder="Search 'milk', 'chips', 'atta', 'vegetables', 'chocolates'..."
              className="w-full pl-10 pr-4 py-2.5 bg-brand-bg border border-brand-border rounded-input text-sm text-brand-text placeholder-brand-muted focus:outline-none focus:ring-2 focus:ring-brand-primary focus:border-brand-primary transition-all font-medium"
            />
            {searchQuery && (
              <button
                onClick={() => setSearchQuery('')}
                className="absolute right-3 top-1/2 -translate-y-1/2 text-xs font-bold text-brand-muted hover:text-brand-text px-1"
              >
                Clear
              </button>
            )}
          </div>
        </div>

        {/* User Account & Cart Button */}
        <div className="flex items-center gap-3 shrink-0">
          {currentUser ? (
            <button
              onClick={() => setIsAccountOpen(true)}
              className="flex items-center gap-2 px-3 py-2 text-sm font-bold text-brand-text hover:text-brand-primary hover:bg-brand-bg rounded-button transition-colors border border-brand-border/80"
              title="Open My Profile, Addresses & Orders"
            >
              <div className="w-6 h-6 rounded-full bg-brand-primary text-white flex items-center justify-center font-black text-xs">
                {currentUser.name.charAt(0).toUpperCase()}
              </div>
              <span className="hidden sm:inline max-w-[100px] truncate">{currentUser.name.split(' ')[0]}</span>
              <ChevronDown size={14} className="text-brand-muted" />
            </button>
          ) : (
            <button
              onClick={() => setIsAuthOpen(true)}
              className="flex items-center gap-1.5 px-3 py-2 text-sm font-bold text-brand-text hover:text-brand-primary hover:bg-brand-bg rounded-button transition-colors border border-brand-border/80"
            >
              <User size={18} className="text-brand-primary" />
              <span>Login</span>
            </button>
          )}

          {/* Cart Pill Button */}
          <button
            onClick={() => setIsCartOpen(true)}
            className="flex items-center gap-2.5 px-4 py-2.5 bg-brand-primary hover:bg-brand-primaryHover text-white rounded-button font-bold text-sm shadow-card transition-all transform active:scale-95"
          >
            <div className="relative">
              <ShoppingCart size={19} />
              {cartCount > 0 && (
                <span className="absolute -top-2 -right-2 bg-brand-accent text-brand-text text-[10px] font-black w-4 h-4 rounded-full flex items-center justify-center shadow-sm">
                  {cartCount}
                </span>
              )}
            </div>
            <div className="flex flex-col items-start leading-tight">
              <span className="text-[11px] font-normal text-white/80">
                {cartCount > 0 ? `${cartCount} items` : 'My Cart'}
              </span>
              <span className="text-xs font-black text-brand-accent">
                {cartCount > 0 ? `₹${cartTotal}` : 'Empty'}
              </span>
            </div>
          </button>
        </div>
      </div>
    </header>
  );
};

export default Header;
