import { NextRequest, NextResponse } from 'next/server';

const BACKEND_URL = process.env.BACKEND_API_URL || 'http://localhost:8080';

export async function GET(req: NextRequest) {
  const { searchParams } = new URL(req.url);
  const storeId = searchParams.get('storeId') || '4';
  const authHeader = req.headers.get('Authorization') || '';

  try {
    const backendRes = await fetch(`${BACKEND_URL}/api/v1/stores/${storeId}/inventory`, {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json',
        ...(authHeader ? { Authorization: authHeader } : {}),
      },
      cache: 'no-store',
    });

    if (backendRes.ok) {
      const data = await backendRes.json();
      return NextResponse.json(data);
    }
  } catch {
    // Backend offline / preview mode fallback
  }

  return NextResponse.json({
    success: true,
    message: 'Store inventory loaded (fallback mode)',
    data: [
      { productId: 1, productName: 'Amul Taaza Homogenised Toned Milk 500ml', sku: 'SKU-001', unit: '500ml', price: 28, quantity: 4, lowStockThreshold: 10, status: 'LOW_STOCK' },
      { productId: 2, productName: 'Britannia 100% Whole Wheat Bread 400g', sku: 'SKU-002', unit: '400g', price: 45, quantity: 18, lowStockThreshold: 5, status: 'IN_STOCK' },
      { productId: 3, productName: 'Lay\'s India\'s Magic Masala Potato Chips 50g', sku: 'SKU-003', unit: '50g', price: 20, quantity: 2, lowStockThreshold: 8, status: 'LOW_STOCK' },
      { productId: 4, productName: 'Coca-Cola Original Taste 750ml PET', sku: 'SKU-004', unit: '750ml', price: 40, quantity: 0, lowStockThreshold: 5, status: 'OUT_OF_STOCK' },
      { productId: 5, productName: 'Fresh Shimla Apple 4 pcs', sku: 'SKU-005', unit: '4 pcs', price: 99, quantity: 14, lowStockThreshold: 5, status: 'IN_STOCK' },
      { productId: 6, productName: 'Tata Salt Vacuum Evaporated Iodised 1kg', sku: 'SKU-006', unit: '1kg', price: 28, quantity: 35, lowStockThreshold: 10, status: 'IN_STOCK' },
      { productId: 7, productName: 'Aashirvaad Shudh Chakki Atta 5kg', sku: 'SKU-007', unit: '5kg', price: 235, quantity: 12, lowStockThreshold: 5, status: 'IN_STOCK' },
    ],
  });
}
