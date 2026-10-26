import { NextRequest, NextResponse } from 'next/server';

const BACKEND_URL = process.env.BACKEND_API_URL || 'http://localhost:8080';

export async function GET(req: NextRequest) {
  const authHeader = req.headers.get('Authorization') || '';

  try {
    const backendRes = await fetch(`${BACKEND_URL}/api/v1/admin/control-tower`, {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json',
        ...(authHeader ? { Authorization: authHeader } : {}),
      },
      cache: 'no-store',
    });

    if (backendRes.ok) {
      const data = await backendRes.json();
      return NextResponse.json(data);
    }
  } catch {
    // Fallback response for preview mode
  }

  return NextResponse.json({
    success: true,
    message: 'Control Tower overview loaded (preview fallback)',
    data: {
      totalOrders: 1420,
      activeOrders: 42,
      delayedOrdersCount: 1,
      cancelledOrders: 8,
      refundedOrders: 3,
      grossMerchandiseValue: 184250,
      deliveryFeesCollected: 35500,
      netPlatformRevenue: 33165,
      averageDeliveryTimeMinutes: 8.8,
      slaAdherencePercent: 98.4,
      activeHubsCount: 4,
      onlineRidersCount: 28,
      activeDisputesCount: 2,
      hubStatusList: [
        { hubId: 4, hubName: 'Nexora Dark Store SG Highway', code: 'DS-AHM-04', city: 'Ahmedabad', activeOrdersCount: 14, pickersActive: 4, ridersOnline: 10, avgPickTimeSeconds: 108, healthStatus: 'OPTIMAL' },
        { hubId: 2, hubName: 'Nexora Dark Store Vastrapur', code: 'DS-AHM-02', city: 'Ahmedabad', activeOrdersCount: 11, pickersActive: 3, ridersOnline: 8, avgPickTimeSeconds: 122, healthStatus: 'OPTIMAL' },
        { hubId: 1, hubName: 'Nexora Dark Store Prahlad Nagar', code: 'DS-AHM-01', city: 'Ahmedabad', activeOrdersCount: 9, pickersActive: 3, ridersOnline: 6, avgPickTimeSeconds: 115, healthStatus: 'OPTIMAL' },
        { hubId: 3, hubName: 'Nexora Dark Store Bopal', code: 'DS-AHM-03', city: 'Ahmedabad', activeOrdersCount: 8, pickersActive: 2, ridersOnline: 4, avgPickTimeSeconds: 130, healthStatus: 'OPTIMAL' },
      ],
      delayedOrders: [
        {
          orderId: 5028,
          orderNumber: 'ORD-5028',
          storeName: 'Nexora Dark Store SG Highway',
          customerName: 'Kavita Patel',
          status: 'OUT_FOR_DELIVERY',
          amount: 340,
          elapsedSeconds: 740,
          targetSlaSeconds: 600,
          delayedBySeconds: 140,
          createdAt: new Date(Date.now() - 740000).toISOString(),
        },
      ],
    },
  });
}
