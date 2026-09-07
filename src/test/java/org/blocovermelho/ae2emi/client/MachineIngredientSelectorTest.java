package org.blocovermelho.ae2emi.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

import org.junit.jupiter.api.Test;

class MachineIngredientSelectorTest {
    @Test
    void selectionPrefersMostSpareStockAndPreservesAlternativeOrderOnTies() {
        var result = MachineIngredientSelector.select(
                List.of(List.of(item("copper", 2), item("tin", 2), item("iron", 2))),
                key -> key.equals("copper") ? 3 : 8,
                64).orElseThrow();

        assertEquals(List.of(new MachineIngredientSelector.Selection<>("tin", 2)), result);
    }

    @Test
    void batchSearchReadsEachItemsAvailabilityOnlyOnce() {
        var reads = new HashMap<String, Integer>();
        var result = MachineIngredientSelector.selectMaximum(
                List.of(input("plate", 2), input("plate", 1), catalyst("lens", 1)),
                key -> {
                    reads.merge(key, 1, Integer::sum);
                    return key.equals("plate") ? 30 : 1;
                },
                Integer.MAX_VALUE,
                64).orElseThrow();

        assertEquals(10, result.batches());
        assertEquals(Map.of("plate", 1, "lens", 1), reads);
        assertTrue(result.selections().contains(new MachineIngredientSelector.Selection<>("plate", 30)));
        assertTrue(result.selections().contains(new MachineIngredientSelector.Selection<>("lens", 1)));
    }

    private static MachineIngredientSelector.Alternative<String> item(String key, long amount) {
        return new MachineIngredientSelector.Alternative<>(key, amount);
    }

    private static MachineIngredientSelector.Ingredient<String> input(String key, long amount) {
        return new MachineIngredientSelector.Ingredient<>(List.of(item(key, amount)), false);
    }

    private static MachineIngredientSelector.Ingredient<String> catalyst(String key, long amount) {
        return new MachineIngredientSelector.Ingredient<>(List.of(item(key, amount)), true);
    }

    @Test
    void repeatedIngredientsAreAggregatedIntoOneWithdrawal() {
        var result = MachineIngredientSelector.select(
                List.of(List.of(item("plate", 2)), List.of(item("plate", 1))),
                key -> Map.of("plate", 3L).getOrDefault(key, 0L),
                64);

        assertEquals(List.of(new MachineIngredientSelector.Selection<>("plate", 3)), result.orElseThrow());
    }

    @Test
    void constrainedIngredientGetsTheOnlySharedAlternative() {
        var result = MachineIngredientSelector.select(
                List.of(
                        List.of(item("copper", 1), item("tin", 1)),
                        List.of(item("copper", 1))),
                key -> Map.of("copper", 1L, "tin", 1L).getOrDefault(key, 0L),
                64);

        assertEquals(
                List.of(
                        new MachineIngredientSelector.Selection<>("copper", 1),
                        new MachineIngredientSelector.Selection<>("tin", 1)),
                result.orElseThrow());
    }

    @Test
    void sharedAvailabilityIsConsumedAcrossRecipeSlots() {
        var result = MachineIngredientSelector.select(
                List.of(List.of(item("lens", 1)), List.of(item("lens", 1))),
                key -> 1,
                64);

        assertTrue(result.isEmpty());
    }

    @Test
    void unavailableIngredientRejectsTheWholeTransfer() {
        var result = MachineIngredientSelector.select(
                List.of(List.of(item("wafer", 1)), List.of(item("lens", 1))),
                key -> key.equals("wafer") ? 1 : 0,
                64);

        assertTrue(result.isEmpty());
    }

    @Test
    void oversizedRequestIsRejectedBeforeNetworking() {
        var result = MachineIngredientSelector.select(
                List.of(List.of(item("plate", 65))),
                key -> 65,
                64);

        assertTrue(result.isEmpty());
    }

    @Test
    void craftAllSelectsEveryRequestedBatch() {
        var result = MachineIngredientSelector.selectMaximum(
                List.of(input("plate", 2)),
                key -> 10,
                4,
                64).orElseThrow();

        assertEquals(4, result.batches());
        assertEquals(List.of(new MachineIngredientSelector.Selection<>("plate", 8)), result.selections());
    }

    @Test
    void craftAllStopsAtTheLastCompleteAvailableBatch() {
        var result = MachineIngredientSelector.selectMaximum(
                List.of(input("plate", 2)),
                key -> 7,
                Integer.MAX_VALUE,
                64).orElseThrow();

        assertEquals(3, result.batches());
        assertEquals(List.of(new MachineIngredientSelector.Selection<>("plate", 6)), result.selections());
    }

    @Test
    void catalystsAreOnlyRequestedOnceForCraftAll() {
        var result = MachineIngredientSelector.selectMaximum(
                List.of(input("wafer", 1), catalyst("lens", 1)),
                key -> key.equals("wafer") ? 5 : 1,
                5,
                64).orElseThrow();

        assertEquals(5, result.batches());
        assertEquals(
                List.of(
                        new MachineIngredientSelector.Selection<>("wafer", 5),
                        new MachineIngredientSelector.Selection<>("lens", 1)),
                result.selections());
    }

    @Test
    void craftAllRespectsTheTransferItemLimit() {
        var result = MachineIngredientSelector.selectMaximum(
                List.of(input("dust", 1)),
                key -> 100,
                100,
                4).orElseThrow();

        assertEquals(4, result.batches());
        assertEquals(List.of(new MachineIngredientSelector.Selection<>("dust", 4)), result.selections());
    }
}
