import { NextRequest, NextResponse } from 'next/server';
import { PRODUCTS } from '@/data/products';

export async function GET(request: NextRequest) {
  const { searchParams } = new URL(request.url);
  const categoryId = searchParams.get('categoryId');
  const search = searchParams.get('q');

  let list = PRODUCTS;

  if (categoryId) {
    list = list.filter(p => p.categoryId === categoryId);
  }

  if (search) {
    const q = search.toLowerCase();
    list = list.filter(p =>
      p.name.toLowerCase().includes(q) ||
      p.brand?.toLowerCase().includes(q) ||
      p.unit.toLowerCase().includes(q)
    );
  }

  return NextResponse.json({
    status: 'SUCCESS',
    count: list.length,
    data: list,
  });
}
