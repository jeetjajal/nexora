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
    const body = await request.json();
    const { otp } = body;

    if (!otp || String(otp).trim().length < 4) {
      return NextResponse.json(
        { status: 'ERROR', message: 'Valid 4-digit OTP is required' },
        { status: 400 }
      );
    }

    if (!authHeader) {
      return NextResponse.json(
        { status: 'ERROR', message: 'Authentication required to verify delivery OTP' },
        { status: 401 }
      );
    }

    try {
      const controller = new AbortController();
      const timeoutId = setTimeout(() => controller.abort(), 3500);

      const res = await fetch(`${BACKEND_URL}/api/v1/orders/${numericId}/verify-delivery`, {
        method: 'POST',
        headers: {
          'Authorization': authHeader,
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ otp: String(otp).trim() }),
        signal: controller.signal,
      });
      clearTimeout(timeoutId);

      if (res.ok) {
        const json = await res.json();
        return NextResponse.json({
          status: 'SUCCESS',
          source: 'SPRING_BOOT_MYSQL',
          message: 'Delivery OTP verified successfully. Order DELIVERED.',
          data: json.data,
        });
      } else {
        const errData = await res.json().catch(() => ({}));
        return NextResponse.json(
          { status: 'ERROR', message: errData.message || 'Incorrect delivery OTP' },
          { status: res.status }
        );
      }
    } catch (err) {
      if (process.env.NODE_ENV === 'production' || process.env.ENFORCE_BACKEND === 'true') {
        return NextResponse.json(
          { status: 'ERROR', message: 'Backend engine unreachable. OTP could not be verified.' },
          { status: 503 }
        );
      }
    }

    // Dev sandbox simulation only
    return NextResponse.json({
      status: 'SUCCESS',
      source: 'LOCAL_DEV_SIMULATION',
      message: 'Delivery OTP verified in local simulation mode',
      data: {
        orderId,
        status: 'DELIVERED',
        deliveredAt: new Date().toISOString(),
      },
    });
  } catch (err: any) {
    return NextResponse.json({ status: 'ERROR', message: err.message }, { status: 500 });
  }
}
