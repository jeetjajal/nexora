'use client';

import React, { useState, useEffect } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { ApiClient, StoreQueueMetrics, StoreQueueOrder } from '@/lib/api-client';
import { RoleSwitcher } from '@/components/common/RoleSwitcher';
import { CameraBarcodeScanner } from '@/components/store/CameraBarcodeScanner';
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
  Camera,
} from 'lucide-react';

export default function PickerPage() {
  const router = useRouter();
  const [queue, setQueue] = useState<StoreQueueMetrics | null>(null);
  const [activeOrder, setActiveOrder] = useState<StoreQueueOrder | null>(null);
  const [scannedBarcode, setScannedBarcode] = useState('');
  const [isLoading, setIsLoading] = useState(true);
  const [actionError, setActionError] = useState('');
  const [actionSuccess, setActionSuccess] = useState('');
  const [isCameraScannerOpen, setIsCameraScannerOpen] = useState(false);

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

  const verifyAndPickBarcode = (rawBarcode: string) => {
    if (!activeOrder || !rawBarcode.trim()) return;
    const barcodeTrimmed = rawBarcode.trim();

    // Match by SKU, item ID, or numeric code
    const matchingItem = activeOrder.items.find(
      (i) =>
        i.sku === barcodeTrimmed ||
        String(i.id) === barcodeTrimmed ||
        String(i.productId) === barcodeTrimmed ||
        barcodeTrimmed.includes(String(i.productId))
    );

    if (matchingItem) {
      if (!matchingItem.isPicked) {
        handleToggleItem(matchingItem.id);
      }
      setScannedBarcode('');
      setActionSuccess(`✓ Verified Barcode: ${matchingItem.productName} (Qty: ${matchingItem.quantity})`);
      setTimeout(() => setActionSuccess(''), 3000);
    } else {
      setActionError(`❌ Barcode "${barcodeTrimmed}" does not match any item in Order #${activeOrder.id}`);
      setTimeout(() => setActionError(''), 4000);
    }
  };

  const handleBarcodeSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    verifyAndPickBarcode(scannedBarcode);
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

  // Next unpicked item for scanner guidance
  const nextUnpickedItem = activeOrder?.items.find((i) => !i.isPicked);

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
              onClick={() => setIsCameraScannerOpen(true)}
              className="px-3 py-1.5 bg-[#176B45] text-white text-xs font-bold rounded-xl flex items-center gap-1.5 hover:bg-[#125638] transition-all shadow-xs"
            >
              <Camera size={14} />
              <span>Camera Scanner</span>
            </button>
            <button
              onClick={loadOrders}
              disabled={isLoading}
              className="p-1.5 hover:bg-slate-100 text-slate-600 rounded-lg transition-colors"
              title="Refresh Queue"
            >
              <RefreshCw size={15} className={isLoading ? 'animate-spin' : ''} />
            </button>
          </div>
        </div>
      </div>

      <main className="max-w-4xl w-full mx-auto p-4 md:p-6 space-y-5 flex-1">
        {actionSuccess && (
          <div className="p-3 bg-emerald-50 text-emerald-800 border border-emerald-200 rounded-2xl text-xs font-bold flex items-center gap-2 animate-in fade-in">
            <CheckCircle2 size={16} />
            <span>{actionSuccess}</span>
          </div>
        )}

        {actionError && (
          <div className="p-3 bg-red-50 text-red-800 border border-red-200 rounded-2xl text-xs font-bold flex items-center gap-2 animate-in fade-in">
            <AlertCircle size={16} />
            <span>{actionError}</span>
          </div>
        )}

        {/* Picker Metric Banner */}
        <section className="grid grid-cols-2 sm:grid-cols-4 gap-3">
          <div className="bg-white p-3.5 rounded-2xl border border-[#E3E8DE] shadow-xs">
            <span className="text-[11px] font-bold text-slate-500 block mb-1">Queue Backlog</span>
            <span className="text-xl font-black text-[#173327]">
              {(queue?.confirmedCount || 0) + (queue?.preparingCount || 0) || 3} Orders
            </span>
            <span className="text-[10px] text-amber-600 font-bold block mt-0.5">High Dispatch Priority</span>
          </div>

          <div className="bg-white p-3.5 rounded-2xl border border-[#E3E8DE] shadow-xs">
            <span className="text-[11px] font-bold text-slate-500 block mb-1">Avg Pick Time</span>
            <span className="text-xl font-black text-[#176B45]">54 Sec</span>
            <span className="text-[10px] text-emerald-700 font-bold block mt-0.5">Well under 90s SLA</span>
          </div>

          <div className="bg-white p-3.5 rounded-2xl border border-[#E3E8DE] shadow-xs">
            <span className="text-[11px] font-bold text-slate-500 block mb-1">Items Picked Today</span>
            <span className="text-xl font-black text-[#173327]">482 SKUs</span>
            <span className="text-[10px] text-slate-500 font-bold block mt-0.5">Accuracy: 99.8%</span>
          </div>

          <div className="bg-white p-3.5 rounded-2xl border border-[#E3E8DE] shadow-xs">
            <span className="text-[11px] font-bold text-slate-500 block mb-1">Picker ID</span>
            <span className="text-xl font-black text-[#173327]">PICK-04</span>
            <span className="text-[10px] text-emerald-700 font-bold block mt-0.5">Station Active</span>
          </div>
        </section>

        {/* Active Picking Batch Card */}
        {activeOrder ? (
          <div className="bg-white rounded-3xl border border-[#E3E8DE] overflow-hidden shadow-xs">
            <div className="p-4 bg-[#173327] text-white flex flex-wrap items-center justify-between gap-3">
              <div>
                <span className="text-[10px] uppercase tracking-wider text-emerald-300 font-bold block">
                  Active Picking Task
                </span>
                <h2 className="text-base font-black flex items-center gap-2">
                  <span>Order #{activeOrder.id}</span>
                  <span className="text-xs bg-emerald-700 text-white font-mono px-2 py-0.5 rounded-full">
                    Tote #TOTE-{activeOrder.id}
                  </span>
                </h2>
              </div>

              <div className="flex items-center gap-2">
                <span className="text-xs font-bold text-emerald-200">
                  {pickedCount} / {totalCount} Items Picked
                </span>
                <div className="w-24 h-2 bg-emerald-950 rounded-full overflow-hidden">
                  <div
                    className="h-full bg-emerald-400 transition-all duration-300"
                    style={{ width: `${totalCount ? (pickedCount / totalCount) * 100 : 0}%` }}
                  />
                </div>
              </div>
            </div>

            {/* Barcode scanner simulator & Camera trigger */}
            <div className="p-4 bg-slate-50 border-b border-slate-200 flex flex-wrap items-center gap-2">
              <form onSubmit={handleBarcodeSubmit} className="flex-1 flex gap-2 min-w-[240px]">
                <div className="relative flex-1">
                  <Barcode size={16} className="absolute left-3 top-2.5 text-slate-400" />
                  <input
                    type="text"
                    value={scannedBarcode}
                    onChange={(e) => setScannedBarcode(e.target.value)}
                    placeholder="Scan product barcode (or SKU e.g. SKU-001)..."
                    className="w-full pl-9 pr-3 py-2 text-xs bg-white border border-slate-200 rounded-xl font-mono focus:outline-none focus:ring-2 focus:ring-[#176B45]"
                  />
                </div>
                <button
                  type="submit"
                  className="px-4 py-2 bg-[#176B45] text-white font-bold text-xs rounded-xl hover:bg-[#125638] transition-all shadow-xs shrink-0"
                >
                  Verify SKU
                </button>
              </form>

              <button
                type="button"
                onClick={() => setIsCameraScannerOpen(true)}
                className="px-3.5 py-2 bg-white border border-slate-300 hover:border-[#176B45] text-slate-700 text-xs font-bold rounded-xl flex items-center gap-1.5 transition-colors shadow-xs"
              >
                <Camera size={14} className="text-[#176B45]" />
                <span>Camera Barcode Scan</span>
              </button>
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

      {/* Camera Barcode Scanner Modal */}
      <CameraBarcodeScanner
        isOpen={isCameraScannerOpen}
        onClose={() => setIsCameraScannerOpen(false)}
        onBarcodeScanned={(code) => {
          verifyAndPickBarcode(code);
        }}
        expectedItemName={nextUnpickedItem?.productName}
        expectedSku={nextUnpickedItem?.sku}
      />
    </div>
  );
}
