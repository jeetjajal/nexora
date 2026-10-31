'use client';

import React, { useState, useEffect } from 'react';
import Link from 'next/link';
import {
  StoreQueueMetrics,
  StoreInventoryItem,
  ApiClient,
  InventoryBatchResponse,
  ColdChainOverviewResponse,
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
  Thermometer,
  ShieldAlert,
  ShieldCheck,
  Snowflake,
  Flame,
  Radio,
  Tag
} from 'lucide-react';

export default function StoreManagerPage() {
  const [activeTab, setActiveTab] = useState<'OPERATIONS' | 'COLD_CHAIN'>('OPERATIONS');
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

  // Cold Chain & FEFO State
  const [coldChainOverview, setColdChainOverview] = useState<ColdChainOverviewResponse | null>(null);
  const [batches, setBatches] = useState<InventoryBatchResponse[]>([]);
  const [coldChainLoading, setColdChainLoading] = useState(false);
  const [telemetryUnit, setTelemetryUnit] = useState('CHILLER_01');
  const [telemetryTemp, setTelemetryTemp] = useState<number>(4.2);
  const [telemetrySubmitting, setTelemetrySubmitting] = useState(false);

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
    } finally {
      setIsLoading(false);
    }
  };

  const fetchColdChainData = async () => {
    setColdChainLoading(true);
    try {
      const [overview, batchList] = await Promise.all([
        ApiClient.getColdChainOverview(4),
        ApiClient.getStoreColdChainBatches(4),
      ]);
      setColdChainOverview(overview);
      setBatches(batchList);
    } catch {
    } finally {
      setColdChainLoading(false);
    }
  };

  useEffect(() => {
    fetchStoreData();
  }, []);

  useEffect(() => {
    if (activeTab === 'COLD_CHAIN') {
      fetchColdChainData();
    }
  }, [activeTab]);

  const handleAdjustStock = async () => {
    if (!isAdjustingStock) return;
    setAdjustSubmitting(true);
    try {
      await ApiClient.adjustStoreStock(4, {
        productId: isAdjustingStock.productId,
        action: adjustmentAction,
        amount: adjustmentAmount,
        reason: adjustmentReason,
      });
      setFeedbackMsg({ text: `Stock updated for ${isAdjustingStock.productName}`, type: 'success' });
      setIsAdjustingStock(null);
      await fetchStoreData();
    } catch (err: any) {
      setFeedbackMsg({ text: err.message || 'Failed to adjust stock', type: 'error' });
    } finally {
      setAdjustSubmitting(false);
      setTimeout(() => setFeedbackMsg(null), 4000);
    }
  };

  const handleToggleQuarantine = async (batchId: number, currentQuarantined: boolean) => {
    try {
      await ApiClient.toggleBatchQuarantine(batchId, !currentQuarantined);
      setBatches(prev => prev.map(b => b.id === batchId ? { ...b, isQuarantined: !currentQuarantined } : b));
      setFeedbackMsg({ text: `Batch #${batchId} quarantine toggled to ${!currentQuarantined}`, type: 'success' });
    } catch (err: any) {
      setFeedbackMsg({ text: err.message, type: 'error' });
    } finally {
      setTimeout(() => setFeedbackMsg(null), 4000);
    }
  };

  const handleSendTelemetry = async (tempToUse?: number) => {
    setTelemetrySubmitting(true);
    try {
      const targetTemp = tempToUse !== undefined ? tempToUse : telemetryTemp;
      const res = await ApiClient.recordColdChainTelemetry(4, telemetryUnit, targetTemp);
      if (res.breachDetected) {
        setFeedbackMsg({ text: `⚠️ TEMPERATURE BREACH DETECTED: Unit ${telemetryUnit} reached ${targetTemp}°C!`, type: 'error' });
      } else {
        setFeedbackMsg({ text: `✅ IoT Sensor Ping logged: ${telemetryUnit} at ${targetTemp}°C (Normal)`, type: 'success' });
      }
      fetchColdChainData();
    } catch (err: any) {
      setFeedbackMsg({ text: err.message, type: 'error' });
    } finally {
      setTelemetrySubmitting(false);
      setTimeout(() => setFeedbackMsg(null), 5000);
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
              <p className="text-[11px] text-slate-500">Hub Code: DS-AHM-04 • NEXORA 3.0 Enterprise Operations</p>
            </div>
          </div>

          <div className="flex items-center gap-2">
            {/* View Switcher Tabs */}
            <div className="flex bg-[#F7F8F2] p-1 rounded-lg border border-[#E3E8DE]">
              <button
                onClick={() => setActiveTab('OPERATIONS')}
                className={`px-3 py-1 text-xs font-bold rounded-md transition-all ${
                  activeTab === 'OPERATIONS'
                    ? 'bg-[#176B45] text-white shadow-xs'
                    : 'text-slate-600 hover:text-slate-900'
                }`}
              >
                Store Queue & Shelf
              </button>
              <button
                onClick={() => setActiveTab('COLD_CHAIN')}
                className={`flex items-center gap-1.5 px-3 py-1 text-xs font-bold rounded-md transition-all ${
                  activeTab === 'COLD_CHAIN'
                    ? 'bg-[#176B45] text-white shadow-xs'
                    : 'text-slate-600 hover:text-slate-900'
                }`}
              >
                <Thermometer size={13} />
                <span>Cold Chain & FEFO</span>
              </button>
            </div>

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
              onClick={() => activeTab === 'OPERATIONS' ? fetchStoreData() : fetchColdChainData()}
              disabled={isLoading || coldChainLoading}
              className="px-3 py-1.5 text-xs font-bold bg-[#176B45] text-white rounded-lg hover:bg-[#125638] transition-all flex items-center gap-1.5 shadow-xs"
            >
              <RefreshCw size={13} className={isLoading || coldChainLoading ? 'animate-spin' : ''} />
              <span>Refresh</span>
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

        {/* ========================================================= */}
        {/* VIEW 1: TRADITIONAL OPERATIONS (QUEUE & SHELF INVENTORY) */}
        {/* ========================================================= */}
        {activeTab === 'OPERATIONS' && (
          <>
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

              <div className="bg-white p-3.5 rounded-2xl border border-[#E3E8DE] shadow-xs">
                <span className="text-[11px] font-bold text-slate-500 block mb-1">Low Stock SKUs</span>
                <span className="text-2xl font-black text-red-600">{metrics?.lowStockCount ?? 0}</span>
                <span className="text-[10px] text-red-500 block mt-0.5">Action required</span>
              </div>
            </section>

            <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
              <section className="lg:col-span-2 space-y-4">
                <div className="bg-white rounded-2xl border border-[#E3E8DE] overflow-hidden shadow-xs">
                  <div className="p-4 border-b border-[#E3E8DE] flex flex-wrap items-center justify-between gap-3 bg-white">
                    <div>
                      <h2 className="text-sm font-black text-[#173327]">Live Order Queue</h2>
                      <p className="text-[11px] text-slate-500">Live customer orders dispatched to SG Highway dark store</p>
                    </div>

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
                              <td className="py-2.5 px-3 font-mono font-bold text-[#176B45]">
                                {o.orderNumber || `NX-${o.id}`}
                              </td>
                              <td className="py-2.5 px-3">
                                <span className="font-bold">{o.items.length} items</span>
                              </td>
                              <td className="py-2.5 px-3">
                                <span
                                  className={`inline-flex items-center gap-1 px-2 py-0.5 rounded-full font-black text-[9px] ${
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
                              <td className="py-2.5 px-3 text-[11px] text-slate-600">
                                {o.pickerName || o.packerName ? `${o.pickerName || '-'} / ${o.packerName || '-'}` : 'Unassigned'}
                              </td>
                              <td className="py-2.5 px-3 font-bold text-slate-800">₹{o.totalAmount}</td>
                              <td className="py-2.5 px-3 text-right">
                                {o.status === 'CONFIRMED' && (
                                  <Link
                                    href="/store/picker"
                                    className="px-2.5 py-1 bg-[#176B45] hover:bg-[#125638] text-white font-bold text-[10px] rounded-lg transition-all inline-flex items-center gap-1 shadow-2xs"
                                  >
                                    Assign Picker
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
          </>
        )}

        {/* ========================================================= */}
        {/* VIEW 2: COLD CHAIN & FEFO BATCH EXPIRY RADAR (FEATURE 4) */}
        {/* ========================================================= */}
        {activeTab === 'COLD_CHAIN' && (
          <div className="space-y-6">
            {/* Storage Units Temperature Gauges Grid */}
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
              {coldChainOverview?.units.map((unit) => (
                <div key={unit.storageUnit} className="bg-white p-4 rounded-2xl border border-[#E3E8DE] shadow-xs">
                  <div className="flex items-center justify-between mb-2">
                    <span className="text-xs font-bold text-slate-700 truncate">{unit.storageUnit}</span>
                    {unit.inBreach ? (
                      <ShieldAlert size={16} className="text-rose-600 animate-bounce" />
                    ) : (
                      <ShieldCheck size={16} className="text-emerald-600" />
                    )}
                  </div>

                  <div className="flex items-baseline justify-between">
                    <span className={`text-2xl font-black ${unit.inBreach ? 'text-rose-600' : 'text-[#176B45]'}`}>
                      {unit.currentTemp}°C
                    </span>
                    <span className="text-[11px] font-mono text-slate-400">
                      Target: {unit.targetTemp}°C
                    </span>
                  </div>

                  <div className="mt-2 text-[10px] text-slate-500 flex items-center justify-between">
                    <span>Safe: {unit.minSafe}°C to {unit.maxSafe}°C</span>
                    <span className="font-semibold text-emerald-700">{unit.lastUpdated}</span>
                  </div>
                </div>
              ))}
            </div>

            {/* IoT Telemetry Simulation Panel */}
            <div className="bg-white p-4 rounded-2xl border border-[#E3E8DE] shadow-xs flex flex-wrap items-center justify-between gap-4">
              <div className="flex items-center gap-3">
                <div className="p-2.5 bg-blue-50 text-blue-700 rounded-xl">
                  <Radio size={20} />
                </div>
                <div>
                  <h3 className="text-sm font-black text-[#173327]">IoT Temperature Sensor Telemetry Ingestion</h3>
                  <p className="text-[11px] text-slate-500">Live sensor pings from chillers, deep freezers, and rider cool-boxes</p>
                </div>
              </div>

              <div className="flex items-center gap-2 flex-wrap text-xs">
                <select
                  value={telemetryUnit}
                  onChange={(e) => setTelemetryUnit(e.target.value)}
                  className="px-3 py-1.5 bg-slate-50 border border-slate-200 rounded-lg font-bold text-slate-700"
                >
                  <option value="CHILLER_01">CHILLER_01 (Dairy)</option>
                  <option value="CHILLER_02">CHILLER_02 (Veggies)</option>
                  <option value="FREEZER_01">FREEZER_01 (Frozen)</option>
                  <option value="TRANSIT_COOLBOX_12">TRANSIT_COOLBOX_12</option>
                </select>

                <input
                  type="number"
                  step="0.1"
                  value={telemetryTemp}
                  onChange={(e) => setTelemetryTemp(parseFloat(e.target.value))}
                  className="w-20 px-2.5 py-1.5 bg-slate-50 border border-slate-200 rounded-lg font-bold text-slate-800"
                />
                <span className="text-slate-500 font-bold">°C</span>

                <button
                  onClick={() => handleSendTelemetry()}
                  disabled={telemetrySubmitting}
                  className="px-3 py-1.5 bg-[#176B45] text-white font-bold rounded-lg hover:bg-[#125638] transition-colors"
                >
                  Simulate Sensor Ping
                </button>

                <button
                  onClick={() => handleSendTelemetry(9.5)}
                  disabled={telemetrySubmitting}
                  className="px-3 py-1.5 bg-rose-600 text-white font-bold rounded-lg hover:bg-rose-700 transition-colors shadow-2xs"
                >
                  Trigger Breach (9.5°C)
                </button>
              </div>
            </div>

            {/* FEFO Batches Table */}
            <div className="bg-white rounded-2xl border border-[#E3E8DE] overflow-hidden shadow-xs">
              <div className="p-4 border-b border-[#E3E8DE] flex items-center justify-between">
                <div>
                  <h3 className="text-sm font-black text-[#173327]">FEFO Batch Ledger (First-Expiry-First-Out)</h3>
                  <p className="text-[11px] text-slate-500">
                    Automated picking allocation orders stock expiring earliest to eliminate store shrink
                  </p>
                </div>
                <div className="flex items-center gap-2 text-xs">
                  <span className="px-2.5 py-1 bg-amber-50 text-amber-800 font-bold rounded-full border border-amber-200">
                    24h: 40% Clearance
                  </span>
                  <span className="px-2.5 py-1 bg-yellow-50 text-yellow-800 font-bold rounded-full border border-yellow-200">
                    48h: 20% Discount
                  </span>
                </div>
              </div>

              <div className="overflow-x-auto">
                <table className="w-full text-left text-xs">
                  <thead className="bg-slate-50 text-slate-600 uppercase font-black tracking-wider text-[10px] border-b border-slate-200">
                    <tr>
                      <th className="py-2.5 px-3">Batch Code</th>
                      <th className="py-2.5 px-3">SKU Name</th>
                      <th className="py-2.5 px-3">Zone</th>
                      <th className="py-2.5 px-3">Expiry Date</th>
                      <th className="py-2.5 px-3">Days Left</th>
                      <th className="py-2.5 px-3">Stock Available</th>
                      <th className="py-2.5 px-3">Discount Rule</th>
                      <th className="py-2.5 px-3">FEFO Status</th>
                      <th className="py-2.5 px-3 text-right">Quarantine</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-100">
                    {batches.map((batch, idx) => (
                      <tr key={batch.id} className="hover:bg-slate-50/70 transition-colors">
                        <td className="py-2.5 px-3 font-mono font-bold text-[#176B45]">
                          {batch.batchNumber}
                        </td>
                        <td className="py-2.5 px-3 font-bold text-slate-800">
                          {batch.productName}
                        </td>
                        <td className="py-2.5 px-3">
                          <span className="text-[10px] font-mono px-2 py-0.5 rounded bg-slate-100 text-slate-700">
                            {batch.temperatureZone}
                          </span>
                        </td>
                        <td className="py-2.5 px-3 font-mono text-slate-600">
                          {batch.expiryDate}
                        </td>
                        <td className="py-2.5 px-3 font-bold">
                          <span className={batch.daysUntilExpiry <= 1 ? 'text-rose-600 font-black' : batch.daysUntilExpiry <= 2 ? 'text-amber-600 font-black' : 'text-slate-700'}>
                            {batch.daysUntilExpiry <= 0 ? 'EXPIRED' : `${batch.daysUntilExpiry} days`}
                          </span>
                        </td>
                        <td className="py-2.5 px-3 font-bold">
                          {batch.availableQuantity} / {batch.initialQuantity}
                        </td>
                        <td className="py-2.5 px-3">
                          {batch.discountAppliedPercent > 0 ? (
                            <span className="inline-flex items-center gap-1 px-2 py-0.5 bg-rose-100 text-rose-800 font-extrabold text-[10px] rounded-full">
                              <Tag size={10} />
                              <span>{batch.discountAppliedPercent}% OFF</span>
                            </span>
                          ) : (
                            <span className="text-slate-400 text-[11px]">Regular</span>
                          )}
                        </td>
                        <td className="py-2.5 px-3">
                          <span
                            className={`inline-flex items-center gap-1 px-2 py-0.5 rounded-full font-black text-[9px] ${
                              batch.isQuarantined
                                ? 'bg-red-100 text-red-800'
                                : batch.freshnessStatus === 'CRITICAL_24H'
                                ? 'bg-rose-100 text-rose-800'
                                : batch.freshnessStatus === 'NEAR_EXPIRY_48H'
                                ? 'bg-amber-100 text-amber-800'
                                : 'bg-emerald-100 text-emerald-800'
                            }`}
                          >
                            {batch.isQuarantined ? 'QUARANTINED' : batch.freshnessStatus}
                          </span>
                        </td>
                        <td className="py-2.5 px-3 text-right">
                          <button
                            onClick={() => handleToggleQuarantine(batch.id, batch.isQuarantined)}
                            className={`px-2.5 py-1 text-[10px] font-bold rounded-lg transition-colors border ${
                              batch.isQuarantined
                                ? 'bg-emerald-50 text-emerald-800 border-emerald-200 hover:bg-emerald-100'
                                : 'bg-red-50 text-red-700 border-red-200 hover:bg-red-100'
                            }`}
                          >
                            {batch.isQuarantined ? 'Release' : 'Quarantine'}
                          </button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          </div>
        )}
      </main>
    </div>
  );
}
