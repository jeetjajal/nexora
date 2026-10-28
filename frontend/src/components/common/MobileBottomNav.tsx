'use client';

import React from 'react';
import { useApp, AppRole } from '@/context/AppContext';
import { ShoppingBag, Bike, ShieldCheck, Box, ShoppingCart, ArrowRight } from 'lucide-react';

export const MobileBottomNav: React.FC = () => {
  const { role, setRole, getCartCount, getCartTotal, setIsCartOpen } = useApp();

  const cartCount = getCartCount();
  const cartTotal = getCartTotal();

  const navItems: { id: AppRole; label: string; icon: React.ReactNode; badge?: string }[] = [
    { id: 'customer', label: 'Store', icon: <ShoppingBag size={19} /> },
    { id: 'delivery', label: 'Rider', icon: <Bike size={19} />, badge: 'Duty' },
    { id: 'store', label: 'Store Ops', icon: <Box size={19} />, badge: 'Hub' },
    { id: 'admin', label: 'Admin', icon: <ShieldCheck size={19} /> },
  ];

  const handleNavClick = (newRole: AppRole) => {
    setRole(newRole);
    if (typeof window !== 'undefined' && window.location.pathname !== '/') {
      window.location.href = '/';
    }
  };

  return (
    <>
      {/* Floating Cart Bar on Mobile when items are in cart & in customer role */}
      {role === 'customer' && cartCount > 0 && (
        <div className="md:hidden fixed bottom-[64px] left-3 right-3 z-40 animate-in slide-in-from-bottom-2 duration-200">
          <button
            onClick={() => setIsCartOpen(true)}
            className="w-full bg-brand-primary text-white rounded-xl px-4 py-2.5 shadow-2xl flex items-center justify-between cursor-pointer active:scale-98 transition-all border border-brand-accent/30"
          >
            <div className="flex items-center gap-2.5">
              <div className="w-8 h-8 rounded-lg bg-brand-accent text-brand-text flex items-center justify-center font-black text-xs">
                {cartCount}
              </div>
              <div className="text-left">
                <div className="text-xs font-black tracking-tight flex items-center gap-1.5">
                  <span>{cartCount} {cartCount === 1 ? 'item' : 'items'}</span>
                  <span>&bull;</span>
                  <span className="text-brand-accent">₹{cartTotal}</span>
                </div>
                <div className="text-[10px] text-white/80 font-medium">
                  Delivered in 10 minutes flat
                </div>
              </div>
            </div>

            <div className="flex items-center gap-1 bg-white/20 hover:bg-white/30 text-white font-bold text-xs px-3 py-1.5 rounded-lg transition-colors">
              <span>View Cart</span>
              <ArrowRight size={14} />
            </div>
          </button>
        </div>
      )}

      {/* Main Persistent Mobile Bottom Navigation Bar */}
      <nav className="md:hidden fixed bottom-0 left-0 right-0 z-40 bg-white/95 backdrop-blur-md border-t border-brand-border/80 shadow-[0_-4px_20px_rgba(0,0,0,0.08)] px-1 py-1.5 pb-safe">
        <div className="flex items-center justify-around max-w-md mx-auto">
          {navItems.map((item) => {
            const isActive = role === item.id;
            return (
              <button
                key={item.id}
                onClick={() => handleNavClick(item.id)}
                className={`flex flex-col items-center justify-center flex-1 py-1 px-1 rounded-lg transition-all ${
                  isActive
                    ? 'text-brand-primary font-black scale-105'
                    : 'text-brand-muted hover:text-brand-text font-medium'
                }`}
              >
                <div className="relative">
                  {item.icon}
                  {item.badge && (
                    <span className="absolute -top-1 -right-2 text-[8px] px-1 py-0 rounded-full font-bold bg-brand-accent text-brand-text shadow-xs">
                      {item.badge}
                    </span>
                  )}
                </div>
                <span className="text-[10px] mt-0.5 tracking-tight font-bold">{item.label}</span>
                {isActive && (
                  <span className="w-1.5 h-1.5 rounded-full bg-brand-primary mt-0.5" />
                )}
              </button>
            );
          })}

          {/* Quick Cart Drawer Trigger */}
          <button
            onClick={() => setIsCartOpen(true)}
            className="flex flex-col items-center justify-center flex-1 py-1 px-1 text-brand-text hover:text-brand-primary font-medium transition-all"
          >
            <div className="relative">
              <ShoppingCart size={19} />
              {cartCount > 0 && (
                <span className="absolute -top-1.5 -right-2 bg-brand-accent text-brand-text text-[9px] font-black w-4 h-4 rounded-full flex items-center justify-center shadow-xs">
                  {cartCount}
                </span>
              )}
            </div>
            <span className="text-[10px] mt-0.5 tracking-tight font-bold">
              {cartCount > 0 ? `₹${cartTotal}` : 'Cart'}
            </span>
          </button>
        </div>
      </nav>
    </>
  );
};

export default MobileBottomNav;
