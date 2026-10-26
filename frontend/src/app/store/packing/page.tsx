'use client';

import React, { useState, useEffect } from 'react';
import Link from 'next/link';
import { ApiClient, StoreQueueMetrics, StoreQueueOrder } from '@/lib/api-client';
import { RoleSwitcher } from '@/components/common/RoleSwitcher';
import {
  PackageCheck,
  ShieldCheck,
  CheckCircle2,
  Clock,
  ArrowRight,
  ArrowLeft,
  RefreshCw,
  AlertCircle,
  Truck,
  Barcode,
} from 'lucide-react';

export default function PackingPage() {
  const [queue, setQueue] = useState<StoreQueueMetrics | null>(null);
  const [activeOrder, setActiveOrder] = useState<StoreQueueOrder | null>(null);
  const [bagCode, setBagCode] = useState('BAG-NX-');
  const [isBagSealed, setIsBagSealed] = useState(false);
  const [isWeightVerified, setIsWeightVerified] = useState(false);
  const [isLoading, setIsLoading] = useState(true);
  const [actionError, setActionError] = useState('');
  const [actionSuccess, setActionSuccess] = useState('');

  const loadOrders = async () => {
    setIsLoading(true);
    try {
      const data = await ApiClient.getStoreQueue(4);
      setQueue(data);
      const readyToPack = data.orders.find((o) => o.status === 'PREPARING');
      if (readyToPack && !activeOrder) {
        setActiveOrder(readyToPack);
        setBagCode(`BAG-NX-${readyToPack.id}`);
      }
    } catch {
      // Handled gracefully
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    loadOrders();
  }, []);

  const handleCompletePacking = async () => {
    if (!activeOrder) return;
    if (!bagCode.trim()) {
      setActionError('Tamper-evident bag barcode is required');
      return;
    }
    if (!isBagSealed || !isWeightVerified) {
      setActionError('Please confirm the bag seal and weight verification checklist');
      return;
    }

    try {
      setActionError('');
      await ApiClient.completePacking(activeOrder.id, bagCode.trim());
      setActionSuccess(
        `Order #${activeOrder.id} successfully sealed with barcode "${bagCode.trim()}"! Order is now READY and dispatched to Delivery Partner.`
      );
      setTimeout(() => {
        loadOrders();
      }, 1500);
    } catch (err: any) {
      setActionError(err.message || 'Failed to complete packing');
    }
  };

  return (
    <div className="min-h-screen bg-[#F7F8F2] flex flex-col font-sans">
      <RoleSwitcher />

      {/* Header Bar */}
      <div className="bg-white border-b border-[#E3E8DE] px-4 py-2.5 shadow-xs">
        <div className="max-w-4xl mx-auto flex items-center justify-between gap-3">
          <div className="flex items-center gap-2">
            <Link
              href="/store/manager"
              className="p-1.5 hover:bg-slate-100 text-slate-600 rounded-lg transition-colors"
              title="Return to Store Operations"
            >
              <ArrowLeft size={18} />
            </Link>
            <div>
              <h1 className="text-sm font-black text-[#173327] flex items-center gap-2">
                <span>Dark Store Packing Station</span>
                <span className="text-[10px] bg-purple-700 text-white px-2 py-0.2 rounded-full font-bold">
                  SG HIGHWAY #04
                </span>
              </h1>
              <p className="text-[11px] text-slate-500">Tamper-evident Bagging &bull; Rider Dispatch Verification</p>
            </div>
          </div>

          <div className="flex items-center gap-2">
            <Link
              href="/store/picker"
              className="px-3 py-1.5 text-xs font-bold bg-[#176B45]/10 text-[#176B45] hover:bg-[#176B45]/20 rounded-lg transition-all flex items-center gap-1.5"
            >
              <ArrowLeft size={13} />
              <span>Picker Station</span>
            </Link>
            <button
              onClick={loadOrders}
              disabled={isLoading}
              className="px-3 py-1.5 text-xs font-bold bg-[#176B45] text-white rounded-lg hover:bg-[#125638] transition-all flex items-center gap-1.5 shadow-xs"
            >
              <RefreshCw size={12} className={isLoading ? 'animate-spin' : ''} />
              <span>Refresh</span>
            </button>
          </div>
        </div>
      </div>

      <main className="max-w-4xl w-full mx-auto p-4 md:p-6 space-y-5 flex-1">
        {actionSuccess && (
          <div className="p-3 bg-emerald-50 text-emerald-800 border border-emerald-200 rounded-xl text-xs font-bold flex items-center gap-2">
            <CheckCircle2 size={16} />
            <span>{actionSuccess}</span>
          </div>
        )}

        {actionError && (
          <div className="p-3 bg-red-50 text-red-800 border border-red-200 rounded-xl text-xs font-bold flex items-center gap-2">
            <AlertCircle size={16} />
            <span>{actionError}</span>
          </div>
        )}

        {/* Packing Order Selector */}
        {queue?.orders && queue.orders.length > 0 && (
          <div className="flex items-center gap-2 overflow-x-auto pb-1">
            <span className="text-[11px] font-bold text-slate-500 uppercase tracking-wider shrink-0">
              Orders for Packing:
            </span>
            {queue.orders
              .filter((o) => o.status === 'PREPARING' || o.status === 'READY')
              .map((o) => (
                <button
                  key={o.id}
                  onClick={() => {
                    setActiveOrder(o);
                    setBagCode(o.packingBagCode || `BAG-NX-${o.id}`);
                  }}
                  className={`px-3 py-1 rounded-full text-xs font-bold shrink-0 transition-all ${
                    activeOrder?.id === o.id
                      ? 'bg-purple-700 text-white shadow-xs'
                      : 'bg-white text-slate-700 border border-[#E3E8DE] hover:bg-slate-50'
                  }`}
                >
                  #{o.id} ({o.items.length} items) &bull; {o.status}
                </button>
              ))}
          </div>
        )}

        {/* Active Packing Card */}
        {activeOrder ? (
          <div className="bg-white rounded-3xl border border-[#E3E8DE] shadow-xs overflow-hidden">
            <div className="bg-purple-700 text-white p-4 flex flex-wrap items-center justify-between gap-3">
              <div className="flex items-center gap-2.5">
                <div className="w-8 h-8 rounded-xl bg-white/10 flex items-center justify-center">
                  <PackageCheck size={18} />
                </div>
                <div>
                  <h2 className="text-sm font-black">Packing Station &bull; Order #{activeOrder.id}</h2>
                  <p className="text-[11px] text-white/80">
                    Picker: {activeOrder.pickerName || 'Prem Kumar'} &bull; Ready for Bag Sealing
                  </p>
                </div>
              </div>

              <span className="text-xs bg-white text-purple-800 font-black px-2.5 py-1 rounded-full uppercase">
                {activeOrder.status}
              </span>
            </div>

            <div className="p-5 space-y-4">
              {/* Item checklist verification */}
              <div>
                <h3 className="text-xs font-bold text-slate-700 uppercase tracking-wider mb-2">
                  1. Verify Picked SKUs ({activeOrder.items.length} Total)
                </h3>
                <div className="divide-y divide-slate-100 border border-slate-200 rounded-2xl overflow-hidden">
                  {activeOrder.items.map((item) => (
                    <div key={item.id} className="p-3 bg-white flex items-center justify-between text-xs">
                      <div className="flex items-center gap-2">
                        <CheckCircle2 size={15} className="text-emerald-600 shrink-0" />
                        <div>
                          <p className="font-bold text-slate-800">{item.productName}</p>
                          <p className="text-[10px] text-slate-400 font-mono">{item.sku || `SKU-00${item.productId}`}</p>
                        </div>
                      </div>
                      <span className="font-mono font-bold text-slate-700 bg-slate-100 px-2 py-0.5 rounded">
                        QTY: {item.quantity}
                      </span>
                    </div>
                  ))}
                </div>
              </div>

              {/* Quality & Tamper-Evident Bag Verification */}
              <div className="bg-slate-50 p-4 rounded-2xl border border-slate-200 space-y-3">
                <h3 className="text-xs font-bold text-slate-700 uppercase tracking-wider">
                  2. Quality Seal & Bagging
                </h3>

                <div>
                  <label className="text-[11px] font-bold text-slate-700 block mb-1">
                    Tamper-Evident Bag Barcode
                  </label>
                  <div className="relative">
                    <Barcode size={16} className="absolute left-3 top-2.5 text-slate-400" />
                    <input
                      type="text"
                      value={bagCode}
                      onChange={(e) => setBagCode(e.target.value)}
                      placeholder="e.g. BAG-NX-5040"
                      className="w-full pl-9 pr-3 py-2 text-xs bg-white border border-slate-200 rounded-xl font-mono font-bold focus:outline-none focus:ring-2 focus:ring-purple-600"
                    />
                  </div>
                </div>

                <div className="space-y-2 pt-1">
                  <label className="flex items-center gap-2.5 text-xs text-slate-700 cursor-pointer">
                    <input
                      type="checkbox"
                      checked={isBagSealed}
                      onChange={(e) => setIsBagSealed(e.target.checked)}
                      className="rounded border-slate-300 text-purple-700 focus:ring-purple-700 w-4 h-4 cursor-pointer"
                    />
                    <span>Tamper-evident green security strip sealed securely over tote bag</span>
                  </label>

                  <label className="flex items-center gap-2.5 text-xs text-slate-700 cursor-pointer">
                    <input
                      type="checkbox"
                      checked={isWeightVerified}
                      onChange={(e) => setIsWeightVerified(e.target.checked)}
                      className="rounded border-slate-300 text-purple-700 focus:ring-purple-700 w-4 h-4 cursor-pointer"
                    />
                    <span>Scale weight verified within tolerance (&plusmn;15g)</span>
                  </label>
                </div>
              </div>

              {/* Handoff button */}
              <div className="pt-2">
                <button
                  onClick={handleCompletePacking}
                  className="w-full py-3.5 bg-purple-700 hover:bg-purple-800 text-white rounded-xl font-black text-xs flex items-center justify-center gap-2 transition-all shadow-xs cursor-pointer active:scale-99"
                >
                  <Truck size={16} />
                  <span>Seal Bag & Dispatch to Delivery Partner (Ramesh Patel)</span>
                </button>
              </div>
            </div>
          </div>
        ) : (
          <div className="bg-white p-12 rounded-3xl border border-[#E3E8DE] text-center shadow-xs">
            <PackageCheck size={40} className="mx-auto text-slate-300 mb-2" />
            <h3 className="text-sm font-bold text-slate-800">No Orders Awaiting Packing</h3>
            <p className="text-xs text-slate-500 mt-1 max-w-sm mx-auto">
              Once store pickers complete item batches, orders will immediately appear here for bag sealing.
            </p>
          </div>
        )}
      </main>
    </div>
  );
}
