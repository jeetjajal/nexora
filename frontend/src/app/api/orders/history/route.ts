import { NextRequest, NextResponse } from 'next/server';

const BACKEND_URL = process.env.BACKEND_URL || 'http://localhost:8080';

const DEFAULT_PAST_ORDERS = [
  {
    orderId: 'NX-942183',
    date: 'Today, 2:15 PM',
    itemsCount: 3,
    itemsSummary: 'Amul Taaza Milk (2), Farm Fresh Tomatoes, Lay\'s Magic Masala',
    amount: 147,
    status: 'DELIVERED',
    darkStore: 'Nexora Dark Store SG Highway #04',
    deliveryOtp: '581294',
    paymentMethod: 'RAZORPAY',
  },
  {
    orderId: 'NX-813904',
    date: 'Yesterday, 8:40 PM',
    itemsCount: 4,
    itemsSummary: 'Whole Wheat Bread, Cavendish Banana, Fortune Sunlite Oil, Maggi 2-Min',
    amount: 320,
    status: 'DELIVERED',
    darkStore: 'Nexora Dark Store SG Highway #04',
    deliveryOtp: '719302',
    paymentMethod: 'COD',
  },
  {
    orderId: 'NX-724119',
    date: '24 Oct 2026, 6:10 PM',
    itemsCount: 2,
    itemsSummary: 'Aashirvaad Shudh Chakki Atta 5kg, Tata Salt',
    amount: 285,
    status: 'DELIVERED',
    darkStore: 'Nexora Dark Store Vastrapur #02',
    deliveryOtp: '482015',
    paymentMethod: 'RAZORPAY',
  },
];

export async function GET(request: NextRequest) {
  const authHeader = request.headers.get('Authorization');

  if (authHeader) {
    try {
      const controller = new AbortController();
      const timeoutId = setTimeout(() => controller.abort(), 2000);

      const res = await fetch(`${BACKEND_URL}/api/v1/orders`, {
        headers: { 'Authorization': authHeader, 'Accept': 'application/json' },
        signal: controller.signal,
      });
      clearTimeout(timeoutId);

      if (res.ok) {
        const json = await res.json();
        if (json.data && json.data.content && json.data.content.length > 0) {
          const orders = json.data.content.map((o: any) => ({
            orderId: o.orderNumber || `NX-${o.id}`,
            date: new Date(o.createdAt).toLocaleDateString('en-IN', {
              day: 'numeric',
              month: 'short',
              hour: '2-digit',
              minute: '2-digit',
            }),
            itemsCount: o.items ? o.items.length : 1,
            itemsSummary: o.items ? o.items.map((i: any) => i.productName).join(', ') : 'Grocery Items',
            amount: o.finalAmount,
            status: o.status,
            darkStore: 'Nexora Dark Store SG Highway #04',
            deliveryOtp: o.deliveryOtp || '492011',
            paymentMethod: o.paymentMethod || 'RAZORPAY',
          }));
          return NextResponse.json({
            status: 'SUCCESS',
            source: 'SPRING_BOOT_MYSQL',
            data: orders,
          });
        }
      }
    } catch {
      // Fallback
    }
  }

  return NextResponse.json({
    status: 'SUCCESS',
    source: 'REGISTRY',
    data: DEFAULT_PAST_ORDERS,
  });
}
