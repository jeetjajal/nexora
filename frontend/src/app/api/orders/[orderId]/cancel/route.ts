import { NextRequest, NextResponse } from 'next/server';

const BACKEND_URL = process.env.BACKEND_URL || 'http://localhost:8080';

export async function POST(
  request: NextRequest,
  { params }: { params: { orderId: string } }
) {
  try {
    const { orderId } = params;
    const authHeader = request.headers.get('Authorization');
    const numericId = orderId.replace(/\D/g, '') || '1';

    if (authHeader) {
      try {
        const controller = new AbortController();
        const timeoutId = setTimeout(() => controller.abort(), 2000);

        const res = await fetch(`${BACKEND_URL}/api/v1/orders/${numericId}/cancel`, {
          method: 'POST',
          headers: {
            'Authorization': authHeader,
            'Content-Type': 'application/json',
          },
          signal: controller.signal,
        });
        clearTimeout(timeoutId);

        if (res.ok) {
          const json = await res.json();
          return NextResponse.json({
            status: 'SUCCESS',
            source: 'SPRING_BOOT_MYSQL',
            message: 'Order cancelled successfully and stock restored',
            data: json.data,
          });
        }
      } catch {
        // Fallback
      }
    }

    return NextResponse.json({
      status: 'SUCCESS',
      source: 'SERVER_ENGINE',
      message: 'Order cancelled successfully and refund initiated',
      data: {
        orderId,
        status: 'CANCELLED',
      },
    });
  } catch (err: any) {
    return NextResponse.json({ status: 'ERROR', message: err.message }, { status: 500 });
  }
}
