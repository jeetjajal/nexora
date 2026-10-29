import { NextRequest, NextResponse } from 'next/server';
import { CATEGORIES } from '@/data/categories';
import { getBackendUrl } from '@/lib/server-config';

const BACKEND_URL = getBackendUrl();

export async function GET(request: NextRequest) {
  const { searchParams } = new URL(request.url);
  const categoryId = searchParams.get('categoryId');

  try {
    const controller = new AbortController();
    const timeoutId = setTimeout(() => controller.abort(), 2000);

    const query = new URLSearchParams();
    if (categoryId) query.set('categoryId', categoryId);

    const res = await fetch(`${BACKEND_URL}/api/v1/subcategories?${query.toString()}`, {
      headers: { 'Accept': 'application/json' },
      signal: controller.signal,
      cache: 'no-store',
    });
    clearTimeout(timeoutId);

    if (res.ok) {
      const json = await res.json();
      if (json && json.data && Array.isArray(json.data) && json.data.length > 0) {
        return NextResponse.json({
          status: 'SUCCESS',
          source: 'SPRING_BOOT_MYSQL',
          count: json.data.length,
          data: json.data,
        });
      }
    }
  } catch {
    // Fallback to authoritative 31-category catalogue seed
  }

  // Extract from CATEGORIES
  let allSubcategories: any[] = [];
  CATEGORIES.forEach(cat => {
    if (!categoryId || String(cat.id) === categoryId) {
      (cat.subcategories || []).forEach(sub => {
        allSubcategories.push({
          id: sub.id,
          categoryId: Number(cat.id),
          categoryName: cat.name,
          name: sub.name,
          slug: sub.slug,
          examples: sub.examples,
          imageUrl: '📦',
          active: true,
        });
      });
    }
  });

  return NextResponse.json({
    status: 'SUCCESS',
    source: 'AUTHORITATIVE_CATALOGUE_SEED',
    count: allSubcategories.length,
    data: allSubcategories,
  });
}
