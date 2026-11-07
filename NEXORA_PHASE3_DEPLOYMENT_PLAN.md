# NEXORA 3.0 — PHASE 3 DEPLOYMENT PLAN: DARK STORE WMS, SMART PICKING & S-SHAPE PATH OPTIMIZATION

**Target Branch:** `feature/nexora-phase3-dark-store-wms`  
**Base Commit:** `0289a36`  
**Deployment Model:** Zero-Downtime Blue/Green Staged Rollout  
**Database Target:** TiDB Cloud (MySQL-Compatible)  
**Backend:** Render Web Service (Java 21 / Spring Boot 3)  
**Frontend:** Vercel (Next.js 14)  

---

## 1. Deployment Overview & Pre-Flight Checklist

Phase 3 introduces Warehouse Management System (WMS) functionality without disrupting existing customer shopping, cart management, payments, or rider dispatch.

### Pre-Deployment Verification Checklist
- [x] All 50 unit and integration tests passing (`mvn test`).
- [x] Zero regressions across Phase 1 and Phase 2 suites.
- [x] Flyway migration script `V8__dark_store_wms_layout_and_picking.sql` verified on clean test database.
- [x] Frontend Next.js production build verified with 56 static pages generated without type errors.
- [ ] Staging database backup completed prior to Flyway V8 execution.
- [ ] User/Executive approval received before merging to `main` or triggering production deployments.

---

## 2. Database Migration Plan (Flyway V8)

### Migration Script: `V8__dark_store_wms_layout_and_picking.sql`
- **Execution Strategy:** Additive schema modifications. No destructive column drops, table drops, or type modifications.
- **Affected Tables:**
  * Added columns to `products`: `sku VARCHAR(64) UNIQUE`, `primary_barcode VARCHAR(64)`.
  * Created 13 new WMS tables:
    1. `store_zones`
    2. `store_aisles`
    3. `store_racks`
    4. `store_shelves`
    5. `store_bins`
    6. `product_storage_locations`
    7. `product_barcodes`
    8. `picker_profiles`
    9. `pick_tasks`
    10. `pick_task_items`
    11. `picking_events`
    12. `packing_tasks`
    13. `dispatch_handovers`
- **Backward Compatibility:** All new tables and columns are nullable or have sensible defaults. Existing checkout and delivery pipelines will continue functioning uninterrupted even before dark-store layout seed data is populated.

---

## 3. Deployment Sequence

### Step 1: Pre-Migration Backup
```bash
# TiDB Cloud Point-In-Time Backup
tidb-cloud-cli backup create --cluster-id nexora-prod --database nexora_db --note "Pre-Phase3-WMS"
```

### Step 2: Database Migration Execution
Apply Flyway migration on staging and production:
```bash
mvn flyway:migrate -Dflyway.configFiles=flyway.properties
```

### Step 3: Backend Deployment (Render)
1. Deploy `feature/nexora-phase3-dark-store-wms` to Staging Environment.
2. Verify health endpoints:
   ```bash
   curl -s https://nexora-backend-staging.onrender.com/api/v1/health | jq
   ```
3. Test WMS endpoints:
   - `GET /api/v1/wms/stores/4/layout`
   - `GET /api/v1/wms/stores/4/dashboard/metrics`
4. Promote build to Production Web Service.

### Step 4: Frontend Deployment (Vercel)
1. Trigger Vercel preview deployment for branch `feature/nexora-phase3-dark-store-wms`.
2. Verify `/store/picker` and `/store/packing` interfaces in browser and mobile viewports.
3. Test camera barcode scanner integration on physical handheld scanner / mobile browser.
4. Promote Vercel preview to Production.

---

## 4. Rollback & Contingency Plan

### Backend Rollback:
If an unhandled exception occurs in picking or packing:
1. Revert Render deployment to previous stable image tag (`0289a36`).
2. The V8 database schema changes are strictly additive and will NOT break Phase 1 or Phase 2 backend binaries.
3. Fallback: Store managers can continue using legacy `/store/queue` operations without S-Shape picking.

### Frontend Rollback:
1. In Vercel dashboard, click "Instant Rollback" to previous production deployment.

---

## 5. Post-Deployment Operational Verification

After deployment, dark store operations team should perform a 5-minute smoke test:
1. Place a test order for Store #4 (Ahmedabad SG Highway).
2. Open `/store/picker` and verify that the test order appears with S-Shape sequence numbers (`#1, #2...`) and bin location badges (`ST04-ZAMB-A01-R01-S01-B01`).
3. Scan product barcode using on-screen scanner or camera input.
4. Complete picking and transfer to `/store/packing`.
5. Enter bag seal barcode (`BAG-TEST-001`) and complete packing.
6. Verify that order transitions to `READY` and triggers Phase 2 nearest rider assignment.
7. Hand over to delivery partner and confirm order advances to `PICKED_UP`.
