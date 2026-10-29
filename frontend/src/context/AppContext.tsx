'use client';

import React, { createContext, useContext, useState, useEffect, useCallback } from 'react';
import { Product, PRODUCTS } from '@/data/products';
import { Category, CATEGORIES } from '@/data/categories';
import { ApiClient, CheckoutPayload, UserProfile, SavedAddress, PastOrder } from '@/lib/api-client';

export type AppRole = 'customer' | 'delivery' | 'admin' | 'store';
export type CustomerTab = 'home' | 'order_again' | 'categories' | 'profile';

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
  darkStore?: string;
  paymentMethod: string;
  createdAt: string;
}

interface AppContextType {
  role: AppRole;
  setRole: (role: AppRole) => void;
  currentUser: UserProfile | null;
  savedAddresses: SavedAddress[];
  pastOrders: PastOrder[];
  isAuthOpen: boolean;
  setIsAuthOpen: (open: boolean) => void;
  isAccountOpen: boolean;
  setIsAccountOpen: (open: boolean) => void;
  login: (email: string, password: string) => Promise<void>;
  register: (name: string, email: string, phone: string, password: string) => Promise<void>;
  updateProfile: (name: string, phone: string) => Promise<void>;
  logout: () => void;
  addNewAddress: (addressData: Partial<SavedAddress>) => Promise<SavedAddress>;
  setDefaultAddress: (id: number) => Promise<void>;
  deleteAddress: (id: number) => Promise<void>;
  categories: Category[];
  products: Product[];
  isLoadingCatalog: boolean;
  refreshCatalog: () => Promise<void>;
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
  createOrderWithBackend: (payload: CheckoutPayload) => Promise<ActiveOrder>;
  updateOrderStatus: (status: ActiveOrder['status']) => void;
  searchQuery: string;
  setSearchQuery: (query: string) => void;
  selectedCategory: string | null;
  setSelectedCategory: (catId: string | null) => void;
  customerTab: CustomerTab;
  setCustomerTab: (tab: CustomerTab) => void;
}

const AppContext = createContext<AppContextType | undefined>(undefined);

