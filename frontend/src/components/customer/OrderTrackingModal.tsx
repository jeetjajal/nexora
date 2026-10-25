'use client';

import React from 'react';
import { useApp } from '@/context/AppContext';
import { X, CheckCircle2, Phone, MapPin, Bike, PackageCheck, Clock, ShieldAlert, Sparkles } from 'lucide-react';

export const OrderTrackingModal: React.FC = () => {
  const {
    isTrackingOpen,
    setIsTrackingOpen,
    activeOrder,
    updateOrderStatus,
  } = useApp();

  if (!isTrackingOpen || !activeOrder) return null;

  const steps = [
    { key: 'PLACED', label: 'Order Confirmed', time: 'Just now', desc: 'Received at Dark Store #04 SG Highway' },
    { key: 'STORE_PICKING', label: 'Picking Items', time: '1 min ago', desc: 'Picker barcode-scanning items' },
    { key: 'PACKED', label: 'Bag Packed & Sealed', time: '3 mins ago', desc: 'Quality checked and ready for rider' },
    { key: 'OUT_FOR_DELIVERY', label: 'Out for Delivery', time: 'On the way', desc: 'Rider is 3.2 km away' },
    { key: 'DELIVERED', label: 'Delivered', time: 'Completed', desc: 'Handed over at doorstep' },
  ];

  const currentIdx = steps.findIndex(s => s.key === activeOrder.status);

  const advanceStatus = () => {
    if (currentIdx < steps.length - 1) {
      updateOrderStatus(steps[currentIdx + 1].key as any);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
      {/* Backdrop */}
      <div
        className="fixed inset-0 bg-brand-text/60 backdrop-blur-sm"
        onClick={() => setIsTrackingOpen(false)}
      />

      {/* Modal */}
      <div className="relative w-full max-w-lg bg-white rounded-card shadow-modal z-10 overflow-hidden border border-brand-border animate-in zoom-in-95 duration-150">
        {/* Header */}
        <div className="bg-brand-primary text-white p-4 flex items-center justify-between">
          <div className="flex items-center gap-2">
            <span className="w-8 h-8 rounded-full bg-brand-accent text-brand-text flex items-center justify-center font-black">
              ⚡
            </span>
            <div>
              <div className="flex items-center gap-2">
                <h3 className="text-base font-black">Live Order Tracker</h3>
                <span className="text-[10px] bg-brand-accent text-brand-text px-2 py-0.5 rounded-full font-black">
                  {activeOrder.id}
                </span>
              </div>
              <p className="text-[11px] text-white/80">
                {activeOrder.status === 'DELIVERED' ? 'Successfully Delivered' : `Arriving in ~${activeOrder.etaMinutes} Mins`}
              </p>
            </div>
          </div>
          <button
            onClick={() => setIsTrackingOpen(false)}
            className="p-1.5 rounded-full hover:bg-white/10 text-white"
          >
            <X size={18} />
          </button>
        </div>

        {/* Live Status Map / Graphic View */}
        <div className="bg-[#eef5ee] border-b border-brand-border p-4 relative overflow-hidden">
          <div className="flex items-center justify-between mb-3">
            <div>
              <p className="text-xs font-bold text-brand-text">Delivery OTP (Share with rider)</p>
              <div className="flex items-center gap-1 mt-1">
                <span className="text-xl font-black tracking-widest text-brand-primary font-mono bg-white px-3 py-1 rounded border border-brand-border shadow-sm">
                  {activeOrder.deliveryOtp}
                </span>
                <span className="text-[10px] text-brand-muted ml-2">Keep this ready for doorstep verification</span>
              </div>
            </div>
            <div className="text-right">
              <span className="inline-flex items-center gap-1 text-[11px] font-bold text-emerald-700 bg-emerald-100 px-2 py-0.5 rounded-full">
                <span className="w-2 h-2 rounded-full bg-emerald-500 animate-ping" />
                Live Dispatch
              </span>
            </div>
          </div>

          {/* Rider Profile Card */}
          <div className="bg-white p-3 rounded-card border border-brand-border shadow-sm flex items-center justify-between">
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 rounded-full bg-brand-primary/10 text-brand-primary flex items-center justify-center font-black text-sm">
                🛵
              </div>
              <div>
                <div className="flex items-center gap-1.5">
                  <h4 className="text-xs font-bold text-brand-text">{activeOrder.riderName}</h4>
                  <span className="text-[10px] bg-amber-100 text-amber-800 font-bold px-1.5 py-0.2 rounded">
                    ★ {activeOrder.riderRating}
                  </span>
                </div>
                <p className="text-[10px] text-brand-muted">Nexora Speed Partner • Electric EV</p>
              </div>
            </div>

            <a
              href={`tel:${activeOrder.riderPhone}`}
              className="px-3 py-1.5 bg-brand-primary/10 hover:bg-brand-primary hover:text-white text-brand-primary rounded-button text-xs font-bold flex items-center gap-1 transition-colors"
            >
              <Phone size={12} />
              <span>Call Rider</span>
            </a>
          </div>
        </div>

        {/* Progress Tracker Steps */}
        <div className="p-5 max-h-[40vh] overflow-y-auto space-y-4">
          <div className="relative pl-6 space-y-5 before:absolute before:left-2.5 before:top-2 before:bottom-2 before:w-0.5 before:bg-brand-border">
            {steps.map((s, idx) => {
              const isDone = idx <= currentIdx;
              const isCurrent = idx === currentIdx;

              return (
                <div key={s.key} className="relative flex items-start justify-between">
                  {/* Step Dot */}
                  <div
                    className={`absolute -left-6 top-0.5 w-5 h-5 rounded-full flex items-center justify-center transition-all ${
                      isDone
                        ? 'bg-brand-primary text-white shadow-sm ring-4 ring-brand-primary/15'
                        : 'bg-white border-2 border-brand-border text-transparent'
                    }`}
                  >
                    {isDone && <CheckCircle2 size={13} strokeWidth={3} />}
                  </div>

                  {/* Step Details */}
                  <div className="flex-1 pr-4">
                    <p className={`text-xs font-bold ${isCurrent ? 'text-brand-primary font-black' : isDone ? 'text-brand-text' : 'text-brand-muted'}`}>
                      {s.label}
                    </p>
                    <p className="text-[11px] text-brand-muted">{s.desc}</p>
                  </div>

                  <span className="text-[10px] font-semibold text-brand-muted shrink-0">
                    {s.time}
                  </span>
                </div>
              );
            })}
          </div>

          {/* Delivery Address Snapshot */}
          <div className="bg-brand-bg p-3 rounded-card border border-brand-border text-xs flex items-start gap-2">
            <MapPin size={14} className="text-brand-primary mt-0.5 shrink-0" />
            <div>
              <p className="font-bold text-brand-text text-[11px]">Delivering to:</p>
              <p className="text-[11px] text-brand-muted">{activeOrder.address}</p>
            </div>
          </div>
        </div>

        {/* Simulation / Interactive controls for user */}
        <div className="p-4 bg-white border-t border-brand-border flex items-center justify-between gap-3">
          <button
            onClick={advanceStatus}
            disabled={currentIdx >= steps.length - 1}
            className="flex-1 py-2.5 bg-brand-accent hover:brightness-105 disabled:opacity-40 text-brand-text font-black rounded-button text-xs transition-all shadow-sm flex items-center justify-center gap-1.5"
          >
            <Sparkles size={13} />
            <span>
              {currentIdx >= steps.length - 1 ? 'Order Completed!' : `Simulate Next: "${steps[Math.min(currentIdx + 1, steps.length - 1)].label}"`}
            </span>
          </button>

          <button
            onClick={() => setIsTrackingOpen(false)}
            className="px-4 py-2.5 bg-brand-bg hover:bg-brand-border text-brand-text font-bold rounded-button text-xs transition-colors"
          >
            Close
          </button>
        </div>
      </div>
    </div>
  );
};

export default OrderTrackingModal;
