import { NextRequest, NextResponse } from 'next/server';
import crypto from 'crypto';

export async function POST(request: NextRequest) {
  try {
    const body = await request.json();
    const { razorpay_order_id, razorpay_payment_id, razorpay_signature } = body;

    const keySecret = process.env.RAZORPAY_KEY_SECRET;

    if (!razorpay_order_id || !razorpay_payment_id) {
      return NextResponse.json(
        { verified: false, message: 'Missing razorpay_order_id or razorpay_payment_id' },
        { status: 400 }
      );
    }

    // In production, keySecret is mandatory
    if (!keySecret) {
      if (process.env.NODE_ENV === 'production') {
        return NextResponse.json(
          { verified: false, message: 'Payment gateway secret not configured on server' },
          { status: 500 }
        );
      }

      // Safe local sandbox / test simulation only when running non-production
      return NextResponse.json({
        verified: true,
        mode: 'SANDBOX_SIMULATION',
        message: 'Sandbox payment verified (Simulation mode - test only)',
      });
    }

    // Cryptographic HMAC-SHA256 signature verification (Mandatory when keySecret exists)
    if (!razorpay_signature) {
      return NextResponse.json(
        { verified: false, message: 'Payment signature missing from request' },
        { status: 400 }
      );
    }

    const generatedSignature = crypto
      .createHmac('sha256', keySecret)
      .update(`${razorpay_order_id}|${razorpay_payment_id}`)
      .digest('hex');

    const isValid = crypto.timingSafeEqual(
      Buffer.from(generatedSignature, 'utf-8'),
      Buffer.from(razorpay_signature, 'utf-8')
    );

    if (isValid) {
      return NextResponse.json({
        verified: true,
        message: 'Payment signature verified successfully',
      });
    } else {
      return NextResponse.json(
        { verified: false, message: 'Invalid payment signature' },
        { status: 400 }
      );
    }
  } catch (err: any) {
    return NextResponse.json({ verified: false, message: err.message }, { status: 500 });
  }
}
