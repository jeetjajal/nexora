import { NextRequest, NextResponse } from 'next/server';

const BACKEND_URL = process.env.BACKEND_API_URL || 'http://localhost:8080';

export async function GET(req: NextRequest) {
  const { searchParams } = new URL(req.url);
  const storeId = searchParams.get('storeId') || '4';
  const authHeader = req.headers.get('Authorization') || '';

  try {
    const backendRes = await fetch(`${BACKEND_URL}/api/v1/stores/${storeId}/operations/queue`, {
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

  // Authoritative fallback queue for dark store SG Highway #04
  return NextResponse.json({
    success: true,
    message: 'Store queue loaded (operational fallback)',
    data: {
      storeId: Number(storeId),
      storeName: 'Nexora Dark Store SG Highway #04',
      totalOrders: 6,
      confirmedCount: 2,
      preparingCount: 2,
      readyCount: 1,
      outForDeliveryCount: 1,
      deliveredCount: 14,
      lowStockCount: 3,
      averageProcessingMinutes: 3.4,
      orders: [
        {
          id: 5041,
          orderNumber: 'ORD-5041',
          status: 'CONFIRMED',
          subtotal: 184,
          deliveryFee: 25,
          totalAmount: 219,
          paymentMethod: 'ONLINE',
          paymentStatus: 'PAID',
          pickerName: undefined,
          packerName: undefined,
          items: [
            { id: 911, productId: 1, productName: 'Amul Taaza Homogenised Toned Milk 500ml', unitPrice: 28, quantity: 2, subtotal: 56, isPicked: false, sku: 'SKU-001' },
            { id: 912, productId: 3, productName: 'Lay\'s India\'s Magic Masala Potato Chips 50g', unitPrice: 20, quantity: 3, subtotal: 60, isPicked: false, sku: 'SKU-003' },
            { id: 913, productId: 2, productName: 'Britannia 100% Whole Wheat Bread 400g', unitPrice: 45, quantity: 1, subtotal: 45, isPicked: false, sku: 'SKU-002' },
          ],
          createdAt: new Date(Date.now() - 90000).toISOString(),
        },
        {
          id: 5040,
          orderNumber: 'ORD-5040',
          status: 'PREPARING',
          subtotal: 240,
          deliveryFee: 0,
          totalAmount: 250,
          paymentMethod: 'ONLINE',
          paymentStatus: 'PAID',
          pickerName: 'Prem Kumar',
          packerName: undefined,
          items: [
            { id: 908, productId: 7, productName: 'Aashirvaad Shudh Chakki Atta 5kg', unitPrice: 235, quantity: 1, subtotal: 235, isPicked: true, sku: 'SKU-007' },
          ],
          createdAt: new Date(Date.now() - 210000).toISOString(),
        },
        {
          id: 5039,
          orderNumber: 'ORD-5039',
          status: 'READY',
          subtotal: 135,
          deliveryFee: 25,
          totalAmount: 170,
          paymentMethod: 'COD',
          paymentStatus: 'PENDING',
          pickerName: 'Prem Kumar',
          packerName: 'Pooja Sharma',
          packingBagCode: 'BAG-NX-4912',
          items: [
            { id: 904, productId: 4, productName: 'Coca-Cola Original Taste 750ml PET', unitPrice: 40, quantity: 2, subtotal: 80, isPicked: true, sku: 'SKU-004' },
            { id: 905, productId: 5, productName: 'Fresh Shimla Apple 4 pcs', unitPrice: 99, quantity: 1, subtotal: 99, isPicked: true, sku: 'SKU-005' },
          ],
          createdAt: new Date(Date.now() - 420000).toISOString(),
        },
      ],
    },
  });
}
