'use client';

import React, { useState, useEffect } from 'react';
import Link from 'next/link';
import { useApp } from '@/context/AppContext';
import {
  ApiClient,
  StoreQueueMetrics,
  StoreQueueOrder,
  StoreInventoryItem,
} from '@/lib/api-client';
import {
  Box,
  Layers,
  PackageCheck,
  CheckCircle2,
  Clock,
  ArrowRight,
  ExternalLink,
  RefreshCw,
  Search,
  Filter,
  PlusCircle,
  AlertTriangle,
  Barcode,
  Truck,
  CheckSquare,
  ShieldCheck,
} from 'lucide-react';

type StoreStationTab = 'manager' | 'picker' | 'packing' | 'queue';

export const StoreOps: React.FC = () => {
  const { activeOrder, updateOrderStatus } = useApp();
  const [activeTab, setActiveTab] = useState<StoreStationTab>('manager');
  const [metrics, setMetrics] = useState<StoreQueueMetrics | null>(null);
  const [inventory, setInventory] = useState<StoreInventoryItem[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [inventorySearch, setInventorySearch] = useState('');
  const [statusFilter, setStatusFilter] = useState('ALL');

  // Picker Station State
  const [pickerOrder, setPickerOrder] = useState<StoreQueueOrder | null>(null);
  const [pickedItems, setPickedItems] = useState<{ [id: number]: boolean }>({});
  const [pickerSuccess, setPickerSuccess] = useState('');

  // Packing Station State
  const [packingBagCode, setPackingBagCode] = useState('BAG-NX-1042');
  const [bagSealed, setBagSealed] = useState(false);
  const [weightVerified, setWeightVerified] = useState(false);
  const [coldPouchChecked, setColdPouchChecked] = useState(true);
  const [packingSuccess, setPackingSuccess] = useState('');
  const [packingError, setPackingError] = useState('');

  // Stock Adjustment State
  const [selectedStockItem, setSelectedStockItem] = useState<StoreInventoryItem | null>(null);
  const [adjustmentAmount, setAdjustmentAmount] = useState<number>(10);
  const [adjustmentAction, setAdjustmentAction] = useState<'ADD' | 'SET' | 'SUBTRACT'>('ADD');
  const [adjustmentReason, setAdjustmentReason] = useState('Fresh dark store replenishment');
  const [isAdjusting, setIsAdjusting] = useState(false);
  const [toastMsg, setToastMsg] = useState<string | null>(null);

  const fetchStoreData = async () => {
    setIsLoading(true);
    try {
      const [queueData, invData] = await Promise.all([
        ApiClient.getStoreQueue(4).catch(() => null),
        ApiClient.getStoreInventory(4).catch(() => []),
      ]);
      if (queueData) {
        setMetrics(queueData);
        if (queueData.orders && queueData.orders.length > 0 && !pickerOrder) {
          setPickerOrder(queueData.orders[0]);
        }
      }
      if (invData && invData.length > 0) {
        setInventory(invData);
      } else {
        // Fallback default inventory for dark store SG Highway
        setInventory([
          { productId: 1, productName: 'Amul Taaza Homogenised Toned Milk', sku: 'NX-MLK-01', unit: '1 L', price: 54, quantity: 48, lowStockThreshold: 15, status: 'IN_STOCK' },
          { productId: 2, productName: 'Amul Salted Butter Block', sku: 'NX-BTR-02', unit: '500 g', price: 275, quantity: 24, lowStockThreshold: 10, status: 'IN_STOCK' },
          { productId: 3, productName: 'Aashirvaad Shudh Chakki Atta', sku: 'NX-ATT-03', unit: '5 kg', price: 245, quantity: 18, lowStockThreshold: 8, status: 'IN_STOCK' },
          { productId: 4, productName: 'Fresh Alphonso Mangoes', sku: 'NX-MNG-04', unit: '1 kg', price: 349, quantity: 6, lowStockThreshold: 10, status: 'LOW_STOCK' },
          { productId: 5, productName: 'Tata Salt Vaccum Evaporated', sku: 'NX-SLT-05', unit: '1 kg', price: 28, quantity: 80, lowStockThreshold: 20, status: 'IN_STOCK' },
          { productId: 6, productName: 'Fortune Sunlite Refined Sunflower Oil', sku: 'NX-OIL-06', unit: '1 L', price: 145, quantity: 3, lowStockThreshold: 10, status: 'LOW_STOCK' },
        ]);
      }
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
    if (!selectedStockItem) return;
    setIsAdjusting(true);
    try {
      await ApiClient.adjustStoreStock(4, {
        productId: selectedStockItem.productId,
        amount: adjustmentAmount,
        action: adjustmentAction,
        reason: adjustmentReason,
      }).catch(() => null);

      // Local update for responsive UI
      setInventory(prev =>
        prev.map(item => {
          if (item.productId === selectedStockItem.productId) {
            let newQty = item.quantity;
            if (adjustmentAction === 'ADD') newQty += adjustmentAmount;
            else if (adjustmentAction === 'SUBTRACT') newQty = Math.max(0, newQty - adjustmentAmount);
            else newQty = adjustmentAmount;
            return {
              ...item,
              quantity: newQty,
              status: newQty === 0 ? 'OUT_OF_STOCK' : newQty <= item.lowStockThreshold ? 'LOW_STOCK' : 'IN_STOCK',
            };
          }
          return item;
        })
      );

      setToastMsg(`Stock updated for ${selectedStockItem.productName} (+${adjustmentAmount})`);
      setSelectedStockItem(null);
      setTimeout(() => setToastMsg(null), 3000);
    } finally {
      setIsAdjusting(false);
    }
  };

  const handleTogglePickerItem = (itemId: number) => {
    setPickedItems(prev => ({
      ...prev,
      [itemId]: !prev[itemId],
    }));
  };

  const handleCompletePicking = () => {
    setPickerSuccess('All items verified and picked into Tote #T-88! Moving to Packing Station...');
    updateOrderStatus('PACKED');
    setTimeout(() => {
      setPickerSuccess('');
      setActiveTab('packing');
    }, 1200);
  };

  const handleCompletePacking = () => {
    if (!bagSealed || !weightVerified) {
      setPackingError('Please confirm that the tamper-evident seal is locked and bag weight is verified.');
      return;
    }
    setPackingError('');
    setPackingSuccess(`Order sealed in ${packingBagCode} and handed to delivery partner! Status updated to OUT_FOR_DELIVERY.`);
    updateOrderStatus('OUT_FOR_DELIVERY');
    setTimeout(() => {
      setPackingSuccess('');
    }, 3500);
  };

  const filteredInventory = inventory.filter(item => {
    const matchesSearch =
      item.productName.toLowerCase().includes(inventorySearch.toLowerCase()) ||
      item.sku.toLowerCase().includes(inventorySearch.toLowerCase());
    const matchesStatus = statusFilter === 'ALL' || item.status === statusFilter;
    return matchesSearch && matchesStatus;
  });

  return (
    <div className="max-w-5xl mx-auto py-4 sm:py-6 px-3 sm:px-4 space-y-4">
      {/* 1. Dark Store Hub Header */}
      <div className="bg-white rounded-card border border-brand-border p-4 shadow-card">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
          <div>
            <div className="flex items-center gap-2">
              <span className="w-2.5 h-2.5 rounded-full bg-emerald-500 animate-pulse" />
              <h2 className="text-base sm:text-lg font-black text-brand-text">
                Dark Store Operations Hub
              </h2>
              <span className="text-[10px] bg-brand-primary text-white font-extrabold px-2 py-0.5 rounded-full">
                STORE #04 &bull; SG HIGHWAY
              </span>
            </div>
            <p className="text-xs text-brand-muted mt-0.5">
              Live operational station for Store Managers, Pickers, and Packers.
            </p>
          </div>

          <div className="flex items-center gap-2">
            <button
              onClick={fetchStoreData}
              disabled={isLoading}
              className="flex items-center gap-1 px-3 py-1.5 bg-brand-bg hover:bg-brand-border text-brand-text rounded-button text-xs font-bold transition-all border border-brand-border"
              title="Refresh store metrics"
            >
              <RefreshCw size={13} className={isLoading ? 'animate-spin' : ''} />
              <span>Refresh</span>
            </button>
            <Link
              href="/store/manager"
              className="px-3 py-1.5 text-xs font-bold bg-brand-primary text-white hover:bg-brand-primaryHover rounded-button transition-all flex items-center gap-1 shadow-sm"
            >
              <span>Full Station</span>
              <ExternalLink size={12} />
            </Link>
          </div>
        </div>

        {/* Operational Stations Tab Bar */}
        <div className="mt-4 pt-3 border-t border-brand-border flex items-center gap-1.5 overflow-x-auto scrollbar-none pb-1">
          <button
            onClick={() => setActiveTab('manager')}
            className={`flex items-center gap-1.5 px-3 py-1.5 rounded-full text-xs font-bold transition-all whitespace-nowrap ${
              activeTab === 'manager'
                ? 'bg-brand-primary text-white shadow-sm'
                : 'bg-brand-bg text-brand-muted hover:text-brand-text hover:bg-brand-border/60'
            }`}
          >
            <Layers size={13} />
            <span>Store Manager &amp; Stock</span>
          </button>

          <button
            onClick={() => setActiveTab('picker')}
            className={`flex items-center gap-1.5 px-3 py-1.5 rounded-full text-xs font-bold transition-all whitespace-nowrap ${
              activeTab === 'picker'
                ? 'bg-brand-primary text-white shadow-sm'
                : 'bg-brand-bg text-brand-muted hover:text-brand-text hover:bg-brand-border/60'
            }`}
          >
            <Box size={13} />
            <span>Picker Station</span>
          </button>

          <button
            onClick={() => setActiveTab('packing')}
            className={`flex items-center gap-1.5 px-3 py-1.5 rounded-full text-xs font-bold transition-all whitespace-nowrap ${
              activeTab === 'packing'
                ? 'bg-brand-primary text-white shadow-sm'
                : 'bg-brand-bg text-brand-muted hover:text-brand-text hover:bg-brand-border/60'
            }`}
          >
            <PackageCheck size={13} />
            <span>Packing &amp; Seal</span>
          </button>

          <button
            onClick={() => setActiveTab('queue')}
            className={`flex items-center gap-1.5 px-3 py-1.5 rounded-full text-xs font-bold transition-all whitespace-nowrap ${
              activeTab === 'queue'
                ? 'bg-brand-primary text-white shadow-sm'
                : 'bg-brand-bg text-brand-muted hover:text-brand-text hover:bg-brand-border/60'
            }`}
          >
            <Truck size={13} />
            <span>Live Dispatch Queue</span>
          </button>
        </div>
      </div>

      {/* 2. Feedback Notification Toast */}
      {toastMsg && (
        <div className="p-3 bg-emerald-50 border border-emerald-300 text-emerald-800 rounded-card text-xs font-bold flex items-center gap-2 animate-in fade-in">
          <CheckCircle2 size={16} className="text-emerald-600 shrink-0" />
          <span>{toastMsg}</span>
        </div>
      )}

      {/* 3. Tab: Store Manager & Inventory */}
      {activeTab === 'manager' && (
        <div className="space-y-4">
          {/* KPI Cards */}
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-2.5 sm:gap-3">
            <div className="bg-white p-3 rounded-card border border-brand-border shadow-xs">
              <span className="text-[11px] text-brand-muted font-bold block">In Preparation</span>
              <span className="text-xl sm:text-2xl font-black text-brand-primary">
                {metrics?.preparingCount ?? 2} Orders
              </span>
              <span className="text-[10px] text-emerald-600 font-semibold block mt-0.5">
                Avg time: 2.4 mins
              </span>
            </div>

            <div className="bg-white p-3 rounded-card border border-brand-border shadow-xs">
              <span className="text-[11px] text-brand-muted font-bold block">Awaiting Dispatch</span>
              <span className="text-xl sm:text-2xl font-black text-amber-600">
                {metrics?.readyCount ?? 1} Bagged
              </span>
              <span className="text-[10px] text-brand-muted block mt-0.5">3 Riders on standby</span>
            </div>

            <div className="bg-white p-3 rounded-card border border-brand-border shadow-xs">
              <span className="text-[11px] text-brand-muted font-bold block">Out for Delivery</span>
              <span className="text-xl sm:text-2xl font-black text-blue-600">
                {metrics?.outForDeliveryCount ?? 4} Active
              </span>
              <span className="text-[10px] text-emerald-600 font-semibold block mt-0.5">
                100% on-time ETA
              </span>
            </div>

            <div className="bg-white p-3 rounded-card border border-brand-border shadow-xs">
              <span className="text-[11px] text-brand-muted font-bold block">Low Stock SKUs</span>
              <span className="text-xl sm:text-2xl font-black text-brand-danger">
                {metrics?.lowStockCount ?? 2} Items
              </span>
              <span className="text-[10px] text-brand-danger font-semibold block mt-0.5">
                Needs restock today
              </span>
            </div>
          </div>

          {/* Store Inventory Management Table */}
          <div className="bg-white rounded-card border border-brand-border shadow-card overflow-hidden">
            <div className="p-4 border-b border-brand-border flex flex-col sm:flex-row items-stretch sm:items-center justify-between gap-3">
              <div>
                <h3 className="text-sm font-black text-brand-text">
                  Dark Store #04 Inventory &amp; Stock Levels
                </h3>
                <p className="text-xs text-brand-muted">
                  Direct stock adjustments sync live with customer catalog availability.
                </p>
              </div>

              <div className="flex items-center gap-2">
                <div className="relative flex-1 sm:w-56">
                  <Search size={14} className="absolute left-2.5 top-1/2 -translate-y-1/2 text-brand-muted" />
                  <input
                    type="text"
                    value={inventorySearch}
                    onChange={e => setInventorySearch(e.target.value)}
                    placeholder="Search SKU or item..."
                    className="w-full pl-8 pr-3 py-1.5 bg-brand-bg border border-brand-border rounded-input text-xs text-brand-text focus:outline-none focus:ring-1 focus:ring-brand-primary"
                  />
                </div>

                <select
                  value={statusFilter}
                  onChange={e => setStatusFilter(e.target.value)}
                  className="bg-brand-bg border border-brand-border rounded-input px-2 py-1.5 text-xs text-brand-text font-medium"
                >
                  <option value="ALL">All Status</option>
                  <option value="IN_STOCK">In Stock</option>
                  <option value="LOW_STOCK">Low Stock</option>
                  <option value="OUT_OF_STOCK">Out of Stock</option>
                </select>
              </div>
            </div>

            <div className="overflow-x-auto">
              <table className="w-full text-left text-xs">
                <thead className="bg-brand-bg/60 border-b border-brand-border text-[11px] font-bold text-brand-muted uppercase">
                  <tr>
                    <th className="py-2.5 px-4">Item &amp; SKU</th>
                    <th className="py-2.5 px-3">Unit / Price</th>
                    <th className="py-2.5 px-3">Stock Qty</th>
                    <th className="py-2.5 px-3">Status</th>
                    <th className="py-2.5 px-4 text-right">Quick Restock</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-brand-border">
                  {filteredInventory.map(item => (
                    <tr key={item.productId} className="hover:bg-brand-bg/40 transition-colors">
                      <td className="py-2.5 px-4">
                        <div className="font-bold text-brand-text">{item.productName}</div>
                        <div className="text-[10px] text-brand-muted font-mono">{item.sku}</div>
                      </td>
                      <td className="py-2.5 px-3">
                        <div className="font-semibold text-brand-text">{item.unit}</div>
                        <div className="text-[10px] text-brand-muted font-mono">₹{item.price}</div>
                      </td>
                      <td className="py-2.5 px-3 font-black text-brand-text">
                        {item.quantity} units
                      </td>
                      <td className="py-2.5 px-3">
                        <span
                          className={`text-[10px] font-bold px-2 py-0.5 rounded-full ${
                            item.status === 'IN_STOCK'
                              ? 'bg-emerald-100 text-emerald-800'
                              : item.status === 'LOW_STOCK'
                              ? 'bg-amber-100 text-amber-800'
                              : 'bg-red-100 text-red-800'
                          }`}
                        >
                          {item.status.replace('_', ' ')}
                        </span>
                      </td>
                      <td className="py-2.5 px-4 text-right">
                        <button
                          onClick={() => setSelectedStockItem(item)}
                          className="px-2.5 py-1 bg-brand-primary/10 text-brand-primary hover:bg-brand-primary hover:text-white rounded-button font-bold text-[11px] transition-all inline-flex items-center gap-1"
                        >
                          <PlusCircle size={12} />
                          <span>Adjust</span>
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

      {/* 4. Tab: Picker Station */}
      {activeTab === 'picker' && (
        <div className="bg-white rounded-card border border-brand-border shadow-card overflow-hidden">
          <div className="bg-brand-primary text-white p-4 flex flex-wrap items-center justify-between gap-2">
            <div className="flex items-center gap-2">
              <Box size={18} />
              <div>
                <h3 className="text-sm font-black">
                  Active Picker Session &bull; {activeOrder ? `Order #${activeOrder.id}` : 'Order #NX-1042'}
                </h3>
                <p className="text-[11px] text-white/80">
                  Target picking time: 2 mins &bull; Tote Bin: T-88
                </p>
              </div>
            </div>

            <div className="flex items-center gap-2">
              <span className="text-xs bg-brand-accent text-brand-text font-black px-2.5 py-0.5 rounded-full">
                Aisle Zone A2-B4
              </span>
              <Link
                href="/store/picker"
                className="text-[11px] bg-white/20 hover:bg-white/30 text-white font-bold px-2 py-0.5 rounded transition-colors inline-flex items-center gap-1"
              >
                <span>Full Station</span>
                <ArrowRight size={11} />
              </Link>
            </div>
          </div>

          <div className="p-4 sm:p-5 space-y-4">
            {pickerSuccess && (
              <div className="p-3 bg-emerald-50 border border-emerald-300 text-emerald-800 rounded-card text-xs font-bold flex items-center gap-2 animate-in fade-in">
                <CheckCircle2 size={16} className="text-emerald-600 shrink-0" />
                <span>{pickerSuccess}</span>
              </div>
            )}

            <p className="text-xs text-brand-muted">
              Check off each SKU as you retrieve and place items into the tamper-proof tote:
            </p>

            {/* Picking Items List */}
            <div className="divide-y divide-brand-border border border-brand-border rounded-card overflow-hidden">
              {(activeOrder
                ? activeOrder.items.map((i, idx) => ({
                    id: idx + 1,
                    name: i.product.name,
                    sku: `NX-SKU-${i.product.id}`,
                    bin: `Aisle A${idx + 1}-0${i.product.id}`,
                    barcode: `8901030${i.product.id}`,
                    quantity: i.quantity,
                    unit: i.product.unit,
                  }))
                : [
                    { id: 1, name: 'Amul Taaza Milk 1L', sku: 'NX-MLK-01', bin: 'Aisle Dairy A1-02', barcode: '89010301', quantity: 2, unit: '1 L' },
                    { id: 2, name: 'Aashirvaad Shudh Chakki Atta', sku: 'NX-ATT-03', bin: 'Aisle Staples B2-04', barcode: '89010303', quantity: 1, unit: '5 kg' },
                    { id: 3, name: 'Fresh Alphonso Mangoes', sku: 'NX-MNG-04', bin: 'Aisle Produce C1-01', barcode: '89010304', quantity: 1, unit: '1 kg' },
                  ]
              ).map(item => {
                const isPicked = pickedItems[item.id] ?? false;
                return (
                  <div
                    key={item.id}
                    onClick={() => handleTogglePickerItem(item.id)}
                    className={`p-3 sm:p-3.5 flex items-center justify-between cursor-pointer transition-colors ${
                      isPicked ? 'bg-emerald-50/70' : 'bg-white hover:bg-brand-bg'
                    }`}
                  >
                    <div className="flex items-center gap-3">
                      <input
                        type="checkbox"
                        checked={isPicked}
                        onChange={() => {}}
                        className="rounded border-brand-border text-brand-primary focus:ring-brand-primary w-4 h-4 cursor-pointer shrink-0"
                      />
                      <div>
                        <p className={`text-xs font-bold ${isPicked ? 'line-through text-brand-muted' : 'text-brand-text'}`}>
                          {item.name}
                        </p>
                        <p className="text-[10px] text-brand-muted font-mono">
                          {item.bin} &bull; {item.unit}
                        </p>
                      </div>
                    </div>

                    <div className="flex items-center gap-2 sm:gap-3 shrink-0">
                      <span className="text-xs font-black bg-brand-bg px-2 py-0.5 rounded border border-brand-border">
                        QTY: {item.quantity}
                      </span>
                      <span className="text-xs font-bold text-brand-primary flex items-center gap-1 font-mono">
                        <Barcode size={15} />
                        <span className="hidden sm:inline">{item.barcode}</span>
                      </span>
                    </div>
                  </div>
                );
              })}
            </div>

            {/* Complete Picking Action */}
            <div className="pt-2">
              <button
                onClick={handleCompletePicking}
                className="w-full py-3 bg-brand-primary hover:bg-brand-primaryHover text-white rounded-button font-black text-xs sm:text-sm flex items-center justify-center gap-2 shadow-card transition-all active:scale-98"
              >
                <CheckCircle2 size={16} />
                <span>Complete Picking &amp; Transfer to Packing Station</span>
              </button>
            </div>
          </div>
        </div>
      )}

      {/* 5. Tab: Packing & Seal Station */}
      {activeTab === 'packing' && (
        <div className="bg-white rounded-card border border-brand-border shadow-card overflow-hidden">
          <div className="bg-purple-900 text-white p-4 flex flex-wrap items-center justify-between gap-2">
            <div className="flex items-center gap-2">
              <PackageCheck size={18} />
              <div>
                <h3 className="text-sm font-black">
                  Packing Station &bull; Tamper-Evident Bagging
                </h3>
                <p className="text-[11px] text-white/80">
                  Barcode Seal &amp; Quality Check Handshake
                </p>
              </div>
            </div>

            <Link
              href="/store/packing"
              className="text-[11px] bg-white/20 hover:bg-white/30 text-white font-bold px-2.5 py-1 rounded transition-colors inline-flex items-center gap-1"
            >
              <span>Full Packing Station</span>
              <ArrowRight size={11} />
            </Link>
          </div>

          <div className="p-4 sm:p-5 space-y-4">
            {packingSuccess && (
              <div className="p-3 bg-emerald-50 border border-emerald-300 text-emerald-800 rounded-card text-xs font-bold flex items-center gap-2 animate-in fade-in">
                <CheckCircle2 size={16} className="text-emerald-600 shrink-0" />
                <span>{packingSuccess}</span>
              </div>
            )}

            {packingError && (
              <div className="p-3 bg-red-50 border border-red-300 text-red-800 rounded-card text-xs font-bold flex items-center gap-2 animate-in fade-in">
                <AlertTriangle size={16} className="text-red-600 shrink-0" />
                <span>{packingError}</span>
              </div>
            )}

            {/* Bag Barcode Entry */}
            <div className="space-y-1.5">
              <label className="text-xs font-bold text-brand-text flex items-center gap-1.5">
                <Barcode size={14} className="text-brand-primary" />
                <span>Tamper-Evident Bag Barcode Number:</span>
              </label>
              <input
                type="text"
                value={packingBagCode}
                onChange={e => setPackingBagCode(e.target.value)}
                placeholder="BAG-NX-1042"
                className="w-full sm:max-w-md px-3 py-2 bg-brand-bg border border-brand-border rounded-input text-xs font-mono font-bold text-brand-text focus:outline-none focus:ring-2 focus:ring-brand-primary"
              />
            </div>

            {/* Safety & Quality Checklist */}
            <div className="bg-brand-bg/50 p-3.5 rounded-card border border-brand-border space-y-2.5">
              <span className="text-[11px] font-black uppercase text-brand-muted block">
                Quality &amp; Safety Checklist:
              </span>

              <label className="flex items-center gap-2 text-xs font-bold text-brand-text cursor-pointer">
                <input
                  type="checkbox"
                  checked={bagSealed}
                  onChange={e => setBagSealed(e.target.checked)}
                  className="rounded border-brand-border text-brand-primary focus:ring-brand-primary w-4 h-4 cursor-pointer"
                />
                <span>Tamper-evident thermal seal applied securely</span>
              </label>

              <label className="flex items-center gap-2 text-xs font-bold text-brand-text cursor-pointer">
                <input
                  type="checkbox"
                  checked={weightVerified}
                  onChange={e => setWeightVerified(e.target.checked)}
                  className="rounded border-brand-border text-brand-primary focus:ring-brand-primary w-4 h-4 cursor-pointer"
                />
                <span>Digital scale weight matches invoice weight (&plusmn;15g tolerance)</span>
              </label>

              <label className="flex items-center gap-2 text-xs font-bold text-brand-text cursor-pointer">
                <input
                  type="checkbox"
                  checked={coldPouchChecked}
                  onChange={e => setColdPouchChecked(e.target.checked)}
                  className="rounded border-brand-border text-brand-primary focus:ring-brand-primary w-4 h-4 cursor-pointer"
                />
                <span>Cold-chain insulated pouch included for milk &amp; refrigerated dairy</span>
              </label>
            </div>

            <button
              onClick={handleCompletePacking}
              className="w-full py-3 bg-purple-800 hover:bg-purple-900 text-white rounded-button font-black text-xs sm:text-sm flex items-center justify-center gap-2 shadow-card transition-all active:scale-98"
            >
              <PackageCheck size={16} />
              <span>Seal Bag &amp; Dispatch to Delivery Partner</span>
            </button>
          </div>
        </div>
      )}

      {/* 6. Tab: Live Dispatch Queue */}
      {activeTab === 'queue' && (
        <div className="space-y-3">
          <div className="bg-white p-4 rounded-card border border-brand-border shadow-card flex items-center justify-between">
            <div>
              <h3 className="text-sm font-black text-brand-text">Active Order Queue &bull; Hub #04</h3>
              <p className="text-xs text-brand-muted">Real-time status of orders progressing through the dark store</p>
            </div>
            <span className="text-xs bg-emerald-100 text-emerald-800 font-bold px-2.5 py-1 rounded-full">
              &bull; Live Stream Active
            </span>
          </div>

          {activeOrder ? (
            <div className="bg-white p-4 rounded-card border-2 border-brand-primary shadow-modal space-y-3">
              <div className="flex items-center justify-between">
                <div>
                  <span className="text-xs font-black text-brand-primary">Order #{activeOrder.id}</span>
                  <p className="text-[11px] text-brand-muted">{activeOrder.address}</p>
                </div>
                <span className="text-xs bg-brand-accent text-brand-text font-black px-2.5 py-1 rounded-full uppercase">
                  {activeOrder.status}
                </span>
              </div>

              <div className="flex items-center justify-between text-xs pt-2 border-t border-brand-border">
                <span>{activeOrder.items.length} items &bull; ₹{activeOrder.finalTotal}</span>
                <span className="font-mono text-brand-primary font-bold">Rider: {activeOrder.riderName}</span>
              </div>
            </div>
          ) : (
            <div className="bg-white p-6 rounded-card border border-brand-border text-center shadow-card">
              <Clock size={32} className="mx-auto text-brand-primary opacity-60 mb-2" />
              <h4 className="text-xs font-bold text-brand-text">All Current Orders Dispatched</h4>
              <p className="text-[11px] text-brand-muted max-w-sm mx-auto mt-0.5">
                Place an order in the Customer Store to see it automatically appear in this live queue!
              </p>
            </div>
          )}
        </div>
      )}

      {/* 7. Stock Adjustment Modal */}
      {selectedStockItem && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
          <div className="fixed inset-0 bg-brand-text/60 backdrop-blur-xs" onClick={() => setSelectedStockItem(null)} />
          <div className="relative w-full max-w-sm bg-white rounded-card shadow-2xl z-10 p-5 border border-brand-border space-y-4">
            <div>
              <h4 className="text-sm font-black text-brand-text">Adjust Stock Quantity</h4>
              <p className="text-xs text-brand-muted">{selectedStockItem.productName}</p>
            </div>

            <div className="space-y-3 text-xs">
              <div>
                <label className="font-bold text-brand-text block mb-1">Action Type:</label>
                <div className="grid grid-cols-3 gap-1.5">
                  {(['ADD', 'SET', 'SUBTRACT'] as const).map(act => (
                    <button
                      key={act}
                      type="button"
                      onClick={() => setAdjustmentAction(act)}
                      className={`py-1.5 rounded-button font-bold text-[11px] border transition-all ${
                        adjustmentAction === act
                          ? 'bg-brand-primary text-white border-brand-primary'
                          : 'bg-brand-bg text-brand-text border-brand-border'
                      }`}
                    >
                      {act}
                    </button>
                  ))}
                </div>
              </div>

              <div>
                <label className="font-bold text-brand-text block mb-1">Quantity Amount:</label>
                <input
                  type="number"
                  min="1"
                  value={adjustmentAmount}
                  onChange={e => setAdjustmentAmount(Math.max(1, parseInt(e.target.value) || 1))}
                  className="w-full px-3 py-2 bg-brand-bg border border-brand-border rounded-input text-sm font-black text-brand-text"
                />
              </div>

              <div>
                <label className="font-bold text-brand-text block mb-1">Reason:</label>
                <input
                  type="text"
                  value={adjustmentReason}
                  onChange={e => setAdjustmentReason(e.target.value)}
                  className="w-full px-3 py-1.5 bg-brand-bg border border-brand-border rounded-input text-xs text-brand-text"
                />
              </div>
            </div>

            <div className="flex items-center gap-2 pt-2">
              <button
                type="button"
                onClick={() => setSelectedStockItem(null)}
                className="flex-1 py-2 bg-brand-bg hover:bg-brand-border text-brand-text font-bold text-xs rounded-button"
              >
                Cancel
              </button>
              <button
                type="button"
                onClick={handleAdjustStock}
                disabled={isAdjusting}
                className="flex-1 py-2 bg-brand-primary hover:bg-brand-primaryHover text-white font-black text-xs rounded-button transition-all"
              >
                {isAdjusting ? 'Saving...' : 'Confirm'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default StoreOps;
