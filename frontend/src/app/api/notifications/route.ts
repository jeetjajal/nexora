import { NextRequest, NextResponse } from 'next/server';
import { getNotificationService, NotificationPayload } from '@/lib/notifications';

export async function POST(request: NextRequest) {
  try {
    const body: NotificationPayload = await request.json();

    if (!body.orderId || !body.recipientPhone || !body.eventType) {
      return NextResponse.json(
        { status: 'ERROR', message: 'Missing orderId, recipientPhone, or eventType' },
        { status: 400 }
      );
    }

    const notificationService = getNotificationService();
    const result = await notificationService.send(body);

    return NextResponse.json({
      status: 'SUCCESS',
      data: result,
    });
  } catch (err: any) {
    return NextResponse.json(
      { status: 'ERROR', message: err.message || 'Notification sending failed' },
      { status: 500 }
    );
  }
}
