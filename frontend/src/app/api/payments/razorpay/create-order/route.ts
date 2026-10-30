import { NextRequest } from 'next/server';
import { POST as handleCreateOrder } from '@/app/api/payment/create-order/route';

export async function POST(request: NextRequest) {
  return handleCreateOrder(request);
}
