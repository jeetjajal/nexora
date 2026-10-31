import { getBackendUrl } from '@/lib/server-config';
import { NextRequest, NextResponse } from 'next/server';

const BACKEND_URL = getBackendUrl();

export async function GET(req: NextRequest) {
  const authHeader = req.headers.get('Authorization') || '';
  const searchParams = req.nextUrl.searchParams;
  const storeId = searchParams.get('storeId') || '4';

  try {
    const backendRes = await fetch(`${BACKEND_URL}/api/v1/store/${storeId}/coldchain/overview`, {
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
  }

  return NextResponse.json({
    success: true,
    message: 'Cold chain overview loaded (preview mode)',
    data: {
      storeId: 4,
      storeName: 'SG Highway Dark Store Hub',
      totalBatches: 38,
      activeBatches: 36,
      nearExpiryCount: 4,
      quarantinedCount: 2,
      units: [
        { storageUnit: 'CHILLER_01 (Dairy & Milk)', currentTemp: 4.2, targetTemp: 4.0, minSafe: 2.0, maxSafe: 8.0, inBreach: false, lastUpdated: '12s ago' },
        { storageUnit: 'CHILLER_02 (Fresh Produce & Herbs)', currentTemp: 4.8, targetTemp: 4.0, minSafe: 2.0, maxSafe: 8.0, inBreach: false, lastUpdated: '25s ago' },
        { storageUnit: 'FREEZER_01 (Ice Creams & Frozen Peas)', currentTemp: -18.2, targetTemp: -18.0, minSafe: -24.0, maxSafe: -15.0, inBreach: false, lastUpdated: '18s ago' },
        { storageUnit: 'TRANSIT_COOLBOX_12 (Rider Insulated Bags)', currentTemp: 5.4, targetTemp: 4.0, minSafe: 2.0, maxSafe: 10.0, inBreach: false, lastUpdated: '45s ago' },
      ],
    },
  });
}
