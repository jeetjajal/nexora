# NEXORA 3.0 — PHASE 3: DARK STORE OPERATIONS GUIDE

**Standard Operating Procedure (SOP):** Dark Store Picking, Packing, Quality Control, and Rider Handover  
**Target Invariant:** Total In-Store Fulfillment Time $\le 180\text{ seconds}$ (3 Minutes)

---

## 1. Dark Store Picking SOP

```
   [1. Audio Ping / Order Assigned]
                 │
                 ▼
     [2. Open S-Shape Picklist]
                 │
                 ▼
   [3. Enter Aisle 1 (Front: Y=0)]
                 │
                 ▼
  [4. Scan Product Barcode at Bin]
         ├── If Match: Beep & Green Indicator
         └── If Mismatch: Vibration & Red Mismatch Warning
                 │
                 ▼
    [5. Move to Next Bin in Sequence]
                 │
                 ▼
   [6. Complete Route at Packing Depot]
```

### Picker Rules:
1. **Always Follow S-Shape Order:** Do not skip forward or backward in the picklist. The traversal sequence is mathematically optimized to eliminate aisle backtracking.
2. **Scan Every Item:** Physical scanning via mobile camera or hardware Bluetooth scanner is mandatory. Bypassing scan validation requires manager supervisor override.
3. **Check FEFO Batch:** Ensure the batch number and expiry date printed on packaging matches or exceeds the allocated batch shown on the picklist.
4. **Report Damaged / Missing Stock:** If a bin is empty or stock is damaged, immediately tap "Report Exception" to alert the store manager and trigger approved substitution or partial fulfillment.

---

## 2. Packing & Quality Control SOP

1. **Verify Bag Condition:** Inspect tamper-evident quick-commerce bag for tears or defects.
2. **Scan Packing Bag Code:** Scan the unique pre-printed bag barcode (e.g. `BAG-AHM-04-1234`).
3. **Item Temperature Segregation:**
   - Place ambient FMCG items in bottom/center.
   - Place chilled dairy (2°C to 8°C) in insulated thermal pouch with ice pack.
   - Place frozen goods in dedicated insulated silver pouch.
4. **Seal Bag:** Peel adhesive strip and seal bag firmly.
5. **Tap "Confirm Packed":** Order transitions to `READY`. System automatically matches and dispatches the nearest delivery partner.

---

## 3. Dispatch Handover SOP

1. Delivery rider arrives at the dark store dispatch bay.
2. Store operator asks rider for Rider ID or Delivery Assignment Code.
3. Operator scans or enters the sealed `BAG-XXXXXX` barcode.
4. Handover is recorded with timestamp and operator ID.
5. Rider receives bag, taps "Order Picked Up" in the Rider Mobile App, and starts transit to customer address.
