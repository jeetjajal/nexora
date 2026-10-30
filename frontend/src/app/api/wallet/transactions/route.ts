import { NextRequest, NextResponse } from 'next/server';
import { WalletService } from '@/lib/wallet';

export async function GET(request: NextRequest) {
  try {
    const userId = request.nextUrl.searchParams.get('userId') || '1';
    const transactions = WalletService.getTransactions(userId);

    return NextResponse.json({
      status: 'SUCCESS',
      data: transactions,
    });
  } catch (err: any) {
    return NextResponse.json(
      { status: 'ERROR', message: err.message || 'Failed to fetch transactions' },
      { status: 500 }
    );
  }
}
