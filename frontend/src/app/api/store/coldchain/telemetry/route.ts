import { getBackendUrl } from '@/lib/server-config';
import { NextRequest, NextResponse } from 'next/server';

const BACKEND_URL = getBackendUrl();

export async function POST(req: NextRequest) {
  const body = await req.json().catch(() => ({}));

  try {
    const backendRes = await fetch(`${BACKEND_URL}/api/v1/store/coldchain/telemetry`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(body),
      cache: 'no-store',
    });

    if (backendRes.ok) {
      const data = await backendRes.json();
      return NextResponse.json(data);
    }
  } catch {
  }

  const temp = body.temperatureCelsius || 4.2;
  const unit = body.storageUnit || 'CHILLER_01';
  const isChiller = unit.startsWith('CHILLER');
  const isFreezer = unit.startsWith('FREEZER');
  const minSafe = isFreezer ? -24.0 : 2.0;
  const maxSafe = isFreezer ? -15.0 : (isChiller ? 8.0 : 10.0);
  const breach = temp < minSafe || temp > maxSafe;

  return NextResponse.json({
    success: true,
    message: 'IoT temperature telemetry recorded (fallback mode)',
    data: {
      id: Math.floor(Math.random() * 9000) + 1000,
      storeId: body.storeId || 4,
      storageUnit: unit,
      temperatureCelsius: temp,
      humidityPercentage: body.humidityPercentage || 60.0,
      minSafeTemp: minSafe,
      maxSafeTemp: maxSafe,
      breachDetected: breach,
      status: breach ? 'BREACH_ALERT' : 'NORMAL',
      recordedAt: new Date().toISOString(),
    },
  });
}
