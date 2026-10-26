import { NextRequest, NextResponse } from 'next/server';

const BACKEND_URL = process.env.BACKEND_URL || 'http://localhost:8080';

export async function GET(
  request: NextRequest,
  { params }: { params: { orderId: string } }
) {
  try {
    const { orderId } = params;
    const authHeader = request.headers.get('Authorization');

    // Extract numeric ID if prefixed with NX-
    const numericId = orderId.replace(/\D/g, '') || '1';

    // Try Spring Boot Backend
    if (authHeader) {
      try {
        const controller = new AbortController();
        const timeoutId = setTimeout(() => controller.abort(), 2000);

        const res = await fetch(`${BACKEND_URL}/api/v1/orders/${numericId}/tracking`, {
          headers: {
            'Authorization': authHeader,
            'Accept': 'application/json',
          },
          signal: controller.signal,
        });
        clearTimeout(timeoutId);

        if (res.ok) {
          const json = await res.json();
          return NextResponse.json({
            status: 'SUCCESS',
            source: 'SPRING_BOOT_MYSQL',
            data: json.data,
          });
        }
      } catch {
        // Fallback
      }
    }

    // Serverless tracking timeline fallback
    const now = new Date();
    const timeline = [
      {
        status: 'PLACED',
        title: 'Order Placed',
        description: 'Received at Nexora Dark Store SG Highway #04',
        timestamp: new Date(now.getTime() - 4 * 60000).toISOString(),
        completed: true,
        current: false,
      },
      {
        status: 'CONFIRMED',
        title: 'Order Confirmed',
        description: 'Stock verified and sent to dark store picking queue',
        timestamp: new Date(now.getTime() - 3 * 60000).toISOString(),
        completed: true,
        current: false,
      },
      {
        status: 'PACKING',
        title: 'Packing Essentials',
        description: 'Picker barcode-scanning items and sealing bag',
        timestamp: new Date(now.getTime() - 2 * 60000).toISOString(),
        completed: true,
        current: true,
      },
      {
        status: 'OUT_FOR_DELIVERY',
        title: 'Out for Delivery',
        description: 'Rider Ramesh Patel en route on Electric EV',
        timestamp: new Date(now.getTime() + 2 * 60000).toISOString(),
        completed: false,
        current: false,
      },
      {
        status: 'DELIVERED',
        title: 'Delivered',
        description: 'Doorstep handover with 4-digit OTP verification',
        timestamp: new Date(now.getTime() + 6 * 60000).toISOString(),
        completed: false,
        current: false,
      },
    ];

    return NextResponse.json({
      status: 'SUCCESS',
      source: 'SERVER_ENGINE',
      data: {
        orderId,
        status: 'PACKING',
        storeName: 'Nexora Dark Store SG Highway #04',
        storeAddress: 'Ground Floor, Shapath V, SG Highway, Ahmedabad',
        deliveryAddress: 'Ahmedabad Serviceable Zone',
        estimatedDeliveryMinutes: 7,
        assignedRiderName: 'Ramesh Patel',
        assignedRiderPhone: '+91 98765 43210',
        assignedRiderVehicle: 'Hero Electric NYX EV',
        timeline,
        createdAt: new Date(now.getTime() - 4 * 60000).toISOString(),
      },
    });
  } catch (err: any) {
    return NextResponse.json({ status: 'ERROR', message: err.message }, { status: 500 });
  }
}
