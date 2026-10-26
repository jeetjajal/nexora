import { NextRequest, NextResponse } from 'next/server';

const BACKEND_BASE = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080';

export async function GET(req: NextRequest) {
  try {
    const authHeader = req.headers.get('authorization');
    const headers: Record<string, string> = {
      'Content-Type': 'application/json',
    };
    if (authHeader) {
      headers['Authorization'] = authHeader;
    }

    const res = await fetch(`${BACKEND_BASE}/api/v1/admin/notifications/providers`, {
      method: 'GET',
      headers,
    });

    const data = await res.json().catch(() => ({}));
    return NextResponse.json(data, { status: res.status });
  } catch (err: any) {
    return NextResponse.json(
      {
        success: false,
        message: err.message || 'Notification providers proxy error',
        data: null,
        timestamp: new Date().toISOString(),
      },
      { status: 500 }
    );
  }
}
