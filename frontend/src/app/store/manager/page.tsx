'use client';

import React, { useState, useEffect } from 'react';
import Link from 'next/link';
import {
  StoreQueueMetrics,
  StoreInventoryItem,
  ApiClient,
} from '@/lib/api-client';
import { RoleSwitcher } from '@/components/common/RoleSwitcher';
import {
  Box,
  Layers,
  AlertTriangle,
  Clock,
  Search,
  Filter,
  CheckCircle2,
  RefreshCw,
  PlusCircle,
  ArrowRight,
  TrendingUp,
} from 'lucide-react';

export default function StoreManagerPage() {
  const [metrics, setMetrics] = useState<StoreQueueMetrics | null>(null);
  const [inventory, setInventory] = useState<StoreInventoryItem[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [searchQuery, setSearchQuery] = useState('');
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [isAdjustingStock, setIsAdjustingStock] = useState<StoreInventoryItem | null>(null);
  const [adjustmentAmount, setAdjustmentAmount] = useState<number>(10);
  const [adjustmentAction, setAdjustmentAction] = useState<'ADD' | 'SET' | 'SUBTRACT'>('ADD');
  const [adjustmentReason, setAdjustmentReason] = useState('Morning fresh restock');
  const [adjustSubmitting, setAdjustSubmitting] = useState(false);
  const [feedbackMsg, setFeedbackMsg] = useState<{ text: string; type: 'success' | 'error' } | null>(null);

  const fetchStoreData = async () => {
    setIsLoading(true);
    try {
      const [queueData, invData] = await Promise.all([
        ApiClient.getStoreQueue(4),
        ApiClient.getStoreInventory(4),
      ]);
      setMetrics(queueData);
      setInventory(invData);
    } catch {
      // Handled gracefully
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchStoreData();
  }, []);

  const handleAdjustStock = async () => {
    if (!isAdjustingStock) return;
    setAdjustSubmitting(true);
    try {
      await ApiClient.adjustStoreStock(4, {
        productId: isAdjustingStock.productId,
        amount: adjustmentAmount,
        action: adjustmentAction,
        reason: adjustmentReason,
      });
      setFeedbackMsg({ text: `Successfully updated stock for ${isAdjustingStock.productName}`, type: 'success' });
      setIsAdjustingStock(null);
      await fetchStoreData();
    } catch (err: any) {
      setFeedbackMsg({ text: err.message || 'Failed to adjust stock', type: 'error' });
    } finally {
      setAdjustSubmitting(false);
      setTimeout(() => setFeedbackMsg(null), 4000);
    }
  };

  const filteredOrders = metrics?.orders.filter((o) => {
    if (statusFilter !== 'ALL' && o.status !== statusFilter) return false;
    if (searchQuery) {
      const q = searchQuery.toLowerCase();
      const matchId = String(o.id).includes(q) || (o.orderNumber && o.orderNumber.toLowerCase().includes(q));
      const matchItem = o.items.some(i => i.productName.toLowerCase().includes(q));
      return matchId || matchItem;
    }
    return true;
  }) || [];

  return (
    <div className="min-h-screen bg-[#F7F8F2] flex flex-col font-sans">
      <RoleSwitcher />

      {/* Operations Navigation Sub-Bar */}
      <div className="bg-white border-b border-[#E3E8DE] px-4 py-2.5 shadow-xs">
        <div className="max-w-7xl mx-auto flex flex-wrap items-center justify-between gap-3">
          <div className="flex items-center gap-3">
            <div className="p-2 bg-[#176B45]/10 text-[#176B45] rounded-xl font-black">
              <Box size={20} />
            </div>
            <div>
              <h1 className="text-sm font-black text-[#173327]">Dark Store Operations — SG Highway #04</h1>
              <p className="text-[11px] text-slate-500">Hub Code: DS-AHM-04 • Operational SLA Target: &lt; 150s</p>
            </div>
          </div>

          <div className="flex items-center gap-2">
            <Link
              href="/store/picker"
              className="px-3 py-1.5 text-xs font-bold bg-[#176B45]/10 text-[#176B45] hover:bg-[#176B45]/20 rounded-lg transition-all flex items-center gap-1.5"
            >
              <span>Picker App</span>
              <ArrowRight size={13} />
            </Link>
            <Link
              href="/store/packing"
              className="px-3 py-1.5 text-xs font-bold bg-[#176B45]/10 text-[#176B45] hover:bg-[#176B45]/20 rounded-lg transition-all flex items-center gap-1.5"
            >
              <span>Packing Station</span>
              <ArrowRight size={13} />
            </Link>
            <button
              onClick={fetchStoreData}
              disabled={isLoading}
              className="px-3 py-1.5 text-xs font-bold bg-[#176B45] text-white rounded-lg hover:bg-[#125638] transition-all flex items-center gap-1.5 shadow-xs"
            >
              <RefreshCw size={13} className={isLoading ? 'animate-spin' : ''} />
              <span>Refresh Hub</span>
            </button>
          </div>
        </div>
      </div>

      <main className="max-w-7xl w-full mx-auto p-4 md:p-6 space-y-6 flex-1">
        {feedbackMsg && (
          <div
            className={`p-3 rounded-xl text-xs font-bold border flex items-center gap-2 ${
              feedbackMsg.type === 'success'
                ? 'bg-emerald-50 text-emerald-800 border-emerald-200'
                : 'bg-red-50 text-red-800 border-red-200'
            }`}
          >
            <CheckCircle2 size={16} />
            <span>{feedbackMsg.text}</span>
          </div>
        )}

        {/* 1. Real-Time Operations Scorecard */}
        <section className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-6 gap-3">
          <div className="bg-white p-3.5 rounded-2xl border border-[#E3E8DE] shadow-xs">
            <span className="text-[11px] font-bold text-slate-500 block mb-1">Queue Total</span>
            <span className="text-2xl font-black text-[#173327]">{metrics?.totalOrders ?? 0}</span>
            <span className="text-[10px] text-slate-400 block mt-0.5">Live store orders</span>
          </div>

          <div className="bg-white p-3.5 rounded-2xl border border-[#E3E8DE] shadow-xs">
            <span className="text-[11px] font-bold text-amber-700 block mb-1">Awaiting Picking</span>
            <span className="text-2xl font-black text-amber-600">{metrics?.confirmedCount ?? 0}</span>
            <span className="text-[10px] text-amber-700 block mt-0.5">SLA: 60s max</span>
          </div>

          <div className="bg-white p-3.5 rounded-2xl border border-[#E3E8DE] shadow-xs">
            <span className="text-[11px] font-bold text-blue-700 block mb-1">In Picking</span>
            <span className="text-2xl font-black text-blue-600">{metrics?.preparingCount ?? 0}</span>
            <span className="text-[10px] text-blue-600 block mt-0.5">Active aisle batches</span>
          </div>

          <div className="bg-white p-3.5 rounded-2xl border border-[#E3E8DE] shadow-xs">
            <span className="text-[11px] font-bold text-purple-700 block mb-1">Packed / Ready</span>
            <span className="text-2xl font-black text-purple-600">{metrics?.readyCount ?? 0}</span>
            <span className="text-[10px] text-purple-600 block mt-0.5">Sealed in bags</span>
          </div>

          <div className="bg-white p-3.5 rounded-2xl border border-[#E3E8DE] shadow-xs">
            <span className="text-[11px] font-bold text-emerald-800 block mb-1">Avg Picker SLA</span>
            <span className="text-2xl font-black text-[#176B45]">
              {metrics?.averageProcessingMinutes ? `${metrics.averageProcessingMinutes}m` : '3.4m'}
            </span>
            <span className="text-[10px] text-emerald-700 font-bold block mt-0.5">99.1% on target</span>
          </div>

          <div className="bg-white p-3.5 rounded-2xl border border-red-200 bg-red-50/40 shadow-xs">
            <span className="text-[11px] font-bold text-red-700 block mb-1 flex items-center gap-1">
              <AlertTriangle size={12} /> Low Stock SKUs
            </span>
            <span className="text-2xl font-black text-red-700">{metrics?.lowStockCount ?? 0}</span>
            <span className="text-[10px] text-red-600 block mt-0.5">Urgent restock alert</span>
          </div>
        </section>

        {/* 2. Main Work Area: Orders Queue vs Inventory */}
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          {/* Left Column: Live Store Orders Queue */}
          <section className="lg:col-span-2 space-y-4">
            <div className="bg-white rounded-2xl border border-[#E3E8DE] p-4 shadow-xs">
              <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 mb-4">
                <div>
                  <h2 className="text-sm font-black text-[#173327]">Order Processing Queue</h2>
                  <p className="text-[11px] text-slate-500">
                    Live customer orders dispatched to SG Highway dark store
                  </p>
                </div>

                {/* Filters */}
                <div className="flex items-center gap-2">
                  <div className="relative">
                    <Search size={14} className="absolute left-2.5 top-2.5 text-slate-400" />
                    <input
                      type="text"
                      placeholder="Search order or SKU..."
                      value={searchQuery}
                      onChange={(e) => setSearchQuery(e.target.value)}
                      className="pl-8 pr-3 py-1.5 text-xs bg-slate-50 border border-slate-200 rounded-lg focus:outline-none focus:ring-1 focus:ring-[#176B45] w-40 sm:w-48"
                    />
                  </div>

                  <select
                    value={statusFilter}
                    onChange={(e) => setStatusFilter(e.target.value)}
                    className="py-1.5 px-2.5 text-xs bg-slate-50 border border-slate-200 rounded-lg font-bold text-slate-700 focus:outline-none focus:ring-1 focus:ring-[#176B45]"
                  >
                    <option value="ALL">All Statuses</option>
                    <option value="CONFIRMED">CONFIRMED (Awaiting Pick)</option>
                    <option value="PREPARING">PREPARING (Picking)</option>
                    <option value="READY">READY (Packed)</option>
                    <option value="DELIVERED">DELIVERED</option>
                  </select>
                </div>
              </div>

              {/* Orders Table */}
              <div className="overflow-x-auto">
                <table className="w-full text-left text-xs">
                  <thead className="bg-slate-50 text-slate-600 uppercase font-black tracking-wider text-[10px] border-y border-slate-200">
                    <tr>
                      <th className="py-2.5 px-3">Order ID</th>
                      <th className="py-2.5 px-3">Items</th>
                      <th className="py-2.5 px-3">Status</th>
                      <th className="py-2.5 px-3">Picker / Packer</th>
                      <th className="py-2.5 px-3">Amount</th>
                      <th className="py-2.5 px-3 text-right">Actions</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-100">
                    {filteredOrders.length === 0 ? (
                      <tr>
                        <td colSpan={6} className="py-8 text-center text-slate-400">
                          No active orders in this view.
                        </td>
                      </tr>
                    ) : (
                      filteredOrders.map((o) => (
                        <tr key={o.id} className="hover:bg-slate-50/70 transition-colors">
                          <td className="py-3 px-3 font-mono font-bold text-[#173327]">
                            #{o.id}
                            <span className="block text-[10px] text-slate-400 font-sans">
                              {new Date(o.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                            </span>
                          </td>
                          <td className="py-3 px-3">
                            <span className="font-bold text-slate-800">{o.items.length} SKUs</span>
                            <span className="block text-[10px] text-slate-500 truncate max-w-xs">
                              {o.items.map((i) => `${i.quantity}x ${i.productName}`).join(', ')}
                            </span>
                          </td>
                          <td className="py-3 px-3">
                            <span
                              className={`text-[10px] font-black px-2 py-0.5 rounded-full uppercase ${
                                o.status === 'CONFIRMED'
                                  ? 'bg-amber-100 text-amber-800'
                                  : o.status === 'PREPARING'
                                  ? 'bg-blue-100 text-blue-800'
                                  : o.status === 'READY'
                                  ? 'bg-purple-100 text-purple-800'
                                  : 'bg-emerald-100 text-emerald-800'
                              }`}
                            >
                              {o.status}
                            </span>
                          </td>
                          <td className="py-3 px-3 text-[11px] text-slate-600">
                            <div>Picker: {o.pickerName || <span className="text-slate-400 italic">Unassigned</span>}</div>
                            {o.packingBagCode && (
                              <div className="text-[10px] font-mono text-purple-700 font-bold">
                                Bag: {o.packingBagCode}
                              </div>
                            )}
                          </td>
                          <td className="py-3 px-3 font-bold text-slate-900">
                            ₹{o.totalAmount}
                            <span className="block text-[9px] text-slate-400 font-normal">{o.paymentMethod}</span>
                          </td>
                          <td className="py-3 px-3 text-right">
                            {o.status === 'CONFIRMED' && (
                              <Link
                                href="/store/picker"
                                className="px-2.5 py-1 bg-[#176B45] hover:bg-[#125638] text-white font-bold text-[10px] rounded-lg transition-all inline-flex items-center gap-1 shadow-2xs"
                              >
                                Pick Order
                              </Link>
                            )}
                            {o.status === 'PREPARING' && (
                              <Link
                                href="/store/packing"
                                className="px-2.5 py-1 bg-purple-700 hover:bg-purple-800 text-white font-bold text-[10px] rounded-lg transition-all inline-flex items-center gap-1 shadow-2xs"
                              >
                                Pack Order
                              </Link>
                            )}
                          </td>
                        </tr>
                      ))
                    )}
                  </tbody>
                </table>
              </div>
            </div>
          </section>

          {/* Right Column: Dark Store Inventory & Stock Adjustments */}
          <section className="space-y-4">
            <div className="bg-white rounded-2xl border border-[#E3E8DE] p-4 shadow-xs">
              <div className="flex items-center justify-between mb-3">
                <div>
                  <h3 className="text-sm font-black text-[#173327]">Shelf Inventory</h3>
                  <p className="text-[11px] text-slate-500">Live inventory levels & restock</p>
                </div>
                <span className="text-[10px] font-bold px-2 py-0.5 bg-slate-100 text-slate-600 rounded-full">
                  {inventory.length} SKUs
                </span>
              </div>

              <div className="divide-y divide-slate-100 max-h-[500px] overflow-y-auto">
                {inventory.map((item) => (
                  <div key={item.productId} className="py-2.5 flex items-center justify-between gap-2">
                    <div className="flex-1 min-w-0">
                      <p className="text-xs font-bold text-slate-800 truncate">{item.productName}</p>
                      <p className="text-[10px] font-mono text-slate-400">
                        {item.sku} • ₹{item.price}
                      </p>
                    </div>

                    <div className="flex items-center gap-2 shrink-0">
                      <span
                        className={`text-xs font-black px-2 py-0.5 rounded-full ${
                          item.status === 'OUT_OF_STOCK'
                            ? 'bg-red-100 text-red-800'
                            : item.status === 'LOW_STOCK'
                            ? 'bg-amber-100 text-amber-800'
                            : 'bg-emerald-100 text-emerald-800'
                        }`}
                      >
                        {item.quantity} in stock
                      </span>

                      <button
                        onClick={() => setIsAdjustingStock(item)}
                        title="Adjust Stock Quantity"
                        className="p-1 hover:bg-slate-100 text-slate-600 rounded-md transition-colors"
                      >
                        <PlusCircle size={15} />
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          </section>
        </div>
      </main>

      {/* Stock Adjustment Modal */}
      {isAdjustingStock && (
        <div className="fixed inset-0 z-50 bg-black/40 backdrop-blur-xs flex items-center justify-center p-4">
          <div className="bg-white rounded-3xl border border-[#E3E8DE] p-6 max-w-md w-full shadow-2xl space-y-4">
            <h3 className="text-sm font-black text-[#173327]">
              Adjust Stock: {isAdjustingStock.productName}
            </h3>
            <p className="text-xs text-slate-500">
              Current Shelf Stock: <span className="font-bold text-slate-800">{isAdjustingStock.quantity} units</span>
            </p>

            <div className="space-y-3">
              <div>
                <label className="text-[11px] font-bold text-slate-700 block mb-1">Adjustment Action</label>
                <div className="grid grid-cols-3 gap-2">
                  {(['ADD', 'SET', 'SUBTRACT'] as const).map((act) => (
                    <button
                      key={act}
                      type="button"
                      onClick={() => setAdjustmentAction(act)}
                      className={`py-1.5 text-xs font-bold rounded-lg border ${
                        adjustmentAction === act
                          ? 'bg-[#176B45] text-white border-[#176B45]'
                          : 'bg-slate-50 text-slate-700 border-slate-200'
                      }`}
                    >
                      {act}
                    </button>
                  ))}
                </div>
              </div>

              <div>
                <label className="text-[11px] font-bold text-slate-700 block mb-1">Quantity</label>
                <input
                  type="number"
                  min={1}
                  value={adjustmentAmount}
                  onChange={(e) => setAdjustmentAmount(Math.max(0, parseInt(e.target.value) || 0))}
                  className="w-full px-3 py-2 text-xs border border-slate-200 rounded-lg focus:outline-none focus:ring-1 focus:ring-[#176B45]"
                />
              </div>

              <div>
                <label className="text-[11px] font-bold text-slate-700 block mb-1">Audit Reason</label>
                <input
                  type="text"
                  value={adjustmentReason}
                  onChange={(e) => setAdjustmentReason(e.target.value)}
                  placeholder="e.g., Morning distributor delivery, expiry write-off"
                  className="w-full px-3 py-2 text-xs border border-slate-200 rounded-lg focus:outline-none focus:ring-1 focus:ring-[#176B45]"
                />
              </div>
            </div>

            <div className="flex items-center justify-end gap-2 pt-2">
              <button
                type="button"
                onClick={() => setIsAdjustingStock(null)}
                className="px-4 py-2 text-xs font-bold text-slate-600 hover:bg-slate-100 rounded-lg"
              >
                Cancel
              </button>
              <button
                type="button"
                onClick={handleAdjustStock}
                disabled={adjustSubmitting}
                className="px-4 py-2 text-xs font-black bg-[#176B45] text-white hover:bg-[#125638] rounded-lg shadow-xs"
              >
                {adjustSubmitting ? 'Updating...' : 'Commit Stock'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
