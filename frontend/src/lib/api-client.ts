/**
 * Nexora Centralized Typed API Client
 * Connects Next.js Frontend to Spring Boot REST Backend (or Next.js API Routes Proxy)
 */

export interface Subcategory {
  id: string;
  name: string;
  slug: string;
  examples: string[];
}

export interface Category {
  id: string;
  name: string;
  slug: string;
  icon: string;
  itemCount: number;
  highlight?: boolean;
  subcategories?: Subcategory[];
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
  assignedRiderLocation?: { lat: number; lng: number };
  speedKmh?: number;
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

  // Subcategories
  static async getSubcategories(categoryId?: string): Promise<Subcategory[]> {
    try {
      const url = categoryId ? `/api/subcategories?categoryId=${categoryId}` : '/api/subcategories';
      const res = await fetch(url, { cache: 'no-store' });
      if (!res.ok) throw new Error('Failed to fetch subcategories');
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

  // Orders: Real-time Live SSE Order Stream
  static subscribeToOrderStream(
    orderId: string,
    onLocation: (location: any) => void,
    onStatus: (status: any) => void,
    onError: (err: any) => void,
    token?: string
  ): () => void {
    if (typeof window === 'undefined' || !('EventSource' in window)) {
      onError(new Error('EventSource not supported'));
      return () => {};
    }

    const url = `/api/orders/${orderId}/track/stream${token ? `?token=${encodeURIComponent(token)}` : ''}`;
    const eventSource = new EventSource(url);

    eventSource.addEventListener('init', (e: MessageEvent) => {
      try {
        const data = JSON.parse(e.data);
        if (data.location) onLocation(data.location);
        if (data.status) onStatus(data.status);
      } catch {}
    });

    eventSource.addEventListener('location', (e: MessageEvent) => {
      try {
        const data = JSON.parse(e.data);
        onLocation(data);
      } catch {}
    });

    eventSource.addEventListener('status', (e: MessageEvent) => {
      try {
        const data = JSON.parse(e.data);
        onStatus(data);
      } catch {}
    });

    eventSource.onerror = (err) => {
      onError(err);
    };

    return () => {
      eventSource.close();
    };
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
    mode?: 'LIVE_GATEWAY' | 'SANDBOX_SIMULATION';
  }> {
    const res = await fetch('/api/payment/create-order', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ amount, receipt }),
    });

    if (!res.ok) {
      throw new Error('Failed to create Razorpay order');
    }

