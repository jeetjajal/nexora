import { NextRequest, NextResponse } from 'next/server';
import { getBackendUrl } from '@/lib/server-config';

export async function POST(
  request: NextRequest,
  { params }: { params: { orderId: string } }
) {
  const BACKEND_URL = getBackendUrl();

  try {
    const { orderId } = params;
    const authHeader = request.headers.get('Authorization');
    const numericId = orderId.replace(/\D/g, '') || '1';

    if (!authHeader) {
      return NextResponse.json(
        { status: 'ERROR', message: 'Authentication required to cancel order' },
        { status: 401 }
      );
    }

    try {
      const controller = new AbortController();
      const timeoutId = setTimeout(() => controller.abort(), 3500);

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
      } else {
        const errJson = await res.json().catch(() => ({}));
        return NextResponse.json(
          {
            status: 'ERROR',
            message: errJson.message || 'Order cannot be cancelled in its current state',
          },
          { status: res.status }
        );
      }
    } catch (err) {
      if (process.env.NODE_ENV === 'production' || process.env.ENFORCE_BACKEND === 'true') {
        return NextResponse.json(
          {
            status: 'ERROR',
            message: 'Commerce engine unreachable; cancellation could not be processed',
          },
          { status: 503 }
        );
      }
    }

    // Dev sandbox simulation only
    return NextResponse.json({
      status: 'SUCCESS',
      source: 'LOCAL_DEV_SIMULATION',
      message: 'Order cancelled (development simulation mode)',
      data: {
        orderId,
        status: 'CANCELLED',
      },
    });
  } catch (err: any) {
    return NextResponse.json({ status: 'ERROR', message: err.message }, { status: 500 });
  }
}
