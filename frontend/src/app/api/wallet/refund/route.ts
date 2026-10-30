import { NextRequest, NextResponse } from 'next/server';
import { WalletService } from '@/lib/wallet';

export async function POST(request: NextRequest) {
  try {
    const body = await request.json();
    const { userId, orderId, amount, reason } = body;

    if (!orderId || !amount) {
      return NextResponse.json(
        { status: 'ERROR', message: 'Missing orderId or refund amount' },
        { status: 400 }
      );
    }

    const targetUserId = userId || '1';
    const tx = WalletService.processOrderRefund(
      targetUserId,
      orderId,
      Number(amount),
      reason || 'Customer Order Cancellation'
    );
    const wallet = WalletService.getWallet(targetUserId);

    return NextResponse.json({
      status: 'SUCCESS',
      message: `Instant refund of ₹${amount} credited to your Nexora Wallet`,
      data: {
        wallet,
        transaction: tx,
      },
    });
  } catch (err: any) {
    return NextResponse.json(
      { status: 'ERROR', message: err.message || 'Refund processing failed' },
      { status: 400 }
    );
  }
}
