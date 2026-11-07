# NEXORA 3.0 — PHASE 2 API & CONTRACT SPECIFICATION

## 1. Non-Breaking Evolution
All existing API response fields (`orderId`, `status`, `storeName`, `estimatedDeliveryMinutes`, `assignedRiderName`, `timeline`, etc.) are 100% preserved. New fields are additive to maintain backward compatibility with all mobile and web clients.

---

## 2. Updated Endpoints

### 2.1. Customer Order Tracking
- **Method & Path:** `GET /api/v1/orders/{id}/tracking`
- **Security:** `ROLE_CUSTOMER` (owning customer), `STORE_OWNER`, `ADMIN`
- **Additive Fields in `data`:**
  - `deliveryWindowStart` (`string`, ISO-8601 LocalDateTime): Earliest arrival time
  - `deliveryWindowEnd` (`string`, ISO-8601 LocalDateTime): Latest estimated arrival time
  - `confidence` (`string`): `"HIGH"` | `"MEDIUM"` | `"LOW"`
  - `roadDistanceMeters` (`number`, double): Total road route distance in meters
  - `roadDurationSeconds` (`number`, integer): Pure road travel duration in seconds
  - `routingProvider` (`string`): `"OSRM"` | `"HAVERSINE_FALLBACK"`
  - `isFallbackEstimate` (`boolean`): Flag indicating whether road routing was degraded to straight-line proximity
  - `riderLatitude` (`number`, double): Assigned rider's latest known latitude
  - `riderLongitude` (`number`, double): Assigned rider's latest known longitude
  - `riderLocationFreshnessSeconds` (`number`, long): Elapsed seconds since last rider GPS ping
  - `isRiderLocationStale` (`boolean`): `true` if > 90 seconds since last GPS update

### 2.2. Rider Location Ingestion
- **Method & Path:** `POST /api/v1/delivery/location`
- **Security:** `ROLE_DELIVERY_PARTNER`, `ADMIN`
- **Request Body:**
  ```json
  {
    "latitude": 23.0375,
    "longitude": 72.5620,
    "accuracy": 4.5,
    "speed": 22.4,
    "heading": 185.0
  }
  ```
- **Backend Flow:**
  - Authenticated user ID is verified against the calling rider.
  - GPS is validated (bounds check: latitude between -90 and 90, longitude between -180 and 180, non-zero).
  - Telemetry written to Redis geospatial index (`nexora:riders:geo`) and metadata hash (`nexora:rider:meta:{id}`).
  - Redis error degradation: if Redis is temporarily offline, falls back to updating `delivery_partners` table in MySQL.

### 2.3. Route Preview Endpoint (Internal & Admin)
- **Method & Path:** `GET /api/v1/delivery/routes/preview`
- **Security:** `ROLE_ADMIN`, `STORE_MANAGER`, `DELIVERY_PARTNER`
- **Query Parameters:** `originLat`, `originLng`, `destLat`, `destLng`, `profile`
- **Response:**
  ```json
  {
    "status": "SUCCESS",
    "data": {
      "distanceMeters": 3210.5,
      "durationSeconds": 540,
      "routingProvider": "OSRM",
      "isFallbackEstimate": false,
      "geometry": "polyline_encoded_string"
    }
  }
  ```
