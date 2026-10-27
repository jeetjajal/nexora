import { NextRequest, NextResponse } from 'next/server';

export async function POST(request: NextRequest) {
  try {
    const { amount, receipt } = await request.json();

    if (!amount || amount <= 0) {
      return NextResponse.json({ status: 'ERROR', message: 'Valid amount is required' }, { status: 400 });
    }

    const keyId = process.env.NEXT_PUBLIC_RAZORPAY_KEY_ID || 'rzp_test_nexora_sandbox';
    const keySecret = process.env.RAZORPAY_KEY_SECRET;

    // Convert to paise (e.g. ₹100 = 10000 paise)
    const amountInPaise = Math.round(Number(amount) * 100);

    // Production check
    if (process.env.NODE_ENV === 'production' && (!keySecret || keyId === 'rzp_test_nexora_sandbox')) {
      return NextResponse.json({
        status: 'ERROR',
        message: 'Payment gateway credentials are not configured in production environment',
      }, { status: 500 });
    }

    // If real Razorpay credentials provided, invoke Razorpay API
    if (keySecret && keyId !== 'rzp_test_nexora_sandbox') {
      const basicAuth = Buffer.from(`${keyId}:${keySecret}`).toString('base64');
      const response = await fetch('https://api.razorpay.com/v1/orders', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Basic ${basicAuth}`,
        },
        body: JSON.stringify({
          amount: amountInPaise,
          currency: 'INR',
          receipt: receipt || `rcpt_${Date.now()}`,
          notes: {
            platform: 'Nexora Quick Commerce',
          },
        }),
      });

      if (response.ok) {
        const orderData = await response.json();
        return NextResponse.json({
          status: 'SUCCESS',
          data: {
            id: orderData.id,
            amount: orderData.amount,
            currency: orderData.currency,
            keyId,
          },
        });
      } else {
        const errJson = await response.json().catch(() => ({}));
        return NextResponse.json({
          status: 'ERROR',
          message: errJson.error?.description || 'Failed to create payment order with gateway',
        }, { status: response.status });
      }
    }

    // Local Sandbox / Test Simulation Mode only (when non-production)
    const mockOrderId = `order_${Math.random().toString(36).substring(2, 12)}`;
    return NextResponse.json({
      status: 'SUCCESS',
      mode: 'SANDBOX_SIMULATION',
      data: {
        id: mockOrderId,
        amount: amountInPaise,
        currency: 'INR',
        keyId,
      },
    });
  } catch (err: any) {
    return NextResponse.json({ status: 'ERROR', message: err.message }, { status: 500 });
  }
}
