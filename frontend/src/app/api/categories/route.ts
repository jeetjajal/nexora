import { NextResponse } from 'next/server';
import { CATEGORIES } from '@/data/categories';
import { getBackendUrl } from '@/lib/server-config';

const BACKEND_URL = getBackendUrl();

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
        // Map backend category response to frontend schema and enrich with subcategories
        const categories = json.data.map((cat: any) => {
          const matched = CATEGORIES.find(
            (c) => String(c.id) === String(cat.id) || c.name.toLowerCase() === String(cat.name).toLowerCase()
          );
          return {
            id: String(cat.id),
            name: cat.name,
            slug: matched?.slug || cat.name.toLowerCase().replace(/[^a-z0-9]+/g, '-'),
            icon: cat.imageUrl && cat.imageUrl !== '📦' && cat.imageUrl.length < 10 ? cat.imageUrl : (matched?.icon || '🛒'),
            itemCount: matched?.itemCount || 45,
            highlight: matched?.highlight || false,
            subcategories: matched?.subcategories || [],
          };
        });

        // Ensure all 31 categories are represented
        if (categories.length >= 31) {
          return NextResponse.json({
            status: 'SUCCESS',
            source: 'SPRING_BOOT_MYSQL',
            count: categories.length,
            data: categories,
          });
        }
      }
    }
  } catch {
    // Backend offline / network unreachable -> fallback to authoritative seed data
  }

  return NextResponse.json({
    status: 'SUCCESS',
    source: 'AUTHORITATIVE_31_CATALOGUE_SEED',
    count: CATEGORIES.length,
    data: CATEGORIES,
  });
}
