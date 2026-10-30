import { NextRequest, NextResponse } from 'next/server';
import { WalletService } from '@/lib/wallet';

export async function GET(request: NextRequest) {
  try {
    const authHeader = request.headers.get('Authorization');
    const userId = request.nextUrl.searchParams.get('userId') || '1';

    const wallet = WalletService.getWallet(userId);
    return NextResponse.json({
      status: 'SUCCESS',
      data: wallet,
    });
  } catch (err: any) {
    return NextResponse.json(
      { status: 'ERROR', message: err.message || 'Failed to fetch wallet' },
      { status: 500 }
    );
  }
}

export async function POST(request: NextRequest) {
  try {
    const body = await request.json();
    const { userId, amount, action, orderId } = body;

    const targetUserId = userId || '1';
    const numAmount = Number(amount);

    if (!numAmount || numAmount <= 0) {
      return NextResponse.json(
        { status: 'ERROR', message: 'Valid positive amount is required' },
        { status: 400 }
      );
    }

    if (action === 'TOPUP') {
      const tx = WalletService.creditWallet(
        targetUserId,
        numAmount,
        'TOPUP',
        null,
        `Quick Topup via UPI / Netbanking`
      );
      const wallet = WalletService.getWallet(targetUserId);

      return NextResponse.json({
        status: 'SUCCESS',
        message: `₹${numAmount} added to your Nexora Wallet successfully`,
        data: { wallet, transaction: tx },
      });
    }

    if (action === 'PAY_ORDER') {
      if (!orderId) {
        return NextResponse.json(
          { status: 'ERROR', message: 'Order ID is required for payment' },
          { status: 400 }
        );
      }

      const tx = WalletService.debitWallet(
        targetUserId,
        numAmount,
        orderId,
        `Payment for Order #${orderId}`
      );
      const wallet = WalletService.getWallet(targetUserId);

      return NextResponse.json({
        status: 'SUCCESS',
        message: `Payment of ₹${numAmount} completed via Nexora Wallet`,
        data: { wallet, transaction: tx },
      });
    }

    return NextResponse.json(
      { status: 'ERROR', message: 'Invalid action. Supported: TOPUP, PAY_ORDER' },
      { status: 400 }
    );
  } catch (err: any) {
    return NextResponse.json(
      { status: 'ERROR', message: err.message || 'Wallet transaction failed' },
      { status: 400 }
    );
  }
}
