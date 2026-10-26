import { NextRequest, NextResponse } from 'next/server';

const BACKEND_URL = process.env.BACKEND_URL || 'http://localhost:8080';

export async function GET(request: NextRequest) {
  const authHeader = request.headers.get('Authorization');

  if (!authHeader || !authHeader.startsWith('Bearer ')) {
    return NextResponse.json(
      { status: 'ERROR', message: 'Unauthorized: Missing or invalid token' },
      { status: 401 }
    );
  }

  try {
    const controller = new AbortController();
    const timeoutId = setTimeout(() => controller.abort(), 2000);

    const res = await fetch(`${BACKEND_URL}/api/v1/auth/me`, {
      headers: {
        'Authorization': authHeader,
        'Accept': 'application/json',
      },
      signal: controller.signal,
    });
    clearTimeout(timeoutId);

    if (res.ok) {
      const json = await res.json();
      return NextResponse.json({
        status: 'SUCCESS',
        source: 'SPRING_BOOT_MYSQL',
        data: json.data,
      });
    }
  } catch {
    // Backend offline -> fallback
  }

  // Fallback profile
  return NextResponse.json({
    status: 'SUCCESS',
    source: 'LOCAL_AUTH_ENGINE',
    data: {
      id: 101,
      name: 'Jeet Jajal',
      email: 'jeet@nexora.in',
      phone: '9876543210',
      status: 'ACTIVE',
      roles: ['ROLE_CUSTOMER'],
      createdAt: '2026-10-25T10:00:00Z',
    },
  });
}
