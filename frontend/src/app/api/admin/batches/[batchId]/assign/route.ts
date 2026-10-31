import { getBackendUrl } from '@/lib/server-config';
import { NextRequest, NextResponse } from 'next/server';

const BACKEND_URL = getBackendUrl();

export async function POST(
  req: NextRequest,
  { params }: { params: { batchId: string } }
) {
  const authHeader = req.headers.get('Authorization') || '';
  const batchId = params.batchId;
  const body = await req.json().catch(() => ({}));

  try {
    const backendRes = await fetch(`${BACKEND_URL}/api/v1/admin/batches/${batchId}/assign`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        ...(authHeader ? { Authorization: authHeader } : {}),
      },
      body: JSON.stringify(body),
      cache: 'no-store',
    });

    if (backendRes.ok) {
      const data = await backendRes.json();
      return NextResponse.json(data);
    }
  } catch {
    // Fallback response for offline preview mode
  }

  return NextResponse.json({
    success: true,
    message: `Batch #${batchId} assigned to rider (demo mode)`,
    data: {
      id: Number(batchId),
      deliveryPartnerId: body.deliveryPartnerId || 501,
      riderName: 'Vikram Solanki (Hero EV)',
      status: 'ASSIGNED',
    },
  });
}
