/**
 * Nexora Centralized Typed API Client
 * Connects Next.js Frontend to Spring Boot REST Backend (or Next.js API Routes Proxy)
 */

export interface Category {
  id: string;
  name: string;
  slug: string;
  icon: string;
  itemCount: number;
  highlight?: boolean;
}

export interface Product {
  id: string;
  name: string;
  categoryId: string;
  price: number;
  mrp: number;
  discount: number;
  unit: string;
  image: string;
  rating: number;
  reviewsCount: number;
  inStock: boolean;
  deliveryTime: string;
  badge?: string;
  brand?: string;
}

export interface UserProfile {
  id: number;
  name: string;
  email: string;
  phone: string;
  status: string;
  roles: string[];
  accessToken?: string;
}

export interface SavedAddress {
  id: number;
  label: string;
  addressLine1: string;
  addressLine2?: string;
  city: string;
  state?: string;
  pincode: string;
  country?: string;
  isDefault: boolean;
  serviceable?: boolean;
  darkStore?: string;
  eta?: string;
}

export interface PastOrder {
  orderId: string;
  date: string;
  itemsCount: number;
  itemsSummary: string;
  amount: number;
  status: string;
  darkStore: string;
  deliveryOtp: string;
  paymentMethod: string;
}

export interface CheckoutPayload {
  items: {
    productId: string;
    name: string;
    price: number;
    quantity: number;
  }[];
  address: {
    name: string;
    phone: string;
    flat: string;
    street: string;
    city: string;
    pincode: string;
    tag: 'HOME' | 'WORK' | 'OTHER';
  };
  deliveryAddressId?: number;
  paymentMethod: 'RAZORPAY' | 'COD';
  couponCode?: string;
}

export interface OrderResponse {
  orderId: string;
  numericId?: number;
  deliveryOtp: string;
  darkStore: string;
  etaMinutes: number;
  finalAmount: number;
  status: string;
  assignedRider?: {
    name: string;
    vehicle: string;
    phone: string;
    rating: number;
  };
  createdAt: string;
}

export interface TrackingTimelineEvent {
  status: string;
  title: string;
  description: string;
  timestamp: string;
  completed: boolean;
  current: boolean;
}

export interface OrderTrackingInfo {
  orderId: string;
  status: string;
  storeName: string;
  storeAddress: string;
  deliveryAddress: string;
  estimatedDeliveryMinutes: number;
  assignedRiderName: string;
  assignedRiderPhone: string;
  assignedRiderVehicle: string;
  deliveryOtp?: string;
  timeline: TrackingTimelineEvent[];
  createdAt: string;
}

const IS_SERVER = typeof window === 'undefined';
const BACKEND_URL = process.env.NEXT_PUBLIC_BACKEND_URL || (IS_SERVER ? 'http://localhost:8080' : '');

export class ApiClient {
  // Categories
  static async getCategories(): Promise<Category[]> {
    try {
      const res = await fetch('/api/categories', { cache: 'no-store' });
      if (!res.ok) throw new Error('Failed to fetch categories');
      const json = await res.json();
      return json.data || [];
    } catch {
      return [];
    }
  }

  // Products
  static async getProducts(params?: { categoryId?: string; search?: string }): Promise<Product[]> {
    try {
      const searchParams = new URLSearchParams();
      if (params?.categoryId) searchParams.set('categoryId', params.categoryId);
      if (params?.search) searchParams.set('q', params.search);

      const res = await fetch(`/api/products?${searchParams.toString()}`, { cache: 'no-store' });
      if (!res.ok) throw new Error('Failed to fetch products');
      const json = await res.json();
      return json.data || [];
    } catch {
      return [];
    }
  }

  // Auth: Login
  static async login(email: string, password: string): Promise<UserProfile> {
    const res = await fetch('/api/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email, password }),
    });

    const json = await res.json();
    if (!res.ok) {
      throw new Error(json.message || 'Login failed');
    }

