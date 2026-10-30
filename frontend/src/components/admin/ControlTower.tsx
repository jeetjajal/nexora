'use client';

import React, { useState } from 'react';
import { useApp } from '@/context/AppContext';
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
  Sliders
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
  const { activeOrder, updateOrderStatus } = useApp();
  const [filterStatus, setFilterStatus] = useState<string>('ALL');

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

  return (
    <div className="max-w-7xl mx-auto py-6 px-4 space-y-6">
      {/* Title & Top Bar */}
      <div className="flex flex-wrap items-center justify-between gap-4 bg-white p-4 rounded-card border border-brand-border shadow-card">
        <div>
          <div className="flex items-center gap-2">
            <h2 className="text-xl font-black text-brand-text">Operations Control Tower</h2>
            <span className="text-[10px] bg-brand-primary text-white font-extrabold px-2 py-0.5 rounded-full">
              LIVE DISPATCH RADAR
            </span>
          </div>
          <p className="text-xs text-brand-muted mt-0.5">
            Real-time monitoring across 4 active dark stores, 64 active riders, and instant orders.
          </p>
        </div>

        <div className="flex items-center gap-2">
          <button className="flex items-center gap-1.5 px-3 py-1.5 bg-brand-bg hover:bg-brand-border text-brand-text text-xs font-bold rounded-button transition-colors">
            <RefreshCw size={13} />
            <span>Auto-Refresh: 5s</span>
          </button>
          <span className="inline-flex items-center gap-1 text-xs text-emerald-700 bg-emerald-100 font-bold px-3 py-1.5 rounded-full">
            <span className="w-2 h-2 rounded-full bg-emerald-500 animate-ping" />
            Socket Connected
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
          <span className="text-xl font-black text-brand-primary">9.1 Mins</span>
          <span className="text-[10px] text-brand-muted font-bold block mt-0.5">SLA Target: &lt;10 mins</span>
        </div>

        <div className="bg-white p-4 rounded-card border border-brand-border shadow-card">
          <div className="flex items-center justify-between text-brand-muted mb-1">
            <span className="text-[11px] font-bold">Active Deliveries</span>
            <Bike size={14} className="text-brand-primary" />
          </div>
          <span className="text-xl font-black text-brand-text">64 Riders</span>
          <span className="text-[10px] text-emerald-600 font-bold block mt-0.5">88% Fleet Utilization</span>
        </div>

        <div className="bg-white p-4 rounded-card border border-brand-border shadow-card">
          <div className="flex items-center justify-between text-brand-muted mb-1">
            <span className="text-[11px] font-bold">Store Fulfillment</span>
            <Building2 size={14} className="text-brand-primary" />
          </div>
          <span className="text-xl font-black text-brand-text">99.6%</span>
          <span className="text-[10px] text-brand-muted font-bold block mt-0.5">Picking SLA: 2.1 mins</span>
        </div>

        <div className="bg-white p-4 rounded-card border border-brand-border shadow-card sm:col-span-2 lg:col-span-1">
          <div className="flex items-center justify-between text-brand-muted mb-1">
            <span className="text-[11px] font-bold">Stock Alerts</span>
            <AlertTriangle size={14} className="text-amber-500" />
          </div>
          <span className="text-xl font-black text-amber-600">3 SKUs Low</span>
          <span className="text-[10px] text-brand-muted font-bold block mt-0.5">Auto-PO generated</span>
        </div>
      </div>

      {/* Real-time Order Stream Table */}
      <div className="bg-white rounded-card border border-brand-border shadow-card overflow-hidden">
        {/* Filter bar */}
        <div className="p-4 border-b border-brand-border flex flex-wrap items-center justify-between gap-3">
          <div className="flex items-center gap-2">
            <h3 className="text-sm font-black text-brand-text">Live Dispatch Feed</h3>
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
    </div>
  );
};

export default ControlTower;
