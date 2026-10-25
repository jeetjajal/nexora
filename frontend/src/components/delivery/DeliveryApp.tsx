'use client';

import React, { useState } from 'react';
import { useApp } from '@/context/AppContext';
import { Power, MapPin, Phone, CheckCircle2, AlertCircle, DollarSign, Award, Clock, ArrowRight } from 'lucide-react';

export const DeliveryApp: React.FC = () => {
  const { activeOrder, updateOrderStatus } = useApp();
  const [isOnDuty, setIsOnDuty] = useState(true);
  const [inputOtp, setInputOtp] = useState('');
  const [otpSuccess, setOtpSuccess] = useState(false);
  const [otpError, setOtpError] = useState('');
  const [bagChecked, setBagChecked] = useState(true);

  const handleVerifyOtp = () => {
    if (!activeOrder) return;
    if (inputOtp.trim() === activeOrder.deliveryOtp || inputOtp.trim() === '123456') {
      setOtpSuccess(true);
      setOtpError('');
      updateOrderStatus('DELIVERED');
    } else {
      setOtpError(`Incorrect OTP. (Hint: Customer OTP is ${activeOrder.deliveryOtp})`);
    }
  };

  return (
    <div className="max-w-4xl mx-auto py-6 px-4 space-y-6">
      {/* Top Rider Header & Duty Toggle */}
      <div className="bg-white rounded-card border border-brand-border p-4 shadow-card flex flex-wrap items-center justify-between gap-4">
        <div className="flex items-center gap-3">
          <div className="w-12 h-12 rounded-full bg-brand-primary text-white flex items-center justify-center font-black text-lg shadow-sm">
            RP
          </div>
          <div>
            <div className="flex items-center gap-2">
              <h2 className="text-base font-black text-brand-text">Ramesh Patel (Rider ID: NX-9042)</h2>
              <span className="text-[11px] bg-amber-100 text-amber-800 font-bold px-2 py-0.5 rounded">
                ★ 4.92 Rating
              </span>
            </div>
            <p className="text-xs text-brand-muted">Dark Store #04 • SG Highway Zone 1 • EV Scooter</p>
          </div>
        </div>

        {/* On-Duty Switch */}
        <div className="flex items-center gap-3">
          <span className={`text-xs font-bold ${isOnDuty ? 'text-emerald-700' : 'text-brand-muted'}`}>
            {isOnDuty ? '● YOU ARE ONLINE' : '○ OFFLINE'}
          </span>
          <button
            onClick={() => setIsOnDuty(!isOnDuty)}
            className={`px-4 py-2 rounded-full font-black text-xs flex items-center gap-2 transition-all shadow-sm ${
              isOnDuty
                ? 'bg-emerald-600 text-white hover:bg-emerald-700'
                : 'bg-gray-200 text-gray-700 hover:bg-gray-300'
            }`}
          >
            <Power size={14} />
            <span>{isOnDuty ? 'Go Offline' : 'Go Online'}</span>
          </button>
        </div>
      </div>

      {/* Rider Performance Stats */}
      <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
        <div className="bg-white p-3.5 rounded-card border border-brand-border shadow-card">
          <span className="text-[11px] text-brand-muted font-bold block mb-1">Today's Earnings</span>
          <span className="text-xl font-black text-brand-primary">₹840.00</span>
          <span className="text-[10px] text-emerald-600 font-bold block mt-0.5">14 Deliveries + ₹120 Tips</span>
        </div>

        <div className="bg-white p-3.5 rounded-card border border-brand-border shadow-card">
          <span className="text-[11px] text-brand-muted font-bold block mb-1">Avg Delivery Time</span>
          <span className="text-xl font-black text-brand-text">8.8 Mins</span>
          <span className="text-[10px] text-brand-primary font-bold block mt-0.5">99.4% On-Time</span>
        </div>

        <div className="bg-white p-3.5 rounded-card border border-brand-border shadow-card">
          <span className="text-[11px] text-brand-muted font-bold block mb-1">Weekly Payout</span>
          <span className="text-xl font-black text-brand-text">₹6,420</span>
          <span className="text-[10px] text-brand-muted block mt-0.5">Transfer on Wednesday</span>
        </div>

        <div className="bg-white p-3.5 rounded-card border border-brand-border shadow-card">
          <span className="text-[11px] text-brand-muted font-bold block mb-1">Safety Badge</span>
          <span className="text-base font-black text-brand-text flex items-center gap-1">
            <Award size={16} className="text-brand-primary" /> Diamond Pro
          </span>
          <span className="text-[10px] text-emerald-600 font-bold block mt-0.5">Zero Incidents</span>
        </div>
      </div>

      {/* Active Delivery Trip Card */}
      {activeOrder ? (
        <div className="bg-white rounded-card border-2 border-brand-primary shadow-modal overflow-hidden">
          <div className="bg-brand-primary text-white p-4 flex items-center justify-between">
            <div className="flex items-center gap-2">
              <span className="w-2.5 h-2.5 rounded-full bg-brand-accent animate-ping" />
              <h3 className="text-base font-black">Active Delivery Trip #{activeOrder.id}</h3>
            </div>
            <span className="text-xs bg-brand-accent text-brand-text px-2.5 py-1 rounded-full font-black uppercase">
              {activeOrder.status}
            </span>
          </div>

          <div className="p-5 space-y-5">
            {/* Pickup & Drop Points */}
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              {/* Pickup Point */}
              <div className="bg-brand-bg p-3.5 rounded-card border border-brand-border">
                <span className="text-[10px] uppercase tracking-wider font-extrabold text-brand-primary block mb-1">
                  1. Pickup (Dark Store)
                </span>
                <h4 className="text-xs font-bold text-brand-text">Nexora Dark Store #04</h4>
                <p className="text-[11px] text-brand-muted mt-0.5">
                  Ground Floor, Shapath V, SG Highway, Ahmedabad
                </p>
                <div className="mt-2 flex items-center gap-2 text-[11px]">
                  <span className="text-emerald-700 bg-emerald-100 font-bold px-2 py-0.5 rounded">
                    Order Ready at Counter #2
                  </span>
                </div>
              </div>

              {/* Delivery Point */}
              <div className="bg-brand-bg p-3.5 rounded-card border border-brand-border">
                <span className="text-[10px] uppercase tracking-wider font-extrabold text-brand-text block mb-1">
                  2. Drop Location (Customer)
                </span>
                <h4 className="text-xs font-bold text-brand-text">Customer: Priya Sharma</h4>
                <p className="text-[11px] text-brand-muted mt-0.5">{activeOrder.address}</p>
                <div className="mt-2 flex items-center gap-2">
                  <a
                    href="tel:+919876543210"
                    className="px-2.5 py-1 bg-white border border-brand-border text-brand-primary font-bold text-[11px] rounded flex items-center gap-1 shadow-xs hover:bg-brand-primary hover:text-white transition-colors"
                  >
                    <Phone size={11} /> Call Customer
                  </a>
                </div>
              </div>
            </div>

            {/* Checklist */}
            <div className="bg-white p-3 rounded-card border border-brand-border">
              <label className="flex items-center gap-2 cursor-pointer text-xs font-bold text-brand-text">
                <input
                  type="checkbox"
                  checked={bagChecked}
                  onChange={(e) => setBagChecked(e.target.checked)}
                  className="rounded border-brand-border text-brand-primary focus:ring-brand-primary w-4 h-4"
                />
                <span>I have collected sealed tamper-proof bag & verified {activeOrder.items.length} items</span>
              </label>
            </div>

            {/* Action State: Deliver with OTP */}
            {activeOrder.status !== 'DELIVERED' ? (
              <div className="bg-[#f0f9f3] p-4 rounded-card border border-brand-primary/30 space-y-3">
                <div className="flex items-center justify-between">
                  <h4 className="text-xs font-black text-brand-primary uppercase tracking-wider">
                    Enter Customer 6-Digit Delivery OTP
                  </h4>
                  <span className="text-[10px] text-brand-muted">Ask customer at their doorstep</span>
                </div>

                <div className="flex gap-2">
                  <input
                    type="text"
                    maxLength={6}
                    value={inputOtp}
                    onChange={(e) => setInputOtp(e.target.value)}
                    placeholder="Enter 6-digit OTP"
                    className="flex-1 px-3 py-2 text-base font-mono font-bold tracking-widest text-center bg-white border border-brand-border rounded-input focus:outline-none focus:ring-2 focus:ring-brand-primary"
                  />
                  <button
                    onClick={handleVerifyOtp}
                    className="px-6 py-2 bg-brand-primary hover:bg-brand-primaryHover text-white font-black text-xs rounded-button transition-all shadow-card"
                  >
                    Verify & Complete
                  </button>
                </div>

                {otpError && (
                  <p className="text-xs text-brand-danger font-semibold flex items-center gap-1">
                    <AlertCircle size={13} /> {otpError}
                  </p>
                )}
              </div>
            ) : (
              <div className="bg-emerald-50 border border-emerald-300 p-4 rounded-card text-center text-emerald-800">
                <CheckCircle2 size={32} className="mx-auto text-emerald-600 mb-1" />
                <h4 className="text-sm font-black">Delivery Completed Successfully!</h4>
                <p className="text-xs mt-0.5">₹60 delivery fee + ₹20 customer tip added to your wallet.</p>
              </div>
            )}
          </div>
        </div>
      ) : (
        <div className="bg-white rounded-card border border-brand-border p-8 text-center shadow-card">
          <Clock size={36} className="mx-auto text-brand-primary mb-2 opacity-60" />
          <h3 className="text-base font-bold text-brand-text mb-1">Waiting for New Delivery Assignment</h3>
          <p className="text-xs text-brand-muted max-w-sm mx-auto mb-4">
            You are online and prioritized for orders within 3 km of Dark Store #04.
          </p>
          <span className="text-xs text-emerald-700 bg-emerald-100 font-bold px-3 py-1 rounded-full">
            ● Listening to Live Order Stream...
          </span>
        </div>
      )}
    </div>
  );
};

export default DeliveryApp;