    const json = await res.json();
    return {
      ...json.data,
      mode: json.mode,
    };
  }

  // Razorpay: Verify Payment Signature
  static async verifyRazorpayPayment(payload: {
    razorpay_order_id: string;
    razorpay_payment_id: string;
    razorpay_signature: string;
  }): Promise<{ verified: boolean; message: string }> {
    const res = await fetch('/api/payment/verify', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload),
    });

    if (!res.ok) {
      throw new Error('Payment verification failed');
    }

    return res.json();
  }

  // Phase 7: Store Operations — Order Queue & Metrics
  static async getStoreQueue(storeId = 4, token?: string): Promise<StoreQueueMetrics> {
    const headers: Record<string, string> = { 'Content-Type': 'application/json' };
    if (token) headers['Authorization'] = `Bearer ${token}`;

    const res = await fetch(`/api/store/queue?storeId=${storeId}`, { headers });
    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new Error(err.message || 'Failed to fetch store operations queue');
    }
    const json = await res.json();
    return json.data;
  }

  // Phase 7: Store Operations — Store Inventory
  static async getStoreInventory(storeId = 4, token?: string): Promise<StoreInventoryItem[]> {
    const headers: Record<string, string> = { 'Content-Type': 'application/json' };
    if (token) headers['Authorization'] = `Bearer ${token}`;

    const res = await fetch(`/api/store/inventory?storeId=${storeId}`, { headers });
    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new Error(err.message || 'Failed to fetch store inventory');
    }
    const json = await res.json();
    return json.data;
  }

  // Phase 7: Store Operations — Stock Adjustment
  static async adjustStoreStock(
    storeId: number,
    payload: { productId: number; amount: number; action: string; reason: string },
    token?: string
  ): Promise<StoreInventoryItem> {
    const headers: Record<string, string> = { 'Content-Type': 'application/json' };
    if (token) headers['Authorization'] = `Bearer ${token}`;

    const res = await fetch(`/api/store/inventory/adjust?storeId=${storeId}`, {
      method: 'POST',
      headers,
      body: JSON.stringify(payload),
    });
    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new Error(err.message || 'Failed to adjust stock');
    }
    const json = await res.json();
    return json.data;
  }

  // Phase 7: Picker Workflow — Start Picking Order
  static async startPicking(orderId: number, token?: string): Promise<StoreQueueOrder> {
    const headers: Record<string, string> = { 'Content-Type': 'application/json' };
    if (token) headers['Authorization'] = `Bearer ${token}`;

    const res = await fetch(`/api/store/picker/start?orderId=${orderId}`, {
      method: 'POST',
      headers,
    });
    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new Error(err.message || 'Failed to start picking');
    }
    const json = await res.json();
    return json.data;
  }

  // Phase 7: Picker Workflow — Pick Item
  static async pickItem(orderId: number, itemId: number, barcode?: string, token?: string): Promise<any> {
    const headers: Record<string, string> = { 'Content-Type': 'application/json' };
    if (token) headers['Authorization'] = `Bearer ${token}`;

    const res = await fetch(`/api/store/picker/item?orderId=${orderId}&itemId=${itemId}`, {
      method: 'POST',
      headers,
      body: JSON.stringify({ barcode }),
    });
    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new Error(err.message || 'Failed to pick item');
    }
    const json = await res.json();
    return json.data;
  }

  // Phase 7: Picker Workflow — Complete Picking
  static async completePicking(orderId: number, token?: string): Promise<StoreQueueOrder> {
    const headers: Record<string, string> = { 'Content-Type': 'application/json' };
    if (token) headers['Authorization'] = `Bearer ${token}`;

    const res = await fetch(`/api/store/picker/complete?orderId=${orderId}`, {
      method: 'POST',
      headers,
    });
    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new Error(err.message || 'Failed to complete picking');
    }
    const json = await res.json();
    return json.data;
  }

  // Phase 7: Packer Workflow — Complete Packing & Hand over to Dispatch
  static async completePacking(orderId: number, bagCode: string, token?: string): Promise<StoreQueueOrder> {
    const headers: Record<string, string> = { 'Content-Type': 'application/json' };
    if (token) headers['Authorization'] = `Bearer ${token}`;

    const res = await fetch(`/api/store/packing/complete?orderId=${orderId}`, {
      method: 'POST',
      headers,
      body: JSON.stringify({ bagCode }),
    });
    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new Error(err.message || 'Failed to complete packing');
    }
    const json = await res.json();
    return json.data;
  }

  // Phase 7: Delivery App — Retrieve Partner Deliveries
  static async getMyDeliveryAssignments(token?: string): Promise<DeliveryAssignmentDetail[]> {
    const headers: Record<string, string> = { 'Content-Type': 'application/json' };
    if (token) headers['Authorization'] = `Bearer ${token}`;

    const res = await fetch('/api/rider/assignments', { headers });
    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new Error(err.message || 'Failed to retrieve delivery assignments');
    }
    const json = await res.json();
    return json.data;
  }

  // Phase 7: Delivery App — Pickup from Dark Store
  static async pickupDeliveryAssignment(assignmentId: number, token?: string): Promise<DeliveryAssignmentDetail> {
    const headers: Record<string, string> = { 'Content-Type': 'application/json' };
    if (token) headers['Authorization'] = `Bearer ${token}`;

    const res = await fetch(`/api/rider/assignments/${assignmentId}/pickup`, {
      method: 'POST',
      headers,
    });
    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new Error(err.message || 'Failed to mark delivery pickup');
    }
    const json = await res.json();
    return json.data;
  }

  // Phase 8: Super Admin Control Tower Overview
  static async getControlTowerOverview(token?: string): Promise<ControlTowerOverview> {
    const headers: Record<string, string> = { 'Content-Type': 'application/json' };
    if (token) headers['Authorization'] = `Bearer ${token}`;

    const res = await fetch('/api/admin/control-tower', { headers });
    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new Error(err.message || 'Failed to fetch control tower overview');
    }
    const json = await res.json();
    return json.data;
  }

  // Phase 8: Fleet Monitoring
  static async getFleetStatus(token?: string): Promise<FleetRiderStatus[]> {
    const headers: Record<string, string> = { 'Content-Type': 'application/json' };
    if (token) headers['Authorization'] = `Bearer ${token}`;

    const res = await fetch('/api/admin/fleet', { headers });
    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new Error(err.message || 'Failed to fetch fleet status');
    }
    const json = await res.json();
    return json.data;
  }

  // Phase 8: Disputes & Refunds
  static async getDisputes(status?: string, token?: string): Promise<DisputeRecord[]> {
    const headers: Record<string, string> = { 'Content-Type': 'application/json' };
    if (token) headers['Authorization'] = `Bearer ${token}`;

    const url = status ? `/api/admin/disputes?status=${status}` : '/api/admin/disputes';
    const res = await fetch(url, { headers });
    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new Error(err.message || 'Failed to fetch disputes');
    }
    const json = await res.json();
    return json.data;
  }

  static async createDispute(
    payload: { orderId: number; reason: string; customerNotes?: string; claimAmount: number },
    token?: string
  ): Promise<DisputeRecord> {
    const headers: Record<string, string> = { 'Content-Type': 'application/json' };
    if (token) headers['Authorization'] = `Bearer ${token}`;

    const res = await fetch('/api/admin/disputes', {
      method: 'POST',
      headers,
      body: JSON.stringify(payload),
    });
    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new Error(err.message || 'Failed to create dispute claim');
    }
    const json = await res.json();
    return json.data;
  }

  static async processDisputeDecision(
    disputeId: number,
    payload: { decision: string; decisionNotes: string },
    token?: string
  ): Promise<DisputeRecord> {
    const headers: Record<string, string> = { 'Content-Type': 'application/json' };
    if (token) headers['Authorization'] = `Bearer ${token}`;

    const res = await fetch(`/api/admin/disputes/${disputeId}/decision`, {
      method: 'POST',
      headers,
      body: JSON.stringify(payload),
    });
    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new Error(err.message || 'Failed to process dispute decision');
    }
    const json = await res.json();
    return json.data;
  }

  // Phase 8: Surge Pricing Rules
  static async getSurgePricingRules(token?: string): Promise<SurgePricingRuleDto[]> {
    const headers: Record<string, string> = { 'Content-Type': 'application/json' };
    if (token) headers['Authorization'] = `Bearer ${token}`;

    const res = await fetch('/api/admin/surge-pricing', { headers });
    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new Error(err.message || 'Failed to fetch surge pricing');
    }
    const json = await res.json();
    return json.data;
  }

  static async updateSurgePricingRule(payload: SurgePricingRuleDto, token?: string): Promise<SurgePricingRuleDto> {
    const headers: Record<string, string> = { 'Content-Type': 'application/json' };
    if (token) headers['Authorization'] = `Bearer ${token}`;

    const res = await fetch('/api/admin/surge-pricing', {
      method: 'PUT',
      headers,
      body: JSON.stringify(payload),
    });
    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new Error(err.message || 'Failed to update surge pricing rule');
    }
    const json = await res.json();
    return json.data;
  }

  // Phase 8: Catalog Price Override
  static async overrideProductPrice(
    productId: number,
    payload: { overridePrice: number; overrideDiscount?: number; reason: string },
    token?: string
  ): Promise<any> {
    const headers: Record<string, string> = { 'Content-Type': 'application/json' };
    if (token) headers['Authorization'] = `Bearer ${token}`;

    const res = await fetch(`/api/admin/catalog/products/${productId}/price-override`, {
      method: 'POST',
      headers,
      body: JSON.stringify(payload),
    });
    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new Error(err.message || 'Failed to apply price override');
    }
    const json = await res.json();
    return json.data;
  }

  // Phase 8: Bulk Catalog Import
  static async bulkImportCatalog(csvContent: string, token?: string): Promise<BulkImportResult> {
    const headers: Record<string, string> = { 'Content-Type': 'text/plain' };
    if (token) headers['Authorization'] = `Bearer ${token}`;

    const res = await fetch('/api/admin/catalog/bulk-import', {
      method: 'POST',
      headers,
      body: csvContent,
    });
    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new Error(err.message || 'Bulk catalog import failed');
    }
    const json = await res.json();
    return json.data;
  }
  static async getAnalyticsOverview(startDate?: string, endDate?: string, storeId?: number, token?: string): Promise<AnalyticsOverviewResponse> {
    const params = new URLSearchParams();
    if (startDate) params.append('startDate', startDate);
    if (endDate) params.append('endDate', endDate);
    if (storeId) params.append('storeId', storeId.toString());

    const headers: Record<string, string> = {};
    if (token) headers['Authorization'] = `Bearer ${token}`;

    const res = await fetch(`/api/admin/analytics/overview?${params.toString()}`, { headers });
    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new Error(err.message || 'Failed to load analytics overview');
    }
    const json = await res.json();
    return json.data;
  }

  static async getAdminNotifications(token?: string): Promise<NotificationLogItem[]> {
    const headers: Record<string, string> = {};
    if (token) headers['Authorization'] = `Bearer ${token}`;

    const res = await fetch('/api/admin/notifications', { headers });
    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new Error(err.message || 'Failed to load admin notifications');
    }
    const json = await res.json();
    return json.data;
  }

  static async getNotificationProviderStatus(token?: string): Promise<Record<string, { channel: string; isConfigured: boolean }>> {
    const headers: Record<string, string> = {};
    if (token) headers['Authorization'] = `Bearer ${token}`;

    const res = await fetch('/api/admin/notifications/providers', { headers });
    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new Error(err.message || 'Failed to load notification providers');
    }
    const json = await res.json();
    return json.data;
  }

  static async getHealthCheck(): Promise<any> {
    const res = await fetch('/api/health');
    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new Error(err.message || 'Health check failed');
    }
    const json = await res.json();
    return json.data;
  }

  // Feature 2: Smart Multi-Order Delivery Batching
  static async getDeliveryBatches(storeId?: number, token?: string): Promise<DeliveryBatch[]> {
    const headers: Record<string, string> = { 'Content-Type': 'application/json' };
    if (token) headers['Authorization'] = `Bearer ${token}`;
    const url = storeId ? `/api/admin/batches?storeId=${storeId}` : '/api/admin/batches';
    const res = await fetch(url, { headers });
    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new Error(err.message || 'Failed to load delivery batches');
    }
    const json = await res.json();
    return json.data || [];
  }

  static async generateDeliveryBatches(storeId?: number, maxBatchSize = 3, maxProximityKm = 2.5, token?: string): Promise<DeliveryBatch[]> {
    const headers: Record<string, string> = { 'Content-Type': 'application/json' };
    if (token) headers['Authorization'] = `Bearer ${token}`;
    const res = await fetch('/api/admin/batches/generate', {
      method: 'POST',
      headers,
      body: JSON.stringify({ storeId, maxBatchSize, maxProximityKm }),
    });
    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new Error(err.message || 'Failed to generate delivery batches');
    }
    const json = await res.json();
    return json.data || [];
  }

  static async assignDeliveryBatch(batchId: number, deliveryPartnerId: number, token?: string): Promise<DeliveryBatch> {
    const headers: Record<string, string> = { 'Content-Type': 'application/json' };
    if (token) headers['Authorization'] = `Bearer ${token}`;
    const res = await fetch(`/api/admin/batches/${batchId}/assign`, {
      method: 'POST',
      headers,
      body: JSON.stringify({ deliveryPartnerId }),
    });
    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new Error(err.message || 'Failed to assign delivery batch');
    }
    const json = await res.json();
    return json.data;
  }

  static async dispatchDeliveryBatch(batchId: number, token?: string): Promise<DeliveryBatch> {
    const headers: Record<string, string> = { 'Content-Type': 'application/json' };
    if (token) headers['Authorization'] = `Bearer ${token}`;
    const res = await fetch(`/api/admin/batches/${batchId}/dispatch`, {
      method: 'POST',
      headers,
    });
    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new Error(err.message || 'Failed to dispatch delivery batch');
    }
    const json = await res.json();
    return json.data;
  }

  static async cancelDeliveryBatch(batchId: number, token?: string): Promise<DeliveryBatch> {
    const headers: Record<string, string> = { 'Content-Type': 'application/json' };
    if (token) headers['Authorization'] = `Bearer ${token}`;
    const res = await fetch(`/api/admin/batches/${batchId}/cancel`, {
      method: 'POST',
      headers,
    });
    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new Error(err.message || 'Failed to cancel delivery batch');
    }
    const json = await res.json();
    return json.data;
  }

  // Feature 3: WhatsApp Conversational Commerce AI Bot
  static async simulateWhatsAppMessage(phone: string, customerName: string, message: string): Promise<WhatsAppBotResponse> {
    const res = await fetch('/api/whatsapp/simulate', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ phone, customerName, message }),
    });
    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new Error(err.message || 'WhatsApp AI processing failed');
    }
    const json = await res.json();
    return json.data;
  }

  static async getWhatsAppConversations(): Promise<WhatsAppConversationSummary[]> {
    const res = await fetch('/api/whatsapp/conversations');
    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new Error(err.message || 'Failed to fetch WhatsApp sessions');
    }
    const json = await res.json();
    return json.data || [];
  }

  // Feature 4: Cold Chain IoT Telemetry & FEFO Batch Inventory
  static async getStoreColdChainBatches(storeId = 4, token?: string): Promise<InventoryBatchResponse[]> {
    const headers: Record<string, string> = {};
    if (token) headers['Authorization'] = `Bearer ${token}`;
    const res = await fetch(`/api/store/coldchain/batches?storeId=${storeId}`, { headers });
    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new Error(err.message || 'Failed to load cold chain batches');
    }
    const json = await res.json();
    return json.data || [];
  }

  static async toggleBatchQuarantine(batchId: number, quarantined: boolean, token?: string): Promise<InventoryBatchResponse> {
    const headers: Record<string, string> = { 'Content-Type': 'application/json' };
    if (token) headers['Authorization'] = `Bearer ${token}`;
    const res = await fetch(`/api/store/coldchain/batches?batchId=${batchId}&quarantined=${quarantined}`, {
      method: 'POST',
      headers,
    });
    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new Error(err.message || 'Failed to update quarantine status');
    }
    const json = await res.json();
    return json.data;
  }

  static async recordColdChainTelemetry(storeId: number, storageUnit: string, temperatureCelsius: number, humidityPercentage = 60.0): Promise<TemperatureTelemetryResponse> {
    const res = await fetch('/api/store/coldchain/telemetry', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ storeId, storageUnit, temperatureCelsius, humidityPercentage }),
    });
    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new Error(err.message || 'Failed to record temperature telemetry');
    }
    const json = await res.json();
    return json.data;
  }

  static async getColdChainOverview(storeId = 4, token?: string): Promise<ColdChainOverviewResponse> {
    const headers: Record<string, string> = {};
    if (token) headers['Authorization'] = `Bearer ${token}`;
    const res = await fetch(`/api/store/coldchain/overview?storeId=${storeId}`, { headers });
    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new Error(err.message || 'Failed to load cold chain overview');
    }
    const json = await res.json();
    return json.data;
  }

  // Feature 5: Rider Shift Selfie & Biometric KYC
  static async getShiftVerificationStatus(token?: string): Promise<RiderShiftVerificationStatus> {
    const headers: Record<string, string> = {};
    if (token) headers['Authorization'] = `Bearer ${token}`;
    const res = await fetch('/api/delivery/verification/status', { headers });
    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new Error(err.message || 'Failed to fetch shift verification status');
    }
    const json = await res.json();
    return json.data;
  }

  static async submitShiftSelfie(
    payload: { imageBase64: string; latitude?: number; longitude?: number },
    token?: string
  ): Promise<RiderShiftVerificationStatus> {
    const headers: Record<string, string> = { 'Content-Type': 'application/json' };
    if (token) headers['Authorization'] = `Bearer ${token}`;
    const res = await fetch('/api/delivery/verification/selfie', {
      method: 'POST',
      headers,
      body: JSON.stringify(payload),
    });
    const json = await res.json();
    if (!res.ok) {
      throw new Error(json.message || 'Selfie verification rejected');
    }
    return json.data;
  }

  static async supervisorOverrideShiftVerification(
    payload: { deliveryPartnerId: number; reason: string },
    token?: string
  ): Promise<RiderShiftVerificationStatus> {
    const headers: Record<string, string> = { 'Content-Type': 'application/json' };
    if (token) headers['Authorization'] = `Bearer ${token}`;
    const res = await fetch('/api/delivery/verification/override', {
      method: 'POST',
      headers,
      body: JSON.stringify(payload),
    });
    const json = await res.json();
    if (!res.ok) {
      throw new Error(json.message || 'Supervisor override failed');
    }
    return json.data;
  }
}

