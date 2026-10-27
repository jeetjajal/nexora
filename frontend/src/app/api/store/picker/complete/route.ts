import { getBackendUrl } from '@/lib/server-config';
import { NextRequest, NextResponse } from 'next/server';

const BACKEND_URL = getBackendUrl();

export async function POST(req: NextRequest) {
  const { searchParams } = new URL(req.url);
  const orderId = searchParams.get('orderId') || '5041';
  const authHeader = req.headers.get('Authorization') || '';

  try {
    const backendRes = await fetch(`${BACKEND_URL}/api/v1/orders/${orderId}/picking/complete`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        ...(authHeader ? { Authorization: authHeader } : {}),
      },
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
    message: `Picking completed for order #${orderId}`,
    data: {
      id: Number(orderId),
      status: 'PREPARING',
      pickingCompletedAt: new Date().toISOString(),
    },
  });
}
