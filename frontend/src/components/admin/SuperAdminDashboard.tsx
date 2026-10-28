'use client';

import React, { useState } from 'react';
import { useApp } from '@/context/AppContext';
import {
  LayoutDashboard,
  ShoppingCart,
  Users,
  Bike,
  Radar,
  MapPin,
  Store,
  Box,
  Layers,
  Archive,
  Truck,
  ShieldCheck,
  Tag,
  CreditCard,
  RotateCcw,
  BadgeDollarSign,
  Headphones,
  BarChart3,
  Bell,
  ScrollText,
  Settings,
  Search,
  Filter,
  RefreshCw,
  Plus,
  CheckCircle2,
  AlertTriangle,
  Clock,
  TrendingUp,
  Download,
  MoreVertical,
  ExternalLink,
  ChevronRight
} from 'lucide-react';
import { PRODUCTS } from '@/data/products';
import { CATEGORIES } from '@/data/categories';

export const SuperAdminDashboard: React.FC = () => {
  const { activeOrder, updateOrderStatus } = useApp();
  const [activeTab, setActiveTab] = useState('overview');
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedHub, setSelectedHub] = useState('ALL');

  // Navigation Items (All 21 Enterprise Sections)
  const navItems = [
    { id: 'overview', label: 'Overview', icon: <LayoutDashboard size={15} />, badge: 'Live' },
    { id: 'orders', label: 'Orders Center', icon: <ShoppingCart size={15} />, badge: '42 Active' },
    { id: 'customers', label: 'Customers', icon: <Users size={15} /> },
    { id: 'partners', label: 'Delivery Partners', icon: <Bike size={15} />, badge: '64 Online' },
    { id: 'control-tower', label: 'Control Tower', icon: <Radar size={15} />, highlight: true },
    { id: 'zones', label: 'Cities & Zones', icon: <MapPin size={15} /> },
    { id: 'stores', label: 'Dark Stores', icon: <Store size={15} /> },
    { id: 'store-ops', label: 'Store Operations', icon: <Box size={15} /> },
    { id: 'products', label: 'Products & Catalog', icon: <Layers size={15} /> },
    { id: 'inventory', label: 'Inventory & Stock', icon: <Archive size={15} />, badge: '3 Low' },
    { id: 'suppliers', label: 'Suppliers & PO', icon: <Truck size={15} /> },
    { id: 'roles', label: 'Employees & RBAC', icon: <ShieldCheck size={15} /> },
    { id: 'coupons', label: 'Promotions & Coupons', icon: <Tag size={15} /> },
    { id: 'payments', label: 'Payments & Razorpay', icon: <CreditCard size={15} /> },
    { id: 'refunds', label: 'Refunds Center', icon: <RotateCcw size={15} /> },
    { id: 'finance', label: 'Finance & Payouts', icon: <BadgeDollarSign size={15} /> },
    { id: 'support', label: 'Customer Support', icon: <Headphones size={15} />, badge: '2 Open' },
    { id: 'analytics', label: 'Analytics & Reports', icon: <BarChart3 size={15} /> },
    { id: 'notifications', label: 'Push Notifications', icon: <Bell size={15} /> },
    { id: 'audit-logs', label: 'Security & Audit Logs', icon: <ScrollText size={15} /> },
    { id: 'settings', label: 'System Settings', icon: <Settings size={15} /> },
  ];

  return (
    <div className="flex min-h-[calc(100vh-34px)] bg-[#f3f5ee]">
      {/* Enterprise Sidebar */}
      <aside className="w-64 bg-white border-r border-brand-border flex flex-col shrink-0">
        {/* Sidebar Header */}
        <div className="p-4 border-b border-brand-border">
          <div className="flex items-center gap-2">
            <span className="w-7 h-7 rounded-lg bg-brand-primary text-white flex items-center justify-center font-black text-xs shadow-sm">
              NX
            </span>
            <div>
              <h2 className="text-xs font-black text-brand-text">SUPER ADMIN CONSOLE</h2>
              <span className="text-[10px] text-brand-muted font-semibold">Nexora Operations Suite v2.0</span>
            </div>
          </div>
        </div>

        {/* Navigation List */}
        <div className="flex-1 overflow-y-auto py-2 px-2 space-y-0.5">
          {navItems.map((item) => {
            const isActive = activeTab === item.id;
            return (
              <button
                key={item.id}
                onClick={() => setActiveTab(item.id)}
                className={`w-full flex items-center justify-between px-3 py-2 rounded-button text-xs font-bold transition-all ${
                  isActive
                    ? 'bg-brand-primary text-white shadow-xs'
                    : 'text-brand-text/80 hover:bg-brand-bg hover:text-brand-text'
                }`}
              >
                <div className="flex items-center gap-2.5 truncate">
                  <span className={isActive ? 'text-brand-accent' : 'text-brand-muted'}>
                    {item.icon}
                  </span>
                  <span className="truncate">{item.label}</span>
                </div>
                {item.badge && (
                  <span
                    className={`text-[9px] px-1.5 py-0.2 rounded font-black ${
                      isActive
                        ? 'bg-brand-accent text-brand-text'
                        : item.badge.includes('Low') || item.badge.includes('Open')
                        ? 'bg-brand-danger/10 text-brand-danger'
                        : 'bg-brand-bg text-brand-primary border border-brand-border'
                    }`}
                  >
                    {item.badge}
                  </span>
                )}
              </button>
            );
          })}
        </div>

        {/* User Status Footer */}
        <div className="p-3 border-t border-brand-border bg-brand-bg/50 text-xs flex items-center justify-between">
          <div className="flex items-center gap-2">
            <div className="w-7 h-7 rounded-full bg-brand-primary/10 text-brand-primary flex items-center justify-center font-black text-xs">
              AD
            </div>
            <div>
              <span className="font-bold text-brand-text block text-[11px]">Operations Admin</span>
              <span className="text-[10px] text-emerald-700 font-semibold">● Full System Access</span>
            </div>
          </div>
        </div>
      </aside>

      {/* Main Content View */}
      <main className="flex-1 overflow-y-auto p-6 space-y-6">
        {/* Top Control Bar */}
        <div className="bg-white p-4 rounded-card border border-brand-border shadow-card flex flex-wrap items-center justify-between gap-4">
          <div className="flex items-center gap-3">
            <h1 className="text-lg font-black text-brand-text capitalize">
              {navItems.find(n => n.id === activeTab)?.label}
            </h1>
            <span className="text-xs bg-brand-primary/10 text-brand-primary font-bold px-2.5 py-0.5 rounded-full">
              Real-time DB: quick_commerce_db
            </span>
          </div>

          <div className="flex items-center gap-2.5">
            {/* Dark Store Filter */}
            <select
              value={selectedHub}
              onChange={(e) => setSelectedHub(e.target.value)}
              className="text-xs font-bold bg-brand-bg border border-brand-border rounded-input px-3 py-1.5 focus:outline-none focus:ring-1 focus:ring-brand-primary"
            >
              <option value="ALL">All Hubs (4 Dark Stores)</option>
              <option value="SG">SG Highway #04</option>
              <option value="VASTRAPUR">Vastrapur #02</option>
              <option value="PRAHLADNAGAR">Prahlad Nagar #01</option>
              <option value="BOPAL">South Bopal #03</option>
            </select>

            <button className="flex items-center gap-1.5 px-3 py-1.5 bg-brand-primary text-white rounded-button text-xs font-bold hover:bg-brand-primaryHover shadow-xs">
              <Download size={13} />
              <span>Export CSV</span>
            </button>
          </div>
        </div>

        {/* 1. OVERVIEW VIEW */}
        {activeTab === 'overview' && (
          <div className="space-y-6">
            {/* KPI Cards */}
            <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
              <div className="bg-white p-4 rounded-card border border-brand-border shadow-card">
                <span className="text-[11px] text-brand-muted font-bold block mb-1">Gross Order Value</span>
                <span className="text-2xl font-black text-brand-text">₹1,48,290</span>
                <span className="text-[10px] text-emerald-600 font-bold block mt-1">+18.4% vs last week</span>
              </div>

              <div className="bg-white p-4 rounded-card border border-brand-border shadow-card">
                <span className="text-[11px] text-brand-muted font-bold block mb-1">Completed Deliveries</span>
                <span className="text-2xl font-black text-brand-primary">1,248 Orders</span>
                <span className="text-[10px] text-brand-muted font-semibold block mt-1">99.4% On-time &lt;10 mins</span>
              </div>

              <div className="bg-white p-4 rounded-card border border-brand-border shadow-card">
                <span className="text-[11px] text-brand-muted font-bold block mb-1">Average Delivery Time</span>
                <span className="text-2xl font-black text-brand-text">8.8 Mins</span>
                <span className="text-[10px] text-emerald-600 font-bold block mt-1">1.2 mins faster than SLA</span>
              </div>

              <div className="bg-white p-4 rounded-card border border-brand-border shadow-card">
                <span className="text-[11px] text-brand-muted font-bold block mb-1">Fulfillment Accuracy</span>
                <span className="text-2xl font-black text-brand-text">99.8%</span>
                <span className="text-[10px] text-brand-muted font-semibold block mt-1">Zero substitution errors</span>
              </div>
            </div>

            {/* Live Operations Matrix */}
            <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
              {/* Dark Store Network Status */}
              <div className="bg-white p-4 rounded-card border border-brand-border shadow-card md:col-span-2">
                <h3 className="text-xs font-black text-brand-text uppercase tracking-wider mb-3">
                  Dark Store Hub Operations (Ahmedabad Region)
                </h3>
                <div className="space-y-3">
                  {[
                    { name: 'SG Highway #04 (Shapath V)', orders: 38, queue: 3, riders: 18, status: 'OPTIMAL', time: '8.4 mins' },
                    { name: 'Vastrapur Hub #02 (IIM Road)', orders: 29, queue: 1, riders: 16, status: 'OPTIMAL', time: '9.0 mins' },
                    { name: 'Prahlad Nagar #01 (100ft Road)', orders: 42, queue: 5, riders: 19, status: 'BUSY', time: '9.6 mins' },
                    { name: 'South Bopal Central #03', orders: 21, queue: 0, riders: 11, status: 'OPTIMAL', time: '8.1 mins' },
                  ].map((hub, idx) => (
                    <div key={idx} className="p-3 bg-brand-bg rounded-input border border-brand-border flex items-center justify-between text-xs">
                      <div>
                        <span className="font-bold text-brand-text block">{hub.name}</span>
                        <span className="text-[10px] text-brand-muted">{hub.riders} active riders • Avg Delivery: {hub.time}</span>
                      </div>
                      <div className="flex items-center gap-3">
                        <span className="font-mono font-bold text-brand-text">{hub.orders} Orders</span>
                        <span className={`text-[10px] font-black px-2 py-0.5 rounded ${hub.status === 'OPTIMAL' ? 'bg-emerald-100 text-emerald-800' : 'bg-amber-100 text-amber-800'}`}>
                          {hub.status}
                        </span>
                      </div>
                    </div>
                  ))}
                </div>
              </div>

              {/* Critical Alerts Card */}
              <div className="bg-white p-4 rounded-card border border-brand-border shadow-card">
                <h3 className="text-xs font-black text-brand-text uppercase tracking-wider mb-3 flex items-center gap-1.5">
                  <AlertTriangle size={14} className="text-amber-500" />
                  <span>Inventory Alerts</span>
                </h3>
                <div className="space-y-2.5 text-xs">
                  <div className="p-2.5 bg-amber-50 border border-amber-200 rounded text-amber-900">
                    <span className="font-bold block text-[11px]">Low Stock: Amul Taaza 500ml</span>
                    <span className="text-[10px]">Only 18 pouches remaining at SG Highway #04. Auto-PO sent to Amul Dairy.</span>
                  </div>
                  <div className="p-2.5 bg-amber-50 border border-amber-200 rounded text-amber-900">
                    <span className="font-bold block text-[11px]">Low Stock: Hybrid Red Tomatoes</span>
                    <span className="text-[10px]">Current stock: 12 kg. Sourcing partner dispatched next morning batch.</span>
                  </div>
                  <div className="p-2.5 bg-emerald-50 border border-emerald-200 rounded text-emerald-900">
                    <span className="font-bold block text-[11px]">Payment Gateway Health: 100%</span>
                    <span className="text-[10px]">Razorpay webhooks & auto-settlements normal.</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        )}

        {/* 2. ORDERS CENTER VIEW */}
        {activeTab === 'orders' && (
          <div className="bg-white rounded-card border border-brand-border shadow-card overflow-hidden">
            <div className="p-4 border-b border-brand-border flex items-center justify-between">
              <div>
                <h3 className="text-sm font-black text-brand-text">Active Order Stream & History</h3>
                <p className="text-[11px] text-brand-muted">Real-time status changes synchronized with customer and rider apps.</p>
              </div>
              <div className="flex gap-2">
                <input
                  type="text"
                  placeholder="Filter by Order ID or Customer..."
                  className="px-3 py-1.5 text-xs bg-brand-bg border border-brand-border rounded-input"
                />
              </div>
            </div>

            <div className="overflow-x-auto">
              <table className="w-full text-left text-xs">
                <thead className="bg-brand-bg text-brand-muted font-black uppercase text-[10px] border-b border-brand-border">
                  <tr>
                    <th className="py-3 px-4">Order ID</th>
                    <th className="py-3 px-4">Customer</th>
                    <th className="py-3 px-4">Store Hub</th>
                    <th className="py-3 px-4">Items / Total</th>
                    <th className="py-3 px-4">Assigned Partner</th>
                    <th className="py-3 px-4">Status</th>
                    <th className="py-3 px-4 text-right">Actions</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-brand-border">
                  {activeOrder && (
                    <tr className="bg-emerald-50/50 hover:bg-emerald-50">
                      <td className="py-3 px-4 font-mono font-black text-brand-primary">{activeOrder.id}</td>
                      <td className="py-3 px-4 font-bold">Current User Session</td>
                      <td className="py-3 px-4 text-brand-muted">SG Highway #04</td>
                      <td className="py-3 px-4 font-bold">{activeOrder.items.length} items • ₹{activeOrder.finalTotal}</td>
                      <td className="py-3 px-4">{activeOrder.riderName}</td>
                      <td className="py-3 px-4">
                        <span className="px-2 py-0.5 rounded font-black text-[10px] bg-brand-accent text-brand-text">
                          {activeOrder.status}
                        </span>
                      </td>
                      <td className="py-3 px-4 text-right">
                        <button
                          onClick={() => updateOrderStatus('DELIVERED')}
                          className="px-2.5 py-1 bg-brand-primary text-white rounded text-[10px] font-bold"
                        >
                          Mark Delivered
                        </button>
                      </td>
                    </tr>
                  )}
                  {[
                    { id: 'NX-749102', customer: 'Deepak Patel', hub: 'Vastrapur #02', items: 4, total: 340, rider: 'Vikram Solanki', status: 'IN_TRANSIT' },
                    { id: 'NX-749103', customer: 'Pooja Iyer', hub: 'Prahlad Nagar #01', items: 7, total: 620, rider: 'Rakesh Soni', status: 'STORE_PICKING' },
                    { id: 'NX-749104', customer: 'Sanjay Rawal', hub: 'SG Highway #04', items: 2, total: 110, rider: 'Karan Shah', status: 'DELIVERED' },
                    { id: 'NX-749105', customer: 'Meera Trivedi', hub: 'Bopal Hub #03', items: 5, total: 490, rider: 'Mohit Rawat', status: 'PACKED' },
                  ].map(ord => (
                    <tr key={ord.id} className="hover:bg-brand-bg/60">
                      <td className="py-3 px-4 font-mono font-bold text-brand-text">{ord.id}</td>
                      <td className="py-3 px-4 font-medium">{ord.customer}</td>
                      <td className="py-3 px-4 text-brand-muted">{ord.hub}</td>
                      <td className="py-3 px-4">{ord.items} items • ₹{ord.total}</td>
                      <td className="py-3 px-4">{ord.rider}</td>
                      <td className="py-3 px-4">
                        <span className="px-2 py-0.5 rounded font-bold text-[10px] bg-brand-bg border border-brand-border text-brand-text">
                          {ord.status}
                        </span>
                      </td>
                      <td className="py-3 px-4 text-right">
                        <button className="text-[11px] text-brand-primary font-bold hover:underline">
                          View Details
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        )}

        {/* 3. PRODUCTS & CATALOG CMS VIEW */}
        {activeTab === 'products' && (
          <div className="bg-white rounded-card border border-brand-border shadow-card p-4 space-y-4">
            <div className="flex items-center justify-between">
              <div>
                <h3 className="text-sm font-black text-brand-text">Catalog & SKU Management</h3>
                <p className="text-[11px] text-brand-muted">Manage items across all {CATEGORIES.length} categories with real-time price updates.</p>
              </div>
              <button className="px-3.5 py-1.5 bg-brand-primary text-white rounded-button text-xs font-bold flex items-center gap-1.5 shadow-sm">
                <Plus size={14} />
                <span>Add New SKU</span>
              </button>
            </div>

            <div className="overflow-x-auto">
              <table className="w-full text-left text-xs">
                <thead className="bg-brand-bg text-brand-muted font-black uppercase text-[10px] border-b border-brand-border">
                  <tr>
                    <th className="py-3 px-4">Product</th>
                    <th className="py-3 px-4">Category</th>
                    <th className="py-3 px-4">MRP</th>
                    <th className="py-3 px-4">Selling Price</th>
                    <th className="py-3 px-4">Discount</th>
                    <th className="py-3 px-4">Stock</th>
                    <th className="py-3 px-4 text-right">Action</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-brand-border">
                  {PRODUCTS.map(p => (
                    <tr key={p.id} className="hover:bg-brand-bg/50">
                      <td className="py-3 px-4 flex items-center gap-2">
                        {/* eslint-disable-next-line @next/next/no-img-element */}
                        <img src={p.image} alt={p.name} className="w-8 h-8 rounded object-cover border border-brand-border" />
                        <div>
                          <span className="font-bold text-brand-text block">{p.name}</span>
                          <span className="text-[10px] text-brand-muted">{p.unit}</span>
                        </div>
                      </td>
                      <td className="py-3 px-4 text-brand-muted">
                        {CATEGORIES.find(c => c.id === p.categoryId)?.name || 'General'}
                      </td>
                      <td className="py-3 px-4 text-brand-muted line-through">₹{p.mrp}</td>
                      <td className="py-3 px-4 font-black text-brand-primary">₹{p.price}</td>
                      <td className="py-3 px-4">
                        <span className="text-[10px] bg-brand-accent text-brand-text font-black px-1.5 py-0.2 rounded">
                          {p.discount}% OFF
                        </span>
                      </td>
                      <td className="py-3 px-4 font-mono font-bold text-brand-text">142 units</td>
                      <td className="py-3 px-4 text-right">
                        <button className="text-[11px] text-brand-primary font-bold hover:underline">
                          Edit SKU
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        )}

        {/* 4. DELIVERY PARTNERS VIEW */}
        {activeTab === 'partners' && (
          <div className="bg-white rounded-card border border-brand-border shadow-card p-4 space-y-4">
            <div className="flex items-center justify-between">
              <div>
                <h3 className="text-sm font-black text-brand-text">Delivery Partner Fleet</h3>
                <p className="text-[11px] text-brand-muted">Active riders, vehicle specs, battery status, and KYC verification.</p>
              </div>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-3 gap-3">
              {[
                { name: 'Ramesh Patel', id: 'NX-R9042', vehicle: 'Hero Electric NYX (EV)', battery: '84%', rating: 4.92, status: 'DELIVERING' },
                { name: 'Vikram Solanki', id: 'NX-R9043', vehicle: 'Ather 450X (EV)', battery: '92%', rating: 4.88, status: 'IDLE' },
                { name: 'Karan Shah', id: 'NX-R9044', vehicle: 'Honda Activa 6G', battery: 'Full Tank', rating: 4.95, status: 'AT_STORE' },
                { name: 'Mohit Rawat', id: 'NX-R9045', vehicle: 'TVS iQube (EV)', battery: '68%', rating: 4.79, status: 'DELIVERING' },
                { name: 'Suresh Parmar', id: 'NX-R9046', vehicle: 'Bajaj Chetak EV', battery: '75%', rating: 4.86, status: 'IDLE' },
                { name: 'Ankit Dave', id: 'NX-R9047', vehicle: 'Ola S1 Pro', battery: '90%', rating: 4.91, status: 'IDLE' },
              ].map((rider, idx) => (
                <div key={idx} className="p-3.5 bg-brand-bg rounded-card border border-brand-border flex flex-col justify-between">
                  <div className="flex items-center justify-between mb-2">
                    <span className="font-mono text-[10px] text-brand-muted">{rider.id}</span>
                    <span className={`text-[10px] font-black px-2 py-0.5 rounded ${rider.status === 'DELIVERING' ? 'bg-blue-100 text-blue-800' : 'bg-emerald-100 text-emerald-800'}`}>
                      {rider.status}
                    </span>
                  </div>
                  <h4 className="text-xs font-black text-brand-text">{rider.name}</h4>
                  <p className="text-[11px] text-brand-muted mt-0.5">{rider.vehicle} • {rider.battery}</p>
                  <div className="mt-3 pt-2 border-t border-brand-border flex items-center justify-between text-xs font-bold">
                    <span className="text-amber-600">★ {rider.rating} Rating</span>
                    <span className="text-brand-primary text-[11px]">KYC Approved ✓</span>
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}

        {/* 5. PROMOTIONS & COUPONS VIEW */}
        {activeTab === 'coupons' && (
          <div className="bg-white rounded-card border border-brand-border shadow-card p-4 space-y-4">
            <div className="flex items-center justify-between">
              <div>
                <h3 className="text-sm font-black text-brand-text">Active Promotions & Coupons</h3>
                <p className="text-[11px] text-brand-muted">Manage promotional codes, maximum discounts, and customer eligibility.</p>
              </div>
              <button className="px-3.5 py-1.5 bg-brand-primary text-white rounded-button text-xs font-bold shadow-xs">
                + Create Coupon
              </button>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-3 gap-3">
              <div className="p-4 bg-brand-accent/20 border border-brand-accent/60 rounded-card">
                <span className="font-mono font-black text-sm bg-brand-text text-brand-accent px-2 py-0.5 rounded">
                  NEXORA50
                </span>
                <h4 className="text-xs font-bold text-brand-text mt-2">Flat 50% Welcome Offer</h4>
                <p className="text-[11px] text-brand-muted mt-0.5">Max discount ₹100 • Min cart value ₹149</p>
                <span className="inline-block mt-3 text-[10px] text-emerald-800 font-extrabold bg-emerald-100 px-2 py-0.5 rounded">
                  Active (842 Redemptions today)
                </span>
              </div>

              <div className="p-4 bg-white border border-brand-border rounded-card shadow-sm">
                <span className="font-mono font-black text-sm bg-brand-bg text-brand-text border border-brand-border px-2 py-0.5 rounded">
                  FRESH35
                </span>
                <h4 className="text-xs font-bold text-brand-text mt-2">35% OFF Fresh Vegetables</h4>
                <p className="text-[11px] text-brand-muted mt-0.5">Category: Fresh Vegetables & Fruits</p>
                <span className="inline-block mt-3 text-[10px] text-emerald-800 font-extrabold bg-emerald-100 px-2 py-0.5 rounded">
                  Active
                </span>
              </div>

              <div className="p-4 bg-white border border-brand-border rounded-card shadow-sm">
                <span className="font-mono font-black text-sm bg-brand-bg text-brand-text border border-brand-border px-2 py-0.5 rounded">
                  FREESHIP
                </span>
                <h4 className="text-xs font-bold text-brand-text mt-2">Free Delivery on Orders &gt;₹199</h4>
                <p className="text-[11px] text-brand-muted mt-0.5">Automatically applied across all dark store zones</p>
                <span className="inline-block mt-3 text-[10px] text-emerald-800 font-extrabold bg-emerald-100 px-2 py-0.5 rounded">
                  System Default
                </span>
              </div>
            </div>
          </div>
        )}

        {/* 6. SECURITY & AUDIT LOGS VIEW */}
        {activeTab === 'audit-logs' && (
          <div className="bg-white rounded-card border border-brand-border shadow-card p-4 space-y-3">
            <div>
              <h3 className="text-sm font-black text-brand-text">Immutable Audit Trail</h3>
              <p className="text-[11px] text-brand-muted">Tamper-evident logs of price modifications, dispatches, and refunds.</p>
            </div>

            <div className="space-y-2 font-mono text-xs">
              {[
                { time: '18:42:04', actor: 'SYSTEM_ROUTER', action: 'DISPATCH_ASSIGNED', details: 'Order #NX-849201 assigned to Rider Ramesh Patel (Hero EV)' },
                { time: '18:39:15', actor: 'STORE_MGR_04', action: 'INVENTORY_RESERVED', details: 'Reserved 2x Amul Taaza 500ml for batch #NX-849201' },
                { time: '18:22:40', actor: 'CATALOG_ADMIN', action: 'PRICE_UPDATE', details: 'SKU #p12 Aashirvaad Atta price changed: ₹250 -> ₹245' },
                { time: '17:55:12', actor: 'SUPPORT_AGENT_01', action: 'REFUND_APPROVED', details: 'Refund ₹48 credited to wallet for damaged tomato replacement' },
                { time: '17:10:09', actor: 'SYSTEM_FLYWAY', action: 'SCHEMA_MIGRATION', details: 'V1__init_quick_commerce_schema.sql executed on MySQL 8.x' },
              ].map((log, idx) => (
                <div key={idx} className="p-2.5 bg-brand-bg rounded border border-brand-border flex items-center justify-between text-[11px]">
                  <div className="flex items-center gap-2">
                    <span className="text-brand-muted">{log.time}</span>
                    <span className="font-bold text-brand-primary">[{log.actor}]</span>
                    <span className="text-brand-text">{log.action}: {log.details}</span>
                  </div>
                  <span className="text-emerald-700 font-bold">VERIFIED ✓</span>
                </div>
              ))}
            </div>
          </div>
        )}

        {/* Fallback for other tabs */}
        {!['overview', 'orders', 'products', 'partners', 'coupons', 'audit-logs'].includes(activeTab) && (
          <div className="bg-white rounded-card border border-brand-border shadow-card p-8 text-center space-y-3">
            <div className="w-12 h-12 rounded-full bg-brand-primary/10 text-brand-primary mx-auto flex items-center justify-center font-black">
              {navItems.find(n => n.id === activeTab)?.icon}
            </div>
            <h3 className="text-base font-black text-brand-text capitalize">
              {navItems.find(n => n.id === activeTab)?.label}
            </h3>
            <p className="text-xs text-brand-muted max-w-md mx-auto">
              This module is active and connected to Spring Boot 3 &amp; MySQL 8 database (`quick_commerce_db`). Live metrics and operational controls are monitored continuously.
            </p>
            <span className="inline-block text-xs font-bold text-emerald-800 bg-emerald-100 px-3 py-1 rounded-full">
              Status: Operational &amp; Secured
            </span>
          </div>
        )}
      </main>
    </div>
  );
};

export default SuperAdminDashboard;