export interface RiderShiftVerificationStatus {
  id: number | null;
  deliveryPartnerId: number;
  riderName: string;
  shiftDate: string;
  status: 'PENDING' | 'APPROVED' | 'REJECTED' | 'SUPERVISOR_OVERRIDE';
  livenessScore?: number | null;
  matchScore?: number | null;
  verifiedAt?: string | null;
  isShiftActive: boolean;
  rejectionReason?: string | null;
}

export interface StoreQueueOrder {
  id: number;
  orderNumber?: string;
  status: string;
  subtotal: number;
  deliveryFee: number;
  totalAmount: number;
  paymentMethod: string;
  paymentStatus: string;
  items: {
    id: number;
    productId: number;
    productName: string;
    unitPrice: number;
    quantity: number;
    subtotal: number;
    isPicked: boolean;
    sku?: string;
  }[];
  pickerName?: string;
  packerName?: string;
  packingBagCode?: string;
  createdAt: string;
}

export interface StoreQueueMetrics {
  storeId: number;
  storeName: string;
  totalOrders: number;
  confirmedCount: number;
  preparingCount: number;
  readyCount: number;
  outForDeliveryCount: number;
  deliveredCount: number;
  lowStockCount: number;
  averageProcessingMinutes: number;
  orders: StoreQueueOrder[];
}

