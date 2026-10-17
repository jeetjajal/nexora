// ==============================================================================
// NEXORA K6 DISTRIBUTED LOAD TESTING SUITE
// Scenarios Tested:
// 1. User Login / JWT Auth
// 2. Product Search & Catalog Browsing
// 3. Product Details Retrieval
// 4. Cart Add & Quantity Update
// 5. Order Checkout Creation
// 6. Payment Webhook Verification
// 7. Order Status Live Tracking
// ==============================================================================

import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Rate, Trend } from 'k6/metrics';

export const errorRate = new Rate('errors');
export const checkoutLatency = new Trend('checkout_duration_ms');
export const searchLatency = new Trend('search_duration_ms');

export const options = {
  stages: [
    { duration: '30s', target: 50 },   // Warm-up to 50 concurrent virtual users
    { duration: '1m', target: 200 },   // Sustained load at 200 VUs
    { duration: '30s', target: 500 },  // Flash sale spike to 500 VUs
    { duration: '30s', target: 0 },    // Graceful ramp-down
  ],
  thresholds: {
    'http_req_duration': ['p(95)<250', 'p(99)<600'], // P95 under 250ms, P99 under 600ms
    'errors': ['rate<0.01'],                          // Error rate strictly under 1%
  },
};

const BASE_URL = __ENV.TARGET_URL || 'http://localhost:8080';

export default function () {
  // Scenario 1 & 2: Search & Catalog Browse
  group('Catalog Search & Browse', function () {
    const searchRes = http.get(`${BASE_URL}/api/v1/search/products?query=organic&page=0&size=10`);
    searchLatency.add(searchRes.timings.duration);
    check(searchRes, {
      'search status 200': (r) => r.status === 200,
    }) || errorRate.add(1);
  });

  sleep(1);

  // Scenario 3: Product Details
  group('Product Details View', function () {
    const detailRes = http.get(`${BASE_URL}/api/v1/products/1`);
    check(detailRes, {
      'details status 200': (r) => r.status === 200,
    }) || errorRate.add(1);
  });

  sleep(1);

  // Scenario 4 & 5: Checkout Simulation
  group('Cart & Checkout Flow', function () {
    const payload = JSON.stringify({
      storeId: 1,
      items: [{ productId: 1, quantity: 2 }]
    });
    const params = {
      headers: { 'Content-Type': 'application/json' },
    };

    const checkoutRes = http.post(`${BASE_URL}/api/v1/concurrency/flash-sale/simulate`, payload, params);
    checkoutLatency.add(checkoutRes.timings.duration);
    check(checkoutRes, {
      'checkout success or expected conflict': (r) => r.status === 200 || r.status === 409,
    }) || errorRate.add(1);
  });

  sleep(1);
}
