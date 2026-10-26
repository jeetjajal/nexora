'use client';

import React, { useState } from 'react';
import Link from 'next/link';
import { useApp } from '@/context/AppContext';
import { Box, CheckSquare, Barcode, ArrowRight, CheckCircle2, Clock, Layers, PackageCheck } from 'lucide-react';

export const StoreOps: React.FC = () => {
  const { activeOrder, updateOrderStatus } = useApp();
  const [checkedItems, setCheckedItems] = useState<{ [id: string]: boolean }>({});

  const toggleItem = (id: string) => {
    setCheckedItems(prev => ({
      ...prev,
      [id]: !prev[id],
    }));
  };

  const handleCompletePacking = () => {
    updateOrderStatus('OUT_FOR_DELIVERY');
  };

  return (
    <div className="max-w-4xl mx-auto py-6 px-4 space-y-6">
      {/* Top Header */}
      <div className="bg-white rounded-card border border-brand-border p-4 shadow-card flex flex-wrap items-center justify-between gap-3">
        <div>
          <div className="flex items-center gap-2">
            <h2 className="text-base font-black text-brand-text">Dark Store Operations Hub</h2>
            <span className="text-[10px] bg-brand-primary text-white font-bold px-2 py-0.5 rounded-full">
              HUB: SG HIGHWAY #04
            </span>
          </div>
          <p className="text-xs text-brand-muted mt-0.5">
            Operational stations for Dark Store Managers, Pickers, and Packers.
          </p>
        </div>

        {/* Quick Station Navigation */}
        <div className="flex flex-wrap items-center gap-2">
          <Link
            href="/store/manager"
            className="px-3 py-1.5 text-xs font-bold bg-brand-primary/10 text-brand-primary hover:bg-brand-primary/20 rounded-button transition-all flex items-center gap-1.5"
          >
            <Layers size={13} />
            <span>Store Manager</span>
          </Link>
          <Link
            href="/store/picker"
            className="px-3 py-1.5 text-xs font-bold bg-[#176B45]/10 text-[#176B45] hover:bg-[#176B45]/20 rounded-button transition-all flex items-center gap-1.5"
          >
            <Box size={13} />
            <span>Picker App</span>
          </Link>
          <Link
            href="/store/packing"
            className="px-3 py-1.5 text-xs font-bold bg-purple-100 hover:bg-purple-200 text-purple-800 rounded-button transition-all flex items-center gap-1.5"
          >
            <PackageCheck size={13} />
            <span>Packing Station</span>
          </Link>
        </div>
      </div>

      {/* Active Picking Task */}
      {activeOrder ? (
        <div className="bg-white rounded-card border border-brand-border shadow-modal overflow-hidden">
          <div className="bg-brand-primary text-white p-4 flex items-center justify-between">
            <div className="flex items-center gap-2">
              <Box size={18} />
              <h3 className="text-sm font-black">Picking Batch #{activeOrder.id}</h3>
            </div>
            <span className="text-xs bg-brand-accent text-brand-text font-black px-2 py-0.5 rounded-full">
              Aisle Zone: A4-B2
            </span>
          </div>

          <div className="p-5 space-y-4">
            <p className="text-xs text-brand-muted">
              Scan barcode or check each SKU as you place items into the tamper-evident tote:
            </p>

            <div className="divide-y divide-brand-border border border-brand-border rounded-card overflow-hidden">
              {activeOrder.items.map(({ product, quantity }) => {
                const isChecked = checkedItems[product.id] ?? false;

                return (
                  <div
                    key={product.id}
                    onClick={() => toggleItem(product.id)}
                    className={`p-3 flex items-center justify-between cursor-pointer transition-colors ${
                      isChecked ? 'bg-emerald-50/60' : 'bg-white hover:bg-brand-bg'
                    }`}
                  >
                    <div className="flex items-center gap-3">
                      <input
                        type="checkbox"
                        checked={isChecked}
                        onChange={() => {}}
                        className="rounded border-brand-border text-brand-primary focus:ring-brand-primary w-4 h-4 cursor-pointer"
                      />
                      <div>
                        <p className={`text-xs font-bold ${isChecked ? 'line-through text-brand-muted' : 'text-brand-text'}`}>
                          {product.name}
                        </p>
                        <p className="text-[10px] text-brand-muted font-mono">
                          Bin: {product.categoryId}-0{product.id} • {product.unit}
                        </p>
                      </div>
                    </div>

                    <div className="flex items-center gap-3">
                      <span className="text-xs font-black bg-brand-bg px-2.5 py-1 rounded border border-brand-border">
                        QTY: {quantity}
                      </span>
                      <span className="text-xs font-bold text-brand-primary flex items-center gap-1 font-mono">
                        <Barcode size={16} /> 890{product.id}44
                      </span>
                    </div>
                  </div>
                );
              })}
            </div>

            {/* Action to dispatch */}
            <div className="pt-2 flex flex-col sm:flex-row gap-2">
              <button
                onClick={handleCompletePacking}
                className="flex-1 py-3 bg-brand-primary hover:bg-brand-primaryHover text-white rounded-button font-black text-xs flex items-center justify-center gap-2 shadow-card transition-all active:scale-98"
              >
                <CheckCircle2 size={16} />
                <span>Seal Bag & Dispatch to Rider</span>
              </button>
              <Link
                href="/store/packing"
                className="py-3 px-4 bg-purple-700 hover:bg-purple-800 text-white rounded-button font-black text-xs flex items-center justify-center gap-1.5 transition-all shadow-card"
              >
                <span>Full Packing Station</span>
                <ArrowRight size={14} />
              </Link>
            </div>
          </div>
        </div>
      ) : (
        <div className="bg-white p-8 rounded-card border border-brand-border text-center shadow-card space-y-4">
          <Box size={36} className="mx-auto text-brand-primary opacity-60" />
          <div>
            <h4 className="text-base font-bold text-brand-text mb-1">No Orders in Picking Queue</h4>
            <p className="text-xs text-brand-muted max-w-sm mx-auto">
              Customer orders placed on the store appear here instantly for batch picking and packing.
            </p>
          </div>

          <div className="flex flex-wrap items-center justify-center gap-3 pt-2">
            <Link
              href="/store/manager"
              className="px-4 py-2 bg-brand-primary text-white rounded-button text-xs font-bold hover:bg-brand-primaryHover transition-all inline-flex items-center gap-1.5"
            >
              <span>Open Store Manager Dashboard</span>
              <ArrowRight size={13} />
            </Link>
            <Link
              href="/store/picker"
              className="px-4 py-2 bg-slate-100 text-slate-800 rounded-button text-xs font-bold hover:bg-slate-200 transition-all inline-flex items-center gap-1.5"
            >
              <span>Open Picker Station</span>
              <ArrowRight size={13} />
            </Link>
          </div>
        </div>
      )}
    </div>
  );
};

export default StoreOps;
