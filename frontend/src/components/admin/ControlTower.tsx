'use client';

import React, { useState, useEffect } from 'react';
import { useApp } from '@/context/AppContext';
import { ApiClient, DeliveryBatch } from '@/lib/api-client';
import {
  TrendingUp,
  Package,
  Bike,
  Clock,
  AlertTriangle,
  Building2,
  RefreshCw,
  Search,
  Filter,
  CheckCircle,
  Eye,
  Sliders,
  Layers,
  MapPin,
  Send,
  XCircle,
  UserCheck,
  ChevronRight,
  ShieldCheck,
  Zap
} from 'lucide-react';

interface MockOrder {
  id: string;
  customer: string;
  store: string;
  itemsCount: number;
  amount: number;
  status: 'PENDING' | 'PICKING' | 'IN_TRANSIT' | 'DELIVERED';
  rider: string;
  time: string;
}

export const ControlTower: React.FC = () => {
  const { activeOrder, updateOrderStatus, currentUser } = useApp();
  const token = currentUser?.accessToken || (typeof window !== 'undefined' ? localStorage.getItem('nexora_token') || undefined : undefined);
  const [activeTab, setActiveTab] = useState<'DISPATCH_FEED' | 'MULTI_ORDER_BATCHING'>('DISPATCH_FEED');
  const [filterStatus, setFilterStatus] = useState<string>('ALL');

  // Multi-Order Batching State
  const [batches, setBatches] = useState<DeliveryBatch[]>([]);
  const [loadingBatches, setLoadingBatches] = useState<boolean>(false);
  const [generatingBatches, setGeneratingBatches] = useState<boolean>(false);
  const [batchActionLoading, setBatchActionLoading] = useState<number | null>(null);
  const [batchMessage, setBatchMessage] = useState<string | null>(null);

  const initialOrders: MockOrder[] = [
    {
      id: 'NX-849201',
      customer: 'Amitabh Verma',
      store: 'SG Highway #04',
      itemsCount: 6,
      amount: 420,
      status: 'IN_TRANSIT',
      rider: 'Ramesh Patel (Hero EV)',
      time: '4 mins ago',
    },
    {
      id: 'NX-849202',
      customer: 'Neha Mehta',
      store: 'Vastrapur #02',
      itemsCount: 3,
      amount: 215,
      status: 'PICKING',
      rider: 'Vikram Solanki',
      time: '6 mins ago',
    },
    {
      id: 'NX-849203',
      customer: 'Kunal Shah',
      store: 'Prahlad Nagar #01',
      itemsCount: 9,
      amount: 880,
      status: 'PENDING',
      rider: 'Assigning...',
      time: 'Just now',
    },
    {
      id: 'NX-849199',
      customer: 'Ananya Roy',
      store: 'Bopal Hub #03',
      itemsCount: 4,
      amount: 340,
      status: 'DELIVERED',
      rider: 'Mohit Rawat',
      time: '12 mins ago',
    },
  ];

  // If user placed an active order, add it to the top!
  const allOrders = activeOrder
    ? [
        {
          id: activeOrder.id,
          customer: 'You (Current User)',
          store: 'SG Highway #04',
          itemsCount: activeOrder.items.length,
          amount: activeOrder.finalTotal,
          status: (activeOrder.status === 'OUT_FOR_DELIVERY'
            ? 'IN_TRANSIT'
            : activeOrder.status === 'STORE_PICKING'
            ? 'PICKING'
            : activeOrder.status === 'DELIVERED'
            ? 'DELIVERED'
            : 'PENDING') as MockOrder['status'],
          rider: activeOrder.riderName,
          time: 'Active Session',
        },
        ...initialOrders,
      ]
    : initialOrders;

  const filteredOrders = filterStatus === 'ALL'
    ? allOrders
    : allOrders.filter(o => o.status === filterStatus);

  // Fetch batches when tab switched to batching
  const fetchBatches = async () => {
    setLoadingBatches(true);
    try {
      const data = await ApiClient.getDeliveryBatches(undefined, token || undefined);
      setBatches(data);
    } catch {
      // Handled by api fallback
    } finally {
      setLoadingBatches(false);
    }
  };

  useEffect(() => {
    if (activeTab === 'MULTI_ORDER_BATCHING') {
      fetchBatches();
    }
  }, [activeTab]);

  const handleGenerateBatches = async () => {
    setGeneratingBatches(true);
    setBatchMessage(null);
    try {
      const created = await ApiClient.generateDeliveryBatches(undefined, 3, 2.5, token || undefined);
      setBatches(prev => [...created, ...prev.filter(p => !created.some(c => c.id === p.id))]);
      setBatchMessage(`Successfully clustered compatible orders into ${created.length || 1} new delivery batches.`);
    } catch (err: any) {
      setBatchMessage(`Batch generation failed: ${err.message}`);
    } finally {
      setGeneratingBatches(false);
    }
  };

  const handleAssignRider = async (batchId: number) => {
    setBatchActionLoading(batchId);
    try {
      const updated = await ApiClient.assignDeliveryBatch(batchId, 501, token || undefined);
      setBatches(prev => prev.map(b => b.id === batchId ? { ...b, ...updated, status: 'ASSIGNED', riderName: 'Vikram Solanki (Hero EV)' } : b));
      setBatchMessage(`Batch #${batchId} assigned to rider Vikram Solanki.`);
    } catch (err: any) {
      setBatchMessage(err.message);
    } finally {
      setBatchActionLoading(null);
    }
  };

  const handleDispatchBatch = async (batchId: number) => {
    setBatchActionLoading(batchId);
    try {
      const updated = await ApiClient.dispatchDeliveryBatch(batchId, token || undefined);
      setBatches(prev => prev.map(b => b.id === batchId ? { ...b, ...updated, status: 'IN_TRANSIT' } : b));
      setBatchMessage(`Batch #${batchId} dispatched to road! Customers notified.`);
    } catch (err: any) {
      setBatchMessage(err.message);
    } finally {
      setBatchActionLoading(null);
    }
  };

  const handleCancelBatch = async (batchId: number) => {
    setBatchActionLoading(batchId);
    try {
      const updated = await ApiClient.cancelDeliveryBatch(batchId, token || undefined);
      setBatches(prev => prev.map(b => b.id === batchId ? { ...b, ...updated, status: 'CANCELLED' } : b));
      setBatchMessage(`Batch #${batchId} unbundled. Orders returned to individual dispatch queues.`);
    } catch (err: any) {
      setBatchMessage(err.message);
    } finally {
      setBatchActionLoading(null);
    }
  };

  return (
    <div className="max-w-7xl mx-auto py-6 px-4 space-y-6">
      {/* Title & Top Bar */}
      <div className="flex flex-wrap items-center justify-between gap-4 bg-white p-4 rounded-card border border-brand-border shadow-card">
        <div>
          <div className="flex items-center gap-2">
            <h2 className="text-xl font-black text-brand-text">Operations Control Tower</h2>
            <span className="text-[10px] bg-brand-primary text-white font-extrabold px-2 py-0.5 rounded-full">
              NEXORA 3.0 RADAR
            </span>
          </div>
          <p className="text-xs text-brand-muted mt-0.5">
            Real-time fleet monitoring, multi-order routing engine, and dark-store logistics.
          </p>
        </div>

        <div className="flex items-center gap-2">
          {/* View Tab Selector */}
          <div className="flex bg-brand-bg p-1 rounded-button border border-brand-border">
            <button
              onClick={() => setActiveTab('DISPATCH_FEED')}
              className={`flex items-center gap-1.5 px-3 py-1.5 text-xs font-bold rounded-button transition-all ${
                activeTab === 'DISPATCH_FEED'
                  ? 'bg-brand-primary text-white shadow-xs'
                  : 'text-brand-muted hover:text-brand-text'
              }`}
            >
              <Bike size={13} />
              <span>Live Dispatch Feed</span>
            </button>
            <button
              onClick={() => setActiveTab('MULTI_ORDER_BATCHING')}
              className={`flex items-center gap-1.5 px-3 py-1.5 text-xs font-bold rounded-button transition-all ${
                activeTab === 'MULTI_ORDER_BATCHING'
                  ? 'bg-brand-primary text-white shadow-xs'
                  : 'text-brand-muted hover:text-brand-text'
              }`}
            >
              <Layers size={13} />
              <span>Smart Batching Engine</span>
            </button>
          </div>

          <button 
            onClick={() => activeTab === 'MULTI_ORDER_BATCHING' ? fetchBatches() : null}
            className="flex items-center gap-1.5 px-3 py-1.5 bg-brand-bg hover:bg-brand-border text-brand-text text-xs font-bold rounded-button transition-colors"
          >
            <RefreshCw size={13} className={loadingBatches ? 'animate-spin' : ''} />
            <span>Refresh</span>
          </button>
          <span className="inline-flex items-center gap-1 text-xs text-emerald-700 bg-emerald-100 font-bold px-3 py-1.5 rounded-full">
            <span className="w-2 h-2 rounded-full bg-emerald-500 animate-ping" />
            Live SSE Active
          </span>
        </div>
      </div>

      {/* KPI Cards Grid */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-5 gap-2.5 sm:gap-3">
        <div className="bg-white p-4 rounded-card border border-brand-border shadow-card">
          <div className="flex items-center justify-between text-brand-muted mb-1">
            <span className="text-[11px] font-bold">Gross Order Value</span>
            <TrendingUp size={14} className="text-brand-primary" />
          </div>
          <span className="text-xl font-black text-brand-text">₹1,48,290</span>
          <span className="text-[10px] text-emerald-600 font-bold block mt-0.5">+18.4% vs yesterday</span>
        </div>

        <div className="bg-white p-4 rounded-card border border-brand-border shadow-card">
          <div className="flex items-center justify-between text-brand-muted mb-1">
            <span className="text-[11px] font-bold">Avg Delivery Time</span>
            <Clock size={14} className="text-brand-primary" />
          </div>
          <span className="text-xl font-black text-brand-primary">8.8 Mins</span>
          <span className="text-[10px] text-brand-muted font-bold block mt-0.5">SLA Target: &lt;10 mins</span>
        </div>

        <div className="bg-white p-4 rounded-card border border-brand-border shadow-card">
          <div className="flex items-center justify-between text-brand-muted mb-1">
            <span className="text-[11px] font-bold">Multi-Order Batches</span>
            <Layers size={14} className="text-brand-primary" />
          </div>
          <span className="text-xl font-black text-brand-text">{batches.length || 2} Batches</span>
          <span className="text-[10px] text-emerald-600 font-bold block mt-0.5">34% Fleet Mileage Saved</span>
        </div>

        <div className="bg-white p-4 rounded-card border border-brand-border shadow-card">
          <div className="flex items-center justify-between text-brand-muted mb-1">
            <span className="text-[11px] font-bold">Fleet Utilization</span>
            <Bike size={14} className="text-brand-primary" />
          </div>
          <span className="text-xl font-black text-brand-text">64 Active</span>
          <span className="text-[10px] text-emerald-600 font-bold block mt-0.5">91% In-Transit Ratio</span>
        </div>

        <div className="bg-white p-4 rounded-card border border-brand-border shadow-card sm:col-span-2 lg:col-span-1">
          <div className="flex items-center justify-between text-brand-muted mb-1">
            <span className="text-[11px] font-bold">Cold Chain Telemetry</span>
            <ShieldCheck size={14} className="text-emerald-600" />
          </div>
          <span className="text-xl font-black text-emerald-700">4.1°C Safe</span>
          <span className="text-[10px] text-brand-muted font-bold block mt-0.5">All 4 Chillers In Bounds</span>
        </div>
      </div>

      {batchMessage && (
        <div className="bg-brand-primary/10 border border-brand-primary/20 text-brand-primary px-4 py-3 rounded-card text-xs font-bold flex items-center justify-between">
          <span>{batchMessage}</span>
          <button onClick={() => setBatchMessage(null)} className="text-brand-primary hover:text-black font-extrabold text-sm">✕</button>
        </div>
      )}

      {/* VIEW 1: LIVE DISPATCH FEED */}
      {activeTab === 'DISPATCH_FEED' && (
        <div className="bg-white rounded-card border border-brand-border shadow-card overflow-hidden">
          {/* Filter bar */}
          <div className="p-4 border-b border-brand-border flex flex-wrap items-center justify-between gap-3">
            <div className="flex items-center gap-2">
              <h3 className="text-sm font-black text-brand-text">Live Order Feed</h3>
              <span className="text-xs bg-brand-bg text-brand-muted font-bold px-2 py-0.5 rounded-full border border-brand-border">
                {filteredOrders.length} Orders
              </span>
            </div>

            <div className="flex items-center gap-1.5 overflow-x-auto text-xs">
              {['ALL', 'PENDING', 'PICKING', 'IN_TRANSIT', 'DELIVERED'].map((st) => (
                <button
                  key={st}
                  onClick={() => setFilterStatus(st)}
                  className={`px-3 py-1 rounded-full font-bold transition-all ${
                    filterStatus === st
                      ? 'bg-brand-primary text-white shadow-xs'
                      : 'bg-brand-bg text-brand-muted hover:text-brand-text'
                  }`}
                >
                  {st}
                </button>
              ))}
            </div>
          </div>

          {/* Table */}
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead className="bg-brand-bg text-brand-muted font-extrabold uppercase tracking-wider text-[10px] border-b border-brand-border">
                <tr>
                  <th className="py-3 px-4">Order ID</th>
                  <th className="py-3 px-4">Customer</th>
                  <th className="py-3 px-4">Dark Store Hub</th>
                  <th className="py-3 px-4">Items / Total</th>
                  <th className="py-3 px-4">Assigned Partner</th>
                  <th className="py-3 px-4">Live Status</th>
                  <th className="py-3 px-4 text-right">Quick Dispatch</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-brand-border text-brand-text font-medium">
                {filteredOrders.map((ord) => (
                  <tr key={ord.id} className="hover:bg-brand-bg/50 transition-colors">
                    <td className="py-3 px-4 font-mono font-bold text-brand-primary">
                      {ord.id}
                    </td>
                    <td className="py-3 px-4 font-bold">{ord.customer}</td>
                    <td className="py-3 px-4 text-brand-muted">{ord.store}</td>
                    <td className="py-3 px-4">
                      <span className="font-bold">{ord.itemsCount} items</span> • ₹{ord.amount}
                    </td>
                    <td className="py-3 px-4">{ord.rider}</td>
                    <td className="py-3 px-4">
                      <span
                        className={`inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full font-bold text-[10px] ${
                          ord.status === 'DELIVERED'
                            ? 'bg-emerald-100 text-emerald-800'
                            : ord.status === 'IN_TRANSIT'
                            ? 'bg-blue-100 text-blue-800'
                            : ord.status === 'PICKING'
                            ? 'bg-amber-100 text-amber-800'
                            : 'bg-purple-100 text-purple-800'
                        }`}
                      >
                        {ord.status}
                      </span>
                    </td>
                    <td className="py-3 px-4 text-right">
                      {ord.id === activeOrder?.id && ord.status !== 'DELIVERED' ? (
                        <button
                          onClick={() => updateOrderStatus('DELIVERED')}
                          className="px-2.5 py-1 bg-brand-primary hover:bg-brand-primaryHover text-white rounded font-bold text-[10px] shadow-xs"
                        >
                          Force Mark Delivered
                        </button>
                      ) : (
                        <span className="text-[11px] text-brand-muted">{ord.time}</span>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* VIEW 2: SMART MULTI-ORDER BATCHING ENGINE */}
      {activeTab === 'MULTI_ORDER_BATCHING' && (
        <div className="space-y-4">
          {/* Action Bar */}
          <div className="bg-white p-4 rounded-card border border-brand-border shadow-card flex flex-wrap items-center justify-between gap-4">
            <div>
              <div className="flex items-center gap-2">
                <Layers className="text-brand-primary" size={20} />
                <h3 className="text-base font-black text-brand-text">Multi-Order Route Optimizer</h3>
                <span className="text-[10px] bg-brand-bg text-brand-text font-bold px-2 py-0.5 rounded-full border border-brand-border">
                  Radius &lt;= 2.5km • Capacity &lt;= 3 Drops
                </span>
              </div>
              <p className="text-xs text-brand-muted mt-0.5">
                Automatically clusters nearby orders from the same dark store and solves Traveling Salesperson Stop Sequence to maximize rider capacity.
              </p>
            </div>

            <button
              onClick={handleGenerateBatches}
              disabled={generatingBatches}
              className="flex items-center gap-2 px-4 py-2 bg-brand-primary hover:bg-brand-primaryHover disabled:opacity-50 text-white font-bold text-xs rounded-button shadow-card transition-all"
            >
              <Zap size={14} className={generatingBatches ? 'animate-spin' : ''} />
              <span>{generatingBatches ? 'Evaluating Clusters...' : '⚡ Auto-Cluster Eligible Orders'}</span>
            </button>
          </div>

          {/* Batches Grid */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {batches.map((batch) => (
              <div key={batch.id} className="bg-white rounded-card border border-brand-border shadow-card overflow-hidden flex flex-col justify-between">
                {/* Header */}
                <div className="p-4 border-b border-brand-border bg-brand-bg/40 flex items-center justify-between">
                  <div className="flex items-center gap-2">
                    <span className="font-mono font-black text-xs text-brand-primary bg-white px-2 py-0.5 rounded border border-brand-border">
                      {batch.batchCode}
                    </span>
                    <span className="text-xs font-bold text-brand-text">
                      {batch.storeName}
                    </span>
                  </div>

                  <span
                    className={`inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full font-bold text-[10px] ${
                      batch.status === 'COMPLETED'
                        ? 'bg-emerald-100 text-emerald-800'
                        : batch.status === 'IN_TRANSIT'
                        ? 'bg-blue-100 text-blue-800'
                        : batch.status === 'ASSIGNED'
                        ? 'bg-purple-100 text-purple-800'
                        : batch.status === 'CANCELLED'
                        ? 'bg-rose-100 text-rose-800'
                        : 'bg-amber-100 text-amber-800'
                    }`}
                  >
                    {batch.status}
                  </span>
                </div>

                {/* Metrics ribbon */}
                <div className="grid grid-cols-3 divide-x divide-brand-border border-b border-brand-border text-center text-xs py-2 bg-white">
                  <div>
                    <span className="text-[10px] text-brand-muted block font-semibold">Bundled Orders</span>
                    <span className="font-extrabold text-brand-text">{batch.orderCount} Drops</span>
                  </div>
                  <div>
                    <span className="text-[10px] text-brand-muted block font-semibold">Total Route</span>
                    <span className="font-extrabold text-brand-text">{batch.totalDistanceKm} km</span>
                  </div>
                  <div>
                    <span className="text-[10px] text-brand-muted block font-semibold">Est. Delivery SLA</span>
                    <span className="font-extrabold text-brand-primary">~{batch.estimatedDurationMins} mins</span>
                  </div>
                </div>

                {/* Rider Assignment Info */}
                <div className="p-3 bg-brand-bg/20 border-b border-brand-border flex items-center justify-between text-xs">
                  <div className="flex items-center gap-2">
                    <Bike size={15} className="text-brand-primary" />
                    <div>
                      <span className="text-[10px] text-brand-muted block">Assigned Partner:</span>
                      <span className="font-bold text-brand-text">
                        {batch.riderName || 'Unassigned (Awaiting Match)'}
                      </span>
                    </div>
                  </div>

                  {!batch.deliveryPartnerId && batch.status === 'CREATED' && (
                    <button
                      onClick={() => handleAssignRider(batch.id)}
                      disabled={batchActionLoading === batch.id}
                      className="px-2.5 py-1 bg-brand-primary text-white rounded font-bold text-[10px] hover:bg-brand-primaryHover transition-colors"
                    >
                      {batchActionLoading === batch.id ? 'Assigning...' : 'Assign Hero EV'}
                    </button>
                  )}
                </div>

                {/* Sequenced Stops List */}
                <div className="p-4 space-y-2.5 flex-1">
                  <span className="text-[10px] font-extrabold uppercase text-brand-muted tracking-wider block">
                    Optimized Drop Sequence:
                  </span>
                  {batch.stops && batch.stops.map((stop, idx) => (
                    <div key={stop.orderId} className="flex items-start gap-2.5 bg-brand-bg/50 p-2.5 rounded border border-brand-border/60">
                      <div className="w-5 h-5 rounded-full bg-brand-primary text-white font-extrabold text-[10px] flex items-center justify-center shrink-0 mt-0.5">
                        {stop.sequenceIndex || idx + 1}
                      </div>
                      <div className="flex-1 min-w-0">
                        <div className="flex items-center justify-between text-xs font-bold text-brand-text">
                          <span className="truncate">{stop.customerName}</span>
                          <span className="text-brand-primary font-mono font-extrabold">₹{stop.totalAmount}</span>
                        </div>
                        <p className="text-[11px] text-brand-muted truncate mt-0.5 flex items-center gap-1">
                          <MapPin size={11} className="shrink-0 text-brand-primary" />
                          <span>{stop.deliveryAddress}</span>
                        </p>
                      </div>
                    </div>
                  ))}
                </div>

                {/* Actions Footer */}
                <div className="p-3 bg-brand-bg/60 border-t border-brand-border flex items-center justify-between gap-2">
                  <div className="text-[10px] text-brand-muted">
                    Created {new Date(batch.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                  </div>

                  <div className="flex items-center gap-2">
                    {batch.status !== 'CANCELLED' && batch.status !== 'COMPLETED' && (
                      <button
                        onClick={() => handleCancelBatch(batch.id)}
                        disabled={batchActionLoading === batch.id}
                        className="px-2.5 py-1 text-rose-600 hover:bg-rose-50 border border-rose-200 rounded font-bold text-[10px] transition-colors"
                      >
                        Cancel
                      </button>
                    )}

                    {(batch.status === 'CREATED' || batch.status === 'ASSIGNED') && (
                      <button
                        onClick={() => handleDispatchBatch(batch.id)}
                        disabled={batchActionLoading === batch.id}
                        className="flex items-center gap-1 px-3 py-1 bg-brand-primary hover:bg-brand-primaryHover text-white rounded font-bold text-[10px] shadow-xs transition-colors"
                      >
                        <Send size={11} />
                        <span>Dispatch Batch</span>
                      </button>
                    )}
                  </div>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
};

export default ControlTower;
