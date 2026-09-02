package org.blocovermelho.ae2emi.network;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.junit.jupiter.api.Test;

class BalancedGridFillerTest {
    private static BalancedGridFiller.Slot<String> slot(String key, int count) {
        return new BalancedGridFiller.Slot<>(key, count, 64);
    }

    private static class Source implements BalancedGridFiller.ItemSource<String> {
        final Map<String, Integer> items = new HashMap<>();
        int maxActualExtraction = Integer.MAX_VALUE;
        int extracted;

        Source(String key, int amount) {
            items.put(key, amount);
        }

        @Override
        public int extract(String key, int amount, boolean simulate) {
            int available = items.getOrDefault(key, 0);
            int moved = Math.min(amount, available);
            if (!simulate) {
                moved = Math.min(moved, maxActualExtraction);
                items.put(key, available - moved);
                extracted += moved;
            }
            return moved;
        }
    }

    @Test
    void nineItemsAreSplitThreeWaysInsteadOfSevenOneOne() {
        var source = new Source("glass", 6); // AE2 already seeded one in each slot.
        var slots = List.of(slot("glass", 1), slot("glass", 1), slot("glass", 1));
        assertArrayEquals(new int[] { 3, 3, 3 }, BalancedGridFiller.fill(slots, 64, source));
        assertEquals(6, source.extracted);
    }

    @Test
    void incompleteExtraBatchStaysInStorage() {
        var source = new Source("glass", 7);
        var slots = List.of(slot("glass", 1), slot("glass", 1), slot("glass", 1));
        assertArrayEquals(new int[] { 3, 3, 3 }, BalancedGridFiller.fill(slots, 64, source));
        assertEquals(1, source.items.get("glass"));
    }

    @Test
    void existingUnevenStacksAreRebalancedWithoutExtracting() {
        var source = new Source("glass", 0);
        var slots = List.of(slot("glass", 7), slot("glass", 1), slot("glass", 1));
        assertArrayEquals(new int[] { 3, 3, 3 }, BalancedGridFiller.fill(slots, 64, source));
        assertEquals(0, source.extracted);
    }

    @Test
    void existingRemainderIsPreservedRatherThanDeleted() {
        var source = new Source("glass", 0);
        var slots = List.of(slot("glass", 8), slot("glass", 1), slot("glass", 1));
        assertArrayEquals(new int[] { 4, 3, 3 }, BalancedGridFiller.fill(slots, 64, source));
    }

    @Test
    void fixedBatchRequestDoesNotOverfill() {
        var source = new Source("glass", 100);
        var slots = List.of(slot("glass", 1), slot("glass", 1), slot("glass", 1));
        assertArrayEquals(new int[] { 2, 2, 2 }, BalancedGridFiller.fill(slots, 2, source));
        assertEquals(3, source.extracted);
    }

    @Test
    void excessAlreadyInGridIsNotLostWhenRequestingFewerBatches() {
        var source = new Source("glass", 100);
        var slots = List.of(slot("glass", 64), slot("glass", 1), slot("glass", 1));
        assertArrayEquals(new int[] { 22, 22, 22 }, BalancedGridFiller.fill(slots, 2, source));
        assertEquals(0, source.extracted);
    }

    @Test
    void distinctItemsAndMetadataKeysNeverShareTheirPool() {
        var source = new Source("glass:clear", 4);
        source.items.put("glass:tinted", 1);
        var slots = List.of(slot("glass:clear", 1), slot("glass:tinted", 1), slot("glass:clear", 1));
        assertArrayEquals(new int[] { 3, 2, 3 }, BalancedGridFiller.fill(slots, 64, source));
    }

    @Test
    void nonStackableToolsDoNotLimitOtherIngredients() {
        var source = new Source("glass", 200);
        var slots = List.of(slot("glass", 1), new BalancedGridFiller.Slot<>("tool", 1, 1), slot("glass", 1));
        assertArrayEquals(new int[] { 64, 1, 64 }, BalancedGridFiller.fill(slots, 64, source));
    }

