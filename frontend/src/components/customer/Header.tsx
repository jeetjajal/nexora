'use client';

import React from 'react';
import { useApp } from '@/context/AppContext';
import { NexoraLogo } from '@/components/brand/Logo';
import { Search, MapPin, ShoppingCart, User, ChevronDown, Clock } from 'lucide-react';

export const Header: React.FC = () => {
  const {
    currentUser,
    savedAddresses,
    customerTab,
    setCustomerTab,
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

  const activeAddr = savedAddresses.find((a) => a.isDefault) || savedAddresses[0];

  return (
    <header className="bg-white border-b border-brand-border sticky top-[33px] z-40 shadow-sm transition-all">
      <div className="max-w-7xl mx-auto px-3 sm:px-4 py-2 sm:py-3">
        {/* Top Flex Row */}
        <div className="flex items-center justify-between gap-3">
          {/* Brand Logo & Tagline */}
          <div className="flex items-center gap-4 shrink-0">
            <NexoraLogo size="sm" showSubtitle={false} className="sm:hidden" />
            <NexoraLogo size="md" showSubtitle={true} className="hidden sm:flex" />

            {/* Location Delivery Selector (Desktop) */}
            <div
              onClick={() => (currentUser ? setIsAccountOpen(true) : setIsAuthOpen(true))}
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

            {/* Desktop Navigation Shortcuts */}
            <div className="hidden xl:flex items-center gap-1.5 ml-1 border-l border-brand-border/80 pl-2">
              <button
                onClick={() => setCustomerTab('home')}
                className={`px-2.5 py-1 rounded-full text-xs font-bold transition-all ${
                  customerTab === 'home' ? 'bg-brand-primary text-white shadow-xs' : 'text-brand-muted hover:text-brand-text hover:bg-brand-bg'
                }`}
              >
                Home
              </button>
              <button
                onClick={() => setCustomerTab('order_again')}
                className={`px-2.5 py-1 rounded-full text-xs font-bold transition-all ${
                  customerTab === 'order_again' ? 'bg-brand-primary text-white shadow-xs' : 'text-brand-muted hover:text-brand-text hover:bg-brand-bg'
                }`}
              >
                Order Again
              </button>
              <button
                onClick={() => setCustomerTab('categories')}
                className={`px-2.5 py-1 rounded-full text-xs font-bold transition-all ${
                  customerTab === 'categories' ? 'bg-brand-primary text-white shadow-xs' : 'text-brand-muted hover:text-brand-text hover:bg-brand-bg'
                }`}
              >
                31 Categories
              </button>
            </div>
          </div>

          {/* Desktop Global Search Bar */}
          <div className="hidden md:flex flex-1 max-w-xl mx-2">
            <div className="relative w-full">
              <Search className="absolute left-3.5 top-1/2 -translate-y-1/2 text-brand-muted" size={18} />
              <input
                type="text"
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                placeholder="Search 'milk', 'chips', 'atta', 'tools', 'umbrella'..."
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

          {/* User Account & Cart Actions */}
          <div className="flex items-center gap-2 sm:gap-3 shrink-0">
            {currentUser ? (
              <button
                onClick={() => setCustomerTab('profile')}
                className="flex items-center gap-1.5 px-2.5 sm:px-3 py-1.5 sm:py-2 text-xs sm:text-sm font-bold text-brand-text hover:text-brand-primary hover:bg-brand-bg rounded-button transition-colors border border-brand-border/80"
                title="Open My Profile, Addresses & Orders"
              >
                <div className="w-6 h-6 rounded-full bg-brand-primary text-white flex items-center justify-center font-black text-xs">
                  {currentUser.name.charAt(0).toUpperCase()}
                </div>
                <span className="hidden sm:inline max-w-[90px] truncate">{currentUser.name.split(' ')[0]}</span>
                <ChevronDown size={13} className="text-brand-muted hidden sm:inline" />
              </button>
            ) : (
              <button
                onClick={() => setIsAuthOpen(true)}
                className="flex items-center gap-1 px-2.5 sm:px-3 py-1.5 sm:py-2 text-xs sm:text-sm font-bold text-brand-text hover:text-brand-primary hover:bg-brand-bg rounded-button transition-colors border border-brand-border/80"
              >
                <User size={16} className="text-brand-primary" />
                <span>Login</span>
              </button>
            )}

            {/* Cart Pill Button */}
            <button
              onClick={() => setIsCartOpen(true)}
              className="flex items-center gap-2 px-3 sm:px-4 py-1.5 sm:py-2.5 bg-brand-primary hover:bg-[#125638] text-white rounded-button font-black text-xs sm:text-sm shadow-card transition-all active:scale-95"
            >
              <div className="relative">
                <ShoppingCart size={17} />
                {cartCount > 0 && (
                  <span className="absolute -top-2 -right-2 bg-brand-accent text-brand-text text-[9px] font-black w-4 h-4 rounded-full flex items-center justify-center shadow-xs">
                    {cartCount}
                  </span>
                )}
              </div>
              <div className="flex flex-col text-left leading-none">
                <span className="hidden sm:inline font-bold text-[11px] text-white/90">
                  {cartCount === 0 ? 'My Cart' : `${cartCount} items`}
                </span>
                <span className="font-black text-xs sm:text-sm text-brand-accent">
                  ₹{cartTotal}
                </span>
              </div>
            </button>
          </div>
        </div>

        {/* Mobile Full-Width Search Bar & Location Chip */}
        <div className="mt-2 md:hidden space-y-1.5">
          <div className="relative w-full">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-brand-muted" size={16} />
            <input
              type="text"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              placeholder="Search across 43 categories & 46 products..."
              className="w-full pl-9 pr-8 py-2 bg-brand-bg border border-brand-border rounded-input text-xs text-brand-text placeholder-brand-muted focus:outline-none focus:ring-2 focus:ring-brand-primary transition-all font-medium"
            />
            {searchQuery && (
              <button
                onClick={() => setSearchQuery('')}
                className="absolute right-2.5 top-1/2 -translate-y-1/2 text-[11px] font-bold text-brand-muted hover:text-brand-text"
              >
                Clear
              </button>
            )}
          </div>

          <div
            onClick={() => (currentUser ? setIsAccountOpen(true) : setIsAuthOpen(true))}
            className="flex items-center justify-between text-[11px] bg-brand-bg/60 border border-brand-border/60 px-2.5 py-1 rounded-lg text-brand-muted cursor-pointer"
          >
            <div className="flex items-center gap-1.5 truncate">
              <MapPin size={11} className="text-brand-primary shrink-0" />
              <span className="truncate text-brand-text font-medium">
                {activeAddr ? `${activeAddr.addressLine1}, ${activeAddr.city}` : 'B-402, Shivalik Residency, Ahmedabad'}
              </span>
            </div>
            <span className="text-[10px] bg-brand-primary/10 text-brand-primary font-black px-1.5 py-0.2 rounded shrink-0">
              ⚡ {activeAddr?.eta || '9 MINS'}
            </span>
          </div>
        </div>
      </div>
    </header>
  );
};

export default Header;
