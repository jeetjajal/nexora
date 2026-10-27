import { NextResponse } from 'next/server';

import { getBackendUrl } from '@/lib/server-config';
const BACKEND_BASE = getBackendUrl();

export async function GET() {
  try {
    const res = await fetch(`${BACKEND_BASE}/api/v1/health`, {
      method: 'GET',
      headers: { 'Content-Type': 'application/json' },
      cache: 'no-store',
    });

    const data = await res.json().catch(() => ({}));
    return NextResponse.json(data, { status: res.status });
  } catch (err: any) {
    return NextResponse.json(
      {
        success: false,
        status: 'DEGRADED',
        backend: 'UNREACHABLE',
        frontend: 'UP',
        message: err.message,
        timestamp: new Date().toISOString(),
      },
      { status: 503 }
    );
  }
}
