import { getBackendUrl } from '@/lib/server-config';
import { NextRequest, NextResponse } from 'next/server';

const BACKEND_URL = getBackendUrl();

export async function GET(req: NextRequest) {
  try {
    const backendRes = await fetch(`${BACKEND_URL}/api/v1/whatsapp/conversations`, {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json',
      },
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
    message: 'WhatsApp conversations retrieved (preview mode)',
    data: [
      {
        id: 1,
        waPhoneNumber: '+919876543210',
        customerName: 'Jignesh Shah',
        currentState: 'ORDER_PLACED',
        preferredLanguage: 'gu',
        lastMessage: '🎉 અભિનંદન! તમારો ઓર્ડર #NX-99841 સફળતાપૂર્વક કન્ફર્મ થયો છે!',
        lastActiveAt: new Date(Date.now() - 3 * 60000).toISOString(),
        messageCount: 6,
      },
      {
        id: 2,
        waPhoneNumber: '+919825123456',
        customerName: 'Priya Sharma',
        currentState: 'AWAITING_CONFIRMATION',
        preferredLanguage: 'hi',
        lastMessage: '🧾 नेक्सोरा 10-मिनट आर्डर सारांश: अमूल ताज़ा दूध + आलू',
        lastActiveAt: new Date(Date.now() - 8 * 60000).toISOString(),
        messageCount: 3,
      },
      {
        id: 3,
        waPhoneNumber: '+919898011223',
        customerName: 'Rohan Mehta',
        currentState: 'IDLE',
        preferredLanguage: 'en',
        lastMessage: '⚡ Hello! Welcome to NEXORA 10-Minute Quick Commerce.',
        lastActiveAt: new Date(Date.now() - 15 * 60000).toISOString(),
        messageCount: 2,
      },
    ],
  });
}
