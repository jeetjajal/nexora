'use client';

import React, { useState } from 'react';
import { useApp } from '@/context/AppContext';
import { ApiClient } from '@/lib/api-client';
import { X, MapPin, CreditCard, Banknote, ShieldCheck, Clock, Check, Plus, AlertCircle } from 'lucide-react';

export const CheckoutModal: React.FC = () => {
  const {
    isCheckoutOpen,
    setIsCheckoutOpen,
    getCartItems,
    getCartTotal,
    appliedDiscount,
    couponCode,
    createOrderWithBackend,
  } = useApp();

  const [selectedAddress, setSelectedAddress] = useState('home');
  const [customAddress, setCustomAddress] = useState({
    name: 'Customer',
    phone: '+91 98765 43210',
    flat: '',
    street: '',
    city: 'Ahmedabad',
    pincode: '380054',
  });
  const [isAddingAddress, setIsAddingAddress] = useState(false);
  const [paymentMethod, setPaymentMethod] = useState<'RAZORPAY' | 'COD'>('RAZORPAY');
  const [deliveryNote, setDeliveryNote] = useState('Please leave near door / ring bell');
  const [isProcessing, setIsProcessing] = useState(false);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);

  if (!isCheckoutOpen) return null;

  const subtotal = getCartTotal();
  const deliveryFee = subtotal >= 199 ? 0 : 25;
  const handlingFee = 4;
  const finalTotal = Math.max(0, subtotal + deliveryFee + handlingFee - appliedDiscount);

  const defaultAddresses = [
    {
      id: 'home',
      label: 'Home',
      name: 'Jeet Jajal',
      phone: '+91 98765 43210',
      flat: 'B-402, Shivalik Residency',
      street: 'Near SG Highway, Bodakdev',
      city: 'Ahmedabad',
      pincode: '380054',
      eta: '9 MINS',
    },
    {
      id: 'office',
      label: 'Office',
      name: 'Jeet Jajal',
      phone: '+91 98765 43210',
      flat: '504, Westgate Business Bay',
      street: 'Corporate Road, Makarba',
      city: 'Ahmedabad',
      pincode: '380051',
      eta: '14 MINS',
    },
  ];

  const handlePlaceOrder = async () => {
    setIsProcessing(true);
    setErrorMsg(null);

    try {
      const chosenAddr = isAddingAddress
        ? {
            name: customAddress.name || 'Customer',
            phone: customAddress.phone || '+91 98765 43210',
            flat: customAddress.flat || 'Flat 101',
            street: customAddress.street || 'SG Highway',
            city: customAddress.city || 'Ahmedabad',
            pincode: customAddress.pincode || '380054',
            tag: 'OTHER' as const,
          }
        : {
            name: defaultAddresses.find(a => a.id === selectedAddress)?.name || 'Jeet Jajal',
            phone: defaultAddresses.find(a => a.id === selectedAddress)?.phone || '+91 98765 43210',
            flat: defaultAddresses.find(a => a.id === selectedAddress)?.flat || 'B-402',
            street: defaultAddresses.find(a => a.id === selectedAddress)?.street || 'SG Highway',
            city: defaultAddresses.find(a => a.id === selectedAddress)?.city || 'Ahmedabad',
            pincode: defaultAddresses.find(a => a.id === selectedAddress)?.pincode || '380054',
            tag: (selectedAddress === 'home' ? 'HOME' : 'WORK') as 'HOME' | 'WORK',
          };

      const cartItems = getCartItems().map(item => ({
        productId: item.product.id,
        name: item.product.name,
        price: item.product.price,
        quantity: item.quantity,
      }));

      // Razorpay Flow
      if (paymentMethod === 'RAZORPAY') {
        // Step 1: Create Razorpay Order on server
        const rzpOrder = await ApiClient.createRazorpayOrder(finalTotal, `rcpt_${Date.now()}`);

        // Step 2: Verify Razorpay Signature (Sandbox simulation or Web SDK)
        await ApiClient.verifyRazorpayPayment({
          razorpay_order_id: rzpOrder.id,
          razorpay_payment_id: `pay_${Math.random().toString(36).substring(2, 10)}`,
          razorpay_signature: 'verified_signature',
        });
      }

      // Step 3: Place authoritative order
      await createOrderWithBackend({
        items: cartItems,
        address: chosenAddr,
        paymentMethod,
        couponCode: couponCode || undefined,
      });

      setIsProcessing(false);
    } catch (err: any) {
      setErrorMsg(err.message || 'Failed to place order. Please try again.');
      setIsProcessing(false);
    }
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
              <p className="text-[11px] text-white/80">Estimated Rapid Delivery: 8-10 Minutes</p>
            </div>
          </div>
          {!isProcessing && (
            <button
              onClick={() => setIsCheckoutOpen(false)}
              className="p-1.5 rounded-full hover:bg-white/10 text-white transition-colors"
            >
              <X size={18} />
            </button>
          )}
        </div>

        {/* Error Alert */}
        {errorMsg && (
          <div className="bg-red-50 text-red-700 px-4 py-2.5 text-xs flex items-center gap-2 border-b border-red-200">
            <AlertCircle size={15} className="shrink-0" />
            <span>{errorMsg}</span>
          </div>
        )}

        {/* Content */}
        <div className="p-5 space-y-4 max-h-[75vh] overflow-y-auto">
          {/* Select Address */}
          <div>
            <div className="flex items-center justify-between mb-2">
              <label className="text-xs font-black text-brand-text uppercase tracking-wider">
                1. Delivery Address
              </label>
              <button
                type="button"
                onClick={() => setIsAddingAddress(!isAddingAddress)}
                className="text-[11px] font-bold text-brand-primary hover:underline flex items-center gap-1"
              >
                <Plus size={12} />
                <span>{isAddingAddress ? 'Choose Saved' : 'Add New'}</span>
              </button>
            </div>

            {isAddingAddress ? (
              <div className="p-3 bg-brand-bg rounded-card border border-brand-border space-y-2 text-xs">
                <div>
                  <label className="block text-[10px] font-bold text-brand-muted mb-0.5">Flat / House / Floor</label>
                  <input
                    type="text"
                    value={customAddress.flat}
                    onChange={(e) => setCustomAddress({ ...customAddress, flat: e.target.value })}
                    placeholder="e.g. Flat 302, Galaxy Heights"
                    className="w-full px-2.5 py-1.5 bg-white border border-brand-border rounded text-xs"
                  />
                </div>
                <div>
                  <label className="block text-[10px] font-bold text-brand-muted mb-0.5">Street / Area / Landmark</label>
                  <input
                    type="text"
                    value={customAddress.street}
                    onChange={(e) => setCustomAddress({ ...customAddress, street: e.target.value })}
                    placeholder="e.g. Near Sindhu Bhavan Road"
                    className="w-full px-2.5 py-1.5 bg-white border border-brand-border rounded text-xs"
                  />
                </div>
                <div className="grid grid-cols-2 gap-2">
                  <div>
                    <label className="block text-[10px] font-bold text-brand-muted mb-0.5">City</label>
                    <input
                      type="text"
                      value={customAddress.city}
                      onChange={(e) => setCustomAddress({ ...customAddress, city: e.target.value })}
                      className="w-full px-2.5 py-1.5 bg-white border border-brand-border rounded text-xs"
                    />
                  </div>
                  <div>
                    <label className="block text-[10px] font-bold text-brand-muted mb-0.5">Pincode</label>
                    <input
                      type="text"
                      value={customAddress.pincode}
                      onChange={(e) => setCustomAddress({ ...customAddress, pincode: e.target.value })}
                      className="w-full px-2.5 py-1.5 bg-white border border-brand-border rounded text-xs"
                    />
                  </div>
                </div>
              </div>
            ) : (
              <div className="space-y-2">
                {defaultAddresses.map((a) => (
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
                      <p className="text-brand-muted leading-relaxed text-[11px]">
                        {a.flat}, {a.street}, {a.city} - {a.pincode}
                      </p>
                    </div>
                    {selectedAddress === a.id && (
                      <div className="w-5 h-5 rounded-full bg-brand-primary text-white flex items-center justify-center shrink-0">
                        <Check size={12} strokeWidth={3} />
                      </div>
                    )}
                  </div>
                ))}
              </div>
            )}
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
                onClick={() => setPaymentMethod('RAZORPAY')}
                className={`p-3 rounded-card border cursor-pointer transition-all flex flex-col justify-between ${
                  paymentMethod === 'RAZORPAY'
                    ? 'border-brand-primary bg-brand-primary/5 ring-1 ring-brand-primary'
                    : 'border-brand-border hover:bg-brand-bg'
                }`}
              >
                <div className="flex items-center justify-between mb-2">
                  <CreditCard
                    size={18}
                    className={paymentMethod === 'RAZORPAY' ? 'text-brand-primary' : 'text-brand-muted'}
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
                onClick={() => setPaymentMethod('COD')}
                className={`p-3 rounded-card border cursor-pointer transition-all flex flex-col justify-between ${
                  paymentMethod === 'COD'
                    ? 'border-brand-primary bg-brand-primary/5 ring-1 ring-brand-primary'
                    : 'border-brand-border hover:bg-brand-bg'
                }`}
              >
                <div className="flex items-center justify-between mb-2">
                  <Banknote
                    size={18}
                    className={paymentMethod === 'COD' ? 'text-brand-primary' : 'text-brand-muted'}
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
                <span>Processing with Dark Store & Dispatch...</span>
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
