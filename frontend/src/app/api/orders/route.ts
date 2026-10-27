import { NextRequest, NextResponse } from 'next/server';
import { getBackendUrl } from '@/lib/server-config';

// Serviceable Dark Stores in Ahmedabad
const DARK_STORES = [
  { name: 'Nexora Dark Store SG Highway #04', code: 'DS-AHM-04', eta: 8 },
  { name: 'Nexora Dark Store Vastrapur #02', code: 'DS-AHM-02', eta: 9 },
  { name: 'Nexora Dark Store Prahlad Nagar #01', code: 'DS-AHM-01', eta: 10 },
  { name: 'Nexora Dark Store Bopal #03', code: 'DS-AHM-03', eta: 11 },
];

export async function POST(request: NextRequest) {
  const BACKEND_URL = getBackendUrl();

  try {
    const authHeader = request.headers.get('Authorization');
    const idempotencyKey = request.headers.get('Idempotency-Key');
    const body = await request.json();
    const { items, address, deliveryAddressId, paymentMethod, couponCode } = body;

    if (!items || !Array.isArray(items) || items.length === 0) {
      return NextResponse.json({ status: 'ERROR', message: 'Cart items cannot be empty' }, { status: 400 });
    }

    if (!address || !address.street || !address.phone) {
      return NextResponse.json({ status: 'ERROR', message: 'Complete delivery address is required' }, { status: 400 });
    }

    // Authoritative calculations
    const subtotal = items.reduce((acc: number, item: any) => acc + (Number(item.price) * Number(item.quantity)), 0);
    const handlingFee = 4;
    const deliveryFee = subtotal >= 199 ? 0 : 25;
    let discount = 0;

    if (couponCode === 'NEXORA50' && subtotal >= 200) {
      discount = 50;
    } else if (couponCode === 'FREEDEL') {
      discount = deliveryFee;
    }

    const finalAmount = Math.max(0, subtotal + handlingFee + deliveryFee - discount);
    const darkStore = DARK_STORES[Math.floor(Math.random() * DARK_STORES.length)];
    const orderId = `NX-${Math.floor(100000 + Math.random() * 900000)}`;
    const deliveryOtp = String(Math.floor(1000 + Math.random() * 9000));

    // Forward to Spring Boot Direct Checkout
    try {
      const controller = new AbortController();
      const timeoutId = setTimeout(() => controller.abort(), 3500);

      const headers: Record<string, string> = {
        'Content-Type': 'application/json',
        'Idempotency-Key': idempotencyKey || orderId,
      };
      if (authHeader) headers['Authorization'] = authHeader;

      const backendItems = items.map((i: any) => ({
        productId: Number(String(i.productId).replace(/\D/g, '')) || 1,
        quantity: Number(i.quantity) || 1,
      }));

      const backendRes = await fetch(`${BACKEND_URL}/api/v1/orders/direct`, {
        method: 'POST',
        headers,
        body: JSON.stringify({
          deliveryAddressId: deliveryAddressId || 1,
          items: backendItems,
          paymentMethod: paymentMethod || 'RAZORPAY',
          couponCode: couponCode || null,
        }),
        signal: controller.signal,
      });
      clearTimeout(timeoutId);

      if (backendRes.ok) {
        const backendData = await backendRes.json();
        const bOrder = backendData.data;

        return NextResponse.json({
          status: 'SUCCESS',
          source: 'SPRING_BOOT_MYSQL',
          message: 'Order created in MySQL database and assigned to dark store',
          data: {
            orderId: bOrder?.id ? `NX-${bOrder.id}` : orderId,
            numericId: bOrder?.id || null,
            items,
            subtotal: bOrder?.subtotal ? Number(bOrder.subtotal) : subtotal,
            handlingFee,
            deliveryFee: bOrder?.deliveryFee ? Number(bOrder.deliveryFee) : deliveryFee,
            discount,
            finalAmount: bOrder?.totalAmount ? Number(bOrder.totalAmount) : finalAmount,
            address,
            paymentMethod: paymentMethod || 'RAZORPAY',
            paymentStatus: bOrder?.paymentStatus || 'PENDING',
            deliveryOtp: bOrder?.deliveryOtp || deliveryOtp,
            darkStore: bOrder?.storeName || darkStore.name,
            etaMinutes: darkStore.eta,
            assignedRider: {
              name: 'Ramesh Patel',
              vehicle: 'Hero Electric NYX EV',
              phone: '+91 98765 43210',
              rating: 4.92,
            },
            createdAt: bOrder?.createdAt || new Date().toISOString(),
          },
        });
      } else {
        // Backend actively returned an error (e.g. 401 Unauthorized, 409 Out of Stock)
        // Must NOT masquerade as success!
        const errJson = await backendRes.json().catch(() => ({}));
        return NextResponse.json({
          status: 'ERROR',
          message: errJson.message || 'Order creation rejected by commerce engine',
          errors: errJson.data || null,
        }, { status: backendRes.status });
      }
    } catch (fetchErr) {
      // Backend is unreachable / timed out
      if (process.env.NODE_ENV === 'production' || process.env.ENFORCE_BACKEND === 'true') {
        return NextResponse.json({
          status: 'ERROR',
          message: 'Backend commerce engine is currently unreachable. Order was not placed to ensure data integrity.',
        }, { status: 503 });
      }
    }

    // Non-production sandbox simulation mode
    return NextResponse.json({
      status: 'SUCCESS',
      source: 'LOCAL_DEV_SIMULATION',
      message: 'Order placed in local simulation mode (development sandbox)',
      data: {
        orderId,
        items,
        subtotal,
        handlingFee,
        deliveryFee,
        discount,
        finalAmount,
        address,
        paymentMethod: paymentMethod || 'RAZORPAY',
        paymentStatus: 'PENDING',
        deliveryOtp,
        darkStore: darkStore.name,
        etaMinutes: darkStore.eta,
        assignedRider: {
          name: 'Ramesh Patel',
          vehicle: 'Hero Electric NYX EV',
          phone: '+91 98765 43210',
          rating: 4.92,
        },
        createdAt: new Date().toISOString(),
      },
    });
  } catch (err: any) {
    return NextResponse.json({ status: 'ERROR', message: err.message }, { status: 500 });
  }
}
