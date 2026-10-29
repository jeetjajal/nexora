'use client';

import React from 'react';
import { useApp } from '@/context/AppContext';
import { ProductCard } from '@/components/customer/ProductCard';
import { RotateCcw, Clock, Sparkles, ShoppingBag, ArrowRight } from 'lucide-react';

export const OrderAgainView: React.FC = () => {
  const { products, pastOrders, addToCart, setIsCartOpen } = useApp();

  // Frequently re-ordered staple products (Milk, Bread, Eggs, Atta, Snacks, Butter)
  const stapleKeywords = ['milk', 'bread', 'eggs', 'atta', 'paneer', 'chips', 'makhana', 'coffee', 'tomato'];
  const stapleProducts = products.filter((p) =>
    stapleKeywords.some((kw) => p.name.toLowerCase().includes(kw))
  );

  return (
    <div className="space-y-5 animate-in fade-in duration-200">
      {/* Top Banner */}
      <div className="bg-gradient-to-r from-emerald-800 to-brand-primary text-white p-5 rounded-card shadow-sm flex flex-col sm:flex-row sm:items-center justify-between gap-3">
        <div>
          <div className="flex items-center gap-2 mb-1">
            <span className="p-1 rounded-full bg-brand-accent text-brand-text">
              <RotateCcw size={16} />
            </span>
            <h2 className="text-base sm:text-lg font-black tracking-tight">
              Order Again &bull; Daily Staples
            </h2>
          </div>
          <p className="text-xs text-white/80">
            Re-order your favorite essentials in just 1 tap &bull; Delivered in 8-10 minutes.
          </p>
        </div>

        <button
          onClick={() => setIsCartOpen(true)}
          className="self-start sm:self-center px-4 py-2 bg-brand-accent text-brand-text font-black text-xs rounded-button hover:brightness-105 transition-all shadow-xs flex items-center gap-1.5"
        >
          <span>View Current Cart</span>
          <ArrowRight size={14} />
        </button>
      </div>

      {/* Past Orders Re-order Shelf */}
      {pastOrders && pastOrders.length > 0 && (
        <section className="bg-white p-4 rounded-card border border-brand-border shadow-xs space-y-3">
          <div className="flex items-center justify-between">
            <h3 className="text-sm font-black text-brand-text flex items-center gap-2">
              <Clock size={16} className="text-brand-primary" />
              <span>From Your Recent Orders</span>
            </h3>
            <span className="text-[11px] text-brand-muted">{pastOrders.length} previous orders</span>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-2.5">
            {pastOrders.slice(0, 4).map((order) => (
              <div
                key={order.orderId}
                className="p-3 bg-brand-bg/50 rounded-lg border border-brand-border flex items-center justify-between gap-3 hover:bg-brand-bg transition-colors"
              >
                <div className="min-w-0">
                  <span className="text-[10px] font-mono text-brand-muted block">
                    {order.date} &bull; Order #{order.orderId}
                  </span>
                  <p className="text-xs font-bold text-brand-text truncate">
                    {order.itemsSummary}
                  </p>
                  <span className="text-xs font-black text-brand-primary">₹{order.amount}</span>
                </div>

                <button
                  onClick={() => {
                    // Quick reorder first 2 matching staple products
                    stapleProducts.slice(0, 2).forEach((p) => addToCart(p.id));
                    setIsCartOpen(true);
                  }}
                  className="px-3 py-1.5 bg-brand-primary hover:bg-brand-primaryHover text-white rounded-button font-black text-xs transition-all shrink-0 active:scale-95"
                >
                  Repeat Order
                </button>
              </div>
            ))}
          </div>
        </section>
      )}

      {/* Quick Repeat Grid */}
      <section className="space-y-3">
        <div className="flex items-center justify-between">
          <h3 className="text-sm sm:text-base font-black text-brand-text flex items-center gap-1.5">
            <Sparkles size={16} className="text-brand-primary" />
            <span>Popular Items You May Need Today</span>
          </h3>
          <span className="text-xs text-brand-muted">Fresh stock available</span>
        </div>

        <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 gap-3 sm:gap-3.5">
          {stapleProducts.map((product) => (
            <ProductCard key={product.id} product={product} />
          ))}
        </div>
      </section>
    </div>
  );
};

export default OrderAgainView;
