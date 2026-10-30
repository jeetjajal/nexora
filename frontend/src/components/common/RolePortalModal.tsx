'use client';

import React from 'react';
import { useApp, AppRole } from '@/context/AppContext';
import {
  ShoppingBag,
  Bike,
  Box,
  ShieldCheck,
  X,
  ArrowRight,
  Sparkles,
  CheckCircle2,
  Lock,
} from 'lucide-react';

interface PortalOption {
  id: AppRole;
  title: string;
  gujaratiTitle: string;
  badge: string;
  badgeColor: string;
  description: string;
  features: string[];
  icon: React.ReactNode;
  iconBg: string;
  loginButtonText: string;
}

export const RolePortalModal: React.FC = () => {
  const { isRolePortalOpen, setIsRolePortalOpen, role, setRole, currentUser, openAuthModal } = useApp();

  if (!isRolePortalOpen) return null;

  const portals: PortalOption[] = [
    {
      id: 'customer',
      title: 'Customer Storefront',
      gujaratiTitle: 'ગ્રાહક સ્ટોર (Customer)',
      badge: '10-Min Delivery',
      badgeColor: 'bg-green-100 text-green-800 border-green-200',
      description: 'Shop 31 categories, fresh vegetables, dairy, munchies and groceries with 8-10 minute lightning delivery.',
      features: ['31 Categories & Subcategories', 'Blinkit Dual-Column View', 'Order Again & Saved Addresses', 'Online Razorpay / COD'],
      icon: <ShoppingBag size={24} className="text-emerald-700" />,
      iconBg: 'bg-emerald-50 border-emerald-200',
      loginButtonText: 'ગ્રાહક લૉગિન / Continue',
    },
    {
      id: 'delivery',
      title: 'Delivery Partner App',
      gujaratiTitle: 'ડિલિવરી પાર્ટનર (Rider)',
      badge: 'Rider Duty',
      badgeColor: 'bg-amber-100 text-amber-800 border-amber-200',
      description: 'Dedicated rider workspace with duty toggle, live trip GPS navigation radar, doorstep 4-digit OTP and earnings.',
      features: ['Rider Onboarding / Registration', 'Live GPS Radar Route to Customer', 'Doorstep 4-Digit OTP Handshake', 'Earnings (Today / Week / Month / Year)'],
      icon: <Bike size={24} className="text-amber-700" />,
      iconBg: 'bg-amber-50 border-amber-200',
      loginButtonText: 'રાઇડર લૉગિન / Register',
    },
    {
      id: 'store',
      title: 'Store Operations Hub',
      gujaratiTitle: 'ડાર્ક સ્ટોર મેનેજર (Store Ops)',
      badge: 'Dark Store #04',
      badgeColor: 'bg-blue-100 text-blue-800 border-blue-200',
      description: 'Scoped micro-hub management for SG Highway Store #04. Complete fulfillment pipeline from order queue to rider dispatch.',
      features: ['Manager Hub Scoped to Store #04', 'Picker Station & Item Bin Locations', 'Packing Verification & Barcodes', 'Sales Volume (Daily / Weekly / Monthly / Yearly)'],
      icon: <Box size={24} className="text-blue-700" />,
      iconBg: 'bg-blue-50 border-blue-200',
      loginButtonText: 'સ્ટોર મેનેજર લૉગિન',
    },
    {
      id: 'admin',
      title: 'Super Admin Control Tower',
      gujaratiTitle: 'સુપર એડમિન (Control Tower)',
      badge: 'Surveillance',
      badgeColor: 'bg-purple-100 text-purple-800 border-purple-200',
      description: 'Central command for executive leadership. Live multi-store surveillance, active rider fleet radar and net revenue.',
      features: ['Live Dark Stores Map & Fleet Radar', 'All-City Order Lifecycle Surveillance', 'Company Financials & Net Revenue', 'Surge Pricing & Catalog Controls'],
      icon: <ShieldCheck size={24} className="text-purple-700" />,
      iconBg: 'bg-purple-50 border-purple-200',
      loginButtonText: 'સુપર એડમિન લૉગિન',
    },
  ];

  const handleSelect = (selectedRole: AppRole) => {
    setIsRolePortalOpen(false);
    openAuthModal(selectedRole);
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-3 sm:p-4 animate-in fade-in duration-150">
      {/* Backdrop */}
      <div
        className="fixed inset-0 bg-brand-text/70 backdrop-blur-sm"
        onClick={() => setIsRolePortalOpen(false)}
      />

      {/* Modal Container */}
      <div className="relative w-full max-w-3xl bg-white rounded-card shadow-2xl z-10 overflow-hidden border border-brand-border flex flex-col max-h-[92vh]">
        {/* Modal Header */}
        <div className="bg-gradient-to-r from-orange-600 via-amber-600 to-emerald-700 text-white p-4 sm:p-5 flex items-center justify-between shrink-0">
          <div className="flex items-center gap-2.5">
            <div className="w-9 h-9 rounded-full bg-white text-orange-600 flex items-center justify-center font-black text-base shadow-sm">
              ⚡
            </div>
            <div>
              <h3 className="text-base sm:text-lg font-black tracking-tight">
                NEXORA 2FA Portal Access · પોર્ટલ પસંદ કરો
              </h3>
              <p className="text-[11px] sm:text-xs text-white/90">
                પોતાના રોલ મુજબ લૉગિન કરો — લૉગિન થતાં જ સીધો તે જ સેક્શન દેખાશે
              </p>
            </div>
          </div>
          <button
            onClick={() => setIsRolePortalOpen(false)}
            className="p-1.5 rounded-full hover:bg-white/10 text-white transition-colors"
            title="Close"
          >
            <X size={19} />
          </button>
        </div>

        {/* Portal Grid */}
        <div className="p-4 sm:p-6 overflow-y-auto grid grid-cols-1 md:grid-cols-2 gap-3.5 sm:gap-4 bg-brand-bg/40">
          {portals.map((p) => {
            const isCurrent = role === p.id;
            return (
              <div
                key={p.id}
                onClick={() => handleSelect(p.id)}
                className={`relative p-4 sm:p-5 rounded-card border bg-white cursor-pointer transition-all hover:shadow-md hover:-translate-y-0.5 flex flex-col justify-between ${
                  isCurrent
                    ? 'border-orange-500 ring-2 ring-orange-400/30 shadow-sm'
                    : 'border-brand-border hover:border-orange-400/60'
                }`}
              >
                {/* Active Indicator */}
                {isCurrent && (
                  <div className="absolute top-3 right-3 flex items-center gap-1 bg-orange-500 text-white text-[10px] font-black px-2 py-0.5 rounded-full shadow-xs">
                    <CheckCircle2 size={11} />
                    <span>Active Workspace</span>
                  </div>
                )}

                <div>
                  {/* Icon & Title */}
                  <div className="flex items-start gap-3 mb-2.5">
                    <div className={`p-2.5 rounded-xl border shrink-0 ${p.iconBg}`}>
                      {p.icon}
                    </div>
                    <div>
                      <div className="flex items-center gap-2">
                        <h4 className="font-black text-sm text-brand-text">
                          {p.title}
                        </h4>
                        <span className={`text-[9px] font-bold px-1.5 py-0.2 rounded-full border ${p.badgeColor}`}>
                          {p.badge}
                        </span>
                      </div>
                      <p className="text-xs font-bold text-orange-600 mt-0.5">
                        {p.gujaratiTitle}
                      </p>
                    </div>
                  </div>

                  {/* Description */}
                  <p className="text-[11px] text-brand-muted leading-relaxed mb-3">
                    {p.description}
                  </p>

                  {/* Highlights */}
                  <ul className="space-y-1 mb-4 text-[11px] text-brand-text/90">
                    {p.features.map((feat, idx) => (
                      <li key={idx} className="flex items-center gap-1.5">
                        <span className="w-1.5 h-1.5 rounded-full bg-orange-500 shrink-0" />
                        <span>{feat}</span>
                      </li>
                    ))}
                  </ul>
                </div>

                {/* Enter Button */}
                <button
                  type="button"
                  className={`w-full py-2.5 px-3 rounded-button font-bold text-xs flex items-center justify-center gap-1.5 transition-all shadow-xs ${
                    isCurrent
                      ? 'bg-orange-500 text-white hover:brightness-105'
                      : 'bg-brand-bg hover:bg-orange-500 hover:text-white text-brand-text border border-brand-border'
                  }`}
                >
                  <Lock size={12} />
                  <span>{isCurrent ? 'Continue Workspace →' : p.loginButtonText}</span>
                  <ArrowRight size={13} />
                </button>
              </div>
            );
          })}
        </div>

        {/* Modal Footer Note */}
        <div className="p-3 bg-white border-t border-brand-border text-center text-[11px] text-brand-muted shrink-0">
          ગ્રાહક સીધા સ્ટોરફ્રન્ટ પર શોપિંગ કરી શકે છે. સ્ટાફ અને પાર્ટનર્સ આ પોર્ટલ અથવા લૉગિન પેજ પરથી સીધા પોતાના ડેડિકેટેડ સેક્શનમાં પ્રવેશી શકે છે.
        </div>
      </div>
    </div>
  );
};

export default RolePortalModal;
