'use client';

import React, { useState } from 'react';
import { useApp } from '@/context/AppContext';
import {
  X,
  User,
  MapPin,
  Package,
  LogOut,
  Plus,
  CheckCircle2,
  Clock,
  ShieldCheck,
  Building,
  AlertCircle,
  Truck,
  Edit2,
  Trash2,
  Check,
} from 'lucide-react';

export const AccountModal: React.FC = () => {
  const {
    isAccountOpen,
    setIsAccountOpen,
    currentUser,
    savedAddresses,
    pastOrders,
    updateProfile,
    logout,
    addNewAddress,
    setDefaultAddress,
    deleteAddress,
  } = useApp();

  const [activeTab, setActiveTab] = useState<'PROFILE' | 'ADDRESSES' | 'ORDERS'>('PROFILE');
  
  // Profile edit states
  const [isEditingProfile, setIsEditingProfile] = useState(false);
  const [profileName, setProfileName] = useState(currentUser?.name || '');
  const [profilePhone, setProfilePhone] = useState(currentUser?.phone || '');
  const [profileMsg, setProfileMsg] = useState<{ type: 'success' | 'error'; text: string } | null>(null);
  const [isUpdatingProfile, setIsUpdatingProfile] = useState(false);

  // Address add states
  const [isAddingAddr, setIsAddingAddr] = useState(false);
  const [addrForm, setAddrForm] = useState({
    label: 'Home',
    addressLine1: '',
    addressLine2: '',
    city: 'Ahmedabad',
    pincode: '380054',
  });
  const [addrError, setAddrError] = useState<string | null>(null);
  const [addrSuccess, setAddrSuccess] = useState<string | null>(null);
  const [isSavingAddr, setIsSavingAddr] = useState(false);

  // Order detail expansion state
  const [expandedOrderId, setExpandedOrderId] = useState<string | null>(null);

  if (!isAccountOpen || !currentUser) return null;

  const handleUpdateProfile = async (e: React.FormEvent) => {
    e.preventDefault();
    setProfileMsg(null);
    setIsUpdatingProfile(true);

    try {
      if (!profileName.trim() || profileName.trim().length < 2) {
        throw new Error('Name must be at least 2 characters long');
      }
      if (!profilePhone.trim() || !/^[6-9]\d{9}$/.test(profilePhone.replace(/[\s+-]/g, '').slice(-10))) {
        throw new Error('Please enter a valid 10-digit Indian phone number');
      }

      await updateProfile(profileName.trim(), profilePhone.trim());
      setProfileMsg({ type: 'success', text: 'Profile updated successfully!' });
      setIsEditingProfile(false);
    } catch (err: any) {
      setProfileMsg({ type: 'error', text: err.message || 'Failed to update profile' });
    } finally {
      setIsUpdatingProfile(false);
    }
  };

  const handleSaveAddress = async (e: React.FormEvent) => {
    e.preventDefault();
    setAddrError(null);
    setAddrSuccess(null);
    setIsSavingAddr(true);

    try {
      if (!addrForm.addressLine1.trim()) throw new Error('Flat / House / Street is required');
      if (!addrForm.pincode.trim()) throw new Error('PIN code is required');

      const saved = await addNewAddress({
        label: addrForm.label,
        addressLine1: addrForm.addressLine1.trim(),
        addressLine2: addrForm.addressLine2.trim() || undefined,
        city: addrForm.city.trim() || 'Ahmedabad',
        pincode: addrForm.pincode.trim(),
        state: 'Gujarat',
      });

      setAddrSuccess(`Address saved! Served by ${saved.darkStore || 'Nearest Dark Store'}`);
      setAddrForm({ label: 'Home', addressLine1: '', addressLine2: '', city: 'Ahmedabad', pincode: '380054' });
      setIsAddingAddr(false);
    } catch (err: any) {
      setAddrError(err.message || 'Failed to save address');
    } finally {
      setIsSavingAddr(false);
    }
  };

  const handleDeleteAddr = async (id: number) => {
    try {
      await deleteAddress(id);
    } catch (err: any) {
      setAddrError(err.message || 'Could not remove address');
    }
  };

  const handleSetDefaultAddr = async (id: number) => {
    try {
      await setDefaultAddress(id);
    } catch (err: any) {
      setAddrError(err.message || 'Could not set default address');
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
      {/* Backdrop */}
      <div
        className="fixed inset-0 bg-brand-text/60 backdrop-blur-sm transition-opacity"
        onClick={() => setIsAccountOpen(false)}
      />

      {/* Modal */}
      <div className="relative w-full max-w-xl bg-white rounded-card shadow-modal z-10 overflow-hidden border border-brand-border animate-in zoom-in-95 duration-150 flex flex-col max-h-[85vh]">
        {/* Header */}
        <div className="bg-brand-primary text-white p-5 flex items-center justify-between shrink-0">
          <div className="flex items-center gap-3">
            <div className="w-11 h-11 rounded-full bg-brand-accent text-brand-text flex items-center justify-center font-black text-lg shadow-sm">
              {currentUser.name.charAt(0).toUpperCase()}
            </div>
            <div>
              <div className="flex items-center gap-2">
                <h3 className="text-base font-black">{currentUser.name}</h3>
                <span className="text-[10px] bg-brand-accent text-brand-text font-black px-2 py-0.5 rounded-full">
                  Verified Member
                </span>
              </div>
              <p className="text-[11px] text-white/80">{currentUser.email} • {currentUser.phone}</p>
            </div>
          </div>
          <button
            onClick={() => setIsAccountOpen(false)}
            className="p-1.5 rounded-full hover:bg-white/10 text-white transition-colors"
          >
            <X size={18} />
          </button>
        </div>

        {/* Navigation Tabs */}
        <div className="flex border-b border-brand-border bg-brand-bg/50 text-xs font-bold shrink-0">
          <button
            onClick={() => setActiveTab('PROFILE')}
            className={`flex-1 py-3 flex items-center justify-center gap-1.5 transition-all ${
              activeTab === 'PROFILE'
                ? 'border-b-2 border-brand-primary text-brand-primary bg-white font-black'
                : 'text-brand-muted hover:text-brand-text'
            }`}
          >
            <User size={14} />
            <span>Profile</span>
          </button>

          <button
            onClick={() => setActiveTab('ADDRESSES')}
            className={`flex-1 py-3 flex items-center justify-center gap-1.5 transition-all ${
              activeTab === 'ADDRESSES'
                ? 'border-b-2 border-brand-primary text-brand-primary bg-white font-black'
                : 'text-brand-muted hover:text-brand-text'
            }`}
          >
            <MapPin size={14} />
            <span>Saved Addresses ({savedAddresses.length})</span>
          </button>

          <button
            onClick={() => setActiveTab('ORDERS')}
            className={`flex-1 py-3 flex items-center justify-center gap-1.5 transition-all ${
              activeTab === 'ORDERS'
                ? 'border-b-2 border-brand-primary text-brand-primary bg-white font-black'
                : 'text-brand-muted hover:text-brand-text'
            }`}
          >
            <Package size={14} />
            <span>Order History ({pastOrders.length})</span>
          </button>
        </div>

        {/* Tab Content */}
        <div className="p-5 overflow-y-auto space-y-4 flex-1 text-xs">
          {/* 1. Profile Tab */}
          {activeTab === 'PROFILE' && (
            <div className="space-y-4">
              {profileMsg && (
                <div
                  className={`p-2.5 rounded border text-xs flex items-center gap-1.5 ${
                    profileMsg.type === 'success'
                      ? 'bg-emerald-50 text-emerald-800 border-emerald-200'
                      : 'bg-red-50 text-red-700 border-red-200'
                  }`}
                >
                  {profileMsg.type === 'success' ? <CheckCircle2 size={13} /> : <AlertCircle size={13} />}
                  <span>{profileMsg.text}</span>
                </div>
              )}

              <div className="bg-brand-bg p-4 rounded-card border border-brand-border space-y-3">
                <div className="flex items-center justify-between">
                  <h4 className="font-black text-brand-text text-sm">Customer Profile Information</h4>
                  <button
                    onClick={() => {
                      setIsEditingProfile(!isEditingProfile);
                      setProfileName(currentUser.name);
                      setProfilePhone(currentUser.phone);
                    }}
                    className="text-[11px] font-bold text-brand-primary hover:underline flex items-center gap-1"
                  >
                    <Edit2 size={12} />
                    <span>{isEditingProfile ? 'Cancel' : 'Edit Profile'}</span>
                  </button>
                </div>

                {isEditingProfile ? (
                  <form onSubmit={handleUpdateProfile} className="space-y-3 pt-2">
                    <div>
                      <label className="block text-[10px] font-bold text-brand-muted mb-1">Full Name</label>
                      <input
                        type="text"
                        value={profileName}
                        onChange={(e) => setProfileName(e.target.value)}
                        className="w-full px-2.5 py-1.5 bg-white border border-brand-border rounded text-xs font-semibold"
                        required
                      />
                    </div>
                    <div>
                      <label className="block text-[10px] font-bold text-brand-muted mb-1">Phone Number (10 digits)</label>
                      <input
                        type="tel"
                        value={profilePhone}
                        onChange={(e) => setProfilePhone(e.target.value)}
                        className="w-full px-2.5 py-1.5 bg-white border border-brand-border rounded text-xs font-mono font-semibold"
                        required
                      />
                    </div>
                    <div className="flex gap-2 pt-1">
                      <button
                        type="submit"
                        disabled={isUpdatingProfile}
                        className="px-4 py-2 bg-brand-primary text-white font-bold rounded text-xs hover:bg-brand-primaryHover transition-colors"
                      >
                        {isUpdatingProfile ? 'Saving...' : 'Save Changes'}
                      </button>
                      <button
                        type="button"
                        onClick={() => setIsEditingProfile(false)}
                        className="px-3 py-2 bg-gray-100 text-brand-text font-bold rounded text-xs hover:bg-gray-200"
                      >
                        Cancel
                      </button>
                    </div>
                  </form>
                ) : (
                  <div className="grid grid-cols-2 gap-3">
                    <div>
                      <span className="text-[11px] text-brand-muted block">Full Name</span>
                      <p className="font-bold text-brand-text">{currentUser.name}</p>
                    </div>
                    <div>
                      <span className="text-[11px] text-brand-muted block">Registered Email</span>
                      <p className="font-bold text-brand-text">{currentUser.email}</p>
                    </div>
                    <div>
                      <span className="text-[11px] text-brand-muted block">Phone Number</span>
                      <p className="font-bold text-brand-text font-mono">{currentUser.phone}</p>
                    </div>
                    <div>
                      <span className="text-[11px] text-brand-muted block">Account Status</span>
                      <span className="inline-flex items-center gap-1 text-[11px] font-bold text-emerald-700 bg-emerald-100 px-2 py-0.5 rounded-full mt-0.5">
                        <CheckCircle2 size={11} /> {currentUser.status || 'ACTIVE'}
                      </span>
                    </div>
                  </div>
                )}
              </div>

              <div className="bg-white p-4 rounded-card border border-brand-border flex items-center justify-between">
                <div>
                  <h4 className="font-bold text-brand-text">Active Assigned Roles</h4>
                  <div className="flex gap-1.5 mt-1.5">
                    {currentUser.roles.map((r) => (
                      <span
                        key={r}
                        className="text-[10px] bg-brand-primary/10 text-brand-primary font-bold px-2 py-0.5 rounded-full"
                      >
                        {r.replace('ROLE_', '')}
                      </span>
                    ))}
                  </div>
                </div>
                <div className="flex items-center gap-1 text-[11px] text-brand-muted">
                  <ShieldCheck size={14} className="text-brand-primary" />
                  <span>Role-Based Access Control</span>
                </div>
              </div>

              <div className="pt-2">
                <button
                  onClick={logout}
                  className="w-full py-2.5 bg-red-50 hover:bg-red-100 text-red-700 rounded-button font-bold text-xs flex items-center justify-center gap-2 border border-red-200 transition-colors"
                >
                  <LogOut size={14} />
                  <span>Log Out of NEXORA</span>
                </button>
              </div>
            </div>
          )}

          {/* 2. Addresses Tab */}
          {activeTab === 'ADDRESSES' && (
            <div className="space-y-4">
              <div className="flex items-center justify-between">
                <div>
                  <h4 className="font-black text-brand-text text-sm">Delivery Addresses</h4>
                  <p className="text-[11px] text-brand-muted">Validated for 10-minute dark store serviceability</p>
                </div>
                <button
                  type="button"
                  onClick={() => setIsAddingAddr(!isAddingAddr)}
                  className="px-3 py-1.5 bg-brand-primary text-white rounded-button text-xs font-bold flex items-center gap-1 hover:bg-brand-primaryHover transition-colors shadow-sm"
                >
                  <Plus size={13} />
                  <span>{isAddingAddr ? 'Cancel' : 'Add New'}</span>
                </button>
              </div>

              {addrSuccess && (
                <div className="bg-emerald-50 text-emerald-800 p-2.5 rounded border border-emerald-200 text-[11px] flex items-center gap-1.5">
                  <CheckCircle2 size={13} className="text-emerald-600 shrink-0" />
                  <span>{addrSuccess}</span>
                </div>
              )}

              {addrError && (
                <div className="bg-red-50 text-red-700 p-2.5 rounded border border-red-200 text-[11px] flex items-center gap-1.5">
                  <AlertCircle size={13} className="text-red-600 shrink-0" />
                  <span>{addrError}</span>
                </div>
              )}

              {isAddingAddr && (
                <form onSubmit={handleSaveAddress} className="p-3.5 bg-brand-bg rounded-card border border-brand-border space-y-2.5">
                  <h5 className="font-bold text-brand-text text-xs flex items-center gap-1.5">
                    <Building size={13} className="text-brand-primary" />
                    <span>Enter Address Details</span>
                  </h5>
                  <div className="grid grid-cols-2 gap-2">
                    <div>
                      <label className="block text-[10px] font-bold text-brand-muted mb-0.5">Label</label>
                      <select
                        value={addrForm.label}
                        onChange={(e) => setAddrForm({ ...addrForm, label: e.target.value })}
                        className="w-full px-2 py-1.5 bg-white border border-brand-border rounded text-xs"
                      >
                        <option value="Home">Home</option>
                        <option value="Office">Office</option>
                        <option value="Other">Other</option>
                      </select>
                    </div>
                    <div>
                      <label className="block text-[10px] font-bold text-brand-muted mb-0.5">Pincode (Ahmedabad)</label>
                      <input
                        type="text"
                        value={addrForm.pincode}
                        onChange={(e) => setAddrForm({ ...addrForm, pincode: e.target.value })}
                        placeholder="e.g. 380054"
                        className="w-full px-2 py-1.5 bg-white border border-brand-border rounded text-xs font-mono"
                        required
                      />
                    </div>
                  </div>

                  <div>
                    <label className="block text-[10px] font-bold text-brand-muted mb-0.5">Flat / House / Floor / Society</label>
                    <input
                      type="text"
                      value={addrForm.addressLine1}
                      onChange={(e) => setAddrForm({ ...addrForm, addressLine1: e.target.value })}
                      placeholder="e.g. Flat 402, Galaxy Heights"
                      className="w-full px-2 py-1.5 bg-white border border-brand-border rounded text-xs"
                      required
                    />
                  </div>

                  <div>
                    <label className="block text-[10px] font-bold text-brand-muted mb-0.5">Street / Area / Landmark</label>
                    <input
                      type="text"
                      value={addrForm.addressLine2}
                      onChange={(e) => setAddrForm({ ...addrForm, addressLine2: e.target.value })}
                      placeholder="e.g. Near SG Highway, Bodakdev"
                      className="w-full px-2 py-1.5 bg-white border border-brand-border rounded text-xs"
                    />
                  </div>

                  <button
                    type="submit"
                    disabled={isSavingAddr}
                    className="w-full py-2 bg-brand-primary text-white font-bold rounded text-xs flex items-center justify-center gap-1.5"
                  >
                    {isSavingAddr ? 'Validating Dark Store Coverage...' : 'Check Serviceability & Save'}
                  </button>
                </form>
              )}

              {/* Address list */}
              <div className="space-y-2.5">
                {savedAddresses.map((a) => (
                  <div
                    key={a.id}
                    className="p-3.5 bg-white rounded-card border border-brand-border flex items-start justify-between gap-3 shadow-sm hover:border-brand-primary/40 transition-colors"
                  >
                    <div className="flex items-start gap-2.5">
                      <div className="w-8 h-8 rounded-full bg-brand-primary/10 text-brand-primary flex items-center justify-center shrink-0 mt-0.5">
                        <MapPin size={15} />
                      </div>
                      <div>
                        <div className="flex items-center gap-2">
                          <span className="font-bold text-brand-text">{a.label}</span>
                          {a.isDefault ? (
                            <span className="text-[9px] bg-brand-primary/10 text-brand-primary font-bold px-1.5 py-0.2 rounded flex items-center gap-0.5">
                              <Check size={9} /> Default
                            </span>
                          ) : (
                            <button
                              onClick={() => handleSetDefaultAddr(a.id)}
                              className="text-[9px] text-brand-muted hover:text-brand-primary hover:underline font-bold"
                            >
                              Set as Default
                            </button>
                          )}
                          <span className="text-[10px] bg-emerald-100 text-emerald-800 font-bold px-1.5 py-0.2 rounded flex items-center gap-0.5">
                            <Clock size={10} /> {a.eta || '8-10 MINS'}
                          </span>
                        </div>
                        <p className="text-brand-muted text-[11px] mt-0.5 leading-relaxed">
                          {a.addressLine1}, {a.addressLine2 && `${a.addressLine2}, `}{a.city} - {a.pincode}
                        </p>
                        <p className="text-[10px] text-brand-primary font-semibold mt-1 flex items-center gap-1">
                          <Truck size={10} /> Routed to: {a.darkStore || 'Nexora Dark Store SG Highway'}
                        </p>
                      </div>
                    </div>

                    <button
                      onClick={() => handleDeleteAddr(a.id)}
                      title="Remove Address"
                      className="p-1 text-gray-400 hover:text-red-600 transition-colors"
                    >
                      <Trash2 size={13} />
                    </button>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* 3. Orders Tab */}
          {activeTab === 'ORDERS' && (
            <div className="space-y-3">
              <div className="flex items-center justify-between mb-2">
                <h4 className="font-black text-brand-text text-sm">Past Orders & Delivery Receipts</h4>
                <span className="text-[11px] text-brand-muted">Showing {pastOrders.length} orders</span>
              </div>

              {pastOrders.length === 0 ? (
                <div className="text-center py-10 bg-brand-bg rounded-card border border-brand-border">
                  <Package size={30} className="mx-auto text-brand-muted mb-2 opacity-60" />
                  <p className="font-bold text-brand-text">No orders placed yet</p>
                  <p className="text-[11px] text-brand-muted mt-1">Order your daily essentials with 10-minute delivery!</p>
                </div>
              ) : (
                <div className="space-y-3">
                  {pastOrders.map((o) => {
                    const isExpanded = expandedOrderId === o.orderId;

                    return (
                      <div
                        key={o.orderId}
                        className="p-3.5 bg-white rounded-card border border-brand-border shadow-sm space-y-2 hover:border-brand-primary/30 transition-colors"
                      >
                        <div className="flex items-center justify-between">
                          <div className="flex items-center gap-2">
                            <span className="font-mono font-black text-brand-text text-xs">{o.orderId}</span>
                            <span className="text-[10px] bg-emerald-100 text-emerald-800 font-bold px-2 py-0.5 rounded-full flex items-center gap-1">
                              <CheckCircle2 size={10} /> {o.status}
                            </span>
                          </div>
                          <span className="font-black text-brand-primary text-sm">₹{o.amount}</span>
                        </div>

                        <p className="text-[11px] text-brand-text font-medium">{o.itemsSummary}</p>

                        <div className="flex items-center justify-between pt-2 border-t border-brand-border/60 text-[10px] text-brand-muted">
                          <span>{o.date} • {o.paymentMethod}</span>
                          <div className="flex items-center gap-2">
                            <div className="flex items-center gap-1">
                              <span>OTP:</span>
                              <span className="font-mono font-bold text-brand-text bg-brand-bg px-1.5 py-0.5 rounded border border-brand-border">
                                {o.deliveryOtp}
                              </span>
                            </div>
                            <button
                              onClick={() => setExpandedOrderId(isExpanded ? null : o.orderId)}
                              className="text-brand-primary font-bold hover:underline"
                            >
                              {isExpanded ? 'Hide Details' : 'View Receipt'}
                            </button>
                          </div>
                        </div>

                        {/* Expanded Receipt Breakdown */}
                        {isExpanded && (
                          <div className="mt-2 pt-2 border-t border-dashed border-brand-border bg-brand-bg/50 p-2.5 rounded text-[11px] space-y-1">
                            <div className="flex justify-between text-brand-muted">
                              <span>Dark Store Fulfillment:</span>
                              <span className="font-bold text-brand-text">{o.darkStore}</span>
                            </div>
                            <div className="flex justify-between text-brand-muted">
                              <span>Total Items:</span>
                              <span className="font-bold text-brand-text">{o.itemsCount} Items</span>
                            </div>
                            <div className="flex justify-between text-brand-muted">
                              <span>Delivery Type:</span>
                              <span className="font-bold text-brand-primary">10-Minute Rapid EV Delivery</span>
                            </div>
                          </div>
                        )}
                      </div>
                    );
                  })}
                </div>
              )}
            </div>
          )}
        </div>

        {/* Modal Footer */}
        <div className="p-3 bg-brand-bg border-t border-brand-border text-center text-[10px] text-brand-muted shrink-0">
          NEXORA Quick Commerce • Signed in as {currentUser.email}
        </div>
      </div>
    </div>
  );
};

export default AccountModal;