export interface StoreInventoryItem {
  productId: number;
  productName: string;
  sku: string;
  unit: string;
  price: number;
  quantity: number;
  lowStockThreshold: number;
  status: 'IN_STOCK' | 'LOW_STOCK' | 'OUT_OF_STOCK';
}

export interface DeliveryAssignmentDetail {
  assignmentId: number;
  orderId: number;
  orderNumber: string;
  storeId?: number;
  storeName: string;
  storeAddress: string;
  storePhone: string;
  customerName: string;
  customerPhone: string;
  deliveryAddress: string;
  subtotal: number;
  deliveryFee: number;
  totalAmount: number;
  paymentMethod: string;
  paymentStatus: string;
  itemCount: number;
  packingBagCode?: string;
  assignmentStatus: 'ASSIGNED' | 'ACCEPTED' | 'PICKED_UP' | 'DELIVERED' | 'REJECTED';
  orderStatus: string;
  assignedAt: string;
  acceptedAt?: string;
  completedAt?: string;
}



export interface HubOperationalStatus {
  hubId: number;
  hubName: string;
  code: string;
  city: string;
  activeOrdersCount: number;
  pickersActive: number;
  ridersOnline: number;
  avgPickTimeSeconds: number;
  healthStatus: 'OPTIMAL' | 'ELEVATED' | 'CRITICAL_DELAY';
}

