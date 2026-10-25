'use client';

import React, { createContext, useContext, useState, useEffect } from 'react';
import { Product, PRODUCTS } from '@/data/products';

export type AppRole = 'customer' | 'delivery' | 'admin' | 'store';

export interface CartItem {
  product: Product;
  quantity: number;
}

export interface ActiveOrder {
  id: string;
  items: CartItem[];
  itemTotal: number;
  deliveryFee: number;
  handlingFee: number;
  discount: number;
  finalTotal: number;
  address: string;
  status: 'PLACED' | 'STORE_PICKING' | 'PACKED' | 'OUT_FOR_DELIVERY' | 'DELIVERED';
  riderName: string;
  riderPhone: string;
  riderRating: number;
  etaMinutes: number;
  deliveryOtp: string;
  createdAt: string;
}

interface AppContextType {
  role: AppRole;
  setRole: (role: AppRole) => void;
  cart: { [productId: string]: number };
  addToCart: (productId: string) => void;
  removeFromCart: (productId: string) => void;
  clearCart: () => void;
  getCartItems: () => CartItem[];
  getCartCount: () => number;
  getCartTotal: () => number;
  couponCode: string;
  setCouponCode: (code: string) => void;
  appliedDiscount: number;
  applyCoupon: (code: string) => boolean;
  isCartOpen: boolean;
  setIsCartOpen: (open: boolean) => void;
  isCheckoutOpen: boolean;
  setIsCheckoutOpen: (open: boolean) => void;
  isTrackingOpen: boolean;
  setIsTrackingOpen: (open: boolean) => void;
  isAIOpen: boolean;
  setIsAIOpen: (open: boolean) => void;
  activeOrder: ActiveOrder | null;
  createOrder: (address: string, paymentMethod: string) => ActiveOrder;
  updateOrderStatus: (status: ActiveOrder['status']) => void;
  searchQuery: string;
  setSearchQuery: (query: string) => void;
  selectedCategory: string | null;
  setSelectedCategory: (catId: string | null) => void;
}

const AppContext = createContext<AppContextType | undefined>(undefined);

export const AppProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [role, setRole] = useState<AppRole>('customer');
  const [cart, setCart] = useState<{ [productId: string]: number }>({});
  const [couponCode, setCouponCode] = useState<string>('');
  const [appliedDiscount, setAppliedDiscount] = useState<number>(0);
  const [isCartOpen, setIsCartOpen] = useState(false);
  const [isCheckoutOpen, setIsCheckoutOpen] = useState(false);
  const [isTrackingOpen, setIsTrackingOpen] = useState(false);
  const [isAIOpen, setIsAIOpen] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedCategory, setSelectedCategory] = useState<string | null>(null);

  // Active simulated order
  const [activeOrder, setActiveOrder] = useState<ActiveOrder | null>(null);

  // Load / initialize some cart data for instant wow-effect if empty
  useEffect(() => {
    setCart({
      'p1': 2, // 2x Taaza Milk
      'p5': 1, // 1x Tomatoes
      'p15': 1, // 1x Lay's Magic Masala
    });
  }, []);

  const addToCart = (productId: string) => {
    setCart(prev => ({
      ...prev,
      [productId]: (prev[productId] || 0) + 1,
    }));
  };

  const removeFromCart = (productId: string) => {
    setCart(prev => {
      const current = prev[productId] || 0;
      if (current <= 1) {
        const next = { ...prev };
        delete next[productId];
        return next;
      }
      return {
        ...prev,
        [productId]: current - 1,
      };
    });
  };

  const clearCart = () => {
    setCart({});
    setAppliedDiscount(0);
    setCouponCode('');
  };

  const getCartItems = (): CartItem[] => {
    return Object.entries(cart)
      .map(([id, quantity]) => {
        const product = PRODUCTS.find(p => p.id === id);
        if (!product) return null;
        return { product, quantity };
      })
      .filter((item): item is CartItem => item !== null);
  };

  const getCartCount = (): number => {
    return Object.values(cart).reduce((a, b) => a + b, 0);
  };

  const getCartTotal = (): number => {
    return getCartItems().reduce((total, item) => total + item.product.price * item.quantity, 0);
  };

  const applyCoupon = (code: string): boolean => {
    const clean = code.trim().toUpperCase();
    if (clean === 'NEXORA50' || clean === 'FRESH50') {
      const subtotal = getCartTotal();
      const discount = Math.min(Math.round(subtotal * 0.5), 100);
      setAppliedDiscount(discount);
      setCouponCode(clean);
      return true;
    }
    return false;
  };

  const createOrder = (address: string, paymentMethod: string): ActiveOrder => {
    const items = getCartItems();
    const itemTotal = getCartTotal();
    const deliveryFee = itemTotal >= 199 ? 0 : 25;
    const handlingFee = 4;
    const finalTotal = Math.max(0, itemTotal + deliveryFee + handlingFee - appliedDiscount);

    const newOrder: ActiveOrder = {
      id: `NEX-${Math.floor(100000 + Math.random() * 900000)}`,
      items,
      itemTotal,
      deliveryFee,
      handlingFee,
      discount: appliedDiscount,
      finalTotal,
      address,
      status: 'PLACED',
      riderName: 'Ramesh Patel',
      riderPhone: '+91 98765 43210',
      riderRating: 4.9,
      etaMinutes: 9,
      deliveryOtp: String(Math.floor(100000 + Math.random() * 900000)),
      createdAt: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
    };

    setActiveOrder(newOrder);
    clearCart();
    setIsCheckoutOpen(false);
    setIsCartOpen(false);
    setIsTrackingOpen(true);
    return newOrder;
  };

  const updateOrderStatus = (status: ActiveOrder['status']) => {
    if (activeOrder) {
      setActiveOrder({
        ...activeOrder,
        status,
      });
    }
  };

  return (
    <AppContext.Provider
      value={{
        role,
        setRole,
        cart,
        addToCart,
        removeFromCart,
        clearCart,
        getCartItems,
        getCartCount,
        getCartTotal,
        couponCode,
        setCouponCode,
        appliedDiscount,
        applyCoupon,
        isCartOpen,
        setIsCartOpen,
        isCheckoutOpen,
        setIsCheckoutOpen,
        isTrackingOpen,
        setIsTrackingOpen,
        isAIOpen,
        setIsAIOpen,
        activeOrder,
        createOrder,
        updateOrderStatus,
        searchQuery,
        setSearchQuery,
        selectedCategory,
        setSelectedCategory,
      }}
    >
      {children}
    </AppContext.Provider>
  );
};

export const useApp = () => {
  const context = useContext(AppContext);
  if (!context) {
    throw new Error('useApp must be used within an AppProvider');
  }
  return context;
};
