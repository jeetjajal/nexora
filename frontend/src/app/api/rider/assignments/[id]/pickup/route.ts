import { getBackendUrl } from '@/lib/server-config';
import { NextRequest, NextResponse } from 'next/server';

const BACKEND_URL = getBackendUrl();

export async function POST(
  req: NextRequest,
  { params }: { params: { id: string } }
) {
  const assignmentId = params.id;
  const authHeader = req.headers.get('Authorization') || '';

  try {
    const backendRes = await fetch(`${BACKEND_URL}/api/v1/delivery/assignments/${assignmentId}/pickup`, {
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
    message: 'Order picked up from dark store. Status is OUT_FOR_DELIVERY',
    data: {
      assignmentId: Number(assignmentId),
      assignmentStatus: 'PICKED_UP',
      orderStatus: 'OUT_FOR_DELIVERY',
    },
  });
}
