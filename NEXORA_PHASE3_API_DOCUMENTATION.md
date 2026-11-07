# NEXORA 3.0 — PHASE 3: WMS API SPECIFICATION

**Base Path:** `/api/v1/wms`  
**Authentication:** Bearer JWT (`Authorization: Bearer <token>`)  
**RBAC Roles:** `STORE_OWNER`, `STORE_MANAGER`, `PICKER`, `PACKER`, `ADMIN`, `SUPER_ADMIN`

---

## 1. Store Layout & Bin Endpoints

### `GET /api/v1/wms/stores/{storeId}/layout`
- **Description:** Retrieves the complete warehouse layout tree: zones, aisles, racks, shelves, and bins.
- **Roles:** `STORE_MANAGER`, `STORE_OWNER`, `ADMIN`, `SUPER_ADMIN`
- **Response:**
  ```json
  {
    "status": "SUCCESS",
    "data": {
      "storeId": 4,
      "zones": [
        {
          "zoneCode": "ZONE-AMBIENT",
          "name": "Ambient Grocery & FMCG Zone",
          "temperatureType": "AMBIENT",
          "aisles": [
            {
              "aisleCode": "A01",
              "aisleNumber": 1,
              "racks": [
                {
                  "rackCode": "R01",
                  "side": "LEFT",
                  "shelves": [
                    {
                      "shelfCode": "S01",
                      "level": 1,
                      "bins": [
                        {
                          "binCode": "B01",
                          "fullLocationCode": "ST04-ZAMB-A01-R01-S01-B01",
                          "traversalSequence": 10,
                          "xCoord": 2.0,
                          "yCoord": 2.5,
                          "capacityUnits": 100
                        }
                      ]
                    }
                  ]
                }
              ]
            }
          ]
        }
      ]
    }
  }
  ```

---

## 2. Barcode & Scanner Resolution Endpoints

### `POST /api/v1/wms/barcode/resolve`
- **Description:** Resolves any barcode (EAN-13, SKU, QR) to product metadata and storage locations.
- **Request:**
  ```json
  {
    "storeId": 4,
    "barcode": "8901262010053"
  }
  ```
- **Response:**
  ```json
  {
    "status": "SUCCESS",
    "data": {
      "productId": 101,
      "productName": "Amul Gold Full Cream Milk 500ml",
      "sku": "NX-MILK-AMUL-500",
      "primaryBin": "ST04-ZCOLD-A04-R01-S02-B01",
      "availableBatches": [
        {
          "batchId": 401,
          "batchNumber": "BAT-20261107-01",
          "expiryDate": "2026-11-12",
          "temperatureZone": "CHILLED_2_TO_8"
        }
      ]
    }
  }
  ```

---

## 3. Pick Task & S-Shape Picklist Endpoints

### `GET /api/v1/wms/orders/{orderId}/picklist`
- **Description:** Generates or retrieves the optimized S-Shape picklist for an order.
- **Roles:** `PICKER`, `STORE_MANAGER`, `ADMIN`
- **Response:**
  ```json
  {
    "status": "SUCCESS",
    "data": {
      "pickTaskId": 501,
      "taskCode": "PICK-9001-A48",
      "orderId": 9001,
      "pickerId": 12,
      "pickerName": "Arjun Sharma",
      "status": "IN_PROGRESS",
      "selectedAlgorithm": "S_SHAPE",
      "estimatedDistanceMeters": 34.5,
      "estimatedTimeSeconds": 48,
      "items": [
        {
          "pickTaskItemId": 1201,
          "orderItemId": 8801,
          "productId": 101,
          "productName": "Amul Gold Milk 500ml",
          "requiredQuantity": 2,
          "pickedQuantity": 0,
          "binCode": "ST04-ZCOLD-A04-R01-S02-B01",
          "batchNumber": "BAT-20261107-01",
          "expiryDate": "2026-11-12",
          "sequenceIndex": 1,
          "status": "PENDING"
        }
      ]
    }
  }
  ```

### `POST /api/v1/wms/picklist/items/{pickTaskItemId}/scan`
- **Description:** Verifies scanned barcode, checks FEFO batch eligibility, and increments picked quantity.
- **Request:**
  ```json
  {
    "scannedBarcode": "8901262010053",
    "scannedBatchNumber": "BAT-20261107-01",
    "quantity": 2
  }
  ```
- **Response:**
  ```json
  {
    "status": "SUCCESS",
    "data": {
      "pickTaskItemId": 1201,
      "status": "PICKED",
      "pickedQuantity": 2,
      "isVerified": true
    }
  }
  ```

---

## 4. Packing, Quality Control & Dispatch Handover Endpoints

### `POST /api/v1/wms/orders/{orderId}/packing/complete`
- **Description:** Verifies 100% picking completion, checks bag seal, and transitions order to READY for delivery dispatch.
- **Request:**
  ```json
  {
    "bagCode": "BAG-AHM-04-88912",
    "qcPassed": true
  }
  ```

### `POST /api/v1/wms/orders/{orderId}/dispatch/handover`
- **Description:** Handover of sealed bag to assigned delivery rider with physical verification.
- **Request:**
  ```json
  {
    "deliveryPartnerId": 201,
    "bagCode": "BAG-AHM-04-88912"
  }
  ```
