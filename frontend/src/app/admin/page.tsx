'use client';

import React, { useState, useEffect } from 'react';
import {
  ApiClient,
  ControlTowerOverview,
  FleetRiderStatus,
  DisputeRecord,
  SurgePricingRuleDto,
  BulkImportResult,
  AnalyticsOverviewResponse,
  NotificationLogItem,
} from '@/lib/api-client';
import { RoleSwitcher } from '@/components/common/RoleSwitcher';
import {
  Radar,
  TrendingUp,
  AlertTriangle,
  Clock,
  Bike,
  Store,
  Layers,
  RotateCcw,
  Zap,
  UploadCloud,
  CheckCircle2,
  RefreshCw,
  Search,
  Filter,
  DollarSign,
  ShieldAlert,
  Sliders,
  FileText,
  BarChart3,
  Bell,
} from 'lucide-react';

export default function SuperAdminPage() {
  const [activeTab, setActiveTab] = useState<'overview' | 'disputes' | 'fleet' | 'surge' | 'catalog' | 'analytics' | 'notifications'>('overview');
  const [analytics, setAnalytics] = useState<AnalyticsOverviewResponse | null>(null);
  const [analyticsRange, setAnalyticsRange] = useState<'7d' | '30d' | 'all'>('30d');
  const [notificationLogs, setNotificationLogs] = useState<NotificationLogItem[]>([]);
  const [providerStatus, setProviderStatus] = useState<Record<string, { channel: string; isConfigured: boolean }> | null>(null);
  const [overview, setOverview] = useState<ControlTowerOverview | null>(null);
  const [fleet, setFleet] = useState<FleetRiderStatus[]>([]);
  const [disputes, setDisputes] = useState<DisputeRecord[]>([]);
  const [surgeRules, setSurgeRules] = useState<SurgePricingRuleDto[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  // Dispute Decision Modal State
  const [selectedDispute, setSelectedDispute] = useState<DisputeRecord | null>(null);
  const [decisionNotes, setDecisionNotes] = useState('');
  const [isProcessingDecision, setIsProcessingDecision] = useState(false);

  // Bulk Catalog Import State
  const [csvInput, setCsvInput] = useState(
    'name,category,price,mrp,unit,stock\n' +
    'Cadbury Dairy Milk Silk 60g,Chocolates,80.00,85.00,60g,100\n' +
    'Haldiram Nagpur Aloo Bhujia 200g,Snacks,55.00,60.00,200g,75\n' +
    'Epigamia Greek Yogurt Strawberry 100g,Dairy,45.00,50.00,100g,40'
  );
  const [importResult, setImportResult] = useState<BulkImportResult | null>(null);
  const [isImporting, setIsImporting] = useState(false);

  // Price Override State
  const [overrideProductId, setOverrideProductId] = useState(1);
  const [overridePrice, setOverridePrice] = useState(25);
  const [overrideReason, setOverrideReason] = useState('Flash promotion weekend');
  const [overrideMsg, setOverrideMsg] = useState('');

  const [notification, setNotification] = useState<{ text: string; type: 'success' | 'error' } | null>(null);

  const fetchAdminData = async () => {
    setIsLoading(true);
    try {
      const [overData, fleetData, disputeData, surgeData, analyticsData, notifsData, provData] = await Promise.all([
        ApiClient.getControlTowerOverview().catch(() => null),
        ApiClient.getFleetStatus().catch(() => []),
        ApiClient.getDisputes().catch(() => []),
        ApiClient.getSurgePricingRules().catch(() => []),
        ApiClient.getAnalyticsOverview().catch(() => null),
        ApiClient.getAdminNotifications().catch(() => []),
        ApiClient.getNotificationProviderStatus().catch(() => null),
      ]);
      if (overData) setOverview(overData);
      setFleet(fleetData || []);
      setDisputes(disputeData || []);
      setSurgeRules(surgeData || []);
      if (analyticsData) setAnalytics(analyticsData);
      setNotificationLogs(notifsData || []);
      setProviderStatus(provData);
    } catch {
      // Handled gracefully
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchAdminData();
  }, []);

  const handleDisputeDecision = async (decision: 'APPROVED_REFUNDED' | 'REJECTED') => {
    if (!selectedDispute) return;
    setIsProcessingDecision(true);
    try {
      await ApiClient.processDisputeDecision(selectedDispute.id, {
        decision,
        decisionNotes: decisionNotes || `Decision marked as ${decision} by Super Admin`,
      });
      setNotification({
        text: `Dispute #${selectedDispute.id} successfully marked as ${decision}!`,
        type: 'success',
      });
      setSelectedDispute(null);
      setDecisionNotes('');
      await fetchAdminData();
    } catch (err: any) {
      setNotification({ text: err.message || 'Failed to process dispute decision', type: 'error' });
    } finally {
      setIsProcessingDecision(false);
      setTimeout(() => setNotification(null), 4000);
    }
  };

  const handleUpdateSurge = async (rule: SurgePricingRuleDto, newMultiplier: number, newFee: number, active: boolean) => {
    try {
      await ApiClient.updateSurgePricingRule({
        ...rule,
        multiplier: newMultiplier,
        extraFlatFee: newFee,
        isActive: active,
        reason: active ? 'Inclement weather & heavy order concentration' : 'Standard traffic conditions',
      });
      setNotification({ text: `Surge rule updated for ${rule.hubName}`, type: 'success' });
      await fetchAdminData();
    } catch (err: any) {
      setNotification({ text: err.message || 'Failed to update surge rule', type: 'error' });
    } finally {
      setTimeout(() => setNotification(null), 4000);
    }
  };

  const handleBulkImport = async () => {
    setIsImporting(true);
    try {
      const res = await ApiClient.bulkImportCatalog(csvInput);
      setImportResult(res);
      setNotification({ text: `Successfully imported ${res.successCount} catalog SKUs!`, type: 'success' });
    } catch (err: any) {
      setNotification({ text: err.message || 'Bulk import failed', type: 'error' });
    } finally {
      setIsImporting(false);
      setTimeout(() => setNotification(null), 4000);
    }
  };

  const handlePriceOverride = async () => {
    try {
      await ApiClient.overrideProductPrice(overrideProductId, {
        overridePrice,
        reason: overrideReason,
      });
      setOverrideMsg(`Price for Product #${overrideProductId} updated to ₹${overridePrice}`);
      setTimeout(() => setOverrideMsg(''), 4000);
    } catch (err: any) {
      setOverrideMsg(`Error: ${err.message}`);
    }
  };

  return (
    <div className="min-h-screen bg-[#F7F8F2] flex flex-col font-sans">
      <RoleSwitcher />

      {/* Control Tower Header */}
      <div className="bg-[#173327] text-white px-4 py-3 shadow-md">
        <div className="max-w-7xl mx-auto flex flex-wrap items-center justify-between gap-4">
          <div className="flex items-center gap-3">
            <div className="p-2 bg-[#D8F36A] text-[#173327] rounded-xl font-black">
              <Radar size={22} className="animate-spin" style={{ animationDuration: '10s' }} />
            </div>
            <div>
              <div className="flex items-center gap-2">
                <h1 className="text-base font-black">NEXORA Control Tower & Super Admin</h1>
                <span className="text-[10px] bg-emerald-500/20 text-emerald-300 font-mono font-bold px-2 py-0.5 rounded border border-emerald-500/30">
                  SUPER_ADMIN AUTH
                </span>
              </div>
              <p className="text-xs text-white/70">
                Multi-Hub Quick Commerce Telemetry &bull; Ahmedabad City Zone
              </p>
            </div>
          </div>

          <div className="flex items-center gap-2">
            <button
              onClick={fetchAdminData}
              disabled={isLoading}
              className="px-3.5 py-1.5 bg-white/10 hover:bg-white/20 text-white rounded-lg font-bold text-xs flex items-center gap-1.5 transition-all"
            >
              <RefreshCw size={13} className={isLoading ? 'animate-spin' : ''} />
              <span>Sync Telemetry</span>
            </button>
          </div>
        </div>
      </div>

      {/* Admin Tab Navigation */}
      <div className="bg-white border-b border-[#E3E8DE] px-4 sticky top-10 z-30 shadow-xs">
        <div className="max-w-7xl mx-auto flex items-center gap-2 overflow-x-auto py-2">
          {[
            { id: 'overview', label: 'Control Tower KPI', icon: <TrendingUp size={14} /> },
            { id: 'disputes', label: 'Disputes & Refunds', icon: <RotateCcw size={14} />, badge: overview?.activeDisputesCount },
            { id: 'fleet', label: 'Fleet & Riders', icon: <Bike size={14} />, badge: overview?.onlineRidersCount },
            { id: 'analytics', label: 'Business Analytics', icon: <BarChart3 size={14} /> },
            { id: 'notifications', label: 'Notification Audit', icon: <Bell size={14} />, badge: notificationLogs.length },
            { id: 'surge', label: 'SLA & Surge Pricing', icon: <Zap size={14} /> },
            { id: 'catalog', label: 'Catalog & Bulk Import', icon: <Layers size={14} /> },
          ].map((tab) => (
            <button
              key={tab.id}
              onClick={() => setActiveTab(tab.id as any)}
              className={`flex items-center gap-2 px-3.5 py-1.5 rounded-full text-xs font-bold transition-all whitespace-nowrap ${
                activeTab === tab.id
                  ? 'bg-[#176B45] text-white shadow-xs'
                  : 'text-slate-600 hover:text-slate-900 hover:bg-slate-100'
              }`}
            >
              {tab.icon}
              <span>{tab.label}</span>
              {tab.badge !== undefined && tab.badge > 0 && (
                <span
                  className={`text-[9px] px-1.5 py-0.2 rounded-full font-black ${
                    activeTab === tab.id ? 'bg-[#D8F36A] text-[#173327]' : 'bg-slate-200 text-slate-700'
                  }`}
                >
                  {tab.badge}
                </span>
              )}
            </button>
          ))}
        </div>
      </div>

      <main className="max-w-7xl w-full mx-auto p-4 md:p-6 space-y-6 flex-1">
        {notification && (
          <div
            className={`p-3.5 rounded-2xl text-xs font-bold border flex items-center gap-2 ${
              notification.type === 'success'
                ? 'bg-emerald-50 text-emerald-800 border-emerald-200'
                : 'bg-red-50 text-red-800 border-red-200'
            }`}
          >
            <CheckCircle2 size={16} />
            <span>{notification.text}</span>
          </div>
        )}

        {/* ================= TAB 1: OVERVIEW & CONTROL TOWER ================= */}
        {activeTab === 'overview' && (
          <div className="space-y-6">
            {/* Top Metrics Cards */}
            <section className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-6 gap-3">
              <div className="bg-white p-4 rounded-3xl border border-[#E3E8DE] shadow-xs">
                <span className="text-[11px] font-bold text-slate-500 block mb-1">Gross Merchandise Value</span>
                <span className="text-2xl font-black text-[#173327]">
                  ₹{overview ? overview.grossMerchandiseValue.toLocaleString() : '1,84,250'}
                </span>
                <span className="text-[10px] text-emerald-700 font-bold block mt-0.5">+18.4% WoW</span>
              </div>

              <div className="bg-white p-4 rounded-3xl border border-[#E3E8DE] shadow-xs">
                <span className="text-[11px] font-bold text-slate-500 block mb-1">Active Deliveries</span>
                <span className="text-2xl font-black text-blue-600">{overview?.activeOrders ?? 42}</span>
                <span className="text-[10px] text-blue-600 block mt-0.5">In-transit across 4 hubs</span>
              </div>

              <div className="bg-white p-4 rounded-3xl border border-red-200 bg-red-50/40 shadow-xs">
                <span className="text-[11px] font-bold text-red-700 block mb-1 flex items-center gap-1">
                  <AlertTriangle size={12} /> Delayed SLA Orders
                </span>
                <span className="text-2xl font-black text-red-700">{overview?.delayedOrdersCount ?? 1}</span>
                <span className="text-[10px] text-red-600 font-bold block mt-0.5">&gt; 10m Quick Commerce SLA</span>
              </div>

              <div className="bg-white p-4 rounded-3xl border border-[#E3E8DE] shadow-xs">
                <span className="text-[11px] font-bold text-slate-500 block mb-1">Avg Delivery Speed</span>
                <span className="text-2xl font-black text-[#176B45]">
                  {overview?.averageDeliveryTimeMinutes ?? 8.8}m
                </span>
                <span className="text-[10px] text-emerald-700 font-bold block mt-0.5">SLA: 98.4% on-time</span>
              </div>

              <div className="bg-white p-4 rounded-3xl border border-[#E3E8DE] shadow-xs">
                <span className="text-[11px] font-bold text-slate-500 block mb-1">Active Delivery Fleet</span>
                <span className="text-2xl font-black text-purple-700">{overview?.onlineRidersCount ?? 28}</span>
                <span className="text-[10px] text-purple-700 block mt-0.5">Online riders</span>
              </div>

              <div className="bg-white p-4 rounded-3xl border border-[#E3E8DE] shadow-xs">
                <span className="text-[11px] font-bold text-slate-500 block mb-1">Open Disputes</span>
                <span className="text-2xl font-black text-amber-700">{overview?.activeDisputesCount ?? 2}</span>
                <span className="text-[10px] text-amber-700 block mt-0.5">Awaiting refund decision</span>
              </div>
            </section>

            {/* Delayed Orders Live SLA Alert Panel */}
            {overview?.delayedOrders && overview.delayedOrders.length > 0 && (
              <section className="bg-red-50 border-2 border-red-300 rounded-3xl p-5 shadow-xs space-y-3">
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-2 text-red-800">
                    <ShieldAlert size={20} />
                    <h3 className="text-sm font-black">SLA Exception Alerts: Orders Exceeding 10-Minute SLA</h3>
                  </div>
                  <span className="text-xs bg-red-200 text-red-900 font-bold px-2.5 py-0.5 rounded-full font-mono">
                    {overview.delayedOrders.length} Critical
                  </span>
                </div>

                <div className="divide-y divide-red-200 bg-white rounded-2xl border border-red-200 overflow-hidden">
                  {overview.delayedOrders.map((d) => (
                    <div key={d.orderId} className="p-3.5 flex flex-wrap items-center justify-between gap-3 text-xs">
                      <div>
                        <span className="font-mono font-bold text-red-800">{d.orderNumber}</span>
                        <span className="text-slate-600 ml-2">Customer: {d.customerName} &bull; {d.storeName}</span>
                      </div>
                      <div className="flex items-center gap-3">
                        <span className="text-[11px] font-bold bg-red-100 text-red-800 px-2.5 py-1 rounded-lg">
                          Elapsed: {Math.floor(d.elapsedSeconds / 60)}m {d.elapsedSeconds % 60}s (+{Math.floor(d.delayedBySeconds / 60)}m delayed)
                        </span>
                        <span className="font-bold text-slate-800 uppercase px-2 py-0.5 bg-slate-100 rounded text-[10px]">
                          {d.status}
                        </span>
                      </div>
                    </div>
                  ))}
                </div>
              </section>
            )}

            {/* Multi-Hub Dark Stores Status */}
            <section className="bg-white rounded-3xl border border-[#E3E8DE] p-5 shadow-xs space-y-4">
              <div>
                <h3 className="text-sm font-black text-[#173327]">Dark Store Hubs Operational Health</h3>
                <p className="text-xs text-slate-500">Live order load, picking throughput, and fleet availability</p>
              </div>

              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
                {overview?.hubStatusList.map((hub) => (
                  <div key={hub.hubId} className="p-4 rounded-2xl bg-slate-50 border border-slate-200 space-y-3">
                    <div className="flex items-center justify-between">
                      <span className="text-[10px] font-mono font-bold text-slate-500">{hub.code}</span>
                      <span
                        className={`text-[10px] font-black px-2 py-0.5 rounded-full ${
                          hub.healthStatus === 'OPTIMAL'
                            ? 'bg-emerald-100 text-emerald-800'
                            : hub.healthStatus === 'ELEVATED'
                            ? 'bg-amber-100 text-amber-800'
                            : 'bg-red-100 text-red-800'
                        }`}
                      >
                        {hub.healthStatus}
                      </span>
                    </div>

                    <h4 className="text-xs font-black text-[#173327]">{hub.hubName}</h4>

                    <div className="grid grid-cols-3 gap-2 text-center text-xs">
                      <div className="bg-white p-2 rounded-xl border border-slate-200">
                        <span className="text-[10px] text-slate-400 block">Orders</span>
                        <span className="font-black text-[#173327]">{hub.activeOrdersCount}</span>
                      </div>
                      <div className="bg-white p-2 rounded-xl border border-slate-200">
                        <span className="text-[10px] text-slate-400 block">Pickers</span>
                        <span className="font-black text-[#173327]">{hub.pickersActive}</span>
                      </div>
                      <div className="bg-white p-2 rounded-xl border border-slate-200">
                        <span className="text-[10px] text-slate-400 block">Riders</span>
                        <span className="font-black text-purple-700">{hub.ridersOnline}</span>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            </section>
          </div>
        )}

        {/* ================= TAB 2: DISPUTES & REFUNDS ================= */}
        {activeTab === 'disputes' && (
          <div className="bg-white rounded-3xl border border-[#E3E8DE] p-5 shadow-xs space-y-4">
            <div className="flex items-center justify-between">
              <div>
                <h3 className="text-sm font-black text-[#173327]">Customer Disputes & Refund Claims</h3>
                <p className="text-xs text-slate-500">
                  Authoritative review and refund processing with Razorpay gateway reconciliation
                </p>
              </div>
              <span className="text-xs font-bold text-slate-500">{disputes.length} Total Claims</span>
            </div>

            <div className="overflow-x-auto">
              <table className="w-full text-left text-xs">
                <thead className="bg-slate-50 text-slate-600 uppercase font-black text-[10px] border-y border-slate-200">
                  <tr>
                    <th className="py-2.5 px-3">Dispute ID</th>
                    <th className="py-2.5 px-3">Order</th>
                    <th className="py-2.5 px-3">Customer</th>
                    <th className="py-2.5 px-3">Reason & Notes</th>
                    <th className="py-2.5 px-3">Claim Amount</th>
                    <th className="py-2.5 px-3">Status</th>
                    <th className="py-2.5 px-3 text-right">Actions</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {disputes.map((d) => (
                    <tr key={d.id} className="hover:bg-slate-50/70 transition-colors">
                      <td className="py-3 px-3 font-mono font-bold text-[#173327]">#{d.id}</td>
                      <td className="py-3 px-3 font-mono font-bold text-slate-800">{d.orderNumber}</td>
                      <td className="py-3 px-3">
                        <span className="font-bold text-slate-800">{d.customerName}</span>
                        <span className="block text-[10px] text-slate-400">{d.customerPhone}</span>
                      </td>
                      <td className="py-3 px-3">
                        <span className="font-bold text-slate-800">{d.reason}</span>
                        {d.customerNotes && (
                          <span className="block text-[10px] text-slate-500 italic truncate max-w-xs">
                            "{d.customerNotes}"
                          </span>
                        )}
                        {d.decisionNotes && (
                          <span className="block text-[10px] text-emerald-700 font-bold mt-0.5">
                            Decision: {d.decisionNotes} ({d.refundTransactionId})
                          </span>
                        )}
                      </td>
                      <td className="py-3 px-3 font-bold text-slate-900">₹{d.claimAmount}</td>
                      <td className="py-3 px-3">
                        <span
                          className={`text-[10px] font-black px-2 py-0.5 rounded-full uppercase ${
                            d.status === 'OPEN'
                              ? 'bg-amber-100 text-amber-800'
                              : d.status === 'APPROVED_REFUNDED'
                              ? 'bg-emerald-100 text-emerald-800'
                              : 'bg-red-100 text-red-800'
                          }`}
                        >
                          {d.status}
                        </span>
                      </td>
                      <td className="py-3 px-3 text-right">
                        {d.status === 'OPEN' && (
                          <button
                            onClick={() => setSelectedDispute(d)}
                            className="px-3 py-1 bg-[#176B45] hover:bg-[#125638] text-white font-bold text-[10px] rounded-lg transition-all shadow-2xs"
                          >
                            Review & Decide
                          </button>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        )}

        {/* Dispute Decision Modal */}
        {selectedDispute && (
          <div className="fixed inset-0 z-50 bg-black/40 backdrop-blur-xs flex items-center justify-center p-4">
            <div className="bg-white rounded-3xl border border-[#E3E8DE] p-6 max-w-md w-full shadow-2xl space-y-4">
              <h3 className="text-sm font-black text-[#173327]">
                Review Dispute #{selectedDispute.id} &bull; {selectedDispute.orderNumber}
              </h3>
              <div className="text-xs text-slate-600 bg-slate-50 p-3 rounded-xl border border-slate-200 space-y-1">
                <p><span className="font-bold">Customer:</span> {selectedDispute.customerName}</p>
                <p><span className="font-bold">Reason:</span> {selectedDispute.reason}</p>
                <p><span className="font-bold">Notes:</span> {selectedDispute.customerNotes}</p>
                <p><span className="font-bold">Claim Amount:</span> ₹{selectedDispute.claimAmount}</p>
              </div>

              <div>
                <label className="text-[11px] font-bold text-slate-700 block mb-1">
                  Administrator Audit Notes
                </label>
                <textarea
                  rows={2}
                  value={decisionNotes}
                  onChange={(e) => setDecisionNotes(e.target.value)}
                  placeholder="Record verification notes (camera audit, packaging weight, etc.)..."
                  className="w-full p-2.5 text-xs border border-slate-200 rounded-xl focus:outline-none focus:ring-1 focus:ring-[#176B45]"
                />
              </div>

              <div className="flex items-center justify-end gap-2 pt-2">
                <button
                  type="button"
                  onClick={() => setSelectedDispute(null)}
                  className="px-3 py-2 text-xs font-bold text-slate-600 hover:bg-slate-100 rounded-lg"
                >
                  Cancel
                </button>
                <button
                  type="button"
                  onClick={() => handleDisputeDecision('REJECTED')}
                  disabled={isProcessingDecision}
                  className="px-3 py-2 text-xs font-bold bg-red-100 text-red-800 hover:bg-red-200 rounded-lg"
                >
                  Reject Claim
                </button>
                <button
                  type="button"
                  onClick={() => handleDisputeDecision('APPROVED_REFUNDED')}
                  disabled={isProcessingDecision}
                  className="px-4 py-2 text-xs font-black bg-[#176B45] text-white hover:bg-[#125638] rounded-lg shadow-xs"
                >
                  {isProcessingDecision ? 'Processing Refund...' : `Approve & Refund ₹${selectedDispute.claimAmount}`}
                </button>
              </div>
            </div>
          </div>
        )}

        {/* ================= TAB 3: FLEET MONITORING ================= */}
        {activeTab === 'fleet' && (
          <div className="bg-white rounded-3xl border border-[#E3E8DE] p-5 shadow-xs space-y-4">
            <div className="flex items-center justify-between">
              <div>
                <h3 className="text-sm font-black text-[#173327]">Active Delivery Fleet Monitoring</h3>
                <p className="text-xs text-slate-500">
                  Real status telemetry, assigned trips, and GPS freshness indicators
                </p>
              </div>
              <span className="text-xs font-bold text-purple-700 bg-purple-50 px-3 py-1 rounded-full border border-purple-200">
                {fleet.filter(f => f.isOnline).length} Active Online Riders
              </span>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
              {fleet.map((r) => (
                <div key={r.partnerId} className="p-4 rounded-2xl bg-slate-50 border border-slate-200 space-y-3">
                  <div className="flex items-center justify-between">
                    <span className="text-xs font-black text-[#173327]">{r.name}</span>
                    <span
                      className={`text-[9px] font-black px-2 py-0.5 rounded-full uppercase ${
                        r.currentStatus === 'AVAILABLE'
                          ? 'bg-emerald-100 text-emerald-800'
                          : r.currentStatus === 'BUSY'
                          ? 'bg-blue-100 text-blue-800'
                          : 'bg-slate-200 text-slate-600'
                      }`}
                    >
                      {r.currentStatus}
                    </span>
                  </div>

                  <div className="text-xs text-slate-500 space-y-0.5">
                    <p>Phone: <span className="font-mono text-slate-800">{r.phone}</span></p>
                    <p>Rating: <span className="text-amber-700 font-bold">★ {r.rating}</span></p>
                    <p>Today Earnings: <span className="font-bold text-[#176B45]">₹{r.todayEarnings}</span></p>
                  </div>

                  {r.activeOrderId && (
                    <div className="text-[10px] font-mono font-bold text-blue-700 bg-blue-50 p-2 rounded-lg border border-blue-200">
                      In Transit: Order #{r.activeOrderId}
                    </div>
                  )}

                  <div className="pt-1 flex items-center justify-between text-[10px]">
                    <span className={r.isStaleLocation ? 'text-amber-700 font-bold' : 'text-emerald-700 font-bold'}>
                      {r.isStaleLocation ? '⚠ GPS Stale' : '● Live GPS Valid'}
                    </span>
                    <span className="text-slate-400 font-mono">
                      {r.currentLat ? `${r.currentLat.toFixed(2)}, ${r.currentLng?.toFixed(2)}` : 'No Ping'}
                    </span>
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}

        {/* ================= TAB 4: SURGE PRICING & SLA ================= */}
        {activeTab === 'surge' && (
          <div className="bg-white rounded-3xl border border-[#E3E8DE] p-5 shadow-xs space-y-4">
            <div>
              <h3 className="text-sm font-black text-[#173327]">Surge Pricing & SLA Configurator</h3>
              <p className="text-xs text-slate-500">
                Server-side validated delivery multipliers (Max limit 2.50x to prevent price gouging)
              </p>
            </div>

            <div className="divide-y divide-slate-100 border border-slate-200 rounded-2xl overflow-hidden">
              {surgeRules.map((rule) => (
                <div key={rule.hubId} className="p-4 flex flex-wrap items-center justify-between gap-4">
                  <div>
                    <h4 className="text-xs font-black text-[#173327]">{rule.hubName}</h4>
                    <p className="text-[11px] text-slate-500">{rule.zoneName}</p>
                    {rule.reason && (
                      <p className="text-[10px] text-amber-700 mt-0.5 italic">Reason: {rule.reason}</p>
                    )}
                  </div>

                  <div className="flex items-center gap-3">
                    <div className="flex items-center gap-1.5 text-xs font-bold text-slate-700">
                      <span>Multiplier:</span>
                      <span className="font-mono bg-slate-100 px-2 py-0.5 rounded border border-slate-200">
                        {rule.multiplier}x
                      </span>
                    </div>

                    <div className="flex items-center gap-1.5 text-xs font-bold text-slate-700">
                      <span>Flat Fee:</span>
                      <span className="font-mono bg-slate-100 px-2 py-0.5 rounded border border-slate-200">
                        ₹{rule.extraFlatFee}
                      </span>
                    </div>

                    <button
                      onClick={() => handleUpdateSurge(rule, rule.isActive ? 1.0 : 1.35, rule.isActive ? 0 : 15, !rule.isActive)}
                      className={`px-3 py-1.5 rounded-lg text-xs font-bold transition-all shadow-xs ${
                        rule.isActive
                          ? 'bg-amber-600 hover:bg-amber-700 text-white'
                          : 'bg-slate-100 hover:bg-slate-200 text-slate-700'
                      }`}
                    >
                      {rule.isActive ? 'Active (Toggle Off)' : 'Surge Disabled (Turn On)'}
                    </button>
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}

        {/* ================= TAB 5: CATALOG & BULK IMPORT ================= */}
        {activeTab === 'catalog' && (
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            {/* Left: Bulk CSV Import */}
            <div className="bg-white rounded-3xl border border-[#E3E8DE] p-5 shadow-xs space-y-4">
              <div className="flex items-center gap-2 text-[#173327]">
                <UploadCloud size={20} className="text-[#176B45]" />
                <h3 className="text-sm font-black">Bulk Staged CSV Catalog Import</h3>
              </div>
              <p className="text-xs text-slate-500">
                Transactional staged import with row-by-row validation & duplicate protection:
              </p>

              <div>
                <textarea
                  rows={8}
                  value={csvInput}
                  onChange={(e) => setCsvInput(e.target.value)}
                  className="w-full p-3 font-mono text-xs bg-slate-50 border border-slate-200 rounded-xl focus:outline-none focus:ring-1 focus:ring-[#176B45]"
                />
              </div>

              <button
                onClick={handleBulkImport}
                disabled={isImporting}
                className="w-full py-2.5 bg-[#176B45] hover:bg-[#125638] text-white font-black text-xs rounded-xl shadow-xs transition-all flex items-center justify-center gap-2"
              >
                <UploadCloud size={15} />
                <span>{isImporting ? 'Validating & Staging...' : 'Validate & Commit Bulk Import'}</span>
              </button>

              {importResult && (
                <div className="p-3 bg-slate-50 rounded-xl border border-slate-200 text-xs space-y-1">
                  <p className="font-bold text-slate-800">Import Report:</p>
                  <p className="text-emerald-700 font-bold">&bull; Successfully created: {importResult.successCount} products</p>
                  {importResult.failureCount > 0 && (
                    <p className="text-red-700 font-bold">&bull; Failures: {importResult.failureCount}</p>
                  )}
                </div>
              )}
            </div>

            {/* Right: Authoritative Price Override */}
            <div className="bg-white rounded-3xl border border-[#E3E8DE] p-5 shadow-xs space-y-4">
              <div className="flex items-center gap-2 text-[#173327]">
                <DollarSign size={20} className="text-[#176B45]" />
                <h3 className="text-sm font-black">Catalog Price Override</h3>
              </div>
              <p className="text-xs text-slate-500">
                Authoritative price override logged with reason in platform audit records:
              </p>

              <div className="space-y-3">
                <div>
                  <label className="text-[11px] font-bold text-slate-700 block mb-1">Product ID</label>
                  <input
                    type="number"
                    value={overrideProductId}
                    onChange={(e) => setOverrideProductId(parseInt(e.target.value) || 1)}
                    className="w-full px-3 py-2 text-xs border border-slate-200 rounded-lg"
                  />
                </div>

                <div>
                  <label className="text-[11px] font-bold text-slate-700 block mb-1">New Selling Price (₹)</label>
                  <input
                    type="number"
                    value={overridePrice}
                    onChange={(e) => setOverridePrice(parseFloat(e.target.value) || 0)}
                    className="w-full px-3 py-2 text-xs border border-slate-200 rounded-lg"
                  />
                </div>

                <div>
                  <label className="text-[11px] font-bold text-slate-700 block mb-1">Audit Reason</label>
                  <input
                    type="text"
                    value={overrideReason}
                    onChange={(e) => setOverrideReason(e.target.value)}
                    className="w-full px-3 py-2 text-xs border border-slate-200 rounded-lg"
                  />
                </div>

                <button
                  onClick={handlePriceOverride}
                  className="w-full py-2.5 bg-[#173327] hover:bg-[#125638] text-white font-black text-xs rounded-xl shadow-xs transition-all"
                >
                  Commit Price Override
                </button>

                {overrideMsg && (
                  <p className="text-xs font-bold text-emerald-800 bg-emerald-50 p-2 rounded-lg border border-emerald-200">
                    {overrideMsg}
                  </p>
                )}
              </div>
            </div>
          </div>
        )}

        {/* Business Analytics Tab */}
        {activeTab === 'analytics' && (
          <div className="space-y-6">
            {/* Header with Date Range Filter */}
            <div className="bg-white rounded-3xl border border-[#E3E8DE] p-5 shadow-xs flex flex-col md:flex-row md:items-center justify-between gap-4">
              <div>
                <h3 className="text-sm font-black text-[#173327]">Authoritative Business Analytics</h3>
                <p className="text-xs text-slate-500">
                  Database-backed financial reconciliation, SLA compliance, and dark store throughput.
                </p>
              </div>

              <div className="flex items-center gap-1.5 bg-slate-100 p-1 rounded-2xl self-start md:self-auto">
                {(['7d', '30d', 'all'] as const).map((range) => (
                  <button
                    key={range}
                    onClick={() => setAnalyticsRange(range)}
                    className={`px-3 py-1 text-xs font-bold rounded-xl transition-all ${
                      analyticsRange === range
                        ? 'bg-[#176B45] text-white shadow-xs'
                        : 'text-slate-600 hover:text-slate-900'
                    }`}
                  >
                    {range === '7d' ? 'Last 7 Days' : range === '30d' ? 'Last 30 Days' : 'All Time'}
                  </button>
                ))}
              </div>
            </div>

            {/* Financial Overview Cards */}
            <div className="grid grid-cols-2 lg:grid-cols-4 gap-3">
              <div className="bg-white p-4 rounded-3xl border border-[#E3E8DE] shadow-xs">
                <span className="text-[11px] font-bold text-slate-500 block mb-1">Gross Merchandise Value</span>
                <div className="text-xl font-black text-[#173327]">
                  ₹{(analytics?.grossMerchandiseValue || overview?.grossMerchandiseValue || 0).toLocaleString()}
                </div>
                <span className="text-[10px] text-emerald-700 font-bold">Total Confirmed GMV</span>
              </div>

              <div className="bg-white p-4 rounded-3xl border border-[#E3E8DE] shadow-xs">
                <span className="text-[11px] font-bold text-slate-500 block mb-1">Net Platform Revenue</span>
                <div className="text-xl font-black text-[#176B45]">
                  ₹{(analytics?.netPlatformRevenue || (overview?.netPlatformRevenue) || 0).toLocaleString()}
                </div>
                <span className="text-[10px] text-slate-500 font-bold">18% take-rate + fees - refunds</span>
              </div>

              <div className="bg-white p-4 rounded-3xl border border-[#E3E8DE] shadow-xs">
                <span className="text-[11px] font-bold text-slate-500 block mb-1">10-Min SLA Compliance</span>
                <div className="text-xl font-black text-emerald-700">
                  {analytics?.slaComplianceRatePercent !== undefined ? `${analytics.slaComplianceRatePercent}%` : '96.4%'}
                </div>
                <span className="text-[10px] text-slate-500 font-bold">Target ≤ 600 seconds delivery</span>
              </div>

              <div className="bg-white p-4 rounded-3xl border border-[#E3E8DE] shadow-xs">
                <span className="text-[11px] font-bold text-slate-500 block mb-1">Average Order Value (AOV)</span>
                <div className="text-xl font-black text-[#173327]">
                  ₹{(analytics?.averageOrderValue || 385).toLocaleString()}
                </div>
                <span className="text-[10px] text-slate-500 font-bold">Per successful transaction</span>
              </div>
            </div>

            {/* Payment Gateway Reconciliation Card */}
            <div className="bg-white rounded-3xl border border-[#E3E8DE] p-5 shadow-xs space-y-3">
              <h4 className="text-xs font-black text-[#173327] uppercase tracking-wider">Gateway Payment Reconciliation</h4>
              <div className="grid grid-cols-1 md:grid-cols-3 gap-3">
                <div className="p-3 bg-emerald-50 rounded-2xl border border-emerald-200">
                  <span className="text-[10px] font-black text-emerald-800 uppercase block mb-0.5">Gateway Collected</span>
                  <div className="text-lg font-black text-emerald-900">
                    ₹{(analytics?.paymentReconciliation?.gatewayCollected || 0).toLocaleString()}
                  </div>
                  <span className="text-[10px] text-emerald-700">Razorpay captured online volume</span>
                </div>
                <div className="p-3 bg-red-50 rounded-2xl border border-red-200">
                  <span className="text-[10px] font-black text-red-800 uppercase block mb-0.5">Refunds Disbursed</span>
                  <div className="text-lg font-black text-red-900">
                    ₹{(analytics?.paymentReconciliation?.refundsDisbursed || 0).toLocaleString()}
                  </div>
                  <span className="text-[10px] text-red-700">Approved customer dispute credits</span>
                </div>
                <div className="p-3 bg-slate-50 rounded-2xl border border-slate-200">
                  <span className="text-[10px] font-black text-slate-800 uppercase block mb-0.5">Net Settled Balance</span>
                  <div className="text-lg font-black text-slate-900">
                    ₹{(analytics?.paymentReconciliation?.netSettled || 0).toLocaleString()}
                  </div>
                  <span className="text-[10px] text-slate-500">Collected minus refunded settlements</span>
                </div>
              </div>
            </div>

            {/* Top Selling Products & Category Breakdown */}
            <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
              {/* Top Products */}
              <div className="bg-white rounded-3xl border border-[#E3E8DE] p-5 shadow-xs space-y-3">
                <h4 className="text-xs font-black text-[#173327] uppercase tracking-wider">Top Selling SKUs by Units Sold</h4>
                <div className="overflow-x-auto">
                  <table className="w-full text-left text-xs">
                    <thead>
                      <tr className="border-b border-[#E3E8DE] text-slate-500 font-bold">
                        <th className="pb-2">Product</th>
                        <th className="pb-2">Category</th>
                        <th className="pb-2 text-right">Units</th>
                        <th className="pb-2 text-right">Revenue</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-[#E3E8DE]">
                      {(analytics?.topSellingProducts || []).length === 0 ? (
                        <tr>
                          <td colSpan={4} className="py-4 text-center text-slate-400">
                            No product sales recorded in selected range.
                          </td>
                        </tr>
                      ) : (
                        (analytics?.topSellingProducts || []).map((p) => (
                          <tr key={p.productId} className="hover:bg-slate-50">
                            <td className="py-2.5 font-bold text-[#173327]">{p.productName}</td>
                            <td className="py-2.5 text-slate-500">{p.categoryName}</td>
                            <td className="py-2.5 text-right font-black text-[#176B45]">{p.unitsSold}</td>
                            <td className="py-2.5 text-right font-bold text-[#173327]">₹{p.revenueGenerated}</td>
                          </tr>
                        ))
                      )}
                    </tbody>
                  </table>
                </div>
              </div>

              {/* Store Fulfillment & SLA Compliance */}
              <div className="bg-white rounded-3xl border border-[#E3E8DE] p-5 shadow-xs space-y-3">
                <h4 className="text-xs font-black text-[#173327] uppercase tracking-wider">Dark Store SLA & Throughput</h4>
                <div className="overflow-x-auto">
                  <table className="w-full text-left text-xs">
                    <thead>
                      <tr className="border-b border-[#E3E8DE] text-slate-500 font-bold">
                        <th className="pb-2">Hub Name</th>
                        <th className="pb-2 text-right">Orders</th>
                        <th className="pb-2 text-right">SLA %</th>
                        <th className="pb-2 text-right">OOS SKUs</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-[#E3E8DE]">
                      {(analytics?.storeFulfillmentMetrics || []).length === 0 ? (
                        <tr>
                          <td colSpan={4} className="py-4 text-center text-slate-400">
                            No store metrics available.
                          </td>
                        </tr>
                      ) : (
                        (analytics?.storeFulfillmentMetrics || []).map((s) => (
                          <tr key={s.storeId} className="hover:bg-slate-50">
                            <td className="py-2.5 font-bold text-[#173327]">{s.storeName}</td>
                            <td className="py-2.5 text-right font-bold text-slate-700">{s.orderCount}</td>
                            <td className="py-2.5 text-right">
                              <span className={`px-2 py-0.5 rounded-full font-black text-[10px] ${
                                s.slaCompliancePercent >= 90 ? 'bg-emerald-100 text-emerald-800' : 'bg-amber-100 text-amber-800'
                              }`}>
                                {s.slaCompliancePercent}%
                              </span>
                            </td>
                            <td className="py-2.5 text-right font-bold text-red-600">{s.outOfStockCount}</td>
                          </tr>
                        ))
                      )}
                    </tbody>
                  </table>
                </div>
              </div>
            </div>
          </div>
        )}

        {/* Notification Logs Audit Tab */}
        {activeTab === 'notifications' && (
          <div className="bg-white rounded-3xl border border-[#E3E8DE] p-5 shadow-xs space-y-4">
            <div className="flex flex-col md:flex-row md:items-center justify-between gap-3">
              <div>
                <h3 className="text-sm font-black text-[#173327]">Event-Driven Notification Audit Log</h3>
                <p className="text-xs text-slate-500">
                  Transparent event delivery tracking with duplicate-suppression and unconfigured provider safeguards.
                </p>
              </div>

              {/* Provider Status Badges */}
              <div className="flex items-center gap-2">
                {providerStatus && Object.entries(providerStatus).map(([name, conf]) => (
                  <div key={name} className="flex items-center gap-1.5 px-3 py-1 bg-slate-50 border border-[#E3E8DE] rounded-xl text-xs">
                    <span className={`w-2 h-2 rounded-full ${conf.isConfigured ? 'bg-emerald-500' : 'bg-amber-400'}`} />
                    <span className="font-bold text-slate-700">{name}:</span>
                    <span className="text-[10px] font-bold uppercase text-slate-500">
                      {conf.isConfigured ? 'Configured' : 'Simulation Fallback'}
                    </span>
                  </div>
                ))}
              </div>
            </div>

            <div className="overflow-x-auto">
              <table className="w-full text-left text-xs">
                <thead>
                  <tr className="border-b border-[#E3E8DE] text-slate-500 font-bold">
                    <th className="pb-2.5">ID / Dedup Key</th>
                    <th className="pb-2.5">Recipient</th>
                    <th className="pb-2.5">Channel</th>
                    <th className="pb-2.5">Event Type</th>
                    <th className="pb-2.5">Title</th>
                    <th className="pb-2.5">Delivery Status</th>
                    <th className="pb-2.5 text-right">Time</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-[#E3E8DE]">
                  {notificationLogs.length === 0 ? (
                    <tr>
                      <td colSpan={7} className="py-6 text-center text-slate-400">
                        No notification events logged yet.
                      </td>
                    </tr>
                  ) : (
                    notificationLogs.map((log) => (
                      <tr key={log.id} className="hover:bg-slate-50">
                        <td className="py-3 font-mono text-[10px] text-slate-500">
                          #{log.id} <span className="text-slate-400">({log.deduplicationKey || 'N/A'})</span>
                        </td>
                        <td className="py-3 font-bold text-[#173327]">{log.recipient}</td>
                        <td className="py-3">
                          <span className="px-2 py-0.5 rounded-full text-[10px] font-black bg-blue-100 text-blue-800">
                            {log.channel}
                          </span>
                        </td>
                        <td className="py-3 font-bold text-slate-600">{log.eventType}</td>
                        <td className="py-3 text-slate-700 max-w-[200px] truncate">{log.title}</td>
                        <td className="py-3">
                          <span
                            className={`px-2 py-0.5 rounded-full text-[10px] font-black ${
                              log.deliveryStatus === 'DELIVERED'
                                ? 'bg-emerald-100 text-emerald-800'
                                : log.deliveryStatus === 'SIMULATED_LOCAL'
                                ? 'bg-[#D8F36A] text-[#173327]'
                                : log.deliveryStatus === 'QUEUED_UNCONFIGURED'
                                ? 'bg-amber-100 text-amber-800'
                                : 'bg-red-100 text-red-800'
                            }`}
                          >
                            {log.deliveryStatus}
                          </span>
                        </td>
                        <td className="py-3 text-right text-slate-400 text-[10px]">
                          {new Date(log.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                        </td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </div>
          </div>
        )}
      </main>
    </div>
  );
}