export const AppProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [role, setRole] = useState<AppRole>('customer');
  const [customerTab, setCustomerTab] = useState<CustomerTab>('home');
  const [categories, setCategories] = useState<Category[]>(CATEGORIES);
  const [products, setProducts] = useState<Product[]>(PRODUCTS);
  const [isLoadingCatalog, setIsLoadingCatalog] = useState(false);

  // Phase 5: Customer Auth & Profile State
  const [currentUser, setCurrentUser] = useState<UserProfile | null>({
    id: 1,
    name: 'Jeet Jajal',
    email: 'aditi@nexora.in',
    phone: '9876543210',
    status: 'ACTIVE',
    roles: ['ROLE_CUSTOMER'],
    accessToken: 'demo_token_authenticated',
  });
  const [savedAddresses, setSavedAddresses] = useState<SavedAddress[]>([]);
  const [pastOrders, setPastOrders] = useState<PastOrder[]>([]);
  const [isAuthOpen, setIsAuthOpen] = useState(false);
  const [isAccountOpen, setIsAccountOpen] = useState(false);

  const [cart, setCart] = useState<{ [productId: string]: number }>({});
  const [couponCode, setCouponCode] = useState<string>('');
  const [appliedDiscount, setAppliedDiscount] = useState<number>(0);
  const [isCartOpen, setIsCartOpen] = useState(false);
  const [isCheckoutOpen, setIsCheckoutOpen] = useState(false);
  const [isTrackingOpen, setIsTrackingOpen] = useState(false);
  const [isAIOpen, setIsAIOpen] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedCategory, setSelectedCategory] = useState<string | null>(null);

  // Active simulated or real order
  const [activeOrder, setActiveOrder] = useState<ActiveOrder | null>(null);

  // Fetch catalog from backend via API client
  const refreshCatalog = useCallback(async () => {
    setIsLoadingCatalog(true);
    try {
      const [fetchedCategories, fetchedProducts] = await Promise.all([
        ApiClient.getCategories(),
        ApiClient.getProducts(),
      ]);

      if (fetchedCategories.length > 0) setCategories(fetchedCategories);
      if (fetchedProducts.length > 0) setProducts(fetchedProducts);
    } catch {
      // Retain fallback data
    } finally {
      setIsLoadingCatalog(false);
    }
  }, []);

  // Fetch addresses and order history on mount
  useEffect(() => {
    refreshCatalog();

    // Fetch initial saved addresses
    ApiClient.getAddresses().then(addrs => {
      if (addrs && addrs.length > 0) setSavedAddresses(addrs);
    });

    // Fetch past orders
    ApiClient.getOrderHistory().then(orders => {
      if (orders && orders.length > 0) setPastOrders(orders);
    });
  }, [refreshCatalog]);

  // Auth: Login
  const login = async (email: string, password: string) => {
    const user = await ApiClient.login(email, password);
    setCurrentUser(user);
    if (typeof window !== 'undefined') {
      localStorage.setItem('nexora_user', JSON.stringify(user));
    }
  };

  // Auth: Register
  const register = async (name: string, email: string, phone: string, password: string) => {
    const user = await ApiClient.register(name, email, phone, password);
    setCurrentUser(user);
    if (typeof window !== 'undefined') {
      localStorage.setItem('nexora_user', JSON.stringify(user));
    }
  };

  // Auth: Update Profile
  const updateProfile = async (name: string, phone: string) => {
    const updated = await ApiClient.updateProfile(name, phone, currentUser?.accessToken);
    setCurrentUser(prev => prev ? { ...prev, name: updated.name, phone: updated.phone } : updated);
    if (typeof window !== 'undefined') {
      const stored = localStorage.getItem('nexora_user');
      if (stored) {
        const parsed = JSON.parse(stored);
        localStorage.setItem('nexora_user', JSON.stringify({ ...parsed, name: updated.name, phone: updated.phone }));
      }
    }
  };

  // Auth: Logout
  const logout = () => {
    setCurrentUser(null);
    if (typeof window !== 'undefined') {
      localStorage.removeItem('nexora_user');
    }
    setIsAccountOpen(false);
  };

  // Address: Add New Address
  const addNewAddress = async (addressData: Partial<SavedAddress>): Promise<SavedAddress> => {
    const saved = await ApiClient.addAddress(addressData, currentUser?.accessToken);
    setSavedAddresses(prev => [saved, ...prev]);
    return saved;
  };

  // Address: Set Default Address
  const setDefaultAddress = async (id: number) => {
    await ApiClient.setDefaultAddress(id, currentUser?.accessToken);
    setSavedAddresses(prev => prev.map(a => ({
      ...a,
      isDefault: a.id === id,
    })));
  };

  // Address: Delete Address
  const deleteAddress = async (id: number) => {
    await ApiClient.deleteAddress(id, currentUser?.accessToken);
    setSavedAddresses(prev => prev.filter(a => a.id !== id));
  };

  // Load starter items in cart for smooth first-time demo
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
        const product = products.find(p => p.id === id) || PRODUCTS.find(p => p.id === id);
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

  // Synchronous local order creation fallback
  const createOrder = (address: string, paymentMethod: string): ActiveOrder => {
    const items = getCartItems();
    const itemTotal = getCartTotal();
    const deliveryFee = itemTotal >= 199 ? 0 : 25;
    const handlingFee = 4;
    const finalTotal = Math.max(0, itemTotal + deliveryFee + handlingFee - appliedDiscount);

    const newOrder: ActiveOrder = {
      id: `NX-${Math.floor(100000 + Math.random() * 900000)}`,
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
      darkStore: 'Nexora Dark Store SG Highway #04',
      paymentMethod,
      createdAt: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
    };

    setActiveOrder(newOrder);

    // Append to past orders
    setPastOrders(prev => [
      {
        orderId: newOrder.id,
        date: 'Just now',
        itemsCount: items.length,
        itemsSummary: items.map(i => `${i.product.name} (${i.quantity})`).join(', '),
        amount: finalTotal,
        status: 'PLACED',
        darkStore: newOrder.darkStore || 'Nexora Dark Store SG Highway #04',
        deliveryOtp: newOrder.deliveryOtp,
        paymentMethod,
      },
      ...prev,
    ]);

    clearCart();
    setIsCheckoutOpen(false);
    setIsCartOpen(false);
    setIsTrackingOpen(true);
    return newOrder;
  };

  // Real backend order creation with Idempotency Key
  const createOrderWithBackend = async (payload: CheckoutPayload): Promise<ActiveOrder> => {
    const items = getCartItems();
    const itemTotal = getCartTotal();
    const deliveryFee = itemTotal >= 199 ? 0 : 25;
    const handlingFee = 4;
    const finalTotal = Math.max(0, itemTotal + deliveryFee + handlingFee - appliedDiscount);

    const idempotencyKey = typeof crypto !== 'undefined' && crypto.randomUUID
      ? crypto.randomUUID()
      : `idemp_${Date.now()}_${Math.random().toString(36).substring(2, 8)}`;

    const res = await ApiClient.placeOrder(payload, idempotencyKey);

    const fullAddress = `${payload.address.flat}, ${payload.address.street}, ${payload.address.city} - ${payload.address.pincode}`;

    const newOrder: ActiveOrder = {
      id: res.orderId,
      items,
      itemTotal,
      deliveryFee,
      handlingFee,
      discount: appliedDiscount,
      finalTotal: res.finalAmount || finalTotal,
      address: fullAddress,
      status: 'PLACED',
      riderName: res.assignedRider?.name || 'Ramesh Patel',
      riderPhone: res.assignedRider?.phone || '+91 98765 43210',
      riderRating: res.assignedRider?.rating || 4.92,
      etaMinutes: res.etaMinutes || 9,
      deliveryOtp: res.deliveryOtp,
      darkStore: res.darkStore || 'Nexora Dark Store SG Highway #04',
      paymentMethod: payload.paymentMethod,
      createdAt: new Date(res.createdAt || Date.now()).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
    };

    setActiveOrder(newOrder);

    // Append to past orders
    setPastOrders(prev => [
      {
        orderId: newOrder.id,
        date: 'Just now',
        itemsCount: items.length,
        itemsSummary: items.map(i => `${i.product.name} (${i.quantity})`).join(', '),
        amount: res.finalAmount || finalTotal,
        status: 'PLACED',
        darkStore: newOrder.darkStore || 'Nexora Dark Store SG Highway #04',
        deliveryOtp: newOrder.deliveryOtp,
        paymentMethod: payload.paymentMethod,
      },
      ...prev,
    ]);

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
        currentUser,
        savedAddresses,
        pastOrders,
        isAuthOpen,
        setIsAuthOpen,
        isAccountOpen,
        setIsAccountOpen,
        login,
        register,
        updateProfile,
        logout,
        addNewAddress,
        setDefaultAddress,
        deleteAddress,
        categories,
        products,
        isLoadingCatalog,
        refreshCatalog,
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
        createOrderWithBackend,
        updateOrderStatus,
        searchQuery,
        setSearchQuery,
        selectedCategory,
        setSelectedCategory,
        customerTab,
        setCustomerTab,
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
