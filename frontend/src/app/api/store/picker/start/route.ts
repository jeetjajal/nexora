import { NextRequest, NextResponse } from 'next/server';

const BACKEND_URL = process.env.BACKEND_API_URL || 'http://localhost:8080';

export async function POST(req: NextRequest) {
  const { searchParams } = new URL(req.url);
  const orderId = searchParams.get('orderId') || '5041';
  const authHeader = req.headers.get('Authorization') || '';

  try {
    const backendRes = await fetch(`${BACKEND_URL}/api/v1/orders/${orderId}/picking/start`, {
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
    message: `Picking started for order #${orderId}`,
    data: {
      id: Number(orderId),
      status: 'PREPARING',
      pickerName: 'Store Picker',
      pickingStartedAt: new Date().toISOString(),
    },
  });
}
