'use client';

import React, { useState, useEffect } from 'react';
import Link from 'next/link';
import {
  ApiClient,
  DeliveryAssignmentDetail,
} from '@/lib/api-client';
import { RoleSwitcher } from '@/components/common/RoleSwitcher';
import {
  Bike,
  Power,
  MapPin,
  Phone,
  CheckCircle2,
  AlertCircle,
  ExternalLink,
  ShieldAlert,
  Clock,
  RefreshCw,
  Navigation,
  Package,
} from 'lucide-react';

export default function RiderOrdersPage() {
  const [assignments, setAssignments] = useState<DeliveryAssignmentDetail[]>([]);
  const [isOnline, setIsOnline] = useState(true);
  const [activeTrip, setActiveTrip] = useState<DeliveryAssignmentDetail | null>(null);
  const [inputOtp, setInputOtp] = useState('');
  const [isLoading, setIsLoading] = useState(true);
  const [otpError, setOtpError] = useState('');
  const [otpSuccess, setOtpSuccess] = useState(false);
  const [isSubmittingOtp, setIsSubmittingOtp] = useState(false);
  const [pickupSuccess, setPickupSuccess] = useState('');

  const loadAssignments = async () => {
    setIsLoading(true);
    try {
      const data = await ApiClient.getMyDeliveryAssignments();
      setAssignments(data);
      if (data.length > 0 && !activeTrip) {
        setActiveTrip(data[0]);
      }
    } catch {
      // Handled gracefully
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    loadAssignments();
  }, []);

  const handlePickup = async (assignmentId: number) => {
    try {
      setOtpError('');
      await ApiClient.pickupDeliveryAssignment(assignmentId);
      setPickupSuccess('Order bag collected! Status changed to OUT_FOR_DELIVERY.');
      await loadAssignments();
      setTimeout(() => setPickupSuccess(''), 4000);
    } catch (err: any) {
      setOtpError(err.message || 'Failed to complete dark store pickup');
    }
  };

  const handleVerifyOtp = async () => {
    if (!activeTrip) return;
    if (inputOtp.trim().length !== 4) {
      setOtpError('Please enter the customer\'s 4-digit delivery PIN');
      return;
    }

    setIsSubmittingOtp(true);
    setOtpError('');
    try {
      await ApiClient.verifyDeliveryOtp(String(activeTrip.orderId), inputOtp.trim());
      setOtpSuccess(true);
      setActiveTrip((prev) => (prev ? { ...prev, orderStatus: 'DELIVERED', assignmentStatus: 'DELIVERED' } : null));
      await loadAssignments();
    } catch (err: any) {
      setOtpError(err.message || 'Incorrect delivery OTP. Please verify with customer.');
    } finally {
      setIsSubmittingOtp(false);
    }
  };

  const mapLink = activeTrip
    ? `https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(
        activeTrip.deliveryAddress + ', Ahmedabad'
      )}`
    : '#';

  return (
    <div className="min-h-screen bg-[#F7F8F2] flex flex-col font-sans">
      <RoleSwitcher />

      {/* Rider Header Bar */}
      <div className="bg-white border-b border-[#E3E8DE] px-4 py-2.5 shadow-xs">
        <div className="max-w-4xl mx-auto flex flex-wrap items-center justify-between gap-3">
          <div className="flex items-center gap-3">
            <div className="w-9 h-9 rounded-full bg-[#176B45] text-white flex items-center justify-center font-black text-sm">
              RP
            </div>
            <div>
              <h1 className="text-sm font-black text-[#173327] flex items-center gap-2">
                <span>Ramesh Patel (NX-9042)</span>
                <span className="text-[10px] bg-amber-100 text-amber-800 font-bold px-2 py-0.2 rounded">
                  ★ 4.92 Rating
                </span>
              </h1>
              <p className="text-[11px] text-slate-500">Dark Store #04 &bull; SG Highway Hub &bull; EV Scooter</p>
            </div>
          </div>

          <div className="flex items-center gap-3">
            <button
              onClick={() => setIsOnline(!isOnline)}
              className={`px-3 py-1.5 rounded-full text-xs font-bold flex items-center gap-1.5 transition-all shadow-xs ${
                isOnline
                  ? 'bg-emerald-600 text-white hover:bg-emerald-700'
                  : 'bg-slate-200 text-slate-700 hover:bg-slate-300'
              }`}
            >
              <Power size={13} />
              <span>{isOnline ? 'Online (Accepting Trips)' : 'Offline'}</span>
            </button>
            <button
              onClick={loadAssignments}
              disabled={isLoading}
              className="p-1.5 hover:bg-slate-100 text-slate-600 rounded-lg transition-colors"
              title="Refresh"
            >
              <RefreshCw size={15} className={isLoading ? 'animate-spin' : ''} />
            </button>
          </div>
        </div>
      </div>

      <main className="max-w-4xl w-full mx-auto p-4 md:p-6 space-y-5 flex-1">
        {pickupSuccess && (
          <div className="p-3 bg-emerald-50 text-emerald-800 border border-emerald-200 rounded-xl text-xs font-bold flex items-center gap-2">
            <CheckCircle2 size={16} />
            <span>{pickupSuccess}</span>
          </div>
        )}

        {/* Rider Shift Summary Cards */}
        <section className="grid grid-cols-2 sm:grid-cols-4 gap-3">
          <div className="bg-white p-3.5 rounded-2xl border border-[#E3E8DE] shadow-xs">
            <span className="text-[11px] font-bold text-slate-500 block mb-1">Today's Earnings</span>
            <span className="text-xl font-black text-[#176B45]">₹840.00</span>
            <span className="text-[10px] text-emerald-700 font-bold block mt-0.5">14 Deliveries + ₹120 Tips</span>
          </div>

          <div className="bg-white p-3.5 rounded-2xl border border-[#E3E8DE] shadow-xs">
            <span className="text-[11px] font-bold text-slate-500 block mb-1">Avg Delivery Time</span>
            <span className="text-xl font-black text-[#173327]">8.8 Mins</span>
            <span className="text-[10px] text-[#176B45] font-bold block mt-0.5">99.4% On-Time</span>
          </div>

          <div className="bg-white p-3.5 rounded-2xl border border-[#E3E8DE] shadow-xs">
            <span className="text-[11px] font-bold text-slate-500 block mb-1">Weekly Payout</span>
            <span className="text-xl font-black text-[#173327]">₹6,420</span>
            <span className="text-[10px] text-slate-400 block mt-0.5">Direct Bank Deposit</span>
          </div>

          <div className="bg-white p-3.5 rounded-2xl border border-[#E3E8DE] shadow-xs">
            <span className="text-[11px] font-bold text-slate-500 block mb-1">Safety Badge</span>
            <span className="text-base font-black text-[#173327] flex items-center gap-1">
              Diamond Pro
            </span>
            <span className="text-[10px] text-emerald-700 font-bold block mt-0.5">Zero Incidents</span>
          </div>
        </section>

        {/* Active Delivery Trip */}
        {activeTrip ? (
          <div className="bg-white rounded-3xl border-2 border-[#176B45] shadow-xs overflow-hidden">
            {/* Trip banner */}
            <div className="bg-[#176B45] text-white p-4 flex flex-wrap items-center justify-between gap-3">
              <div className="flex items-center gap-2">
                <span className="w-2.5 h-2.5 rounded-full bg-[#D8F36A] animate-ping" />
                <h2 className="text-sm font-black">
                  Active Delivery Trip &bull; Order #{activeTrip.orderId}
                </h2>
              </div>

              <div className="flex items-center gap-2">
                <span className="text-xs bg-[#D8F36A] text-[#173327] px-2.5 py-0.5 rounded-full font-black uppercase">
                  {activeTrip.orderStatus}
                </span>
                <span className="text-xs bg-white/20 text-white px-2 py-0.5 rounded-full font-mono">
                  {activeTrip.paymentMethod} &bull; ₹{activeTrip.totalAmount}
                </span>
              </div>
            </div>

            <div className="p-5 space-y-5">
              {/* Pickup & Drop Details */}
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                {/* 1. Pickup Station */}
                <div className="bg-[#F7F8F2] p-4 rounded-2xl border border-[#E3E8DE] space-y-2">
                  <span className="text-[10px] uppercase tracking-wider font-extrabold text-[#176B45] block">
                    1. Dark Store Pickup
                  </span>
                  <h3 className="text-xs font-bold text-[#173327]">{activeTrip.storeName}</h3>
                  <p className="text-[11px] text-slate-500">{activeTrip.storeAddress}</p>
                  {activeTrip.packingBagCode && (
                    <div className="text-[11px] font-mono font-bold text-purple-700 bg-purple-50 p-1.5 rounded-lg border border-purple-200">
                      Bag Barcode: {activeTrip.packingBagCode}
                    </div>
                  )}

                  {activeTrip.orderStatus === 'READY' && (
                    <button
                      onClick={() => handlePickup(activeTrip.assignmentId)}
                      className="mt-2 w-full py-2 bg-[#176B45] hover:bg-[#125638] text-white text-xs font-bold rounded-lg shadow-2xs transition-all flex items-center justify-center gap-1.5"
                    >
                      <Package size={14} />
                      <span>Confirm Bag Pickup & Start Trip</span>
                    </button>
                  )}
                </div>

                {/* 2. Customer Drop Station */}
                <div className="bg-[#F7F8F2] p-4 rounded-2xl border border-[#E3E8DE] space-y-2">
                  <span className="text-[10px] uppercase tracking-wider font-extrabold text-slate-700 block">
                    2. Customer Doorstep Drop
                  </span>
                  <h3 className="text-xs font-bold text-[#173327]">{activeTrip.customerName}</h3>
                  <p className="text-[11px] text-slate-500">{activeTrip.deliveryAddress}</p>

                  <div className="pt-2 flex flex-wrap items-center gap-2">
                    <a
                      href={`tel:${activeTrip.customerPhone}`}
                      className="px-3 py-1.5 bg-white border border-slate-200 text-[#176B45] font-bold text-xs rounded-lg hover:bg-[#176B45] hover:text-white transition-all flex items-center gap-1 shadow-2xs"
                    >
                      <Phone size={12} />
                      <span>Call Customer</span>
                    </a>
                    <a
                      href={mapLink}
                      target="_blank"
                      rel="noopener noreferrer"
                      className="px-3 py-1.5 bg-white border border-slate-200 text-blue-600 font-bold text-xs rounded-lg hover:bg-blue-600 hover:text-white transition-all flex items-center gap-1 shadow-2xs"
                    >
                      <Navigation size={12} />
                      <span>Google Maps Route</span>
                      <ExternalLink size={10} />
                    </a>
                  </div>
                </div>
              </div>

              {/* 3. Doorstep OTP Handshake Form */}
              {activeTrip.orderStatus !== 'DELIVERED' ? (
                <div className="bg-emerald-50/60 p-4 rounded-2xl border border-[#176B45]/30 space-y-3">
                  <div className="flex items-center justify-between">
                    <div>
                      <h4 className="text-xs font-black text-[#176B45] uppercase tracking-wider">
                        Doorstep Delivery Verification
                      </h4>
                      <p className="text-[11px] text-slate-600">
                        Ask the customer for their authoritative 4-digit PIN upon arrival:
                      </p>
                    </div>
                    <span className="text-[10px] bg-white text-[#176B45] font-mono font-bold px-2 py-0.5 rounded border border-emerald-200">
                      Max 3 Attempts
                    </span>
                  </div>

                  <div className="flex flex-col sm:flex-row gap-2">
                    <input
                      type="text"
                      maxLength={4}
                      value={inputOtp}
                      onChange={(e) => setInputOtp(e.target.value.replace(/\D/g, ''))}
                      placeholder="4-digit OTP"
                      className="px-4 py-2.5 text-base font-mono font-black text-center tracking-widest bg-white border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-[#176B45] sm:w-44"
                    />
                    <button
                      onClick={handleVerifyOtp}
                      disabled={isSubmittingOtp || inputOtp.length !== 4}
                      className={`px-6 py-2.5 rounded-xl font-black text-xs transition-all shadow-xs flex items-center justify-center gap-2 ${
                        inputOtp.length === 4 && !isSubmittingOtp
                          ? 'bg-[#176B45] hover:bg-[#125638] text-white cursor-pointer'
                          : 'bg-slate-200 text-slate-400 cursor-not-allowed'
                      }`}
                    >
                      <CheckCircle2 size={16} />
                      <span>{isSubmittingOtp ? 'Verifying OTP...' : 'Verify OTP & Complete Delivery'}</span>
                    </button>
                  </div>

                  {otpError && (
                    <div className="text-xs font-bold text-red-700 bg-red-100 p-2.5 rounded-xl flex items-center gap-1.5">
                      <ShieldAlert size={15} />
                      <span>{otpError}</span>
                    </div>
                  )}
                </div>
              ) : (
                <div className="bg-emerald-50 p-4 rounded-2xl border border-emerald-300 text-center space-y-1">
                  <div className="w-10 h-10 rounded-full bg-emerald-600 text-white flex items-center justify-center mx-auto">
                    <CheckCircle2 size={22} />
                  </div>
                  <h4 className="text-sm font-black text-emerald-900">Delivery Completed Successfully!</h4>
                  <p className="text-xs text-emerald-700">
                    Order #{activeTrip.orderId} delivered and ₹{activeTrip.deliveryFee} credited to your earnings wallet.
                  </p>
                </div>
              )}
            </div>
          </div>
        ) : (
          <div className="bg-white p-12 rounded-3xl border border-[#E3E8DE] text-center shadow-xs">
            <Bike size={40} className="mx-auto text-slate-300 mb-2" />
            <h3 className="text-sm font-bold text-slate-800">No Active Deliveries Assigned</h3>
            <p className="text-xs text-slate-500 mt-1 max-w-sm mx-auto">
              You are online and available. When orders are packed at the SG Highway hub, you will be dispatched automatically!
            </p>
          </div>
        )}
      </main>
    </div>
  );
}