export interface DelayedOrderAlert {
  orderId: number;
  orderNumber: string;
  storeName: string;
  customerName: string;
  status: string;
  amount: number;
  elapsedSeconds: number;
  targetSlaSeconds: number;
  delayedBySeconds: number;
  createdAt: string;
}

export interface ControlTowerOverview {
  totalOrders: number;
  activeOrders: number;
  delayedOrdersCount: number;
  cancelledOrders: number;
  refundedOrders: number;
  grossMerchandiseValue: number;
  deliveryFeesCollected: number;
  netPlatformRevenue: number;
  averageDeliveryTimeMinutes: number;
  slaAdherencePercent: number;
  activeHubsCount: number;
  onlineRidersCount: number;
  activeDisputesCount: number;
  hubStatusList: HubOperationalStatus[];
  delayedOrders: DelayedOrderAlert[];
}

export interface FleetRiderStatus {
  partnerId: number;
  userId?: number;
  name: string;
  phone: string;
  vehicleType: string;
  rating: number;
  isOnline: boolean;
  currentStatus: 'AVAILABLE' | 'BUSY' | 'OFFLINE';
  activeOrderId?: number;
  currentLat?: number;
  currentLng?: number;
  todayEarnings: number;
  lastPingTime: string;
  isStaleLocation: boolean;
}

