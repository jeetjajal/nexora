# NEXORA 3.0 — PHASE 3: PICK-PATH OPTIMIZATION ALGORITHMS

**Component:** `PickPathOptimizationService`  
**Algorithms:** S-Shape (Serpentine) Heuristic, Nearest-Neighbor Heuristic, Metric Benchmark  
**Objective:** Minimize picker walking distance and eliminate backtracking in dark-store aisles.

---

## 1. Algorithm Mathematical Formulations

### Standard Warehouse Manhattan Metric with Cross-Aisles
Let a warehouse have aisles separated by center-to-center distance $W_a$ (aisle width + rack depth) and aisle length $L_a$.
Cross-aisles run along the front ($Y = 0$) and back ($Y = L_a$).
The walking distance between two locations $P_1 = (x_1, y_1)$ and $P_2 = (x_2, y_2)$ is:

$$D(P_1, P_2) = \begin{cases} 
|y_1 - y_2| & \text{if } x_1 = x_2 \text{ (same aisle)} \\
|x_1 - x_2| + \min(y_1 + y_2, \, 2L_a - (y_1 + y_2)) & \text{if } x_1 \neq x_2 \text{ (cross-aisle transfer)}
\end{cases}$$

---

## 2. S-Shape (Serpentine) Heuristic

### Traversal Rule
1. **Identify Required Aisles:** Find the set of aisles $\mathcal{A}_{\text{active}} = \{A_1, A_2, \dots, A_k\}$ containing at least one item in the order, sorted ascending by aisle number.
2. **Alternating Directions:**
   - **Odd Step $i \in \{1, 3, 5, \dots\}$:** Enter aisle $A_i$ from the front cross-aisle ($Y=0$). Pick all items in $A_i$ in ascending $Y$ order. Traverse completely to the back cross-aisle ($Y=L_a$).
   - **Cross-Aisle Transfer:** Move along back cross-aisle to next active aisle $A_{i+1}$.
   - **Even Step $i \in \{2, 4, 6, \dots\}$:** Enter aisle $A_i$ from the back cross-aisle ($Y=L_a$). Pick all items in $A_i$ in descending $Y$ order. Traverse completely to the front cross-aisle ($Y=0$).
   - **Cross-Aisle Transfer:** Move along front cross-aisle to next active aisle $A_{i+1}$.
3. **Last Aisle Adjustment:**
   - If the last active aisle $A_k$ ends at the back ($Y=L_a$), picker walks back to depot $(0,0)$ via the shortest perimeter aisle or cross-aisle.
4. **Empty Aisle Bypassing:** Aisles with zero pick items are completely bypassed.

```
       [Back Cross-Aisle: Y = L]
      ▲                      │
      │ (Aisle 1: Ascending) │ (Aisle 2: Descending)
      │ [Pick B03]           │ [Pick B05]
      │ [Pick B01]           │ [Pick B04]
      │                      ▼
       [Front Cross-Aisle: Y = 0]
      ▲ (Depot / Packing Station: X=0, Y=0)
```

---

## 3. Nearest-Neighbor Heuristic

### Traversal Rule
1. Start at depot $P_0 = (0, 0)$.
2. Set of unvisited items $\mathcal{U} = \{P_1, P_2, \dots, P_n\}$.
3. While $\mathcal{U} \neq \emptyset$:
   $$P_{\text{next}} = \arg\min_{P \in \mathcal{U}} D(P_{\text{current}}, P)$$
   Add $P_{\text{next}}$ to pick sequence, remove from $\mathcal{U}$, and set $P_{\text{current}} = P_{\text{next}}$.
4. Return from last item to depot $P_0$.

---

## 4. Empirical Algorithm Selection Logic

| Order Profile | Best Algorithm | Reason |
| :--- | :--- | :--- |
| **Small Orders (1–3 items)** | **Nearest-Neighbor** | No need to traverse entire aisles when items are clustered near depot. |
| **Medium / Large Orders ($\ge 4$ items)** | **S-Shape (Serpentine)** | Eliminates backtracking across aisles; deterministic, predictable flow for pickers. |
| **Restricted Aisle Present** | **Constrained S-Shape / NN** | Avoids blocked aisles by rerouting through adjacent cross-aisles. |

The `PickPathOptimizationService` computes both candidate routes in parallel ($<2\text{ms}$ execution time), selects the one with minimum total walking distance, and attaches the algorithm name to the `PickTask`.
