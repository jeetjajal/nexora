import { NextRequest, NextResponse } from 'next/server';

export async function POST(request: NextRequest) {
  try {
    const body = await request.json();
    const { items, address, paymentMethod, couponCode } = body;

    const orderId = `NX-${Math.floor(100000 + Math.random() * 900000)}`;
    const deliveryOtp = String(Math.floor(100000 + Math.random() * 900000));

    return NextResponse.json({
      status: 'SUCCESS',
      message: 'Order placed successfully and routed to nearest dark store',
      data: {
        orderId,
        items,
        address,
        paymentMethod: paymentMethod || 'RAZORPAY',
        deliveryOtp,
        darkStore: 'Nexora Dark Store SG Highway #04',
        etaMinutes: 9,
        assignedRider: {
          name: 'Ramesh Patel',
          vehicle: 'Hero Electric NYX',
          phone: '+91 98765 43210',
          rating: 4.92,
        },
        createdAt: new Date().toISOString(),
      },
    });
  } catch (err: any) {
    return NextResponse.json({ status: 'ERROR', message: err.message }, { status: 400 });
  }
}