export interface DisputeRecord {
  id: number;
  orderId: number;
  orderNumber: string;
  customerId: number;
  customerName: string;
  customerPhone: string;
  reason: string;
  customerNotes?: string;
  claimAmount: number;
  status: 'OPEN' | 'UNDER_REVIEW' | 'APPROVED_REFUNDED' | 'REJECTED';
  decisionNotes?: string;
  handledBy?: string;
  refundTransactionId?: string;
  createdAt: string;
  resolvedAt?: string;
}

export interface SurgePricingRuleDto {
  id?: number;
  hubId: number;
  hubName?: string;
  zoneName?: string;
  isActive: boolean;
  multiplier: number;
  extraFlatFee: number;
  reason?: string;
  updatedBy?: string;
  updatedAt?: string;
}

export interface BulkImportResult {
  totalRows: number;
  successCount: number;
  failureCount: number;
  duplicateCount: number;
  importedProductIds: number[];
  validationErrors: string[];
}

export interface AnalyticsOverviewResponse {
  startDate: string;
  endDate: string;
  storeId?: number;
  totalOrders: number;
  grossMerchandiseValue: number;
  netPlatformRevenue: number;
  totalDeliveryFees: number;
  totalDiscounts: number;
  averageOrderValue: number;
  slaComplianceRatePercent: number;
  averageDeliveryTimeSeconds: number;
  averageFulfillmentTimeSeconds: number;
  totalCancellations: number;
  totalDisputes: number;
  totalRefundsAmount: number;
  paymentReconciliation: {
    gatewayCollected: number;
    refundsDisbursed: number;
    netSettled: number;
  };
  topSellingProducts: {
    productId: number;
    productName: string;
    categoryName: string;
    unitsSold: number;
    revenueGenerated: number;
  }[];
  categoryPerformance: {
    categoryId: number;
    categoryName: string;
    unitsSold: number;
    grossRevenue: number;
  }[];
  storeFulfillmentMetrics: {
    storeId: number;
    storeName: string;
    city: string;
    orderCount: number;
    gmv: number;
    slaCompliancePercent: number;
    avgDeliveryTimeSeconds: number;
    avgFulfillmentTimeSeconds: number;
    outOfStockCount: number;
  }[];
}

