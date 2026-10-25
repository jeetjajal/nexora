'use client';

import React, { useState } from 'react';
import { useApp } from '@/context/AppContext';
import { X, MapPin, CreditCard, Banknote, ShieldCheck, Clock, Check } from 'lucide-react';

export const CheckoutModal: React.FC = () => {
  const {
    isCheckoutOpen,
    setIsCheckoutOpen,
    getCartTotal,
    appliedDiscount,
    createOrder,
  } = useApp();

  const [selectedAddress, setSelectedAddress] = useState('home');
  const [paymentMethod, setPaymentMethod] = useState<'razorpay' | 'cod'>('razorpay');
  const [deliveryNote, setDeliveryNote] = useState('Please leave near door / ring bell');
  const [isProcessing, setIsProcessing] = useState(false);

  if (!isCheckoutOpen) return null;

  const subtotal = getCartTotal();
  const deliveryFee = subtotal >= 199 ? 0 : 25;
  const handlingFee = 4;
  const finalTotal = Math.max(0, subtotal + deliveryFee + handlingFee - appliedDiscount);

  const addresses = [
    {
      id: 'home',
      label: 'Home',
      address: 'B-402, Shivalik Residency, Near SG Highway, Ahmedabad, Gujarat - 380054',
      eta: '9 MINS',
    },
    {
      id: 'office',
      label: 'Office',
      address: '504, Westgate Business Bay, Makarba, Ahmedabad, Gujarat - 380051',
      eta: '14 MINS',
    },
  ];

  const handlePlaceOrder = () => {
    setIsProcessing(true);
    setTimeout(() => {
      const addr = addresses.find(a => a.id === selectedAddress)?.address || addresses[0].address;
      createOrder(addr, paymentMethod);
      setIsProcessing(false);
    }, 1200);
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
      {/* Backdrop */}
      <div
        className="fixed inset-0 bg-brand-text/60 backdrop-blur-sm"
        onClick={() => !isProcessing && setIsCheckoutOpen(false)}
      />

      {/* Modal Card */}
      <div className="relative w-full max-w-lg bg-white rounded-card shadow-modal z-10 overflow-hidden border border-brand-border animate-in zoom-in-95 duration-150">
        {/* Header */}
        <div className="bg-brand-primary text-white p-4 flex items-center justify-between">
          <div className="flex items-center gap-2">
            <span className="w-8 h-8 rounded-full bg-brand-accent text-brand-text flex items-center justify-center font-black text-sm">
              ⚡
            </span>
            <div>
              <h3 className="text-base font-black">Confirm Order & Address</h3>
              <p className="text-[11px] text-white/80">Estimated Rapid Delivery: 9-10 Minutes</p>
            </div>
          </div>
          {!isProcessing && (
            <button
              onClick={() => setIsCheckoutOpen(false)}
              className="p-1.5 rounded-full hover:bg-white/10 text-white"
            >
              <X size={18} />
            </button>
          )}
        </div>

        {/* Content */}
        <div className="p-5 space-y-4 max-h-[75vh] overflow-y-auto">
          {/* Select Address */}
          <div>
            <label className="block text-xs font-black text-brand-text mb-2 uppercase tracking-wider">
              1. Delivery Address
            </label>
            <div className="space-y-2">
              {addresses.map((a) => (
                <div
                  key={a.id}
                  onClick={() => setSelectedAddress(a.id)}
                  className={`p-3 rounded-card border cursor-pointer transition-all flex items-start gap-3 ${
                    selectedAddress === a.id
                      ? 'border-brand-primary bg-brand-primary/5 shadow-sm ring-1 ring-brand-primary'
                      : 'border-brand-border bg-white hover:border-brand-border/80'
                  }`}
                >
                  <MapPin
                    size={16}
                    className={`mt-0.5 shrink-0 ${
                      selectedAddress === a.id ? 'text-brand-primary' : 'text-brand-muted'
                    }`}
                  />
                  <div className="flex-1 text-xs">
                    <div className="flex items-center justify-between font-bold text-brand-text mb-0.5">
                      <span>{a.label}</span>
                      <span className="text-[10px] text-brand-primary bg-brand-primary/10 px-1.5 py-0.2 rounded font-bold">
                        {a.eta}
                      </span>
                    </div>
                    <p className="text-brand-muted leading-relaxed text-[11px]">{a.address}</p>
                  </div>
                  {selectedAddress === a.id && (
                    <div className="w-5 h-5 rounded-full bg-brand-primary text-white flex items-center justify-center shrink-0">
                      <Check size={12} strokeWidth={3} />
                    </div>
                  )}
                </div>
              ))}
            </div>
          </div>

          {/* Delivery Instructions */}
          <div>
            <label className="block text-xs font-black text-brand-text mb-1.5 uppercase tracking-wider">
              2. Delivery Instruction
            </label>
            <input
              type="text"
              value={deliveryNote}
              onChange={(e) => setDeliveryNote(e.target.value)}
              placeholder="e.g. Ring doorbell, leave at gate"
              className="w-full px-3 py-2 text-xs bg-brand-bg border border-brand-border rounded-input focus:outline-none focus:ring-1 focus:ring-brand-primary"
            />
          </div>

          {/* Payment Method */}
          <div>
            <label className="block text-xs font-black text-brand-text mb-2 uppercase tracking-wider">
              3. Payment Option
            </label>
            <div className="grid grid-cols-2 gap-3">
              {/* Razorpay */}
              <div
                onClick={() => setPaymentMethod('razorpay')}
                className={`p-3 rounded-card border cursor-pointer transition-all flex flex-col justify-between ${
                  paymentMethod === 'razorpay'
                    ? 'border-brand-primary bg-brand-primary/5 ring-1 ring-brand-primary'
                    : 'border-brand-border hover:bg-brand-bg'
                }`}
              >
                <div className="flex items-center justify-between mb-2">
                  <CreditCard
                    size={18}
                    className={paymentMethod === 'razorpay' ? 'text-brand-primary' : 'text-brand-muted'}
                  />
                  <span className="text-[10px] bg-brand-accent text-brand-text font-black px-1.5 py-0.2 rounded">
                    Recommended
                  </span>
                </div>
                <div>
                  <p className="text-xs font-bold text-brand-text">Razorpay / UPI</p>
                  <p className="text-[10px] text-brand-muted">GPay, PhonePe, Cards, Netbanking</p>
                </div>
              </div>

              {/* COD */}
              <div
                onClick={() => setPaymentMethod('cod')}
                className={`p-3 rounded-card border cursor-pointer transition-all flex flex-col justify-between ${
                  paymentMethod === 'cod'
                    ? 'border-brand-primary bg-brand-primary/5 ring-1 ring-brand-primary'
                    : 'border-brand-border hover:bg-brand-bg'
                }`}
              >
                <div className="flex items-center justify-between mb-2">
                  <Banknote
                    size={18}
                    className={paymentMethod === 'cod' ? 'text-brand-primary' : 'text-brand-muted'}
                  />
                </div>
                <div>
                  <p className="text-xs font-bold text-brand-text">Cash on Delivery</p>
                  <p className="text-[10px] text-brand-muted">Pay cash or UPI to rider at doorstep</p>
                </div>
              </div>
            </div>
          </div>

          {/* Summary Box */}
          <div className="bg-brand-bg p-3 rounded-card border border-brand-border flex items-center justify-between text-xs">
            <div>
              <span className="text-brand-muted">Total Payable Amount:</span>
              <p className="text-base font-black text-brand-primary">₹{finalTotal}</p>
            </div>
            <div className="flex items-center gap-1 text-[11px] text-brand-muted">
              <ShieldCheck size={14} className="text-brand-primary" />
              <span>100% Encrypted & Safe</span>
            </div>
          </div>
        </div>

        {/* Action Button */}
        <div className="p-4 bg-white border-t border-brand-border">
          <button
            disabled={isProcessing}
            onClick={handlePlaceOrder}
            className="w-full py-3 bg-brand-primary hover:bg-brand-primaryHover disabled:opacity-70 text-white rounded-button font-black text-sm flex items-center justify-center gap-2 shadow-card transition-all active:scale-98"
          >
            {isProcessing ? (
              <>
                <div className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin" />
                <span>Confirming Order with Dark Store...</span>
              </>
            ) : (
              <>
                <span>Place Order • ₹{finalTotal}</span>
                <Clock size={16} />
              </>
            )}
          </button>
        </div>
      </div>
    </div>
  );
};

export default CheckoutModal;