    return json.data;
  }

  // Auth: Register
  static async register(name: string, email: string, phone: string, password: string): Promise<UserProfile> {
    const res = await fetch('/api/auth/register', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ name, email, phone, password }),
    });

    const json = await res.json();
    if (!res.ok) {
      throw new Error(json.message || 'Registration failed');
    }

    return json.data;
  }

  // Auth: Get Current Profile
  static async getMe(token: string): Promise<UserProfile> {
    const res = await fetch('/api/auth/me', {
      headers: { 'Authorization': `Bearer ${token}` },
    });

    const json = await res.json();
    if (!res.ok) {
      throw new Error(json.message || 'Failed to fetch user profile');
    }

    return json.data;
  }

  // Auth: Update Profile
  static async updateProfile(name: string, phone: string, token?: string): Promise<UserProfile> {
    const headers: Record<string, string> = { 'Content-Type': 'application/json' };
    if (token) headers['Authorization'] = `Bearer ${token}`;

    const res = await fetch('/api/auth/me', {
      method: 'PUT',
      headers,
      body: JSON.stringify({ name, phone }),
    });

    const json = await res.json();
    if (!res.ok) {
      throw new Error(json.message || 'Failed to update profile');
    }

    return json.data;
  }

  // Addresses: List Saved Addresses
  static async getAddresses(token?: string): Promise<SavedAddress[]> {
    try {
      const headers: Record<string, string> = {};
      if (token) headers['Authorization'] = `Bearer ${token}`;

      const res = await fetch('/api/addresses', { headers });
      if (!res.ok) throw new Error('Failed to fetch addresses');
      const json = await res.json();
      return json.data || [];
    } catch {
      return [];
    }
  }

  // Addresses: Add New Address with Serviceability Check
  static async addAddress(addressData: Partial<SavedAddress>, token?: string): Promise<SavedAddress> {
    const headers: Record<string, string> = { 'Content-Type': 'application/json' };
    if (token) headers['Authorization'] = `Bearer ${token}`;

    const res = await fetch('/api/addresses', {
      method: 'POST',
      headers,
      body: JSON.stringify(addressData),
    });

    const json = await res.json();
    if (!res.ok) {
      throw new Error(json.message || 'Address could not be added');
    }

    return json.data;
  }

  // Addresses: Set as Default Address
  static async setDefaultAddress(id: number, token?: string): Promise<void> {
    const headers: Record<string, string> = { 'Content-Type': 'application/json' };
    if (token) headers['Authorization'] = `Bearer ${token}`;

    const res = await fetch('/api/addresses', {
      method: 'PATCH',
      headers,
      body: JSON.stringify({ id, isDefault: true }),
    });

    if (!res.ok) {
      const json = await res.json().catch(() => ({}));
      throw new Error(json.message || 'Failed to set default address');
    }
  }

  // Addresses: Delete Address
  static async deleteAddress(id: number, token?: string): Promise<void> {
    const headers: Record<string, string> = {};
    if (token) headers['Authorization'] = `Bearer ${token}`;

    const res = await fetch(`/api/addresses?id=${id}`, {
      method: 'DELETE',
      headers,
    });

    if (!res.ok) {
      const json = await res.json().catch(() => ({}));
      throw new Error(json.message || 'Failed to delete address');
    }
  }

  // Orders: Past Order History
  static async getOrderHistory(token?: string): Promise<PastOrder[]> {
    try {
      const headers: Record<string, string> = {};
      if (token) headers['Authorization'] = `Bearer ${token}`;

      const res = await fetch('/api/orders/history', { headers });
      if (!res.ok) throw new Error('Failed to fetch orders');
      const json = await res.json();
      return json.data || [];
    } catch {
      return [];
    }
  }

  // Orders: Place Order
  static async placeOrder(payload: CheckoutPayload, idempotencyKey?: string, token?: string): Promise<OrderResponse> {
    const headers: Record<string, string> = {
      'Content-Type': 'application/json',
      'Idempotency-Key': idempotencyKey || (typeof crypto !== 'undefined' && crypto.randomUUID ? crypto.randomUUID() : String(Date.now())),
    };
    if (token) headers['Authorization'] = `Bearer ${token}`;

    const res = await fetch('/api/orders', {
      method: 'POST',
      headers,
      body: JSON.stringify(payload),
    });

    if (!res.ok) {
      const err = await res.json().catch(() => ({ message: 'Failed to place order' }));
      throw new Error(err.message || 'Checkout failed');
    }

    const json = await res.json();
    return json.data;
  }

  // Orders: Real-time Order Tracking
  static async trackOrder(orderId: string, token?: string): Promise<OrderTrackingInfo> {
    const headers: Record<string, string> = {};
    if (token) headers['Authorization'] = `Bearer ${token}`;

    const res = await fetch(`/api/orders/${orderId}/track`, { headers, cache: 'no-store' });
    if (!res.ok) {
      throw new Error('Failed to fetch order tracking');
    }

    const json = await res.json();
    return json.data;
  }

  // Orders: Cancel Order
  static async cancelOrder(orderId: string, token?: string): Promise<void> {
    const headers: Record<string, string> = { 'Content-Type': 'application/json' };
    if (token) headers['Authorization'] = `Bearer ${token}`;

    const res = await fetch(`/api/orders/${orderId}/cancel`, {
      method: 'POST',
      headers,
    });

    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new Error(err.message || 'Failed to cancel order');
    }
  }

  // Orders: Verify Delivery OTP (Delivery Partner Doorstep Handshake)
  static async verifyDeliveryOtp(orderId: string, otp: string, token?: string): Promise<void> {
    const headers: Record<string, string> = { 'Content-Type': 'application/json' };
    if (token) headers['Authorization'] = `Bearer ${token}`;

    const res = await fetch(`/api/orders/${orderId}/verify-delivery`, {
      method: 'POST',
      headers,
      body: JSON.stringify({ otp }),
    });

    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new Error(err.message || 'Delivery OTP verification failed');
    }
  }

  // Razorpay: Create Payment Order
  static async createRazorpayOrder(amount: number, receipt: string): Promise<{
    id: string;
    amount: number;
    currency: string;
    keyId: string;
  }> {
    const res = await fetch('/api/payments/razorpay/create-order', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ amount, receipt }),
    });

    if (!res.ok) {
      throw new Error('Failed to create Razorpay order');
    }

    const json = await res.json();
    return json.data;
  }

  // Razorpay: Verify Payment Signature
  static async verifyRazorpayPayment(payload: {
    razorpay_order_id: string;
    razorpay_payment_id: string;
    razorpay_signature: string;
  }): Promise<{ verified: boolean; message: string }> {
    const res = await fetch('/api/payments/razorpay/verify', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload),
    });

    if (!res.ok) {
      throw new Error('Payment verification failed');
    }

    return res.json();
  }
}