export interface NotificationLogItem {
  id: number;
  orderId?: number;
  recipient: string;
  channel: 'EMAIL' | 'SMS' | 'PUSH';
  eventType: string;
  title: string;
  sanitizedBody: string;
  deliveryStatus: 'DELIVERED' | 'FAILED' | 'QUEUED_UNCONFIGURED' | 'SIMULATED_LOCAL';
  providerResponse?: string;
  deduplicationKey?: string;
  retryCount: number;
  createdAt: string;
  deliveredAt?: string;
}


export interface BatchOrderStop {
  orderId: number;
  orderNumber: string;
  sequenceIndex: number;
  customerName: string;
  customerPhone?: string;
  deliveryAddress: string;
  latitude?: number;
  longitude?: number;
  totalAmount: number;
  status: string;
  estimatedDeliveryTime?: string;
  actualDeliveryTime?: string;
}

export interface DeliveryBatch {
  id: number;
  batchCode: string;
  storeId: number;
  storeName: string;
  deliveryPartnerId?: number;
  riderName?: string;
  riderPhone?: string;
  status: 'CREATED' | 'ASSIGNED' | 'IN_TRANSIT' | 'COMPLETED' | 'CANCELLED';
  orderCount: number;
  totalDistanceKm: number;
  estimatedDurationMins: number;
  stops: BatchOrderStop[];
  dispatchedAt?: string;
  completedAt?: string;
  createdAt: string;
  updatedAt: string;
}


