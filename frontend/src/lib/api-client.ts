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
  paymentMethod: 'RAZORPAY' | 'COD';
  couponCode?: string;
}

export interface OrderResponse {
  orderId: string;
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

const IS_SERVER = typeof window === 'undefined';
const BACKEND_URL = process.env.NEXT_PUBLIC_BACKEND_URL || (IS_SERVER ? 'http://localhost:8080' : '');

export class ApiClient {
  private static async request<T>(endpoint: string, options: RequestInit = {}): Promise<T> {
    const url = endpoint.startsWith('http') ? endpoint : `${BACKEND_URL}${endpoint}`;
    const headers = {
      'Content-Type': 'application/json',
      'Accept': 'application/json',
      ...(options.headers || {}),
    };

    const res = await fetch(url, {
      ...options,
      headers,
      cache: options.cache || 'no-store',
    });

    if (!res.ok) {
      const errorData = await res.json().catch(() => ({ message: res.statusText }));
      throw new Error(errorData.message || `API Error: ${res.status}`);
    }

    return res.json();
  }

  // Categories
  static async getCategories(): Promise<Category[]> {
    try {
      const res = await fetch('/api/categories', { cache: 'no-store' });
      if (!res.ok) throw new Error('Failed to fetch categories');
      const json = await res.json();
      return json.data || [];
    } catch {
      // Fallback
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

  // Place Order
  static async placeOrder(payload: CheckoutPayload, idempotencyKey?: string): Promise<OrderResponse> {
    const res = await fetch('/api/orders', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Idempotency-Key': idempotencyKey || (typeof crypto !== 'undefined' ? crypto.randomUUID() : String(Date.now())),
      },
      body: JSON.stringify(payload),
    });

    if (!res.ok) {
      const err = await res.json().catch(() => ({ message: 'Failed to place order' }));
      throw new Error(err.message || 'Checkout failed');
    }

    const json = await res.json();
    return json.data;
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
