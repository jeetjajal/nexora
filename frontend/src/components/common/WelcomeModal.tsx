'use client';

import React from 'react';
import { useApp, AppRole } from '@/context/AppContext';
import { NexoraLogo } from '@/components/brand/Logo';
import { ShoppingBag, Bike, ShieldCheck, ArrowRight, Zap, CheckCircle2 } from 'lucide-react';

interface WelcomeModalProps {
  isOpen: boolean;
  onClose: () => void;
}

export const WelcomeModal: React.FC<WelcomeModalProps> = ({ isOpen, onClose }) => {
  const { setRole } = useApp();

  if (!isOpen) return null;

  const handleSelectRole = (selectedRole: AppRole) => {
    setRole(selectedRole);
    onClose();
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
      {/* Backdrop */}
      <div className="fixed inset-0 bg-brand-text/60 backdrop-blur-sm" />

      {/* Modal Dialog */}
      <div className="relative w-full max-w-lg bg-white rounded-card shadow-modal z-10 overflow-hidden border border-brand-border animate-in zoom-in-95 duration-200">
        {/* Decorative Top Accent */}
        <div className="h-2 bg-gradient-to-r from-brand-primary via-brand-accent to-brand-primary" />

        <div className="p-6 text-center">
          {/* Logo */}
          <div className="flex justify-center mb-3">
            <NexoraLogo size="lg" showSubtitle={true} />
          </div>

          <h2 className="text-xl font-black text-brand-text mb-1">
            Welcome to Nexora
          </h2>
          <p className="text-xs text-brand-muted max-w-sm mx-auto mb-6">
            India's next-generation 10-minute quick commerce platform. Choose how you want to experience the platform:
          </p>

          {/* Role Cards */}
          <div className="space-y-3 text-left">
            {/* Customer Option */}
            <div
              onClick={() => handleSelectRole('customer')}
              className="p-4 rounded-card border-2 border-brand-primary/20 hover:border-brand-primary bg-brand-bg/50 hover:bg-brand-primary/5 cursor-pointer transition-all flex items-center justify-between group shadow-sm hover:shadow-card"
            >
              <div className="flex items-center gap-3.5">
                <div className="w-11 h-11 rounded-full bg-brand-primary text-white flex items-center justify-center shrink-0 shadow-sm group-hover:scale-105 transition-transform">
                  <ShoppingBag size={20} />
                </div>
                <div>
                  <div className="flex items-center gap-2">
                    <h3 className="text-sm font-black text-brand-text">Continue as Customer</h3>
                    <span className="text-[10px] bg-brand-accent text-brand-text font-black px-1.5 py-0.2 rounded">
                      10-Min Fast
                    </span>
                  </div>
                  <p className="text-xs text-brand-muted mt-0.5">
                    Shop groceries and daily essentials delivered to your doorstep.
                  </p>
                </div>
              </div>
              <ArrowRight size={18} className="text-brand-muted group-hover:text-brand-primary group-hover:translate-x-1 transition-all shrink-0 ml-2" />
            </div>

            {/* Delivery Partner Option */}
            <div
              onClick={() => handleSelectRole('delivery')}
              className="p-4 rounded-card border-2 border-brand-border hover:border-brand-primary bg-white hover:bg-brand-primary/5 cursor-pointer transition-all flex items-center justify-between group shadow-sm hover:shadow-card"
            >
              <div className="flex items-center gap-3.5">
                <div className="w-11 h-11 rounded-full bg-emerald-600 text-white flex items-center justify-center shrink-0 shadow-sm group-hover:scale-105 transition-transform">
                  <Bike size={20} />
                </div>
                <div>
                  <div className="flex items-center gap-2">
                    <h3 className="text-sm font-black text-brand-text">Continue as Delivery Partner</h3>
                    <span className="text-[10px] bg-emerald-100 text-emerald-800 font-bold px-1.5 py-0.2 rounded">
                      Rider Mode
                    </span>
                  </div>
                  <p className="text-xs text-brand-muted mt-0.5">
                    Deliver orders, manage deliveries and track your daily earnings.
                  </p>
                </div>
              </div>
              <ArrowRight size={18} className="text-brand-muted group-hover:text-brand-primary group-hover:translate-x-1 transition-all shrink-0 ml-2" />
            </div>

            {/* Super Admin & Control Tower Option */}
            <div
              onClick={() => handleSelectRole('admin')}
              className="p-4 rounded-card border-2 border-brand-border hover:border-brand-primary bg-white hover:bg-brand-primary/5 cursor-pointer transition-all flex items-center justify-between group shadow-sm hover:shadow-card"
            >
              <div className="flex items-center gap-3.5">
                <div className="w-11 h-11 rounded-full bg-brand-text text-brand-accent flex items-center justify-center shrink-0 shadow-sm group-hover:scale-105 transition-transform">
                  <ShieldCheck size={20} />
                </div>
                <div>
                  <div className="flex items-center gap-2">
                    <h3 className="text-sm font-black text-brand-text">Super Admin & Control Tower</h3>
                    <span className="text-[10px] bg-purple-100 text-purple-800 font-bold px-1.5 py-0.2 rounded">
                      21 Modules
                    </span>
                  </div>
                  <p className="text-xs text-brand-muted mt-0.5">
                    Monitor dark stores, fleet dispatch, catalog CMS & finance.
                  </p>
                </div>
              </div>
              <ArrowRight size={18} className="text-brand-muted group-hover:text-brand-primary group-hover:translate-x-1 transition-all shrink-0 ml-2" />
            </div>
          </div>

          <div className="mt-5 pt-4 border-t border-brand-border flex items-center justify-center gap-4 text-[11px] text-brand-muted font-medium">
            <span className="flex items-center gap-1">
              <CheckCircle2 size={13} className="text-brand-primary" /> Leaf Green #176B45 Theme
            </span>
            <span>•</span>
            <span>Switch roles anytime via top header bar</span>
          </div>
        </div>
      </div>
    </div>
  );
};

export default WelcomeModal;
