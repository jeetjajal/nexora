package com.nexora.wms;

import com.nexora.order.entity.OrderItem;
import com.nexora.product.entity.Product;
import com.nexora.wms.entity.*;
import com.nexora.wms.enums.PickPathAlgorithm;
import com.nexora.wms.service.PickPathOptimizationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Phase 3: WS3 — S-Shape & Pick Path Optimization Algorithm Tests")
class PickPathOptimizationServiceTest {

    private PickPathOptimizationService optimizationService;

    private StoreAisle aisle1;
    private StoreAisle aisle2;
    private StoreAisle aisle3;

    private StoreRack rack1;
    private StoreRack rack2;
    private StoreRack rack3;

    private StoreShelf shelf1;
    private StoreShelf shelf2;
    private StoreShelf shelf3;

    @BeforeEach
    void setUp() {
        optimizationService = new PickPathOptimizationService();

        aisle1 = StoreAisle.builder().id(1L).aisleNumber(1).build();
        aisle2 = StoreAisle.builder().id(2L).aisleNumber(2).build();
        aisle3 = StoreAisle.builder().id(3L).aisleNumber(3).build();

        rack1 = StoreRack.builder().id(10L).aisle(aisle1).build();
        rack2 = StoreRack.builder().id(20L).aisle(aisle2).build();
        rack3 = StoreRack.builder().id(30L).aisle(aisle3).build();

        shelf1 = StoreShelf.builder().id(100L).rack(rack1).build();
        shelf2 = StoreShelf.builder().id(200L).rack(rack2).build();
        shelf3 = StoreShelf.builder().id(300L).rack(rack3).build();
    }

    private PickTaskItem createItem(Long id, StoreShelf shelf, double x, double y, String name) {
        Product p = new Product();
        p.setId(id);
        p.setName(name);

        StoreBin bin = StoreBin.builder()
                .id(id + 1000)
                .shelf(shelf)
                .xCoord(BigDecimal.valueOf(x))
                .yCoord(BigDecimal.valueOf(y))
                .fullLocationCode("BIN-" + id)
                .build();

        OrderItem oi = new OrderItem();
        oi.setId(id);
        oi.setProduct(p);
        oi.setQuantity(1);

        return PickTaskItem.builder()
                .id(id)
                .orderItem(oi)
                .product(p)
                .bin(bin)
                .requiredQuantity(1)
                .pickedQuantity(0)
                .build();
    }

    @Test
    @DisplayName("WS3: Empty or single item picklist handling")
    void testSingleAndEmptyItems() {
        PickPathOptimizationService.OptimizedRouteResult emptyResult =
                optimizationService.optimizePickRoute(new ArrayList<>());
        assertEquals(0, emptyResult.getOrderedItems().size());
        assertEquals(0.0, emptyResult.getTotalDistanceMeters());

        PickTaskItem single = createItem(1L, shelf1, 2.0, 4.0, "Bread");
        PickPathOptimizationService.OptimizedRouteResult singleResult =
                optimizationService.optimizePickRoute(List.of(single));

        assertEquals(1, singleResult.getOrderedItems().size());
        assertTrue(singleResult.getTotalDistanceMeters() > 0);
        assertTrue(singleResult.getEstimatedDurationSeconds() > 0);
    }

    @Test
    @DisplayName("WS3: Serpentine S-Shape alternating aisle direction traversal")
    void testSerpentineSShapeTraversal() {
        // Items in Aisle 1 (Odd: traverse ascending y 2.0 -> 8.0)
        PickTaskItem a1Item1 = createItem(1L, shelf1, 2.0, 8.0, "Item A1-High");
        PickTaskItem a1Item2 = createItem(2L, shelf1, 2.0, 2.0, "Item A1-Low");

        // Items in Aisle 2 (Even: traverse descending y 10.0 -> 3.0)
        PickTaskItem a2Item1 = createItem(3L, shelf2, 5.0, 3.0, "Item A2-Low");
        PickTaskItem a2Item2 = createItem(4L, shelf2, 5.0, 10.0, "Item A2-High");

        // Items in Aisle 3 (Odd: traverse ascending y 1.0 -> 7.0)
        PickTaskItem a3Item1 = createItem(5L, shelf3, 8.0, 7.0, "Item A3-High");
        PickTaskItem a3Item2 = createItem(6L, shelf3, 8.0, 1.0, "Item A3-Low");

        List<PickTaskItem> items = List.of(a1Item1, a1Item2, a2Item1, a2Item2, a3Item1, a3Item2);

        PickPathOptimizationService.OptimizedRouteResult result = optimizationService.optimizePickRoute(items);

        assertEquals(6, result.getOrderedItems().size());
        assertEquals(PickPathAlgorithm.S_SHAPE, result.getSelectedAlgorithm());

        List<PickTaskItem> ordered = result.getOrderedItems();

        // Sequence indices must be 1 to 6
        for (int i = 0; i < ordered.size(); i++) {
            assertEquals(i + 1, ordered.get(i).getSequenceIndex());
        }

        // In Aisle 1 (odd), Item A1-Low (y=2.0) must precede Item A1-High (y=8.0)
        int idxA1Low = ordered.indexOf(a1Item2);
        int idxA1High = ordered.indexOf(a1Item1);
        assertTrue(idxA1Low < idxA1High, "Odd aisle must traverse ascending Y coordinates");

        // In Aisle 2 (even), Item A2-High (y=10.0) must precede Item A2-Low (y=3.0)
        int idxA2High = ordered.indexOf(a2Item2);
        int idxA2Low = ordered.indexOf(a2Item1);
        assertTrue(idxA2High < idxA2Low, "Even aisle must traverse descending Y coordinates (serpentine turn)");

        // In Aisle 3 (odd), Item A3-Low (y=1.0) must precede Item A3-High (y=7.0)
        int idxA3Low = ordered.indexOf(a3Item2);
        int idxA3High = ordered.indexOf(a3Item1);
        assertTrue(idxA3Low < idxA3High, "Next odd aisle must traverse ascending Y coordinates");
    }

    @Test
    @DisplayName("WS3: Empty aisle skipping preserves continuity")
    void testEmptyAisleSkipping() {
        // Items in Aisle 1 and Aisle 3 (Aisle 2 is completely empty)
        PickTaskItem itemA1 = createItem(1L, shelf1, 2.0, 5.0, "Butter");
        PickTaskItem itemA3 = createItem(2L, shelf3, 8.0, 5.0, "Juice");

        List<PickTaskItem> items = List.of(itemA1, itemA3);
        PickPathOptimizationService.OptimizedRouteResult result = optimizationService.optimizePickRoute(items);

        assertEquals(2, result.getOrderedItems().size());
        assertTrue(result.getTotalDistanceMeters() > 0);
        assertTrue(result.getEstimatedDurationSeconds() > 0);
    }
}
