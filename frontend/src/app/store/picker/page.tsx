'use client';

import React, { useState, useEffect } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { ApiClient, StoreQueueMetrics, StoreQueueOrder } from '@/lib/api-client';
import { RoleSwitcher } from '@/components/common/RoleSwitcher';
import {
  Box,
  Barcode,
  CheckCircle2,
  Clock,
  ArrowRight,
  ArrowLeft,
  RefreshCw,
  AlertCircle,
  Package,
} from 'lucide-react';

export default function PickerPage() {
  const router = useRouter();
  const [queue, setQueue] = useState<StoreQueueMetrics | null>(null);
  const [activeOrder, setActiveOrder] = useState<StoreQueueOrder | null>(null);
  const [scannedBarcode, setScannedBarcode] = useState('');
  const [isLoading, setIsLoading] = useState(true);
  const [actionError, setActionError] = useState('');
  const [actionSuccess, setActionSuccess] = useState('');

  const loadOrders = async () => {
    setIsLoading(true);
    try {
      const data = await ApiClient.getStoreQueue(4);
      setQueue(data);
      const pickable = data.orders.find((o) => o.status === 'CONFIRMED' || o.status === 'PREPARING');
      if (pickable && !activeOrder) {
        setActiveOrder(pickable);
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

  const handleStartPicking = async (orderId: number) => {
    try {
      setActionError('');
      await ApiClient.startPicking(orderId);
      setActionSuccess(`Picking session started for Order #${orderId}`);
      await loadOrders();
    } catch (err: any) {
      setActionError(err.message || 'Failed to start picking');
    }
  };

  const handleToggleItem = async (itemId: number) => {
    if (!activeOrder) return;
    try {
      setActionError('');
      await ApiClient.pickItem(activeOrder.id, itemId);
      setActiveOrder((prev) => {
        if (!prev) return null;
        return {
          ...prev,
          items: prev.items.map((i) => (i.id === itemId ? { ...i, isPicked: !i.isPicked } : i)),
        };
      });
    } catch (err: any) {
      setActionError(err.message || 'Failed to update item pick state');
    }
  };

  const handleBarcodeSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!activeOrder || !scannedBarcode.trim()) return;
    const barcodeTrimmed = scannedBarcode.trim();

    // Match by SKU, item ID, or numeric code
    const matchingItem = activeOrder.items.find(
      (i) => i.sku === barcodeTrimmed || String(i.id) === barcodeTrimmed || String(i.productId) === barcodeTrimmed
    );

    if (matchingItem) {
      handleToggleItem(matchingItem.id);
      setScannedBarcode('');
      setActionSuccess(`Barcode verified: ${matchingItem.productName}`);
      setTimeout(() => setActionSuccess(''), 3000);
    } else {
      setActionError(`Barcode "${barcodeTrimmed}" does not match any SKU in Order #${activeOrder.id}`);
      setTimeout(() => setActionError(''), 4000);
    }
  };

  const handleCompletePicking = async () => {
    if (!activeOrder) return;
    try {
      setActionError('');
      await ApiClient.completePicking(activeOrder.id);
      setActionSuccess(`Order #${activeOrder.id} picking complete! Transferring to Packing Station.`);
      setTimeout(() => {
        router.push('/store/packing');
      }, 1200);
    } catch (err: any) {
      setActionError(err.message || 'All items must be picked before completing batch');
    }
  };

  const pickedCount = activeOrder?.items.filter((i) => i.isPicked).length || 0;
  const totalCount = activeOrder?.items.length || 0;
  const isAllPicked = totalCount > 0 && pickedCount === totalCount;

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
                <span>Dark Store Picker Station</span>
                <span className="text-[10px] bg-[#176B45] text-white px-2 py-0.2 rounded-full font-bold">
                  SG HIGHWAY #04
                </span>
              </h1>
              <p className="text-[11px] text-slate-500">Aisle Route Optimization • Target: &lt; 90s pick SLA</p>
            </div>
          </div>

          <div className="flex items-center gap-2">
            <button
              onClick={loadOrders}
              disabled={isLoading}
              className="px-3 py-1.5 text-xs font-bold bg-slate-100 hover:bg-slate-200 text-slate-700 rounded-lg transition-all flex items-center gap-1.5"
            >
              <RefreshCw size={12} className={isLoading ? 'animate-spin' : ''} />
              <span>Refresh</span>
            </button>
            <Link
              href="/store/packing"
              className="px-3 py-1.5 text-xs font-bold bg-purple-100 hover:bg-purple-200 text-purple-800 rounded-lg transition-all flex items-center gap-1.5"
            >
              <span>Packing Station</span>
              <ArrowRight size={13} />
            </Link>
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

        {/* Order Selector Tabs */}
        {queue?.orders && queue.orders.length > 0 && (
          <div className="flex items-center gap-2 overflow-x-auto pb-1">
            <span className="text-[11px] font-bold text-slate-500 uppercase tracking-wider shrink-0">
              Active Batches:
            </span>
            {queue.orders
              .filter((o) => o.status === 'CONFIRMED' || o.status === 'PREPARING')
              .map((o) => (
                <button
                  key={o.id}
                  onClick={() => setActiveOrder(o)}
                  className={`px-3 py-1 rounded-full text-xs font-bold shrink-0 transition-all ${
                    activeOrder?.id === o.id
                      ? 'bg-[#176B45] text-white shadow-xs'
                      : 'bg-white text-slate-700 border border-[#E3E8DE] hover:bg-slate-50'
                  }`}
                >
                  #{o.id} ({o.items.length} items) • {o.status}
                </button>
              ))}
          </div>
        )}

        {/* Active Picking Card */}
        {activeOrder ? (
          <div className="bg-white rounded-3xl border border-[#E3E8DE] shadow-xs overflow-hidden">
            {/* Header info */}
            <div className="bg-[#176B45] text-white p-4 flex flex-wrap items-center justify-between gap-3">
              <div className="flex items-center gap-2.5">
                <div className="w-8 h-8 rounded-xl bg-white/10 flex items-center justify-center">
                  <Box size={18} />
                </div>
                <div>
                  <h2 className="text-sm font-black">Picking Batch #{activeOrder.id}</h2>
                  <p className="text-[11px] text-white/80">
                    Target Aisle: Zone A4 (Ambient & Staples) &bull; Bin Sector 02
                  </p>
                </div>
              </div>

              <div className="flex items-center gap-3">
                <span className="text-xs bg-[#D8F36A] text-[#173327] font-black px-2.5 py-1 rounded-full">
                  {pickedCount} / {totalCount} SKUs Picked
                </span>
                {activeOrder.status === 'CONFIRMED' && (
                  <button
                    onClick={() => handleStartPicking(activeOrder.id)}
                    className="px-3 py-1 bg-white text-[#176B45] font-black text-xs rounded-full hover:bg-slate-100 transition-all shadow-xs"
                  >
                    Start Session
                  </button>
                )}
              </div>
            </div>

            {/* Barcode scanner simulator */}
            <div className="p-4 bg-slate-50 border-b border-slate-200">
              <form onSubmit={handleBarcodeSubmit} className="flex gap-2">
                <div className="relative flex-1">
                  <Barcode size={16} className="absolute left-3 top-2.5 text-slate-400" />
                  <input
                    type="text"
                    value={scannedBarcode}
                    onChange={(e) => setScannedBarcode(e.target.value)}
                    placeholder="Scan product barcode (or enter SKU e.g. SKU-001)..."
                    className="w-full pl-9 pr-3 py-2 text-xs bg-white border border-slate-200 rounded-xl font-mono focus:outline-none focus:ring-2 focus:ring-[#176B45]"
                  />
                </div>
                <button
                  type="submit"
                  className="px-4 py-2 bg-[#176B45] text-white font-bold text-xs rounded-xl hover:bg-[#125638] transition-all shadow-xs"
                >
                  Verify SKU
                </button>
              </form>
            </div>

            {/* Items Checklist */}
            <div className="p-4 space-y-3">
              <p className="text-xs text-slate-500 font-medium">
                Verify product weight and barcode on shelf, then check off each SKU:
              </p>

              <div className="divide-y divide-slate-100 border border-slate-200 rounded-2xl overflow-hidden">
                {activeOrder.items.map((item) => (
                  <div
                    key={item.id}
                    onClick={() => handleToggleItem(item.id)}
                    className={`p-3.5 flex items-center justify-between gap-3 cursor-pointer transition-colors ${
                      item.isPicked ? 'bg-emerald-50/60' : 'bg-white hover:bg-slate-50'
                    }`}
                  >
                    <div className="flex items-center gap-3">
                      <input
                        type="checkbox"
                        checked={item.isPicked}
                        onChange={() => {}}
                        className="rounded border-slate-300 text-[#176B45] focus:ring-[#176B45] w-4 h-4 cursor-pointer"
                      />
                      <div>
                        <p className={`text-xs font-bold ${item.isPicked ? 'line-through text-slate-400' : 'text-slate-800'}`}>
                          {item.productName}
                        </p>
                        <p className="text-[10px] text-slate-400 font-mono">
                          Bin: A4-0{item.productId} &bull; {item.sku || `SKU-00${item.productId}`}
                        </p>
                      </div>
                    </div>

                    <div className="flex items-center gap-3">
                      <span className="text-xs font-black bg-slate-100 text-slate-700 px-2.5 py-1 rounded-lg border border-slate-200">
                        QTY: {item.quantity}
                      </span>
                      {item.isPicked && (
                        <span className="text-[10px] font-black text-emerald-700 bg-emerald-100 px-2 py-0.5 rounded-full flex items-center gap-1">
                          <CheckCircle2 size={11} /> Picked
                        </span>
                      )}
                    </div>
                  </div>
                ))}
              </div>

              {/* Complete Picking Action */}
              <div className="pt-3">
                <button
                  onClick={handleCompletePicking}
                  disabled={!isAllPicked}
                  className={`w-full py-3 rounded-xl font-black text-xs flex items-center justify-center gap-2 transition-all shadow-xs ${
                    isAllPicked
                      ? 'bg-[#176B45] hover:bg-[#125638] text-white cursor-pointer'
                      : 'bg-slate-200 text-slate-400 cursor-not-allowed'
                  }`}
                >
                  <CheckCircle2 size={15} />
                  <span>
                    {isAllPicked
                      ? `Complete Picking Batch #${activeOrder.id} & Send to Packing Station`
                      : `Pick Remaining ${totalCount - pickedCount} Items to Complete Batch`}
                  </span>
                </button>
              </div>
            </div>
          </div>
        ) : (
          <div className="bg-white p-12 rounded-3xl border border-[#E3E8DE] text-center shadow-xs">
            <Package size={40} className="mx-auto text-slate-300 mb-2" />
            <h3 className="text-sm font-bold text-slate-800">No Orders in Picking Queue</h3>
            <p className="text-xs text-slate-500 mt-1 max-w-sm mx-auto">
              All confirmed customer orders have been picked. New incoming orders will appear here automatically.
            </p>
          </div>
        )}
      </main>
    </div>
  );
}
