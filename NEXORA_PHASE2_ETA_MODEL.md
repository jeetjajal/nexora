# NEXORA 3.0 — PHASE 2 CONFIDENCE-BASED ETA MODEL

## 1. Executive Summary
Blinkit and other high-performing quick commerce platforms promise delivery in 8–15 minutes. Achieving reliable customer trust requires decomposing total delivery time into discrete physical steps and providing an honest confidence interval rather than a single static number.

---

## 2. End-to-End Delivery Pipeline Components

The total delivery duration $T_{\text{total}}$ consists of 5 deterministic & stochastic segments:

$$T_{\text{total}} = T_{\text{prep}} + T_{\text{dispatch}} + T_{\text{pickup\_transit}} + T_{\text{delivery\_transit}} + T_{\text{handover}}$$

| Segment | Description | Default Nominal Value | Variance Range |
| :--- | :--- | :---: | :---: |
| **$T_{\text{prep}}$** | Dark store picking, packing, bag sealing | $180\text{ s}$ (3.0 mins) | $120\text{ s} - 300\text{ s}$ |
| **$T_{\text{dispatch}}$** | Rider discovery, matching, and acknowledgment | $60\text{ s}$ (1.0 min) | $30\text{ s} - 120\text{ s}$ |
| **$T_{\text{pickup\_transit}}$** | Rider travel from current GPS location to dark store | Computed via Road Routing | $60\text{ s} - 300\text{ s}$ |
| **$T_{\text{delivery\_transit}}$** | Transit from dark store to customer delivery address | Computed via Road Routing | Variable (Road duration) |
| **$T_{\text{handover}}$** | Parking, security gate, elevator, doorstep OTP verification | $120\text{ s}$ (2.0 mins) | $60\text{ s} - 240\text{ s}$ |

---

## 3. Delivery Window & Confidence Scoring

### 3.1. Delivery Window Formulation
- **Window Start ($T_{\min}$):**
  $$T_{\min} = T_{\text{prep},\min} + T_{\text{dispatch},\min} + T_{\text{delivery\_transit}} \times 0.90 + T_{\text{handover},\min}$$
- **Window Expected ($T_{\text{expected}}$):**
  $$T_{\text{expected}} = T_{\text{prep},\text{nominal}} + T_{\text{dispatch},\text{nominal}} + T_{\text{pickup\_transit}} + T_{\text{delivery\_transit}} + T_{\text{handover},\text{nominal}}$$
- **Window End ($T_{\max}$):**
  $$T_{\max} = T_{\text{prep},\max} + T_{\text{dispatch},\max} + T_{\text{pickup\_transit}} + T_{\text{delivery\_transit}} \times 1.30 + T_{\text{handover},\max}$$

### 3.2. Confidence Classification Matrix

| Confidence Level | Criteria | Visual Representation |
| :--- | :--- | :---: |
| **HIGH** | Road routing succeeded via OSRM + Fresh rider telemetry (<30s old) + Rider within 1.5km of dark store | Green Badge: *"High Confidence (Road Routing)"* |
| **MEDIUM** | Road routing succeeded via OSRM, but rider telemetry is between 30s–90s old or rider transit to store > 1.5km | Blue Badge: *"Estimated Window"* |
| **LOW** | Fallback Haversine routing used (OSRM down) OR Rider telemetry is stale (>90s) OR Uncalibrated initial network | Amber Badge: *"Approximate Window (Proximity Fallback)"* |

---

## 4. Response Contract

```json
{
  "orderId": 8801,
  "estimatedDeliveryMinutes": 12,
  "deliveryWindowStart": "2026-11-07T16:45:00",
  "deliveryWindowEnd": "2026-11-07T16:51:00",
  "confidence": "HIGH",
  "roadDistanceMeters": 2450.0,
  "roadDurationSeconds": 480,
  "routingProvider": "OSRM",
  "isFallbackEstimate": false,
  "isRiderLocationStale": false,
  "calculatedAt": "2026-11-07T16:35:10"
}
```
