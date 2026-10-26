'use client';

import React, { useState } from 'react';
import { useApp } from '@/context/AppContext';
import { X, Lock, Mail, Phone, User, ArrowRight, ShieldCheck, Sparkles, AlertCircle } from 'lucide-react';

export const AuthModal: React.FC = () => {
  const { isAuthOpen, setIsAuthOpen, login, register } = useApp();

  const [mode, setMode] = useState<'LOGIN' | 'REGISTER'>('LOGIN');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [name, setName] = useState('');
  const [phone, setPhone] = useState('');
  const [errorMsg, setErrorMsg] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(false);

  if (!isAuthOpen) return null;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMsg(null);
    setIsLoading(true);

    try {
      if (mode === 'LOGIN') {
        if (!email.trim() || !password) {
          throw new Error('Please enter both email and password');
        }
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
      setIsAuthOpen(false);
    } catch (err: any) {
      setErrorMsg(err.message || 'Authentication failed. Please check details.');
    } finally {
      setIsLoading(false);
    }
  };

  const handleQuickDemo = async () => {
    setErrorMsg(null);
    setIsLoading(true);
    try {
      await login('aditi@nexora.in', 'SecurePass123');
      setIsAuthOpen(false);
    } catch (err: any) {
      setErrorMsg(err.message);
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
      {/* Backdrop */}
      <div
        className="fixed inset-0 bg-brand-text/60 backdrop-blur-sm transition-opacity"
        onClick={() => !isLoading && setIsAuthOpen(false)}
      />

      {/* Modal Card */}
      <div className="relative w-full max-w-md bg-white rounded-card shadow-modal z-10 overflow-hidden border border-brand-border animate-in zoom-in-95 duration-150">
        {/* Header */}
        <div className="bg-brand-primary text-white p-5 flex items-center justify-between">
          <div className="flex items-center gap-2.5">
            <div className="w-9 h-9 rounded-full bg-brand-accent text-brand-text flex items-center justify-center font-black text-base shadow-sm">
              ⚡
            </div>
            <div>
              <h3 className="text-base font-black">
                {mode === 'LOGIN' ? 'Welcome Back to NEXORA' : 'Create Customer Account'}
              </h3>
              <p className="text-[11px] text-white/80">India’s 10-Minute Rapid Grocery Delivery</p>
            </div>
          </div>
          {!isLoading && (
            <button
              onClick={() => setIsAuthOpen(false)}
              className="p-1.5 rounded-full hover:bg-white/10 text-white transition-colors"
            >
              <X size={18} />
            </button>
          )}
        </div>

        {/* Tab Switcher */}
        <div className="flex border-b border-brand-border bg-brand-bg/50 text-xs font-bold">
          <button
            type="button"
            onClick={() => { setMode('LOGIN'); setErrorMsg(null); }}
            className={`flex-1 py-3 text-center transition-all ${
              mode === 'LOGIN'
                ? 'border-b-2 border-brand-primary text-brand-primary bg-white font-black'
                : 'text-brand-muted hover:text-brand-text'
            }`}
          >
            Sign In
          </button>
          <button
            type="button"
            onClick={() => { setMode('REGISTER'); setErrorMsg(null); }}
            className={`flex-1 py-3 text-center transition-all ${
              mode === 'REGISTER'
                ? 'border-b-2 border-brand-primary text-brand-primary bg-white font-black'
                : 'text-brand-muted hover:text-brand-text'
            }`}
          >
            New Customer Register
          </button>
        </div>

        {/* Error Alert */}
        {errorMsg && (
          <div className="bg-red-50 text-red-700 px-4 py-2.5 text-xs flex items-center gap-2 border-b border-red-200">
            <AlertCircle size={15} className="shrink-0" />
            <span>{errorMsg}</span>
          </div>
        )}

        {/* Form */}
        <form onSubmit={handleSubmit} className="p-5 space-y-3.5 text-xs">
          {mode === 'REGISTER' && (
            <>
              <div>
                <label className="block font-bold text-brand-text mb-1">Full Name</label>
                <div className="relative">
                  <User size={15} className="absolute left-3 top-1/2 -translate-y-1/2 text-brand-muted" />
                  <input
                    type="text"
                    value={name}
                    onChange={(e) => setName(e.target.value)}
                    placeholder="e.g. Aditi Sharma"
                    className="w-full pl-9 pr-3 py-2 bg-brand-bg border border-brand-border rounded-input text-brand-text focus:outline-none focus:ring-1 focus:ring-brand-primary"
                    required
                  />
                </div>
              </div>

              <div>
                <label className="block font-bold text-brand-text mb-1">Mobile Number (10 digits)</label>
                <div className="relative">
                  <Phone size={15} className="absolute left-3 top-1/2 -translate-y-1/2 text-brand-muted" />
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
            <label className="block font-bold text-brand-text mb-1">Email Address</label>
            <div className="relative">
              <Mail size={15} className="absolute left-3 top-1/2 -translate-y-1/2 text-brand-muted" />
              <input
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="aditi@example.com"
                className="w-full pl-9 pr-3 py-2 bg-brand-bg border border-brand-border rounded-input text-brand-text focus:outline-none focus:ring-1 focus:ring-brand-primary"
                required
              />
            </div>
          </div>

          <div>
            <label className="block font-bold text-brand-text mb-1">Password</label>
            <div className="relative">
              <Lock size={15} className="absolute left-3 top-1/2 -translate-y-1/2 text-brand-muted" />
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
              <p className="text-[10px] text-brand-muted mt-1">Must be at least 8 characters long</p>
            )}
          </div>

          <button
            type="submit"
            disabled={isLoading}
            className="w-full py-2.5 bg-brand-primary hover:bg-brand-primaryHover disabled:opacity-70 text-white rounded-button font-black text-xs flex items-center justify-center gap-2 shadow-sm transition-all"
          >
            {isLoading ? (
              <div className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin" />
            ) : (
              <>
                <span>{mode === 'LOGIN' ? 'Sign In & Shop' : 'Create My Account'}</span>
                <ArrowRight size={14} />
              </>
            )}
          </button>

          {/* Quick Demo Login Preset Button */}
          {mode === 'LOGIN' && (
            <div className="pt-2 border-t border-brand-border">
              <button
                type="button"
                onClick={handleQuickDemo}
                disabled={isLoading}
                className="w-full py-2 bg-brand-accent/20 hover:bg-brand-accent/30 border border-brand-accent text-brand-text rounded-button font-bold text-xs flex items-center justify-center gap-1.5 transition-colors"
              >
                <Sparkles size={13} className="text-brand-primary" />
                <span>1-Click Demo Login as Aditi (Verified Customer)</span>
              </button>
            </div>
          )}
        </form>

        {/* Footer Security Badge */}
        <div className="bg-brand-bg p-3 border-t border-brand-border flex items-center justify-center gap-1.5 text-[11px] text-brand-muted">
          <ShieldCheck size={13} className="text-brand-primary" />
          <span>Stateless JWT Authentication • BCrypt Hashed Passwords</span>
        </div>
      </div>
    </div>
  );
};

export default AuthModal;
