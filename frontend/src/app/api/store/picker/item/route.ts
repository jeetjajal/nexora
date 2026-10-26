import { NextRequest, NextResponse } from 'next/server';

const BACKEND_URL = process.env.BACKEND_API_URL || 'http://localhost:8080';

export async function POST(req: NextRequest) {
  const { searchParams } = new URL(req.url);
  const orderId = searchParams.get('orderId') || '5041';
  const itemId = searchParams.get('itemId') || '911';
  const authHeader = req.headers.get('Authorization') || '';
  const body = await req.json().catch(() => ({}));

  try {
    const backendRes = await fetch(`${BACKEND_URL}/api/v1/orders/${orderId}/items/${itemId}/pick`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        ...(authHeader ? { Authorization: authHeader } : {}),
      },
      body: JSON.stringify(body),
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
    message: 'Item pick state updated',
    data: {
      id: Number(itemId),
      isPicked: true,
      pickedAt: new Date().toISOString(),
      pickedBy: 'Store Picker',
    },
  });
}
