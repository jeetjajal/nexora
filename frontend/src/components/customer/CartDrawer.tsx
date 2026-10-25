'use client';

import React, { useState } from 'react';
import { useApp } from '@/context/AppContext';
import { X, Plus, Minus, Trash2, ArrowRight, Tag, ShieldCheck, HeartHandshake, CheckCircle2 } from 'lucide-react';

export const CartDrawer: React.FC = () => {
  const {
    isCartOpen,
    setIsCartOpen,
    getCartItems,
    getCartTotal,
    addToCart,
    removeFromCart,
    clearCart,
    couponCode,
    appliedDiscount,
    applyCoupon,
    setIsCheckoutOpen,
  } = useApp();

  const [inputCoupon, setInputCoupon] = useState('');
  const [couponError, setCouponError] = useState('');
  const [selectedTip, setSelectedTip] = useState<number | null>(20);

  if (!isCartOpen) return null;

  const items = getCartItems();
  const subtotal = getCartTotal();
  const deliveryFee = subtotal >= 199 ? 0 : 25;
  const handlingFee = items.length > 0 ? 4 : 0;
  const tipAmount = selectedTip || 0;
  const finalTotal = Math.max(0, subtotal + deliveryFee + handlingFee + tipAmount - appliedDiscount);

  const handleApply = (e: React.FormEvent) => {
    e.preventDefault();
    if (!inputCoupon.trim()) return;
    const success = applyCoupon(inputCoupon);
    if (success) {
      setCouponError('');
    } else {
      setCouponError('Invalid coupon. Try NEXORA50 for 50% discount!');
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex justify-end">
      {/* Backdrop */}
      <div
        className="fixed inset-0 bg-brand-text/50 backdrop-blur-sm transition-opacity"
        onClick={() => setIsCartOpen(false)}
      />

      {/* Drawer Container */}
      <div className="relative w-full max-w-md bg-brand-bg h-full flex flex-col z-10 shadow-modal animate-in slide-in-from-right duration-200">
        {/* Drawer Header */}
        <div className="bg-white px-4 py-3.5 border-b border-brand-border flex items-center justify-between">
          <div className="flex items-center gap-2">
            <h3 className="text-base font-black text-brand-text">My Cart</h3>
            <span className="text-xs bg-brand-primary/10 text-brand-primary font-bold px-2 py-0.5 rounded-full">
              {items.length} items
            </span>
          </div>
          <button
            onClick={() => setIsCartOpen(false)}
            className="p-1.5 rounded-full hover:bg-brand-bg text-brand-muted hover:text-brand-text transition-colors"
          >
            <X size={18} />
          </button>
        </div>

        {/* Content Area */}
        {items.length === 0 ? (
          <div className="flex-1 flex flex-col items-center justify-center p-6 text-center">
            <div className="w-20 h-20 bg-white rounded-full flex items-center justify-center text-4xl mb-4 shadow-sm border border-brand-border">
              🛒
            </div>
            <h4 className="text-lg font-bold text-brand-text mb-1">Your cart is empty</h4>
            <p className="text-xs text-brand-muted max-w-xs mb-6">
              Add fresh groceries, daily essentials, snacks, and milk to get 10-minute instant delivery!
            </p>
            <button
              onClick={() => setIsCartOpen(false)}
              className="px-6 py-2.5 bg-brand-primary text-white font-bold rounded-button text-sm hover:bg-brand-primaryHover shadow-sm"
            >
              Browse Products
            </button>
          </div>
        ) : (
          <div className="flex-1 overflow-y-auto p-4 space-y-4">
            {/* Delivery Time Banner */}
            <div className="bg-white p-3 rounded-card border border-brand-border flex items-center gap-3 shadow-card">
              <div className="w-10 h-10 rounded-full bg-brand-accent flex items-center justify-center text-lg shrink-0">
                ⚡
              </div>
              <div>
                <p className="text-xs font-black text-brand-text">Delivery in 9 MINS</p>
                <p className="text-[11px] text-brand-muted">Shipment of {items.length} items from Dark Store #04</p>
              </div>
            </div>

            {/* Cart Items List */}
            <div className="bg-white rounded-card border border-brand-border p-3 divide-y divide-brand-border shadow-card">
              {items.map(({ product, quantity }) => (
                <div key={product.id} className="py-2.5 first:pt-0 last:pb-0 flex items-center justify-between gap-3">
                  {/* eslint-disable-next-line @next/next/no-img-element */}
                  <img
                    src={product.image}
                    alt={product.name}
                    className="w-12 h-12 object-cover rounded-md border border-brand-border shrink-0"
                  />
                  <div className="flex-1 min-w-0">
                    <h5 className="text-xs font-bold text-brand-text truncate">{product.name}</h5>
                    <p className="text-[11px] text-brand-muted">{product.unit}</p>
                    <div className="flex items-center gap-1.5 mt-0.5">
                      <span className="text-xs font-black text-brand-text">₹{product.price * quantity}</span>
                      {product.mrp > product.price && (
                        <span className="text-[10px] text-brand-muted line-through">₹{product.mrp * quantity}</span>
                      )}
                    </div>
                  </div>

                  {/* Quantity Counter */}
                  <div className="flex items-center bg-brand-primary text-white rounded-button overflow-hidden shadow-sm shrink-0">
                    <button
                      onClick={() => removeFromCart(product.id)}
                      className="px-2 py-1 hover:bg-brand-primaryHover"
                    >
                      <Minus size={11} strokeWidth={3} />
                    </button>
                    <span className="px-2 text-xs font-black min-w-[16px] text-center">{quantity}</span>
                    <button
                      onClick={() => addToCart(product.id)}
                      className="px-2 py-1 hover:bg-brand-primaryHover"
                    >
                      <Plus size={11} strokeWidth={3} />
                    </button>
                  </div>
                </div>
              ))}

              <div className="pt-2 flex justify-end">
                <button
                  onClick={clearCart}
                  className="text-[11px] text-brand-danger font-semibold flex items-center gap-1 hover:underline"
                >
                  <Trash2 size={11} /> Clear All
                </button>
              </div>
            </div>

            {/* Coupon Code Section */}
            <div className="bg-white rounded-card border border-brand-border p-3.5 shadow-card">
              <form onSubmit={handleApply} className="flex gap-2">
                <div className="relative flex-1">
                  <Tag size={14} className="absolute left-3 top-1/2 -translate-y-1/2 text-brand-muted" />
                  <input
                    type="text"
                    value={inputCoupon}
                    onChange={(e) => setInputCoupon(e.target.value.toUpperCase())}
                    placeholder="Enter Coupon (e.g. NEXORA50)"
                    className="w-full pl-8 pr-3 py-1.5 text-xs font-mono uppercase bg-brand-bg border border-brand-border rounded-input focus:outline-none focus:ring-1 focus:ring-brand-primary"
                  />
                </div>
                <button
                  type="submit"
                  className="px-3.5 py-1.5 bg-brand-primary text-white rounded-button font-bold text-xs hover:bg-brand-primaryHover transition-all shrink-0"
                >
                  Apply
                </button>
              </form>

              {appliedDiscount > 0 && (
                <div className="mt-2 flex items-center justify-between bg-[#edf7ed] p-2 rounded text-xs text-brand-primary font-bold">
                  <span className="flex items-center gap-1">
                    <CheckCircle2 size={13} /> Code {couponCode} applied!
                  </span>
                  <span>-₹{appliedDiscount}</span>
                </div>
              )}
              {couponError && <p className="text-[11px] text-brand-danger mt-1.5 font-medium">{couponError}</p>}
            </div>

            {/* Tip Your Delivery Partner */}
            <div className="bg-white rounded-card border border-brand-border p-3.5 shadow-card">
              <div className="flex items-center gap-1.5 text-xs font-bold text-brand-text mb-2">
                <HeartHandshake size={14} className="text-brand-primary" />
                <span>Tip Your Delivery Partner</span>
              </div>
              <p className="text-[11px] text-brand-muted mb-2.5">
                100% of your tip goes directly to the delivery rider who braves the traffic.
              </p>
              <div className="flex gap-2">
                {[10, 20, 30, 50].map((amount) => (
                  <button
                    key={amount}
                    onClick={() => setSelectedTip(selectedTip === amount ? null : amount)}
                    className={`flex-1 py-1.5 rounded-button text-xs font-bold transition-all border ${
                      selectedTip === amount
                        ? 'bg-brand-accent text-brand-text border-brand-accent shadow-sm'
                        : 'bg-brand-bg text-brand-text border-brand-border hover:bg-white'
                    }`}
                  >
                    ₹{amount}
                  </button>
                ))}
              </div>
            </div>

            {/* Bill Summary */}
            <div className="bg-white rounded-card border border-brand-border p-3.5 shadow-card space-y-2 text-xs">
              <h5 className="font-black text-brand-text mb-1 uppercase tracking-wider text-[11px]">Bill Details</h5>
              <div className="flex justify-between text-brand-muted">
                <span>Items Subtotal</span>
                <span className="font-semibold text-brand-text">₹{subtotal}</span>
              </div>
              <div className="flex justify-between text-brand-muted">
                <span>Delivery Charge</span>
                <span className="font-semibold text-brand-text">
                  {deliveryFee === 0 ? <span className="text-brand-primary font-bold">FREE</span> : `₹${deliveryFee}`}
                </span>
              </div>
              <div className="flex justify-between text-brand-muted">
                <span>Handling Fee</span>
                <span className="font-semibold text-brand-text">₹{handlingFee}</span>
              </div>
              {appliedDiscount > 0 && (
                <div className="flex justify-between text-brand-primary font-bold">
                  <span>Coupon Discount ({couponCode})</span>
                  <span>-₹{appliedDiscount}</span>
                </div>
              )}
              {selectedTip && (
                <div className="flex justify-between text-brand-muted">
                  <span>Delivery Partner Tip</span>
                  <span className="font-semibold text-brand-text">₹{selectedTip}</span>
                </div>
              )}
              <div className="border-t border-brand-border pt-2 flex justify-between font-black text-sm text-brand-text">
                <span>To Pay</span>
                <span className="text-base text-brand-primary">₹{finalTotal}</span>
              </div>
            </div>

            {/* Trust Badges */}
            <div className="flex items-center justify-center gap-3 text-[11px] text-brand-muted font-medium py-1">
              <span className="flex items-center gap-1">
                <ShieldCheck size={13} className="text-brand-primary" /> Genuine Brands
              </span>
              <span>•</span>
              <span>No-Questions-Asked Return</span>
            </div>
          </div>
        )}

        {/* Drawer Footer CTA */}
        {items.length > 0 && (
          <div className="bg-white p-3.5 border-t border-brand-border shadow-modal">
            <button
              onClick={() => {
                setIsCartOpen(false);
                setIsCheckoutOpen(true);
              }}
              className="w-full py-3 bg-brand-primary hover:bg-brand-primaryHover text-white rounded-button font-black text-sm flex items-center justify-between px-4 shadow-card transition-all active:scale-98"
            >
              <div className="flex flex-col items-start leading-tight">
                <span className="text-xs text-brand-accent font-bold">₹{finalTotal} TOTAL</span>
                <span className="text-[10px] text-white/80 font-normal">Proceed to Address & Pay</span>
              </div>
              <div className="flex items-center gap-1">
                <span>Checkout</span>
                <ArrowRight size={16} />
              </div>
            </button>
          </div>
        )}
      </div>
    </div>
  );
};

export default CartDrawer;
