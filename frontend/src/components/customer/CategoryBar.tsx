'use client';

import React from 'react';
import { CATEGORIES } from '@/data/categories';
import { useApp } from '@/context/AppContext';

export const CategoryBar: React.FC = () => {
  const { selectedCategory, setSelectedCategory } = useApp();

  return (
    <div className="py-3 border-b border-brand-border/60">
      <div className="flex items-center justify-between mb-2 px-1">
        <h2 className="text-sm font-black text-brand-text uppercase tracking-wider flex items-center gap-1.5">
          <span>Explore 31 Fresh Categories</span>
          <span className="text-[10px] bg-brand-primary/10 text-brand-primary px-2 py-0.5 rounded-full font-bold">
            All 10-Min Delivery
          </span>
        </h2>
        {selectedCategory && (
          <button
            onClick={() => setSelectedCategory(null)}
            className="text-xs text-brand-primary font-bold hover:underline"
          >
            Show All
          </button>
        )}
      </div>

      {/* Horizontal Scrollable Categories */}
      <div className="flex items-center gap-2 overflow-x-auto pb-2 scrollbar-none">
        <button
          onClick={() => setSelectedCategory(null)}
          className={`flex items-center gap-2 px-3.5 py-2 rounded-full text-xs font-bold shrink-0 transition-all ${
            selectedCategory === null
              ? 'bg-brand-primary text-white shadow-sm'
              : 'bg-white text-brand-text border border-brand-border hover:border-brand-primary/40'
          }`}
        >
          <span>✨</span>
          <span>All Items</span>
        </button>

        {CATEGORIES.map((cat) => {
          const isSelected = selectedCategory === cat.id;
          return (
            <button
              key={cat.id}
              onClick={() => setSelectedCategory(isSelected ? null : cat.id)}
              className={`flex items-center gap-2 px-3 py-1.5 rounded-full text-xs font-medium shrink-0 transition-all ${
                isSelected
                  ? 'bg-brand-primary text-white font-bold shadow-sm'
                  : 'bg-white text-brand-text border border-brand-border hover:bg-brand-bg hover:border-brand-primary/30'
              }`}
            >
              <span className="text-sm">{cat.icon}</span>
              <span className="whitespace-nowrap">{cat.name}</span>
              {cat.highlight && (
                <span className={`w-1.5 h-1.5 rounded-full ${isSelected ? 'bg-brand-accent' : 'bg-brand-primary'}`} />
              )}
            </button>
          );
        })}
      </div>
    </div>
  );
};

export default CategoryBar;
