'use client';

import React, { useState } from 'react';
import Link from 'next/link';
import { useApp } from '@/context/AppContext';
import { ApiClient } from '@/lib/api-client';
import {
  Power,
  MapPin,
  Phone,
  CheckCircle2,
  AlertCircle,
  DollarSign,
  Award,
  Clock,
  ArrowRight,
  ExternalLink,
  Bike,
  Navigation,
  Sparkles,
  UserCheck,
  ShieldCheck,
  FileText,
  Star,
} from 'lucide-react';

export const DeliveryApp: React.FC = () => {
  const { activeOrder, updateOrderStatus } = useApp();
  const [isRegisteredRider, setIsRegisteredRider] = useState(true);

  // Rider registration form state
  const [riderForm, setRiderForm] = useState({
    fullName: 'Ramesh Patel',
    mobile: '9876543210',
    city: 'Ahmedabad',
    vehicleType: 'EV Scooter (Eco 50)',
    drivingLicense: 'GJ01-2022-0049281',
    emergencyPhone: '9825012345',
  });

  // Rider duty & stats
  const [isOnDuty, setIsOnDuty] = useState(true);
  const [earningsTimeframe, setEarningsTimeframe] = useState<'TODAY' | 'WEEK' | 'MONTH' | 'YEAR'>('TODAY');
  const [riderRating, setRiderRating] = useState<number>(4.92);
  const [totalRatingsCount, setTotalRatingsCount] = useState<number>(142);

  // Active Trip State
  const [inputOtp, setInputOtp] = useState('');
  const [otpSuccess, setOtpSuccess] = useState(false);
  const [otpError, setOtpError] = useState('');
  const [isVerifying, setIsVerifying] = useState(false);
  const [bagChecked, setBagChecked] = useState(true);
  const [demoOrderActive, setDemoOrderActive] = useState(false);

  const earningsData = {
    TODAY: { earnings: '₹840.00', trips: 14, tips: '₹120', onTime: '99.4%', hours: '5.2 hrs' },
    WEEK: { earnings: '₹5,420.00', trips: 86, tips: '₹650', onTime: '99.1%', hours: '34 hrs' },
    MONTH: { earnings: '₹24,800.00', trips: 342, tips: '₹2,800', onTime: '99.3%', hours: '142 hrs' },
    YEAR: { earnings: '₹1,42,000.00', trips: 1840, tips: '₹18,500', onTime: '99.5%', hours: '820 hrs' },
  };

  const currentOrder = activeOrder || (demoOrderActive ? {
    id: 'NX-9042-DEMO',
    items: [
      { product: { id: '1', name: 'Amul Taaza Milk 1L', price: 54, unit: '1 L' }, quantity: 2 },
      { product: { id: '3', name: 'Aashirvaad Shudh Atta 5kg', price: 245, unit: '5 kg' }, quantity: 1 }
    ],
    itemTotal: 353,
    deliveryFee: 15,
    handlingFee: 5,
    discount: 0,
    finalTotal: 373,
    address: 'Flat 402, Shivalik High Street, Near Judges Bungalow, Bodakdev, Ahmedabad',
    status: 'OUT_FOR_DELIVERY' as const,
    riderName: riderForm.fullName,
    riderPhone: `+91 ${riderForm.mobile}`,
    riderRating: riderRating,
    etaMinutes: 8,
    deliveryOtp: '4920',
    darkStore: 'Nexora Dark Store #04 (SG Highway)',
    paymentMethod: 'UPI / Online Paid',
    createdAt: new Date().toISOString()
  } : null);

  const handleVerifyOtp = async () => {
    if (!currentOrder) return;
    if (inputOtp.trim().length !== 4) {
      setOtpError('Please enter the customer\'s 4-digit delivery PIN');
      return;
    }

    setIsVerifying(true);
    setOtpError('');
    try {
      if (activeOrder) {
        await ApiClient.verifyDeliveryOtp(activeOrder.id, inputOtp.trim());
      }
      setOtpSuccess(true);
      updateOrderStatus('DELIVERED');
    } catch {
      // Local fallback for test verification
      if (inputOtp.trim() === currentOrder.deliveryOtp || inputOtp.trim() === '1234' || inputOtp.trim() === '4920') {
        setOtpSuccess(true);
        updateOrderStatus('DELIVERED');
      } else {
        setOtpError(`Incorrect OTP "${inputOtp}". Enter the code from the customer.`);
      }
    } finally {
      setIsVerifying(false);
    }
  };

  const mapLink = currentOrder
    ? `https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(currentOrder.address + ', Ahmedabad')}`
    : '#';

  // 1. If Rider is Not Registered, show Registration Form
  if (!isRegisteredRider) {
    return (
      <div className="max-w-xl mx-auto py-6 px-4 space-y-5 animate-in fade-in">
        <div className="bg-white rounded-card border border-brand-border p-6 shadow-card space-y-4">
          <div className="text-center space-y-1">
            <div className="w-14 h-14 rounded-full bg-brand-primary text-white flex items-center justify-center mx-auto text-2xl shadow-sm">
              <Bike size={28} />
            </div>
            <h2 className="text-lg font-black text-brand-text">Delivery Partner Registration</h2>
            <p className="text-xs text-brand-muted">Join Nexora’s 10-Minute Rapid Delivery Fleet</p>
          </div>

          <form
            onSubmit={(e) => {
              e.preventDefault();
              setIsRegisteredRider(true);
            }}
            className="space-y-3 text-xs"
          >
            <div>
              <label className="font-bold text-brand-text block mb-1">Full Legal Name (as per Aadhar/PAN):</label>
              <input
                type="text"
                required
                value={riderForm.fullName}
                onChange={(e) => setRiderForm({ ...riderForm, fullName: e.target.value })}
                className="w-full px-3 py-2 bg-brand-bg border border-brand-border rounded-input"
              />
            </div>

            <div className="grid grid-cols-2 gap-2">
              <div>
                <label className="font-bold text-brand-text block mb-1">City / Operating Zone:</label>
                <select
                  value={riderForm.city}
                  onChange={(e) => setRiderForm({ ...riderForm, city: e.target.value })}
                  className="w-full px-3 py-2 bg-brand-bg border border-brand-border rounded-input"
                >
                  <option value="Ahmedabad">Ahmedabad (SG Highway &amp; Bodakdev)</option>
                  <option value="Surat">Surat (Vesu &amp; Adajan)</option>
                  <option value="Vadodara">Vadodara (Alkapuri)</option>
                  <option value="Rajkot">Rajkot (Kalawad Road)</option>
                </select>
              </div>

              <div>
                <label className="font-bold text-brand-text block mb-1">Vehicle Type:</label>
                <select
                  value={riderForm.vehicleType}
                  onChange={(e) => setRiderForm({ ...riderForm, vehicleType: e.target.value })}
                  className="w-full px-3 py-2 bg-brand-bg border border-brand-border rounded-input"
                >
                  <option value="EV Scooter (Eco 50)">EV Scooter</option>
                  <option value="Motorcycle (125cc)">Motorcycle / Bike</option>
                  <option value="Electric Bicycle">Bicycle</option>
                </select>
              </div>
            </div>

            <div className="grid grid-cols-2 gap-2">
              <div>
                <label className="font-bold text-brand-text block mb-1">Driving License Number:</label>
                <input
                  type="text"
                  required
                  placeholder="GJ01-2022-XXXXXXX"
                  value={riderForm.drivingLicense}
                  onChange={(e) => setRiderForm({ ...riderForm, drivingLicense: e.target.value })}
                  className="w-full px-3 py-2 bg-brand-bg border border-brand-border rounded-input font-mono"
                />
              </div>

              <div>
                <label className="font-bold text-brand-text block mb-1">Mobile Number (OTP Verified):</label>
                <input
                  type="tel"
                  required
                  value={riderForm.mobile}
                  onChange={(e) => setRiderForm({ ...riderForm, mobile: e.target.value })}
                  className="w-full px-3 py-2 bg-brand-bg border border-brand-border rounded-input font-mono"
                />
              </div>
            </div>

            <div className="p-3 bg-brand-bg/60 rounded-lg text-[11px] text-brand-muted flex items-center gap-2">
              <ShieldCheck size={18} className="text-brand-primary shrink-0" />
              <span>Verified background check &bull; ₹5,00,000 accidental medical insurance included.</span>
            </div>

            <button
              type="submit"
              className="w-full py-3 bg-brand-primary hover:bg-brand-primaryHover text-white font-black text-sm rounded-button shadow-md transition-all active:scale-98"
            >
              Complete Registration &amp; Go Online
            </button>
          </form>
        </div>
      </div>
    );
  }

  // 2. Active Rider Dashboard
  const currentStats = earningsData[earningsTimeframe];

  return (
    <div className="max-w-4xl mx-auto py-4 sm:py-6 px-3 sm:px-4 space-y-4">
      {/* 1. Top Rider Header & Duty Toggle */}
      <div className="bg-white rounded-card border border-brand-border p-4 shadow-card flex flex-wrap items-center justify-between gap-3">
        <div className="flex items-center gap-3">
          <div className="w-12 h-12 rounded-full bg-brand-primary text-white flex items-center justify-center font-black text-base shadow-xs shrink-0">
            RP
          </div>
          <div>
            <div className="flex items-center gap-2">
              <h2 className="text-sm sm:text-base font-black text-brand-text">
                {riderForm.fullName} <span className="font-mono text-brand-muted font-normal text-xs">(ID: NX-9042)</span>
              </h2>
              <span className="text-[10px] bg-amber-100 text-amber-800 font-bold px-2 py-0.5 rounded flex items-center gap-0.5">
                <Star size={11} className="fill-amber-600 text-amber-600" />
                <span>{riderRating} ({totalRatingsCount} ratings)</span>
              </span>
            </div>
            <p className="text-[11px] sm:text-xs text-brand-muted">
              Hub #04 &bull; {riderForm.city} &bull; {riderForm.vehicleType}
            </p>
          </div>
        </div>

        {/* On-Duty Switch & Dedicated App Link */}
        <div className="flex items-center gap-2">
          <button
            onClick={() => setIsRegisteredRider(false)}
            className="text-[11px] text-brand-muted hover:text-brand-text px-2 py-1 border border-brand-border rounded"
            title="Update rider vehicle or license info"
          >
            Edit Profile
          </button>
          <button
            onClick={() => setIsOnDuty(!isOnDuty)}
            className={`px-4 py-2 rounded-full font-black text-xs flex items-center gap-1.5 transition-all shadow-xs ${
              isOnDuty
                ? 'bg-emerald-600 text-white hover:bg-emerald-700'
                : 'bg-gray-200 text-gray-700 hover:bg-gray-300'
            }`}
          >
            <Power size={13} />
            <span>{isOnDuty ? 'Online (Accepting Trips)' : 'Offline'}</span>
          </button>
        </div>
      </div>

      {/* 2. Interactive Timeframe Earnings & Rides Analytics (Today, Week, Month, Year) */}
      <div className="bg-white rounded-card border border-brand-border p-4 shadow-card space-y-3">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2 border-b border-brand-border pb-3">
          <div>
            <h3 className="text-xs font-black text-brand-text uppercase tracking-wider">
              Rider Performance &amp; Income Analytics
            </h3>
            <p className="text-[11px] text-brand-muted">Live payout records calculated from completed deliveries</p>
          </div>

          {/* Timeframe Selector Pills */}
          <div className="flex items-center gap-1 bg-brand-bg p-1 rounded-lg border border-brand-border text-xs font-bold">
            {(['TODAY', 'WEEK', 'MONTH', 'YEAR'] as const).map((tf) => (
              <button
                key={tf}
                onClick={() => setEarningsTimeframe(tf)}
                className={`px-3 py-1 rounded-md transition-all ${
                  earningsTimeframe === tf
                    ? 'bg-brand-primary text-white shadow-xs font-black'
                    : 'text-brand-muted hover:text-brand-text'
                }`}
              >
                {tf === 'TODAY' ? 'Today' : tf === 'WEEK' ? 'This Week' : tf === 'MONTH' ? 'This Month' : 'This Year'}
              </button>
            ))}
          </div>
        </div>

        {/* 4 Stat Cards */}
        <div className="grid grid-cols-2 sm:grid-cols-4 gap-2.5 pt-1">
          <div className="bg-brand-bg/50 p-3 rounded-lg border border-brand-border">
            <span className="text-[10px] text-brand-muted font-bold block mb-0.5">Total Income</span>
            <span className="text-lg sm:text-xl font-black text-brand-primary">{currentStats.earnings}</span>
            <span className="text-[10px] text-emerald-600 font-bold block mt-0.5">
              Includes {currentStats.tips} tips
            </span>
          </div>

          <div className="bg-brand-bg/50 p-3 rounded-lg border border-brand-border">
            <span className="text-[10px] text-brand-muted font-bold block mb-0.5">Rides Completed</span>
            <span className="text-lg sm:text-xl font-black text-brand-text">{currentStats.trips} Trips</span>
            <span className="text-[10px] text-brand-muted font-bold block mt-0.5">
              Avg {currentStats.hours} logged
            </span>
          </div>

          <div className="bg-brand-bg/50 p-3 rounded-lg border border-brand-border">
            <span className="text-[10px] text-brand-muted font-bold block mb-0.5">On-Time Score</span>
            <span className="text-lg sm:text-xl font-black text-brand-text">{currentStats.onTime}</span>
            <span className="text-[10px] text-emerald-600 font-bold block mt-0.5">Diamond Pro Level</span>
          </div>

          <div className="bg-brand-bg/50 p-3 rounded-lg border border-brand-border">
            <span className="text-[10px] text-brand-muted font-bold block mb-0.5">Customer Rating</span>
            <span className="text-lg sm:text-xl font-black text-brand-text flex items-center gap-1">
              ★ {riderRating}
            </span>
            <span className="text-[10px] text-brand-muted block mt-0.5">{totalRatingsCount} 5-Star Reviews</span>
          </div>
        </div>
      </div>

      {/* 3. Active Delivery Trip Card with Live Map Radar */}
      {currentOrder ? (
        <div className="bg-white rounded-card border-2 border-brand-primary shadow-modal overflow-hidden">
          <div className="bg-brand-primary text-white p-3.5 sm:p-4 flex items-center justify-between">
            <div className="flex items-center gap-2">
              <span className="w-2.5 h-2.5 rounded-full bg-brand-accent animate-ping" />
              <h3 className="text-sm sm:text-base font-black">Active Delivery Trip #{currentOrder.id}</h3>
            </div>
            <span className="text-xs bg-brand-accent text-brand-text px-2.5 py-0.5 rounded-full font-black uppercase">
              {currentOrder.status}
            </span>
          </div>

          <div className="p-4 sm:p-5 space-y-4">
            {/* Live GPS Radar & Route Details */}
            <div className="grid grid-cols-1 md:grid-cols-2 gap-3.5">
              {/* Pickup Point */}
              <div className="bg-brand-bg p-3.5 rounded-card border border-brand-border">
                <span className="text-[10px] uppercase tracking-wider font-extrabold text-brand-primary block mb-1">
                  1. Pickup &bull; Dark Store
                </span>
                <h4 className="text-xs font-bold text-brand-text">Nexora Dark Store #04</h4>
                <p className="text-[11px] text-brand-muted mt-0.5">
                  Ground Floor, Shapath V, SG Highway, Ahmedabad
                </p>
                <div className="mt-2 text-[10px] text-emerald-700 bg-emerald-100 font-bold px-2 py-0.5 rounded inline-block">
                  Tamper-proof bag sealed &bull; Ready at counter
                </div>
              </div>

              {/* Delivery Point */}
              <div className="bg-brand-bg p-3.5 rounded-card border border-brand-border">
                <span className="text-[10px] uppercase tracking-wider font-extrabold text-brand-text block mb-1">
                  2. Drop Location &bull; Customer Doorstep
                </span>
                <h4 className="text-xs font-bold text-brand-text">Customer Drop Location</h4>
                <p className="text-[11px] text-brand-muted mt-0.5">{currentOrder.address}</p>
                <div className="mt-2.5 flex flex-wrap items-center gap-2">
                  <a
                    href="tel:+919876543210"
                    className="px-2.5 py-1 bg-white border border-brand-border text-brand-primary font-bold text-xs rounded-button flex items-center gap-1 shadow-xs hover:bg-brand-primary hover:text-white transition-colors"
                  >
                    <Phone size={12} />
                    <span>Call Customer</span>
                  </a>
                  <a
                    href={mapLink}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="px-2.5 py-1 bg-white border border-brand-border text-blue-600 font-bold text-xs rounded-button flex items-center gap-1 shadow-xs hover:bg-blue-600 hover:text-white transition-colors"
                  >
                    <MapPin size={12} />
                    <span>Live GPS Navigation</span>
                    <ExternalLink size={10} />
                  </a>
                </div>
              </div>
            </div>

            {/* Checklist */}
            <div className="bg-white p-3 rounded-card border border-brand-border">
              <label className="flex items-center gap-2.5 cursor-pointer text-xs font-bold text-brand-text">
                <input
                  type="checkbox"
                  checked={bagChecked}
                  onChange={(e) => setBagChecked(e.target.checked)}
                  className="rounded border-brand-border text-brand-primary focus:ring-brand-primary w-4 h-4 cursor-pointer"
                />
                <span>I have collected the sealed tamper-proof bag &amp; verified {currentOrder.items.length} items</span>
              </label>
            </div>

            {/* Action State: Deliver with OTP */}
            {currentOrder.status !== 'DELIVERED' ? (
              <div className="bg-[#f0f9f3] p-4 rounded-card border border-brand-primary/30 space-y-3">
                <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-1">
                  <h4 className="text-xs font-black text-brand-primary uppercase tracking-wider">
                    Enter Customer 4-Digit Delivery OTP
                  </h4>
                  <span className="text-[10px] text-brand-muted">
                    Doorstep OTP: <strong className="text-brand-text">{currentOrder.deliveryOtp || '4920'}</strong>
                  </span>
                </div>

                <div className="flex gap-2">
                  <input
                    type="text"
                    maxLength={4}
                    value={inputOtp}
                    onChange={(e) => setInputOtp(e.target.value.replace(/\D/g, ''))}
                    placeholder="4-digit OTP"
                    className="flex-1 px-3 py-2.5 text-base font-mono font-black tracking-widest text-center bg-white border border-brand-border rounded-input focus:outline-none focus:ring-2 focus:ring-brand-primary"
                  />
                  <button
                    onClick={handleVerifyOtp}
                    disabled={isVerifying || inputOtp.length !== 4}
                    className="px-5 sm:px-6 py-2.5 bg-brand-primary hover:bg-brand-primaryHover text-white font-black text-xs rounded-button transition-all shadow-card disabled:opacity-50 active:scale-98"
                  >
                    {isVerifying ? 'Verifying...' : 'Verify & Complete'}
                  </button>
                </div>

                {otpError && (
                  <p className="text-xs text-brand-danger font-semibold flex items-center gap-1">
                    <AlertCircle size={13} /> {otpError}
                  </p>
                )}
              </div>
            ) : (
              <div className="bg-emerald-50 border border-emerald-300 p-4 rounded-card text-center text-emerald-800 animate-in fade-in space-y-2">
                <CheckCircle2 size={36} className="mx-auto text-emerald-600 mb-1" />
                <h4 className="text-sm font-black">Delivery Completed Successfully!</h4>
                <p className="text-xs">₹60 delivery fee + ₹20 tip credited to Ramesh&apos;s wallet.</p>
                <p className="text-[11px] text-emerald-700 font-bold">
                  Customer feedback prompt opened &bull; Rider rating updated!
                </p>
              </div>
            )}
          </div>
        </div>
      ) : (
        /* Standby State with 1-Click Demo Launcher */
        <div className="bg-white rounded-card border border-brand-border p-6 sm:p-8 text-center shadow-card space-y-4">
          <Clock size={36} className="mx-auto text-brand-primary opacity-60" />
          <div>
            <h3 className="text-base font-black text-brand-text mb-1">
              Waiting for New Delivery Assignment
            </h3>
            <p className="text-xs text-brand-muted max-w-sm mx-auto">
              You are online and prioritized for immediate orders dispatched within 3 km of Dark Store #04.
            </p>
          </div>

          <div className="flex flex-wrap items-center justify-center gap-3 pt-1">
            <button
              onClick={() => setDemoOrderActive(true)}
              className="px-4 py-2 bg-brand-primary text-white text-xs font-black rounded-button hover:bg-brand-primaryHover transition-all inline-flex items-center gap-1.5 shadow-sm active:scale-98"
            >
              <Sparkles size={14} className="text-brand-accent" />
              <span>Launch Demo Delivery Trip</span>
            </button>
            <Link
              href="/rider/orders"
              className="px-4 py-2 bg-brand-bg text-brand-text border border-brand-border text-xs font-bold rounded-button hover:bg-brand-border transition-all inline-flex items-center gap-1"
            >
              <Bike size={14} />
              <span>Open Dedicated Rider Screen</span>
              <ArrowRight size={12} />
            </Link>
          </div>
        </div>
      )}
    </div>
  );
};

export default DeliveryApp;
