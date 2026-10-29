'use client';

import React, { useState } from 'react';
import { useApp } from '@/context/AppContext';
import {
  User,
  Phone,
  Mail,
  MapPin,
  Package,
  Wallet,
  Headphones,
  ShieldCheck,
  LogOut,
  LayoutGrid,
  ChevronRight,
  Plus,
  CheckCircle2,
  Trash2,
  Download,
  Clock,
  Sparkles,
  ArrowRight,
} from 'lucide-react';

export const ProfileView: React.FC = () => {
  const {
    currentUser,
    savedAddresses,
    pastOrders,
    logout,
    setIsAuthOpen,
    addNewAddress,
    deleteAddress,
    setDefaultAddress,
    setIsAIOpen,
    setIsCartOpen,
    addToCart,
    products,
    setIsRolePortalOpen,
  } = useApp();

  const [activeSection, setActiveSection] = useState<'OVERVIEW' | 'ADDRESSES' | 'ORDERS' | 'WALLET'>('OVERVIEW');
  const [isAddingAddr, setIsAddingAddr] = useState(false);
  const [addrForm, setAddrForm] = useState({
    label: 'Home',
    addressLine1: '',
    addressLine2: '',
    city: 'Ahmedabad',
    pincode: '380054',
  });
  const [addrMsg, setAddrMsg] = useState<string | null>(null);

  if (!currentUser) {
    return (
      <div className="bg-white rounded-card border border-brand-border p-8 text-center shadow-card max-w-md mx-auto my-6 space-y-4 animate-in fade-in">
        <div className="w-16 h-16 rounded-full bg-brand-primary/10 text-brand-primary flex items-center justify-center mx-auto text-2xl font-black">
          👤
        </div>
        <div>
          <h3 className="text-lg font-black text-brand-text">Login to Your Account</h3>
          <p className="text-xs text-brand-muted mt-1 max-w-xs mx-auto">
            Access your saved addresses, track live 10-minute orders, and view past receipts just like Blinkit.
          </p>
        </div>

        <button
          onClick={() => setIsAuthOpen(true)}
          className="w-full py-3 bg-brand-primary hover:bg-brand-primaryHover text-white font-black text-sm rounded-button transition-all shadow-md active:scale-98"
        >
          Login or Register
        </button>
      </div>
    );
  }

  const handleSaveAddress = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!addrForm.addressLine1.trim()) {
      setAddrMsg('Please enter flat/house number and street name');
      return;
    }
    try {
      await addNewAddress({
        label: addrForm.label,
        addressLine1: addrForm.addressLine1,
        addressLine2: addrForm.addressLine2,
        city: addrForm.city,
        pincode: addrForm.pincode,
        isDefault: savedAddresses.length === 0,
      });
      setIsAddingAddr(false);
      setAddrForm({ label: 'Home', addressLine1: '', addressLine2: '', city: 'Ahmedabad', pincode: '380054' });
      setAddrMsg('Address added successfully!');
      setTimeout(() => setAddrMsg(null), 3000);
    } catch {
      setAddrMsg('Failed to save address');
    }
  };

  return (
    <div className="max-w-4xl mx-auto space-y-5 animate-in fade-in duration-200">
      {/* 1. Profile Header Card */}
      <div className="bg-white rounded-card border border-brand-border p-5 shadow-card flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div className="flex items-center gap-4">
          <div className="w-14 h-14 rounded-full bg-brand-primary text-white flex items-center justify-center font-black text-xl shadow-sm shrink-0">
            {currentUser.name.charAt(0).toUpperCase()}
          </div>
          <div>
            <div className="flex items-center gap-2">
              <h2 className="text-base sm:text-lg font-black text-brand-text">{currentUser.name}</h2>
              <span className="text-[10px] bg-brand-accent text-brand-text font-black px-2 py-0.5 rounded-full">
                Nexora Club VIP
              </span>
            </div>
            <p className="text-xs text-brand-muted mt-0.5 flex items-center gap-3">
              <span>📱 +91 {currentUser.phone}</span>
              <span>&bull;</span>
              <span>✉️ {currentUser.email}</span>
            </p>
          </div>
        </div>

        <button
          onClick={logout}
          className="self-start sm:self-center px-3.5 py-1.5 bg-brand-bg hover:bg-red-50 text-red-600 border border-brand-border hover:border-red-200 rounded-button text-xs font-bold transition-all flex items-center gap-1.5"
        >
          <LogOut size={13} />
          <span>Log Out</span>
        </button>
      </div>

      {/* 2. Blinkit-Style Profile Navigation Tabs */}
      <div className="flex items-center gap-2 border-b border-brand-border pb-1 overflow-x-auto scrollbar-none">
        <button
          onClick={() => setActiveSection('OVERVIEW')}
          className={`px-4 py-2 rounded-button text-xs font-bold transition-all whitespace-nowrap ${
            activeSection === 'OVERVIEW'
              ? 'bg-brand-primary text-white shadow-xs'
              : 'bg-white text-brand-muted hover:text-brand-text border border-brand-border'
          }`}
        >
          Dashboard &amp; Wallet
        </button>
        <button
          onClick={() => setActiveSection('ORDERS')}
          className={`px-4 py-2 rounded-button text-xs font-bold transition-all whitespace-nowrap ${
            activeSection === 'ORDERS'
              ? 'bg-brand-primary text-white shadow-xs'
              : 'bg-white text-brand-muted hover:text-brand-text border border-brand-border'
          }`}
        >
          Your Orders ({pastOrders.length})
        </button>
        <button
          onClick={() => setActiveSection('ADDRESSES')}
          className={`px-4 py-2 rounded-button text-xs font-bold transition-all whitespace-nowrap ${
            activeSection === 'ADDRESSES'
              ? 'bg-brand-primary text-white shadow-xs'
              : 'bg-white text-brand-muted hover:text-brand-text border border-brand-border'
          }`}
        >
          Saved Addresses ({savedAddresses.length})
        </button>
      </div>

      {/* 3. Section: Dashboard & Wallet Overview */}
      {activeSection === 'OVERVIEW' && (
        <div className="space-y-4">
          {/* Quick Metrics */}
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
            <div className="bg-white p-3.5 rounded-card border border-brand-border shadow-xs">
              <span className="text-[11px] text-brand-muted font-bold block mb-0.5">Nexora Wallet</span>
              <span className="text-xl font-black text-brand-primary">₹250.00</span>
              <span className="text-[10px] text-emerald-600 font-bold block mt-0.5">Cashback balance</span>
            </div>

            <div className="bg-white p-3.5 rounded-card border border-brand-border shadow-xs">
              <span className="text-[11px] text-brand-muted font-bold block mb-0.5">Total Orders</span>
              <span className="text-xl font-black text-brand-text">{pastOrders.length || 6} Placed</span>
              <span className="text-[10px] text-brand-primary font-bold block mt-0.5">100% on-time</span>
            </div>

            <div className="bg-white p-3.5 rounded-card border border-brand-border shadow-xs">
              <span className="text-[11px] text-brand-muted font-bold block mb-0.5">Total Savings</span>
              <span className="text-xl font-black text-emerald-600">₹640</span>
              <span className="text-[10px] text-brand-muted block mt-0.5">Discounts &amp; coupons</span>
            </div>

            <div className="bg-white p-3.5 rounded-card border border-brand-border shadow-xs">
              <span className="text-[11px] text-brand-muted font-bold block mb-0.5">Avg Delivery</span>
              <span className="text-xl font-black text-brand-text">8.4 Mins</span>
              <span className="text-[10px] text-brand-muted block mt-0.5">Nearest Hub #04</span>
            </div>
          </div>

          {/* Blinkit-Style Action Cards */}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
            <div
              onClick={() => setActiveSection('ORDERS')}
              className="bg-white p-4 rounded-card border border-brand-border shadow-xs hover:border-brand-primary cursor-pointer transition-all flex items-center justify-between"
            >
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 rounded-full bg-blue-50 text-blue-600 flex items-center justify-center font-bold">
                  <Package size={20} />
                </div>
                <div>
                  <h4 className="text-xs font-black text-brand-text">Your Past Orders</h4>
                  <p className="text-[11px] text-brand-muted">View order receipts &amp; 1-click reorder</p>
                </div>
              </div>
              <ChevronRight size={16} className="text-brand-muted" />
            </div>

            <div
              onClick={() => setActiveSection('ADDRESSES')}
              className="bg-white p-4 rounded-card border border-brand-border shadow-xs hover:border-brand-primary cursor-pointer transition-all flex items-center justify-between"
            >
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 rounded-full bg-emerald-50 text-emerald-600 flex items-center justify-center font-bold">
                  <MapPin size={20} />
                </div>
                <div>
                  <h4 className="text-xs font-black text-brand-text">Saved Delivery Addresses</h4>
                  <p className="text-[11px] text-brand-muted">Manage Home, Office &amp; GPS pins</p>
                </div>
              </div>
              <ChevronRight size={16} className="text-brand-muted" />
            </div>

            <div
              onClick={() => setIsAIOpen(true)}
              className="bg-white p-4 rounded-card border border-brand-border shadow-xs hover:border-brand-primary cursor-pointer transition-all flex items-center justify-between"
            >
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 rounded-full bg-amber-50 text-amber-600 flex items-center justify-center font-bold">
                  <Headphones size={20} />
                </div>
                <div>
                  <h4 className="text-xs font-black text-brand-text">24x7 Customer Support</h4>
                  <p className="text-[11px] text-brand-muted">Instant Nexora AI shopping &amp; refunds</p>
                </div>
              </div>
              <ChevronRight size={16} className="text-brand-muted" />
            </div>

            <div className="bg-white p-4 rounded-card border border-brand-border shadow-xs flex items-center justify-between">
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 rounded-full bg-purple-50 text-purple-600 flex items-center justify-center font-bold">
                  <ShieldCheck size={20} />
                </div>
                <div>
                  <h4 className="text-xs font-black text-brand-text">100% Quality Guarantee</h4>
                  <p className="text-[11px] text-brand-muted">Instant refund or replacement at doorstep</p>
                </div>
              </div>
              <span className="text-[10px] bg-emerald-100 text-emerald-800 font-bold px-2 py-0.5 rounded">
                Active
              </span>
            </div>

            {/* Staff & Partner Portal Entry */}
            <div
              onClick={() => setIsRolePortalOpen(true)}
              className="bg-gradient-to-r from-emerald-50 to-green-50 p-4 rounded-card border border-brand-primary/30 shadow-xs hover:border-brand-primary cursor-pointer transition-all flex items-center justify-between sm:col-span-2"
            >
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 rounded-full bg-brand-primary text-white flex items-center justify-center font-bold shadow-xs">
                  <LayoutGrid size={20} />
                </div>
                <div>
                  <div className="flex items-center gap-2">
                    <h4 className="text-xs font-black text-brand-text">Staff &amp; Partner Portal (પોર્ટલ પસંદ કરો)</h4>
                    <span className="text-[10px] bg-brand-accent text-brand-text font-black px-1.5 py-0.2 rounded">
                      Rider / Store / Admin
                    </span>
                  </div>
                  <p className="text-[11px] text-brand-muted">Switch to Delivery Partner, Dark Store Operations, or Super Admin Tower</p>
                </div>
              </div>
              <ChevronRight size={16} className="text-brand-primary" />
            </div>
          </div>
        </div>
      )}

      {/* 4. Section: Orders History */}
      {activeSection === 'ORDERS' && (
        <div className="bg-white rounded-card border border-brand-border shadow-xs p-4 sm:p-5 space-y-4">
          <div className="flex items-center justify-between">
            <h3 className="text-sm font-black text-brand-text">Past Order Receipts</h3>
            <span className="text-xs text-brand-muted">{pastOrders.length} orders on record</span>
          </div>

          <div className="divide-y divide-brand-border">
            {pastOrders.map((order) => (
              <div key={order.orderId} className="py-3.5 flex flex-col sm:flex-row sm:items-center justify-between gap-3">
                <div>
                  <div className="flex items-center gap-2">
                    <span className="text-xs font-black text-brand-text">Order #{order.orderId}</span>
                    <span className="text-[10px] bg-emerald-100 text-emerald-800 font-bold px-2 py-0.2 rounded-full">
                      {order.status}
                    </span>
                  </div>
                  <p className="text-xs text-brand-muted mt-0.5">{order.itemsSummary}</p>
                  <p className="text-[11px] text-brand-muted mt-0.5">
                    {order.date} &bull; Paid via {order.paymentMethod} &bull; OTP: <span className="font-mono font-bold text-brand-text">{order.deliveryOtp}</span>
                  </p>
                </div>

                <div className="flex items-center gap-2 shrink-0">
                  <span className="text-sm font-black text-brand-primary mr-2">₹{order.amount}</span>
                  <button
                    onClick={() => {
                      // Add first 2 staple products to cart
                      if (products.length > 0) addToCart(products[0].id);
                      setIsCartOpen(true);
                    }}
                    className="px-3 py-1.5 bg-brand-primary text-white rounded-button text-xs font-black hover:bg-brand-primaryHover transition-all active:scale-95"
                  >
                    Repeat Order
                  </button>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* 5. Section: Saved Addresses */}
      {activeSection === 'ADDRESSES' && (
        <div className="bg-white rounded-card border border-brand-border shadow-xs p-4 sm:p-5 space-y-4">
          <div className="flex items-center justify-between">
            <h3 className="text-sm font-black text-brand-text">Your Delivery Locations</h3>
            <button
              onClick={() => setIsAddingAddr(!isAddingAddr)}
              className="px-3 py-1.5 bg-brand-primary text-white rounded-button text-xs font-bold hover:bg-brand-primaryHover transition-all inline-flex items-center gap-1"
            >
              <Plus size={13} />
              <span>Add New Address</span>
            </button>
          </div>

          {addrMsg && (
            <div className="p-2.5 bg-emerald-50 text-emerald-800 rounded-lg text-xs font-semibold flex items-center gap-2">
              <CheckCircle2 size={14} className="text-emerald-600" />
              <span>{addrMsg}</span>
            </div>
          )}

          {/* Add Address Form */}
          {isAddingAddr && (
            <form onSubmit={handleSaveAddress} className="p-4 bg-brand-bg rounded-card border border-brand-border space-y-3">
              <h4 className="text-xs font-black text-brand-text">Add New Delivery Address</h4>
              <div className="grid grid-cols-1 sm:grid-cols-3 gap-2">
                <input
                  type="text"
                  placeholder="Address Tag (Home, Work, Gym)"
                  value={addrForm.label}
                  onChange={(e) => setAddrForm({ ...addrForm, label: e.target.value })}
                  className="px-3 py-2 bg-white border border-brand-border rounded-input text-xs"
                />
                <input
                  type="text"
                  placeholder="Flat / House No. & Building"
                  value={addrForm.addressLine1}
                  onChange={(e) => setAddrForm({ ...addrForm, addressLine1: e.target.value })}
                  className="px-3 py-2 bg-white border border-brand-border rounded-input text-xs sm:col-span-2"
                  required
                />
              </div>
              <div className="grid grid-cols-2 gap-2">
                <input
                  type="text"
                  placeholder="City (e.g. Ahmedabad)"
                  value={addrForm.city}
                  onChange={(e) => setAddrForm({ ...addrForm, city: e.target.value })}
                  className="px-3 py-2 bg-white border border-brand-border rounded-input text-xs"
                />
                <input
                  type="text"
                  placeholder="Pincode (e.g. 380054)"
                  value={addrForm.pincode}
                  onChange={(e) => setAddrForm({ ...addrForm, pincode: e.target.value })}
                  className="px-3 py-2 bg-white border border-brand-border rounded-input text-xs"
                />
              </div>
              <div className="flex justify-end gap-2 pt-1">
                <button
                  type="button"
                  onClick={() => setIsAddingAddr(false)}
                  className="px-3 py-1.5 bg-white border border-brand-border rounded-button text-xs font-bold text-brand-text"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="px-4 py-1.5 bg-brand-primary text-white rounded-button text-xs font-black hover:bg-brand-primaryHover"
                >
                  Save Address
                </button>
              </div>
            </form>
          )}

          {/* Addresses List */}
          <div className="space-y-2.5">
            {savedAddresses.map((addr) => (
              <div
                key={addr.id}
                className="p-3.5 bg-brand-bg/50 rounded-card border border-brand-border flex items-center justify-between gap-3"
              >
                <div className="flex items-start gap-3">
                  <div className="w-8 h-8 rounded-full bg-brand-primary/10 text-brand-primary flex items-center justify-center shrink-0 mt-0.5">
                    <MapPin size={16} />
                  </div>
                  <div>
                    <div className="flex items-center gap-2">
                      <h4 className="text-xs font-black text-brand-text">{addr.label}</h4>
                      {addr.isDefault && (
                        <span className="text-[9px] bg-emerald-100 text-emerald-800 font-bold px-1.5 py-0.2 rounded">
                          Default
                        </span>
                      )}
                      <span className="text-[10px] bg-brand-primary/10 text-brand-primary font-bold px-1.5 py-0.2 rounded">
                        ⚡ {addr.eta || '9 MINS'}
                      </span>
                    </div>
                    <p className="text-xs text-brand-muted mt-0.5">{addr.addressLine1}, {addr.city} - {addr.pincode}</p>
                  </div>
                </div>

                <div className="flex items-center gap-2 shrink-0">
                  {!addr.isDefault && (
                    <button
                      onClick={() => setDefaultAddress(addr.id)}
                      className="px-2.5 py-1 text-[11px] font-bold text-brand-primary hover:bg-brand-primary/10 rounded transition-colors"
                    >
                      Make Default
                    </button>
                  )}
                  <button
                    onClick={() => deleteAddress(addr.id)}
                    className="p-1.5 text-brand-muted hover:text-red-600 rounded transition-colors"
                    title="Delete address"
                  >
                    <Trash2 size={14} />
                  </button>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
};

export default ProfileView;
