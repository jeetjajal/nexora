import { NextRequest, NextResponse } from 'next/server';
import { PRODUCTS } from '@/data/products';
import { getBackendUrl } from '@/lib/server-config';

const BACKEND_URL = getBackendUrl();

export async function GET(request: NextRequest) {
  const { searchParams } = new URL(request.url);
  const categoryId = searchParams.get('categoryId');
  const subcategoryId = searchParams.get('subcategoryId');
  const search = searchParams.get('q');
  const minPrice = searchParams.get('minPrice');
  const maxPrice = searchParams.get('maxPrice');

  try {
    const controller = new AbortController();
    const timeoutId = setTimeout(() => controller.abort(), 2000);

    const query = new URLSearchParams();
    if (search) query.set('name', search);
    if (minPrice) query.set('minPrice', minPrice);
    if (maxPrice) query.set('maxPrice', maxPrice);

    const res = await fetch(`${BACKEND_URL}/api/v1/products?${query.toString()}`, {
      headers: { 'Accept': 'application/json' },
      signal: controller.signal,
      cache: 'no-store',
    });
    clearTimeout(timeoutId);

    if (res.ok) {
      const json = await res.json();
      if (json && json.data && json.data.content && Array.isArray(json.data.content) && json.data.content.length > 0) {
        const backendProducts = json.data.content.map((p: any) => ({
          id: String(p.id),
          name: p.name,
          categoryId: p.categoryNames && p.categoryNames[0] ? '1' : '1',
          price: Number(p.price),
          mrp: Number(p.price) + Math.round(Number(p.discount || 0)),
          discount: Number(p.discount || 0),
          unit: '1 unit',
          image: p.imageUrl || 'https://images.unsplash.com/photo-1542838132-92c53300491e?auto=format&fit=crop&w=500&q=80',
          rating: 4.8,
          reviewsCount: 120,
          inStock: p.available ?? true,
          deliveryTime: '9 MINS',
          brand: 'Nexora Fresh',
        }));

        let filtered = backendProducts;
        if (categoryId) {
          filtered = filtered.filter((p: any) => p.categoryId === categoryId);
        }

        return NextResponse.json({
          status: 'SUCCESS',
          source: 'SPRING_BOOT_MYSQL',
          count: filtered.length,
          data: filtered,
        });
      }
    }
  } catch {
    // Backend offline -> fallback to high-fidelity frontend seed data
  }

  let list = PRODUCTS;

  if (categoryId) {
    list = list.filter(p => p.categoryId === categoryId);
  }

  if (subcategoryId) {
    list = list.filter(p => p.subcategoryId === subcategoryId);
  }

  if (search) {
    const q = search.toLowerCase();
    list = list.filter(p =>
      p.name.toLowerCase().includes(q) ||
      p.brand?.toLowerCase().includes(q) ||
      p.unit.toLowerCase().includes(q)
    );
  }

  if (minPrice) {
    list = list.filter(p => p.price >= Number(minPrice));
  }

  if (maxPrice) {
    list = list.filter(p => p.price <= Number(maxPrice));
  }

  return NextResponse.json({
    status: 'SUCCESS',
    source: 'SEED_FALLBACK',
    count: list.length,
    data: list,
  });
}
