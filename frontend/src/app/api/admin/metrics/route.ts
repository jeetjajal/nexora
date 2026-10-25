import { NextResponse } from 'next/server';

export async function GET() {
  return NextResponse.json({
    status: 'SUCCESS',
    timestamp: new Date().toISOString(),
    metrics: {
      grossOrderValue: 148290.0,
      completedOrders: 1248,
      averageDeliveryMinutes: 8.8,
      fulfillmentRatePercent: 99.6,
      activeRiders: 64,
      lowStockSkus: 3,
      darkStores: [
        { code: 'DS-AHM-04', name: 'SG Highway #04', activeOrders: 38, avgEta: 8.4 },
        { code: 'DS-AHM-02', name: 'Vastrapur #02', activeOrders: 29, avgEta: 9.0 },
        { code: 'DS-AHM-01', name: 'Prahlad Nagar #01', activeOrders: 42, avgEta: 9.6 },
        { code: 'DS-AHM-03', name: 'South Bopal #03', activeOrders: 21, avgEta: 8.1 },
      ],
    },
  });
}
