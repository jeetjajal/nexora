import { getBackendUrl } from '@/lib/server-config';
import { NextRequest, NextResponse } from 'next/server';

const BACKEND_BASE = getBackendUrl();

export async function GET(req: NextRequest) {
  try {
    const authHeader = req.headers.get('authorization');
    const { searchParams } = new URL(req.url);

    const headers: Record<string, string> = {
      'Content-Type': 'application/json',
    };
    if (authHeader) {
      headers['Authorization'] = authHeader;
    }

    const backendUrl = `${BACKEND_BASE}/api/v1/admin/analytics/overview?${searchParams.toString()}`;
    const res = await fetch(backendUrl, {
      method: 'GET',
      headers,
    });

    const data = await res.json().catch(() => ({}));
    return NextResponse.json(data, { status: res.status });
  } catch (err: any) {
    return NextResponse.json(
      {
        success: false,
        message: err.message || 'Analytics overview proxy failure',
        data: null,
        timestamp: new Date().toISOString(),
      },
      { status: 500 }
    );
  }
}