    @Test
    void respectsSmallStackLimits() {
        var source = new Source("pearl", 100);
        var slots = List.of(new BalancedGridFiller.Slot<>("pearl", 1, 16),
                new BalancedGridFiller.Slot<>("pearl", 1, 16));
        assertArrayEquals(new int[] { 16, 16 }, BalancedGridFiller.fill(slots, 64, source));
        assertEquals(30, source.extracted);
    }

    @Test
    void unlimitedFillIsStillBoundedByEachItemsStackLimit() {
        var source = new Source("glass", 1000);
        var slots = List.of(slot("glass", 1), slot("glass", 1), slot("glass", 1));
        assertArrayEquals(new int[] { 64, 64, 64 },
                BalancedGridFiller.fill(slots, Integer.MAX_VALUE, source));
        assertEquals(189, source.extracted);
    }

    @Test
    void redistributingExistingItemsRespectsDifferentSlotLimits() {
        var source = new Source("glass", 0);
        var slots = List.of(slot("glass", 64), new BalancedGridFiller.Slot<>("glass", 1, 16), slot("glass", 1));
        assertArrayEquals(new int[] { 25, 16, 25 }, BalancedGridFiller.fill(slots, 64, source));
    }

    @Test
    void partialExtractionIsBalancedAndConservesActualItems() {
        var source = new Source("glass", 100);
        source.maxActualExtraction = 4; // Storage/power changed after simulation.
        var slots = List.of(slot("glass", 1), slot("glass", 1), slot("glass", 1));
        assertArrayEquals(new int[] { 3, 2, 2 }, BalancedGridFiller.fill(slots, 64, source));
        assertEquals(4, source.extracted);
    }

    @Test
    void zeroActualExtractionDoesNotCreateItems() {
        var source = new Source("glass", 100);
        source.maxActualExtraction = 0;
        var slots = List.of(slot("glass", 1), slot("glass", 1), slot("glass", 1));
        assertArrayEquals(new int[] { 1, 1, 1 }, BalancedGridFiller.fill(slots, 64, source));
        assertEquals(0, source.extracted);
    }

    @Test
    void singleFillPreservesExistingStacksAndNeverExtracts() {
        var slots = List.of(slot("glass", 1), slot("glass", 64), slot("glass", 1));
        assertArrayEquals(new int[] { 1, 64, 1 }, BalancedGridFiller.fill(slots, 1,
                (key, amount, simulate) -> { throw new AssertionError("Single fill must not extract"); }));
    }

    @Test
    void emptyGridDoesNothing() {
        assertArrayEquals(new int[0], BalancedGridFiller.fill(List.of(), 64,
                (key, amount, simulate) -> { throw new AssertionError("Empty grid must not extract"); }));
    }

    @Test
    void randomizedFillsConserveItemsAndStayBalancedWithinLimits() {
        var random = new Random(0xAE2);
        for (int trial = 0; trial < 1000; trial++) {
            int size = random.nextInt(9) + 1;
            var slots = new ArrayList<BalancedGridFiller.Slot<String>>();
            for (int i = 0; i < size; i++) {
                int limit = random.nextInt(64) + 1;
                slots.add(new BalancedGridFiller.Slot<>("item", random.nextInt(limit) + 1, limit));
            }
            var source = new Source("item", random.nextInt(1000));
            source.maxActualExtraction = random.nextInt(600);
            int[] result = BalancedGridFiller.fill(slots, random.nextInt(63) + 2, source);
            assertEquals(slots.stream().mapToInt(BalancedGridFiller.Slot::count).sum() + source.extracted,
                    Arrays.stream(result).sum());
            for (int i = 0; i < size; i++) {
                assertTrue(result[i] >= 1 && result[i] <= slots.get(i).limit());
                for (int j = 0; j < size; j++) {
                    assertTrue(result[i] >= result[j] - 1 || result[i] == slots.get(i).limit());
                }
            }
        }
    }
}