export interface WhatsAppBotResponse {
  replyMessage: string;
  conversationState: string;
  detectedLanguage: string;
  orderId?: number;
  paymentUrl?: string;
  matchedItems?: {
    productId: number;
    productName: string;
    quantity: number;
    unitPrice: number;
    subtotal: number;
    inStock: boolean;
  }[];
}

export interface WhatsAppConversationSummary {
  id: number;
  waPhoneNumber: string;
  customerName: string;
  currentState: string;
  preferredLanguage: string;
  lastMessage: string;
  lastActiveAt: string;
  messageCount: number;
}


export interface InventoryBatchResponse {
  id: number;
  productId: number;
  productName: string;
  storeId: number;
  storeName: string;
  batchNumber: string;
  mfgDate?: string;
  expiryDate: string;
  daysUntilExpiry: number;
  initialQuantity: number;
  availableQuantity: number;
  reservedQuantity: number;
  temperatureZone: string;
  isQuarantined: boolean;
  discountAppliedPercent: number;
  isExpired: boolean;
  freshnessStatus: 'FRESH' | 'NEAR_EXPIRY_48H' | 'CRITICAL_24H' | 'EXPIRED';
}

export interface TemperatureTelemetryResponse {
  id: number;
  storeId: number;
  storageUnit: string;
  temperatureCelsius: number;
  humidityPercentage?: number;
  minSafeTemp: number;
  maxSafeTemp: number;
  breachDetected: boolean;
  status: 'NORMAL' | 'BREACH_ALERT';
  recordedAt: string;
}

export interface ColdChainOverviewResponse {
  storeId: number;
  storeName: string;
  totalBatches: number;
  activeBatches: number;
  nearExpiryCount: number;
  quarantinedCount: number;
  units: {
    storageUnit: string;
    currentTemp: number;
    targetTemp: number;
    minSafe: number;
    maxSafe: number;
    inBreach: boolean;
    lastUpdated: string;
  }[];
}
