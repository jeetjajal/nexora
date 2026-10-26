import { NextRequest, NextResponse } from 'next/server';

const BACKEND_URL = process.env.BACKEND_API_URL || 'http://localhost:8080';

export async function GET(req: NextRequest) {
  const authHeader = req.headers.get('Authorization') || '';

  try {
    const backendRes = await fetch(`${BACKEND_URL}/api/v1/admin/fleet`, {
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
    // Fallback response
  }

  return NextResponse.json({
    success: true,
    message: 'Fleet monitoring status loaded (preview fallback)',
    data: [
      { partnerId: 201, userId: 105, name: 'Ramesh Patel', phone: '+91 98765 43210', vehicleType: 'ELECTRIC_SCOOTER', rating: 4.92, isOnline: true, currentStatus: 'BUSY', activeOrderId: 5039, currentLat: 23.0225, currentLng: 72.5065, todayEarnings: 840, lastPingTime: new Date().toISOString(), isStaleLocation: false },
      { partnerId: 202, userId: 106, name: 'Vikram Joshi', phone: '+91 98765 43211', vehicleType: 'ELECTRIC_SCOOTER', rating: 4.88, isOnline: true, currentStatus: 'AVAILABLE', activeOrderId: null, currentLat: 23.0360, currentLng: 72.5293, todayEarnings: 720, lastPingTime: new Date().toISOString(), isStaleLocation: false },
      { partnerId: 203, userId: 107, name: 'Anil Rathod', phone: '+91 98765 43212', vehicleType: 'MOTORBIKE', rating: 4.79, isOnline: true, currentStatus: 'AVAILABLE', activeOrderId: null, currentLat: 23.0121, currentLng: 72.5108, todayEarnings: 650, lastPingTime: new Date(Date.now() - 1200000).toISOString(), isStaleLocation: true },
      { partnerId: 204, userId: 108, name: 'Sanjay Prajapati', phone: '+91 98765 43213', vehicleType: 'ELECTRIC_SCOOTER', rating: 4.95, isOnline: false, currentStatus: 'OFFLINE', activeOrderId: null, currentLat: null, currentLng: null, todayEarnings: 400, lastPingTime: new Date(Date.now() - 7200000).toISOString(), isStaleLocation: true },
    ],
  });
}
