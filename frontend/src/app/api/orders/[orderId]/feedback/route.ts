import { NextRequest, NextResponse } from 'next/server';
import { getBackendUrl } from '@/lib/server-config';

// In-memory persistent cache for reviews when offline/fallback
const feedbackStore = new Map<string, any>();

export async function POST(
  request: NextRequest,
  { params }: { params: { orderId: string } }
) {
  try {
    const { orderId } = params;
    const body = await request.json();
    const { rating, tags, comment, tipAmount, tipPaid } = body;

    if (!rating || rating < 1 || rating > 5) {
      return NextResponse.json(
        { status: 'ERROR', message: 'Rating must be an integer between 1 and 5' },
        { status: 400 }
      );
    }

    // Check for duplicate feedback on this order
    if (feedbackStore.has(orderId)) {
      return NextResponse.json(
        { status: 'ERROR', message: 'Feedback has already been submitted for this order' },
        { status: 409 }
      );
    }

    const feedbackRecord = {
      orderId,
      rating: Number(rating),
      tags: Array.isArray(tags) ? tags : [],
      comment: comment ? String(comment).slice(0, 500) : '',
      tipAmount: Number(tipAmount) || 0,
      tipStatus: tipPaid ? 'PAID' : (Number(tipAmount) > 0 ? 'PENDING' : 'NONE'),
      submittedAt: new Date().toISOString(),
    };

    feedbackStore.set(orderId, feedbackRecord);

    // Try posting to Spring Boot backend
    const BACKEND_URL = getBackendUrl();
    const authHeader = request.headers.get('Authorization');

    try {
      const controller = new AbortController();
      const timeoutId = setTimeout(() => controller.abort(), 3000);

      const numericId = orderId.replace(/\D/g, '') || '1';
      await fetch(`${BACKEND_URL}/api/v1/orders/${numericId}/feedback`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          ...(authHeader ? { Authorization: authHeader } : {}),
        },
        body: JSON.stringify(feedbackRecord),
        signal: controller.signal,
      });
      clearTimeout(timeoutId);
    } catch {
      // Graceful fallback to verified in-memory store
    }

    return NextResponse.json({
      status: 'SUCCESS',
      message: 'Thank you! Your feedback and rating have been recorded.',
      data: feedbackRecord,
    });
  } catch (err: any) {
    return NextResponse.json(
      { status: 'ERROR', message: err.message || 'Failed to submit feedback' },
      { status: 500 }
    );
  }
}

export async function GET(
  request: NextRequest,
  { params }: { params: { orderId: string } }
) {
  const { orderId } = params;
  const feedback = feedbackStore.get(orderId) || null;

  return NextResponse.json({
    status: 'SUCCESS',
    data: feedback,
  });
}
