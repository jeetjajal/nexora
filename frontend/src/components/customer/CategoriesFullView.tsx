'use client';

import React, { useState } from 'react';
import { useApp } from '@/context/AppContext';
import { ProductCard } from '@/components/customer/ProductCard';
import { Layers, ChevronRight, Sparkles } from 'lucide-react';

export const CategoriesFullView: React.FC = () => {
  const { categories, products, selectedCategory, setSelectedCategory } = useApp();
  const [activeSubcategoryId, setActiveSubcategoryId] = useState<string | null>(null);

  const currentCatId = selectedCategory || categories[0]?.id || '1';
  const currentCat = categories.find((c) => c.id === currentCatId) || categories[0];

  const handleSelectCategory = (catId: string) => {
    setSelectedCategory(catId);
    setActiveSubcategoryId(null);
  };

  // Filter products by active category and subcategory
  const categoryProducts = products.filter((p) => {
    if (p.categoryId !== currentCatId) return false;
    if (activeSubcategoryId && p.subcategoryId && p.subcategoryId !== activeSubcategoryId) {
      return false;
    }
    return true;
  });

  return (
    <div className="bg-white rounded-card border border-brand-border shadow-xs overflow-hidden animate-in fade-in duration-200">
      {/* Top Banner Header */}
      <div className="p-4 border-b border-brand-border bg-brand-bg/40 flex items-center justify-between">
        <div>
          <h2 className="text-base sm:text-lg font-black text-brand-text flex items-center gap-2">
            <span>{currentCat?.icon}</span>
            <span>{currentCat?.name}</span>
          </h2>
          <p className="text-xs text-brand-muted mt-0.5">
            Explore 31 curated quick-commerce aisles &bull; Delivered in 8-10 minutes
          </p>
        </div>
        <span className="text-xs font-black bg-brand-primary/10 text-brand-primary px-3 py-1 rounded-full">
          {categoryProducts.length} Products
        </span>
      </div>

      <div className="flex flex-col md:flex-row min-h-[600px]">
        {/* Left Vertical Categories Rail (Blinkit Style) */}
        <aside className="w-full md:w-64 border-b md:border-b-0 md:border-r border-brand-border bg-brand-bg/30 overflow-x-auto md:overflow-y-auto md:max-h-[750px] scrollbar-thin">
          <div className="flex md:flex-col p-1.5 md:p-2 gap-1">
            {categories.map((cat) => {
              const isActive = cat.id === currentCatId;
              return (
                <button
                  key={cat.id}
                  onClick={() => handleSelectCategory(cat.id)}
                  className={`flex items-center gap-2.5 px-3 py-2.5 rounded-lg text-left transition-all text-xs shrink-0 md:shrink md:w-full ${
                    isActive
                      ? 'bg-brand-primary text-white font-black shadow-xs'
                      : 'text-brand-text hover:bg-brand-border/40 font-medium'
                  }`}
                >
                  <span className="text-base shrink-0">{cat.icon}</span>
                  <div className="min-w-0 flex-1">
                    <span className="truncate block">{cat.name}</span>
                  </div>
                  <ChevronRight
                    size={14}
                    className={`hidden md:block shrink-0 ${isActive ? 'text-white' : 'text-brand-muted opacity-50'}`}
                  />
                </button>
              );
            })}
          </div>
        </aside>

        {/* Right Content Area */}
        <main className="flex-1 p-4 sm:p-5 space-y-4">
          {/* Subcategories Horizontal Filter Bar */}
          {currentCat && currentCat.subcategories && currentCat.subcategories.length > 0 && (
            <div className="space-y-2">
              <span className="text-[11px] font-black uppercase tracking-wider text-brand-muted block">
                Subcategories in {currentCat.name}:
              </span>
              <div className="flex items-center gap-1.5 overflow-x-auto scrollbar-none pb-1">
                <button
                  onClick={() => setActiveSubcategoryId(null)}
                  className={`px-3 py-1.5 rounded-full text-xs font-bold transition-all whitespace-nowrap ${
                    activeSubcategoryId === null
                      ? 'bg-brand-text text-white shadow-xs'
                      : 'bg-brand-bg text-brand-muted hover:text-brand-text border border-brand-border'
                  }`}
                >
                  All Items
                </button>

                {currentCat.subcategories.map((sub) => {
                  const isSubActive = activeSubcategoryId === sub.id;
                  return (
                    <button
                      key={sub.id}
                      onClick={() => setActiveSubcategoryId(sub.id)}
                      className={`px-3 py-1.5 rounded-full text-xs font-bold transition-all whitespace-nowrap ${
                        isSubActive
                          ? 'bg-brand-primary text-white shadow-xs'
                          : 'bg-brand-bg text-brand-text hover:bg-brand-border/50 border border-brand-border'
                      }`}
                    >
                      <span>{sub.name}</span>
                    </button>
                  );
                })}
              </div>

              {/* Subcategory Examples Tagline */}
              {activeSubcategoryId && (
                <div className="text-[11px] text-brand-muted bg-brand-bg p-2 rounded-lg border border-brand-border/60">
                  <strong className="text-brand-text">Popular: </strong>
                  {currentCat.subcategories
                    .find((s) => s.id === activeSubcategoryId)
                    ?.examples.join(', ')}
                </div>
              )}
            </div>
          )}

          {/* Product Cards Grid */}
          {categoryProducts.length > 0 ? (
            <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 gap-3 sm:gap-3.5 pt-2">
              {categoryProducts.map((product) => (
                <ProductCard key={product.id} product={product} />
              ))}
            </div>
          ) : (
            <div className="p-12 text-center bg-brand-bg/40 rounded-card border border-brand-border space-y-2">
              <div className="text-3xl">🛒</div>
              <h4 className="text-sm font-bold text-brand-text">Restocking Fresh Items</h4>
              <p className="text-xs text-brand-muted max-w-sm mx-auto">
                Items for this subcategory are being replenished in Dark Store #04. Check back in a few minutes!
              </p>
            </div>
          )}
        </main>
      </div>
    </div>
  );
};

export default CategoriesFullView;
