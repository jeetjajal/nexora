'use client';

import React, { useState, useEffect } from 'react';
import { useApp, AppRole } from '@/context/AppContext';
import {
  X,
  Lock,
  Mail,
  Phone,
  User,
  ArrowRight,
  ShieldCheck,
  Sparkles,
  AlertCircle,
  ShoppingBag,
  Bike,
  Box,
  KeyRound,
  Store,
  CheckCircle2,
} from 'lucide-react';

export const AuthModal: React.FC = () => {
  const { isAuthOpen, setIsAuthOpen, authInitialRole, setRole, login, register } = useApp();

  const [activeRole, setActiveRole] = useState<AppRole>('customer');
  const [mode, setMode] = useState<'LOGIN' | 'REGISTER'>('LOGIN');

  // Customer Form State
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [name, setName] = useState('');
  const [phone, setPhone] = useState('');

  // Rider Form State
  const [riderVehicle, setRiderVehicle] = useState('Hero Electric NYX (EV)');
  const [riderLicense, setRiderLicense] = useState('');
  const [riderCity, setRiderCity] = useState('Ahmedabad');

  // Store Manager Form State
  const [managerStoreHub, setManagerStoreHub] = useState('DS-AHM-04');

  // Admin Security PIN State
  const [adminPin, setAdminPin] = useState('9042');

  const [errorMsg, setErrorMsg] = useState<string | null>(null);
  const [successToast, setSuccessToast] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(false);

  // Sync initial role when modal opens
  useEffect(() => {
    if (authInitialRole) {
      setActiveRole(authInitialRole);
    }
  }, [authInitialRole, isAuthOpen]);

  if (!isAuthOpen) return null;

  // Handle Submission per Role
  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMsg(null);
    setSuccessToast(null);
    setIsLoading(true);

    try {
      if (activeRole === 'customer') {
        if (mode === 'LOGIN') {
          if (!email.trim() || !password) throw new Error('Please enter email/phone and password');
          await login(email.trim(), password);
        } else {
          if (!name.trim()) throw new Error('Please enter your full name');
          if (!email.trim()) throw new Error('Please enter your email address');
          if (!phone.trim() || phone.replace(/\D/g, '').length !== 10) {
            throw new Error('Please enter a valid 10-digit mobile number');
          }
          if (password.length < 8) {
            throw new Error('Password must be at least 8 characters long');
          }
          await register(name.trim(), email.trim(), phone.trim(), password);
        }
        setRole('customer');
      } else if (activeRole === 'delivery') {
        if (mode === 'LOGIN') {
          if (!email.trim() && !phone.trim()) throw new Error('Enter Rider ID or Registered Phone');
          if (!password) throw new Error('Enter password');
        } else {
          if (!name.trim()) throw new Error('Enter rider full name');
          if (!phone.trim() || phone.replace(/\D/g, '').length !== 10) {
            throw new Error('Enter valid 10-digit mobile for rider onboarding');
          }
          if (!riderLicense.trim()) throw new Error('Valid Driving License Number required');
        }
        await login(email.trim() || 'ramesh.patel@rider.nexora.in', password || 'RiderSecure123');
        setRole('delivery');
      } else if (activeRole === 'store') {
        if (!email.trim()) throw new Error('Store Manager official email is required');
        if (!password) throw new Error('Store Manager credentials required');
        await login(email.trim(), password);
        setRole('store');
      } else if (activeRole === 'admin') {
        if (!email.trim()) throw new Error('Super Admin console email required');
        if (!password) throw new Error('Admin password required');
        if (adminPin !== '9042') throw new Error('Invalid 2FA Security PIN (Hint: 9042)');
        await login(email.trim(), password);
        setRole('admin');
      }

      setSuccessToast(`✓ Logged in successfully as ${activeRole.toUpperCase()}`);
      setTimeout(() => {
        setIsAuthOpen(false);
      }, 500);
    } catch (err: any) {
      setErrorMsg(err.message || 'Authentication failed. Please check credentials.');
    } finally {
      setIsLoading(false);
    }
  };

  // 1-Click Demo Login Handlers
  const handleQuickDemo = async (roleToDemo: AppRole) => {
    setErrorMsg(null);
    setIsLoading(true);
    try {
      if (roleToDemo === 'customer') {
        await login('aditi@nexora.in', 'SecurePass123');
        setRole('customer');
      } else if (roleToDemo === 'delivery') {
        await login('ramesh.patel@rider.nexora.in', 'RiderPass9042');
        setRole('delivery');
      } else if (roleToDemo === 'store') {
        await login('manager.sg@nexora.in', 'StoreOpsManager04');
        setRole('store');
      } else if (roleToDemo === 'admin') {
        await login('admin@nexora.in', 'SuperAdminSecretKey');
        setRole('admin');
      }
      setIsAuthOpen(false);
    } catch (err: any) {
      setErrorMsg(err.message);
    } finally {
      setIsLoading(false);
    }
  };

  // Role Metadata (Labels, Gujarati titles, Icons, Themes)
  const roleTabs = [
    {
      id: 'customer' as AppRole,
      title: 'ગ્રાહક (User)',
      englishTitle: 'Customer 2FA',
      icon: <ShoppingBag size={15} />,
      badge: '10-Min Store',
      tagline: 'ગ્રાહક સેવા સુરક્ષિત લૉગિન (Customer Single Sign-On)',
      desc: '31 કેટેગરીઝ, તાજા શાકભાજી, ડેરી અને કરિયાણું 8-10 મિનિટમાં ડિલિવરી મેળવો.',
    },
    {
      id: 'delivery' as AppRole,
      title: 'ડિલિવરી પાર્ટનર',
      englishTitle: 'Rider Portal',
      icon: <Bike size={15} />,
      badge: 'Rider Duty',
      tagline: 'ડિલિવરીમેન સુરક્ષિત લૉગિન (Delivery Partner 2FA)',
      desc: 'લાઈવ જીપીએસ રડાર નેવિગેશન, રાઇડર ડ્યુટી ટોગલ, 4-અંકનો OTP અને દૈનિક આવક.',
    },
    {
      id: 'store' as AppRole,
      title: 'સ્ટોર મેનેજર',
      englishTitle: 'Store Ops',
      icon: <Box size={15} />,
      badge: 'Hub #04',
      tagline: 'ડાર્ક સ્ટોર મેનેજર પોર્ટલ (Store Fulfillment Manager)',
      desc: 'SG Highway #04 માઇક્રો-હબ ઓર્ડર કતાર, પિકર/પેકર સ્ટેશન અને ઇન્વેન્ટરી સ્કેનર.',
    },
    {
      id: 'admin' as AppRole,
      title: 'સુપર એડમિન',
      englishTitle: 'Control Tower',
      icon: <ShieldCheck size={15} />,
      badge: 'Surveillance',
      tagline: 'સુપર એડમિન કંટ્રોલ ટાવર (Executive Surveillance)',
      desc: 'ઓલ-સિટી ઓર્ડર લાઇફસાઇકલ, લાઈવ ડાર્ક સ્ટોર્સ મેપ, ફ્લીટ રડાર અને નેટ રેવન્યુ.',
    },
  ];

  const currentTabInfo = roleTabs.find((t) => t.id === activeRole) || roleTabs[0];

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-3 sm:p-4 overflow-y-auto animate-in fade-in duration-150">
      {/* Backdrop */}
      <div
        className="fixed inset-0 bg-brand-text/70 backdrop-blur-sm transition-opacity"
        onClick={() => !isLoading && setIsAuthOpen(false)}
      />

      {/* Main Responsive Modal Card */}
      <div className="relative w-full max-w-lg bg-white rounded-card shadow-modal z-10 overflow-hidden border border-brand-border flex flex-col my-auto max-h-[95vh]">
        {/* 1. Top Role Selector Tabs (NEXORA Theme Pill Bar) */}
        <div className="bg-brand-bg p-2.5 border-b border-brand-border">
          <div className="text-[10px] font-extrabold text-brand-muted uppercase tracking-wider px-2 py-1 flex items-center justify-between">
            <span className="flex items-center gap-1.5 text-brand-text">
              <span className="w-2 h-2 rounded-full bg-brand-primary" />
              <span>પોર્ટલ ભૂમિકા પસંદ કરો (Select Role)</span>
            </span>
            <span className="text-[9px] bg-brand-primary/10 text-brand-primary px-2 py-0.5 rounded-full font-black">
              4 Roles Active
            </span>
          </div>

          <div className="grid grid-cols-2 sm:grid-cols-4 gap-1.5 pt-1.5">
            {roleTabs.map((tab) => {
              const isActive = activeRole === tab.id;
              return (
                <button
                  key={tab.id}
                  type="button"
                  onClick={() => {
                    setActiveRole(tab.id);
                    setErrorMsg(null);
                    setSuccessToast(null);
                  }}
                  className={`flex flex-col items-center justify-center py-2 px-1.5 rounded-xl font-bold transition-all text-xs ${
                    isActive
                      ? 'bg-brand-primary text-white shadow-card scale-[1.02] ring-2 ring-brand-accent/40'
                      : 'bg-white hover:bg-brand-bg text-brand-text/80 hover:text-brand-text border border-brand-border'
                  }`}
                >
                  <div className="flex items-center gap-1.5">
                    <span className={isActive ? 'text-brand-accent' : 'text-brand-primary'}>
                      {tab.icon}
                    </span>
                    <span className="text-[11px] font-black">{tab.title}</span>
                  </div>
                  <span
                    className={`text-[9px] mt-0.5 font-semibold ${
                      isActive ? 'text-brand-accent' : 'text-brand-muted'
                    }`}
                  >
                    {tab.englishTitle}
                  </span>
                </button>
              );
            })}
          </div>
        </div>

        {/* 2. Hero Header Card for Active Role (NEXORA Leaf Green Gradient & Zesty Lime) */}
        <div className="bg-gradient-to-r from-[#0f462d] via-[#176B45] to-[#0c831f] text-white p-4 sm:p-5 flex items-start justify-between relative overflow-hidden border-b border-brand-primaryHover">
          <div className="relative z-10 max-w-[85%]">
            <div className="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full bg-brand-accent/20 backdrop-blur-xs text-[10px] font-black tracking-wide uppercase mb-1.5 border border-brand-accent/30 text-brand-accent">
              <span>{currentTabInfo.badge}</span>
              <span>•</span>
              <span>2FA Secured</span>
            </div>
            <h3 className="text-base sm:text-lg font-black leading-tight drop-shadow-xs">
              {currentTabInfo.tagline}
            </h3>
            <p className="text-[11px] text-white/90 mt-1 leading-snug font-medium">
              {currentTabInfo.desc}
            </p>
          </div>

          {!isLoading && (
            <button
              onClick={() => setIsAuthOpen(false)}
              className="p-1.5 rounded-full hover:bg-white/20 text-white transition-colors self-start shrink-0"
              aria-label="Close modal"
            >
              <X size={18} />
            </button>
          )}
        </div>

        {/* 3. Login vs Register Switcher (Available for Customer and Rider) */}
        {(activeRole === 'customer' || activeRole === 'delivery') && (
          <div className="flex border-b border-brand-border bg-brand-bg/50 text-xs font-bold">
            <button
              type="button"
              onClick={() => {
                setMode('LOGIN');
                setErrorMsg(null);
              }}
              className={`flex-1 py-2.5 text-center transition-all ${
                mode === 'LOGIN'
                  ? 'border-b-2 border-brand-primary text-brand-primary bg-white font-black'
                  : 'text-brand-muted hover:text-brand-text'
              }`}
            >
              લૉગિન (Sign In)
            </button>
            <button
              type="button"
              onClick={() => {
                setMode('REGISTER');
                setErrorMsg(null);
              }}
              className={`flex-1 py-2.5 text-center transition-all ${
                mode === 'REGISTER'
                  ? 'border-b-2 border-brand-primary text-brand-primary bg-white font-black'
                  : 'text-brand-muted hover:text-brand-text'
              }`}
            >
              {activeRole === 'customer' ? 'નવું રજીસ્ટ્રેશન (Register)' : 'રાઇડર ઓનબોર્ડિંગ (Register)'}
            </button>
          </div>
        )}

        {/* Alerts */}
        {errorMsg && (
          <div className="bg-red-50 text-brand-danger px-4 py-2.5 text-xs flex items-center gap-2 border-b border-red-200">
            <AlertCircle size={15} className="shrink-0" />
            <span className="font-semibold">{errorMsg}</span>
          </div>
        )}

        {successToast && (
          <div className="bg-emerald-50 text-emerald-800 px-4 py-2 text-xs flex items-center gap-2 border-b border-emerald-200 font-bold animate-in fade-in">
            <CheckCircle2 size={15} className="text-emerald-600 shrink-0" />
            <span>{successToast}</span>
          </div>
        )}

        {/* 4. Form Content (Scrollable & Responsive) */}
        <form onSubmit={handleSubmit} className="p-4 sm:p-5 space-y-3.5 text-xs overflow-y-auto">
          {/* ================= A. CUSTOMER FORM ================= */}
          {activeRole === 'customer' && (
            <>
              {mode === 'REGISTER' && (
                <>
                  <div>
                    <label className="block font-bold text-brand-text mb-1">
                      ગ્રાહકનું પૂરું નામ (Full Name)
                    </label>
                    <div className="relative">
                      <User
                        size={15}
                        className="absolute left-3 top-1/2 -translate-y-1/2 text-brand-muted"
                      />
                      <input
                        type="text"
                        value={name}
                        onChange={(e) => setName(e.target.value)}
                        placeholder="દા.ત. અદિતિ શર્મા / Aditi Sharma"
                        className="w-full pl-9 pr-3 py-2 bg-brand-bg border border-brand-border rounded-input text-brand-text focus:outline-none focus:ring-1 focus:ring-brand-primary"
                        required
                      />
                    </div>
                  </div>

                  <div>
                    <label className="block font-bold text-brand-text mb-1">
                      મોબાઇલ નંબર (10-Digit Mobile Number)
                    </label>
                    <div className="relative">
                      <Phone
                        size={15}
                        className="absolute left-3 top-1/2 -translate-y-1/2 text-brand-muted"
                      />
                      <input
                        type="tel"
                        value={phone}
                        onChange={(e) => setPhone(e.target.value)}
                        placeholder="9876543210"
                        maxLength={10}
                        className="w-full pl-9 pr-3 py-2 bg-brand-bg border border-brand-border rounded-input text-brand-text focus:outline-none focus:ring-1 focus:ring-brand-primary font-mono"
                        required
                      />
                    </div>
                  </div>
                </>
              )}

              <div>
                <label className="block font-bold text-brand-text mb-1">
                  ઈમેલ અથવા મોબાઇલ (Email or Mobile)
                </label>
                <div className="relative">
                  <Mail
                    size={15}
                    className="absolute left-3 top-1/2 -translate-y-1/2 text-brand-muted"
                  />
                  <input
                    type="text"
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    placeholder="aditi@nexora.in અથવા 9876543210"
                    className="w-full pl-9 pr-3 py-2 bg-brand-bg border border-brand-border rounded-input text-brand-text focus:outline-none focus:ring-1 focus:ring-brand-primary"
                    required
                  />
                </div>
              </div>

              <div>
                <label className="block font-bold text-brand-text mb-1">પાસવર્ડ (Password)</label>
                <div className="relative">
                  <Lock
                    size={15}
                    className="absolute left-3 top-1/2 -translate-y-1/2 text-brand-muted"
                  />
                  <input
                    type="password"
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    placeholder="••••••••"
                    className="w-full pl-9 pr-3 py-2 bg-brand-bg border border-brand-border rounded-input text-brand-text focus:outline-none focus:ring-1 focus:ring-brand-primary"
                    required
                  />
                </div>
                {mode === 'REGISTER' && (
                  <p className="text-[10px] text-brand-muted mt-1">ઓછામાં ઓછા 8 અક્ષરો જરૂરી છે</p>
                )}
              </div>
            </>
          )}

          {/* ================= B. DELIVERY PARTNER (RIDER) FORM ================= */}
          {activeRole === 'delivery' && (
            <>
              {mode === 'REGISTER' && (
                <>
                  <div>
                    <label className="block font-bold text-brand-text mb-1">
                      રાઇડરનું પૂરું નામ (Rider Full Name)
                    </label>
                    <div className="relative">
                      <User
                        size={15}
                        className="absolute left-3 top-1/2 -translate-y-1/2 text-brand-muted"
                      />
                      <input
                        type="text"
                        value={name}
                        onChange={(e) => setName(e.target.value)}
                        placeholder="દા.ત. રમેશ પટેલ / Ramesh Patel"
                        className="w-full pl-9 pr-3 py-2 bg-brand-bg border border-brand-border rounded-input text-brand-text focus:outline-none focus:ring-1 focus:ring-brand-primary"
                        required
                      />
                    </div>
                  </div>

                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-2.5">
                    <div>
                      <label className="block font-bold text-brand-text mb-1">શહેર (City)</label>
                      <input
                        type="text"
                        value={riderCity}
                        onChange={(e) => setRiderCity(e.target.value)}
                        className="w-full px-3 py-2 bg-brand-bg border border-brand-border rounded-input text-brand-text font-bold"
                        readOnly
                      />
                    </div>

                    <div>
                      <label className="block font-bold text-brand-text mb-1">
                        વાહન પ્રકાર (Vehicle Type)
                      </label>
                      <select
                        value={riderVehicle}
                        onChange={(e) => setRiderVehicle(e.target.value)}
                        className="w-full px-3 py-2 bg-brand-bg border border-brand-border rounded-input text-brand-text text-xs font-bold"
                      >
                        <option value="Hero Electric NYX (EV)">Hero Electric NYX (EV)</option>
                        <option value="Ather 450X (EV)">Ather 450X (EV)</option>
                        <option value="TVS iQube (EV)">TVS iQube (EV)</option>
                        <option value="Honda Activa 6G">Honda Activa 6G</option>
                      </select>
                    </div>
                  </div>

                  <div>
                    <label className="block font-bold text-brand-text mb-1">
                      ડ્રાઇવિંગ લાઇસન્સ નંબર (Driving License No.)
                    </label>
                    <div className="relative">
                      <KeyRound
                        size={15}
                        className="absolute left-3 top-1/2 -translate-y-1/2 text-brand-muted"
                      />
                      <input
                        type="text"
                        value={riderLicense}
                        onChange={(e) => setRiderLicense(e.target.value.toUpperCase())}
                        placeholder="GJ-01-2024-009842"
                        className="w-full pl-9 pr-3 py-2 bg-brand-bg border border-brand-border rounded-input text-brand-text font-mono focus:outline-none focus:ring-1 focus:ring-brand-primary"
                        required
                      />
                    </div>
                  </div>
                </>
              )}

              <div>
                <label className="block font-bold text-brand-text mb-1">
                  રાઇડર ID અથવા રજીસ્ટર્ડ મોબાઇલ (Rider ID / Phone)
                </label>
                <div className="relative">
                  <Phone
                    size={15}
                    className="absolute left-3 top-1/2 -translate-y-1/2 text-brand-muted"
                  />
                  <input
                    type="text"
                    value={phone || email}
                    onChange={(e) => {
                      setPhone(e.target.value);
                      setEmail(e.target.value);
                    }}
                    placeholder="દા.ત. NX-R9042 અથવા 9876543210"
                    className="w-full pl-9 pr-3 py-2 bg-brand-bg border border-brand-border rounded-input text-brand-text focus:outline-none focus:ring-1 focus:ring-brand-primary"
                    required
                  />
                </div>
              </div>

              <div>
                <label className="block font-bold text-brand-text mb-1">પાસવર્ડ (Password)</label>
                <div className="relative">
                  <Lock
                    size={15}
                    className="absolute left-3 top-1/2 -translate-y-1/2 text-brand-muted"
                  />
                  <input
                    type="password"
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    placeholder="••••••••"
                    className="w-full pl-9 pr-3 py-2 bg-brand-bg border border-brand-border rounded-input text-brand-text focus:outline-none focus:ring-1 focus:ring-brand-primary"
                    required
                  />
                </div>
              </div>
            </>
          )}

          {/* ================= C. STORE MANAGER FORM ================= */}
          {activeRole === 'store' && (
            <>
              <div>
                <label className="block font-bold text-brand-text mb-1">
                  ડાર્ક સ્ટોર હબ પસંદ કરો (Assigned Dark Store Hub)
                </label>
                <div className="relative">
                  <Store
                    size={15}
                    className="absolute left-3 top-1/2 -translate-y-1/2 text-brand-muted"
                  />
                  <select
                    value={managerStoreHub}
                    onChange={(e) => setManagerStoreHub(e.target.value)}
                    className="w-full pl-9 pr-3 py-2 bg-brand-bg border border-brand-border rounded-input text-brand-text text-xs font-bold focus:outline-none focus:ring-1 focus:ring-brand-primary"
                  >
                    <option value="DS-AHM-04">SG Highway #04 (Shapath V - Bodakdev)</option>
                    <option value="DS-AHM-02">Vastrapur Hub #02 (IIM Road)</option>
                    <option value="DS-AHM-01">Prahlad Nagar Hub #01 (100ft Road)</option>
                    <option value="DS-AHM-03">South Bopal Central #03</option>
                  </select>
                </div>
              </div>

              <div>
                <label className="block font-bold text-brand-text mb-1">
                  મેનેજર ઓફિશિયલ ઈમેલ (Store Manager Email)
                </label>
                <div className="relative">
                  <Mail
                    size={15}
                    className="absolute left-3 top-1/2 -translate-y-1/2 text-brand-muted"
                  />
                  <input
                    type="email"
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    placeholder="manager.sg@nexora.in"
                    className="w-full pl-9 pr-3 py-2 bg-brand-bg border border-brand-border rounded-input text-brand-text focus:outline-none focus:ring-1 focus:ring-brand-primary"
                    required
                  />
                </div>
              </div>

              <div>
                <label className="block font-bold text-brand-text mb-1">પાસવર્ડ (Password)</label>
                <div className="relative">
                  <Lock
                    size={15}
                    className="absolute left-3 top-1/2 -translate-y-1/2 text-brand-muted"
                  />
                  <input
                    type="password"
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    placeholder="••••••••"
                    className="w-full pl-9 pr-3 py-2 bg-brand-bg border border-brand-border rounded-input text-brand-text focus:outline-none focus:ring-1 focus:ring-brand-primary"
                    required
                  />
                </div>
              </div>
            </>
          )}

          {/* ================= D. SUPER ADMIN FORM ================= */}
          {activeRole === 'admin' && (
            <>
              <div>
                <label className="block font-bold text-brand-text mb-1">
                  સુપર એડમિન ઈમેલ (Super Admin Email)
                </label>
                <div className="relative">
                  <Mail
                    size={15}
                    className="absolute left-3 top-1/2 -translate-y-1/2 text-brand-muted"
                  />
                  <input
                    type="email"
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    placeholder="admin@nexora.in"
                    className="w-full pl-9 pr-3 py-2 bg-brand-bg border border-brand-border rounded-input text-brand-text focus:outline-none focus:ring-1 focus:ring-brand-primary"
                    required
                  />
                </div>
              </div>

              <div>
                <label className="block font-bold text-brand-text mb-1">
                  માસ્ટર પાસવર્ડ (Master Security Password)
                </label>
                <div className="relative">
                  <Lock
                    size={15}
                    className="absolute left-3 top-1/2 -translate-y-1/2 text-brand-muted"
                  />
                  <input
                    type="password"
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    placeholder="••••••••"
                    className="w-full pl-9 pr-3 py-2 bg-brand-bg border border-brand-border rounded-input text-brand-text focus:outline-none focus:ring-1 focus:ring-brand-primary"
                    required
                  />
                </div>
              </div>

              <div>
                <label className="block font-bold text-brand-text mb-1">
                  2FA સિક્યોરિટી પિન (Master Security PIN)
                </label>
                <div className="relative">
                  <KeyRound
                    size={15}
                    className="absolute left-3 top-1/2 -translate-y-1/2 text-brand-muted"
                  />
                  <input
                    type="password"
                    value={adminPin}
                    onChange={(e) => setAdminPin(e.target.value)}
                    placeholder="4-digit PIN (9042)"
                    maxLength={4}
                    className="w-full pl-9 pr-3 py-2 bg-brand-bg border border-brand-border rounded-input text-brand-text font-mono tracking-widest focus:outline-none focus:ring-1 focus:ring-brand-primary"
                    required
                  />
                </div>
              </div>
            </>
          )}

          {/* Submit Action Button */}
          <button
            type="submit"
            disabled={isLoading}
            className="w-full py-3 bg-brand-primary hover:bg-brand-primaryHover disabled:opacity-70 text-white rounded-button font-black text-xs flex items-center justify-center gap-2 shadow-button transition-all active:scale-98"
          >
            {isLoading ? (
              <div className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin" />
            ) : (
              <>
                <span>
                  {mode === 'LOGIN'
                    ? `${currentTabInfo.title} લૉગિન કરો (Enter Workspace)`
                    : `${currentTabInfo.title} રજીસ્ટ્રેશન પૂર્ણ કરો`}
                </span>
                <ArrowRight size={14} className="text-brand-accent" />
              </>
            )}
          </button>

          {/* 1-Click Fast Preset Demo Buttons for Immediate Evaluation */}
          <div className="pt-2 border-t border-brand-border">
            <div className="text-[10px] font-bold text-brand-muted mb-1.5 flex items-center gap-1">
              <Sparkles size={11} className="text-brand-primary" />
              <span>1-ક્લિક ડાયરેક્ટ ડેમો એક્સેસ (Instant 1-Click Verification):</span>
            </div>

            {activeRole === 'customer' && (
              <button
                type="button"
                onClick={() => handleQuickDemo('customer')}
                disabled={isLoading}
                className="w-full py-2 bg-brand-primary/10 hover:bg-brand-primary/20 border border-brand-primary/30 text-brand-primary rounded-button font-bold text-[11px] flex items-center justify-center gap-1.5 transition-colors"
              >
                <span>👤 1-Click Demo Customer Login (અદિતિ શર્મા)</span>
              </button>
            )}

            {activeRole === 'delivery' && (
              <button
                type="button"
                onClick={() => handleQuickDemo('delivery')}
                disabled={isLoading}
                className="w-full py-2 bg-brand-primary/10 hover:bg-brand-primary/20 border border-brand-primary/30 text-brand-primary rounded-button font-bold text-[11px] flex items-center justify-center gap-1.5 transition-colors"
              >
                <span>🛵 1-Click Demo Rider Login (રમેશ પટેલ • EV NX-R9042)</span>
              </button>
            )}

            {activeRole === 'store' && (
              <button
                type="button"
                onClick={() => handleQuickDemo('store')}
                disabled={isLoading}
                className="w-full py-2 bg-brand-primary/10 hover:bg-brand-primary/20 border border-brand-primary/30 text-brand-primary rounded-button font-bold text-[11px] flex items-center justify-center gap-1.5 transition-colors"
              >
                <span>🏪 1-Click Demo Manager Login (SG Highway #04 Hub)</span>
              </button>
            )}

            {activeRole === 'admin' && (
              <button
                type="button"
                onClick={() => handleQuickDemo('admin')}
                disabled={isLoading}
                className="w-full py-2 bg-brand-primary/10 hover:bg-brand-primary/20 border border-brand-primary/30 text-brand-primary rounded-button font-bold text-[11px] flex items-center justify-center gap-1.5 transition-colors"
              >
                <span>🛡️ 1-Click Demo Super Admin Login (Full Control Tower)</span>
              </button>
            )}
          </div>
        </form>

        {/* Footer Security Badges */}
        <div className="bg-brand-bg p-3 border-t border-brand-border flex flex-wrap items-center justify-between gap-2 text-[10px] text-brand-muted font-medium">
          <div className="flex items-center gap-1">
            <ShieldCheck size={13} className="text-brand-primary shrink-0" />
            <span>2FA Auth &amp; DPOP Encrypted Session</span>
          </div>
          <span className="font-mono font-bold text-brand-primary">Nexora Leaf Green Theme</span>
        </div>
      </div>
    </div>
  );
};

export default AuthModal;
