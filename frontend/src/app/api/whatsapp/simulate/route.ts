import { getBackendUrl } from '@/lib/server-config';
import { NextRequest, NextResponse } from 'next/server';

const BACKEND_URL = getBackendUrl();

export async function POST(req: NextRequest) {
  const body = await req.json().catch(() => ({}));

  try {
    const backendRes = await fetch(`${BACKEND_URL}/api/v1/whatsapp/simulate`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
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

  // Realistic fallback responder for offline/mock demo
  const userMsg = (body.message || '').toLowerCase();
  let reply = '';
  let lang = 'en';
  let state = 'IDLE';

  if (userMsg.includes('batata') || userMsg.includes('dudh') || userMsg.includes('tameta')) {
    lang = 'gu';
    state = 'AWAITING_CONFIRMATION';
    reply = `🧾 *નેક્સોરા 10-મિનિટ ઓર્ડર સમરી:*\n━━━━━━━━━━━━━━━━━━\n• તાજા બટાટા (1kg) × 2 = *₹70.00*\n• અમુલ તાજા દૂધ (500ml) × 1 = *₹27.00*\n━━━━━━━━━━━━━━━━━━\nસબટોટલ: ₹97.00\nડિલિવરી ફી: ₹25.00\n💰 *કુલ ચૂકવવાપાત્ર રકમ: ₹122.00*\n\n👉 ઓર્ડર કન્ફર્મ કરવા *'HA'* અથવા *'CONFIRM'* લખો.`;
  } else if (userMsg.includes('aalu') || userMsg.includes('doodh') || userMsg.includes('tamatar')) {
    lang = 'hi';
    state = 'AWAITING_CONFIRMATION';
    reply = `🧾 *नेक्सोरा 10-मिनट आर्डर सारांश:*\n━━━━━━━━━━━━━━━━━━\n• अमूल ताज़ा दूध (500ml) × 1 = *₹27.00*\n• ताज़ा आलू (1kg) × 2 = *₹70.00*\n━━━━━━━━━━━━━━━━━━\nसामान राशि: ₹97.00\nडिलीवरी शुल्क: ₹25.00\n💰 *कुल देय राशि: ₹122.00*\n\n👉 आर्डर की पुष्टि के लिए *'HAAN'* या *'CONFIRM'* लिखें.`;
  } else if (userMsg.includes('confirm') || userMsg.includes('ha') || userMsg.includes('haan')) {
    state = 'ORDER_PLACED';
    reply = `🎉 *Confirmed! Your NEXORA Order #NX-99841 is confirmed!*\n\n🚴‍♂️ A delivery partner is assigned. Expected delivery in *10 minutes*.\n💰 Amount Due: *₹122.00*\n\n👉 Live Delivery Tracking: https://nexora-mu-eosin.vercel.app/?trackingOrder=99841`;
  } else if (userMsg.includes('track')) {
    reply = `📦 *Order Status Update (NX-849201):*\n• Status: *IN_TRANSIT*\n• Assigned Rider: *Ramesh Patel (Hero EV)*\n• ETA: *4 minutes*\n\n⚡ Live Map: https://nexora-mu-eosin.vercel.app/?trackingOrder=849201`;
  } else {
    reply = `⚡ *Hello! Welcome to NEXORA 10-Minute Quick Commerce.*\nYou can order directly in English, Hindi, or Gujarati:\n👉 '2 kilo batata ane 1 packet dudh moko'\n👉 '1 packet doodh aur 2 kg aalu bhejo'`;
  }

  return NextResponse.json({
    success: true,
    message: 'Processed by NEXORA AI WhatsApp Bot',
    data: {
      replyMessage: reply,
      conversationState: state,
      detectedLanguage: lang,
      orderId: state === 'ORDER_PLACED' ? 99841 : undefined,
      paymentUrl: state === 'ORDER_PLACED' ? 'https://nexora-mu-eosin.vercel.app/?trackingOrder=99841' : undefined,
    },
  });
}
