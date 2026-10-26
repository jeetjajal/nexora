import { NextResponse } from 'next/server';
import { CATEGORIES } from '@/data/categories';

const BACKEND_URL = process.env.BACKEND_URL || 'http://localhost:8080';

export async function GET() {
  try {
    // Attempt to fetch from Spring Boot backend
    const controller = new AbortController();
    const timeoutId = setTimeout(() => controller.abort(), 2000);

    const res = await fetch(`${BACKEND_URL}/api/v1/categories`, {
      headers: { 'Accept': 'application/json' },
      signal: controller.signal,
      cache: 'no-store',
    });
    clearTimeout(timeoutId);

    if (res.ok) {
      const json = await res.json();
      if (json && json.data && Array.isArray(json.data) && json.data.length > 0) {
        // Map backend category response to frontend schema
        const categories = json.data.map((cat: any) => ({
          id: String(cat.id),
          name: cat.name,
          slug: cat.name.toLowerCase().replace(/[^a-z0-9]+/g, '-'),
          icon: cat.imageUrl || '🛒',
          itemCount: 40,
        }));
        return NextResponse.json({
          status: 'SUCCESS',
          source: 'SPRING_BOOT_MYSQL',
          count: categories.length,
          data: categories,
        });
      }
    }
  } catch {
    // Backend offline / network unreachable -> fallback to authoritative seed data
  }

  return NextResponse.json({
    status: 'SUCCESS',
    source: 'SEED_FALLBACK',
    count: CATEGORIES.length,
    data: CATEGORIES,
  });
}
