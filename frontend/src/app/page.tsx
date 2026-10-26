'use client';

import React from 'react';
import { useApp } from '@/context/AppContext';
import { RoleSwitcher } from '@/components/common/RoleSwitcher';
import { Header } from '@/components/customer/Header';
import { HeroBanner } from '@/components/customer/HeroBanner';
import { CategoryBar } from '@/components/customer/CategoryBar';
import { ProductCard } from '@/components/customer/ProductCard';
import { CartDrawer } from '@/components/customer/CartDrawer';
import { CheckoutModal } from '@/components/customer/CheckoutModal';
import { OrderTrackingModal } from '@/components/customer/OrderTrackingModal';
import { AIChatbot } from '@/components/customer/AIChatbot';
import { DeliveryApp } from '@/components/delivery/DeliveryApp';
import { SuperAdminDashboard } from '@/components/admin/SuperAdminDashboard';
import { StoreOps } from '@/components/store/StoreOps';
import { WelcomeModal } from '@/components/common/WelcomeModal';
import { Sparkles, ShieldCheck, Truck, RefreshCw } from 'lucide-react';

export default function HomePage() {
  const {
    role,
    searchQuery,
    selectedCategory,
    categories,
    products,
    isLoadingCatalog,
    refreshCatalog,
    setIsAIOpen,
    getCartCount,
    getCartTotal,
    setIsCartOpen,
  } = useApp();

  const [isWelcomeOpen, setIsWelcomeOpen] = React.useState(false);

  // Filter products by selectedCategory or searchQuery
  const filteredProducts = products.filter((p) => {
    if (selectedCategory && p.categoryId !== selectedCategory) {
      return false;
    }
    if (searchQuery) {
      const q = searchQuery.toLowerCase();
      return (
        p.name.toLowerCase().includes(q) ||
        p.brand?.toLowerCase().includes(q) ||
        p.unit.toLowerCase().includes(q)
      );
    }
    return true;
  });

  const activeCategoryObj = categories.find(c => c.id === selectedCategory);
  const cartCount = getCartCount();
  const cartTotal = getCartTotal();

  return (
    <div className="min-h-screen bg-brand-bg flex flex-col">
      {/* 1. Global Multi-Role Bar for Single-URL Experience */}
      <RoleSwitcher />

      {/* 2. Main Role Views */}
      {role === 'customer' && (
        <>
          <Header />

          <main className="flex-1 max-w-7xl w-full mx-auto px-4 py-3 space-y-4">
            {/* Promotional Banners */}
            {!selectedCategory && !searchQuery && <HeroBanner />}

            {/* Blinkit-Style Categories Navigation */}
            <CategoryBar />

            {/* Product Catalog Grid */}
            <section className="py-2">
              <div className="flex items-center justify-between mb-4">
                <div>
                  <h3 className="text-base font-black text-brand-text flex items-center gap-2">
                    {activeCategoryObj ? (
                      <>
                        <span>{activeCategoryObj.icon}</span>
                        <span>{activeCategoryObj.name}</span>
                      </>
                    ) : searchQuery ? (
                      `Search Results for "${searchQuery}"`
                    ) : (
                      'Popular Daily Essentials in Your Neighborhood'
                    )}
                  </h3>
                  <p className="text-xs text-brand-muted mt-0.5">
                    {filteredProducts.length} items in stock • Delivered within 10 minutes
                  </p>
                </div>

                <button
                  onClick={() => refreshCatalog()}
                  disabled={isLoadingCatalog}
                  className="flex items-center gap-1.5 px-3 py-1.5 text-xs font-bold text-brand-primary bg-brand-primary/10 hover:bg-brand-primary/20 rounded-button transition-all"
                  title="Refresh catalog from backend"
                >
                  <RefreshCw size={13} className={isLoadingCatalog ? 'animate-spin' : ''} />
                  <span className="hidden sm:inline">Sync Live Catalog</span>
                </button>
              </div>

              {filteredProducts.length === 0 ? (
                <div className="bg-white rounded-card border border-brand-border p-12 text-center shadow-card">
                  <div className="text-4xl mb-3">🔍</div>
                  <h4 className="text-base font-bold text-brand-text mb-1">No products found</h4>
                  <p className="text-xs text-brand-muted max-w-md mx-auto mb-4">
                    We could not find items matching your search. Try searching for milk, chips, bread, apples, or atta!
                  </p>
                </div>
              ) : (
                <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 gap-3.5">
                  {filteredProducts.map((product) => (
                    <ProductCard key={product.id} product={product} />
                  ))}
                </div>
              )}
            </section>

            {/* Value Propositions / Trust Bar */}
            <section className="pt-6 pb-8 border-t border-brand-border/80">
              <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
                <div className="bg-white p-4 rounded-card border border-brand-border shadow-card flex items-center gap-3">
                  <div className="w-10 h-10 rounded-full bg-brand-primary/10 text-brand-primary flex items-center justify-center shrink-0">
                    <Truck size={20} />
                  </div>
                  <div>
                    <h4 className="text-xs font-black text-brand-text">10-Minute Rapid Delivery</h4>
                    <p className="text-[11px] text-brand-muted">
                      Hyperlocal dark stores within 2 km of your doorstep.
                    </p>
                  </div>
                </div>

                <div className="bg-white p-4 rounded-card border border-brand-border shadow-card flex items-center gap-3">
                  <div className="w-10 h-10 rounded-full bg-brand-primary/10 text-brand-primary flex items-center justify-center shrink-0">
                    <ShieldCheck size={20} />
                  </div>
                  <div>
                    <h4 className="text-xs font-black text-brand-text">100% Quality Guaranteed</h4>
                    <p className="text-[11px] text-brand-muted">
                      Freshly checked vegetables, dairy & genuine brands.
                    </p>
                  </div>
                </div>

                <div className="bg-white p-4 rounded-card border border-brand-border shadow-card flex items-center gap-3">
                  <div className="w-10 h-10 rounded-full bg-brand-primary/10 text-brand-primary flex items-center justify-center shrink-0">
                    <RefreshCw size={20} />
                  </div>
                  <div>
                    <h4 className="text-xs font-black text-brand-text">Instant Replacement</h4>
                    <p className="text-[11px] text-brand-muted">
                      No questions asked doorstep refund or replacement.
                    </p>
                  </div>
                </div>
              </div>
            </section>
          </main>

          {/* Customer Footer */}
          <footer className="bg-white border-t border-brand-border py-8 text-xs text-brand-muted">
            <div className="max-w-7xl mx-auto px-4 flex flex-col sm:flex-row items-center justify-between gap-4">
              <div className="flex items-center gap-2 font-bold text-brand-text">
                <span className="w-3 h-3 rounded-full bg-brand-primary" />
                <span>NEXORA Quick Commerce Platform</span>
              </div>
              <p className="text-center sm:text-right text-[11px]">
                Built with precision • Brand Theme: Leaf Green <span className="font-mono text-brand-primary">#176B45</span> & Zesty Lime <span className="font-mono text-emerald-600">#D8F36A</span>
              </p>
            </div>
          </footer>
        </>
      )}

      {role === 'delivery' && <DeliveryApp />}
      {role === 'admin' && <SuperAdminDashboard />}
      {role === 'store' && <StoreOps />}

      {/* 3. Global Floating AI Assistant Button */}
      <button
        onClick={() => setIsAIOpen(true)}
        className="fixed bottom-6 right-6 z-40 p-3.5 bg-brand-primary hover:bg-brand-primaryHover text-white rounded-full shadow-modal hover:scale-105 transition-all flex items-center gap-2 group border-2 border-brand-accent"
        aria-label="Open Nexora AI Shopping Assistant"
      >
        <Sparkles size={18} className="text-brand-accent animate-spin" style={{ animationDuration: '4s' }} />
        <span className="text-xs font-black hidden sm:inline pr-1">Nexora AI</span>
      </button>

      {/* 4. Mobile Sticky Bottom Cart Bar */}
      {cartCount > 0 && role === 'customer' && (
        <div className="sm:hidden fixed bottom-3 left-3 right-3 z-40">
          <button
            onClick={() => setIsCartOpen(true)}
            className="w-full py-3 px-4 bg-brand-primary text-white rounded-button font-black text-xs flex items-center justify-between shadow-modal border border-brand-accent/30 active:scale-98"
          >
            <div className="flex flex-col items-start leading-tight">
              <span className="text-[10px] text-white/80 font-normal">{cartCount} items in cart</span>
              <span className="text-sm font-black text-brand-accent">₹{cartTotal} TOTAL</span>
            </div>
            <div className="flex items-center gap-1.5 bg-brand-accent text-brand-text px-3 py-1.5 rounded-full font-bold">
              <span>View Cart</span>
              <span>&rarr;</span>
            </div>
          </button>
        </div>
      )}

      {/* 5. Drawers and Modals */}
      <CartDrawer />
      <CheckoutModal />
      <OrderTrackingModal />
      <AIChatbot />
      <WelcomeModal isOpen={isWelcomeOpen} onClose={() => setIsWelcomeOpen(false)} />
    </div>
  );
}
