import { NextRequest } from 'next/server';
import { POST as handleVerify } from '@/app/api/payment/verify/route';

export async function POST(request: NextRequest) {
  return handleVerify(request);
}
