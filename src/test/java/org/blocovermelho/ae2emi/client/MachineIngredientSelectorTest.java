package org.blocovermelho.ae2emi.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.time.Duration;

import org.junit.jupiter.api.Test;

class MachineIngredientSelectorTest {
    @Test
    void selectionKeepsConsumedAndCatalystAmountsSeparateForTheSameItem() {
        var result = MachineIngredientSelector.selectMaximum(
                List.of(input("plate", 2), input("plate", 1), catalyst("plate", 2)),
                key -> 32, 20, 2304).orElseThrow();
        assertEquals(10, result.batches());
        assertEquals(List.of(new MachineIngredientSelector.Selection<>("plate", 32, 2)), result.selections());
    }

    @Test
    void backtrackingRetainsTheCatalystChosenForTheSuccessfulAssignment() {
        var result = MachineIngredientSelector.select(
                List.of(List.of(new MachineIngredientSelector.Alternative<>("a", 1, true),
                                new MachineIngredientSelector.Alternative<>("b", 1, true)),
                        List.of(item("a", 1), item("c", 1)),
                        List.of(item("a", 1), item("c", 1))),
                key -> 1, 64).orElseThrow();
        assertTrue(result.contains(new MachineIngredientSelector.Selection<>("b", 1, 1)));
        assertTrue(result.contains(new MachineIngredientSelector.Selection<>("a", 1, 0)));
        assertTrue(result.contains(new MachineIngredientSelector.Selection<>("c", 1, 0)));
    }

    @Test
    void largeMostlyUnavailableTagsStillSelectTheStockedAlternative() {
        var alternatives = new ArrayList<MachineIngredientSelector.Alternative<String>>();
        for (int i = 0; i < 10000; i++) {
            alternatives.add(item("absent" + i, 2));
        }
        alternatives.add(item("available", 2));
        var result = MachineIngredientSelector.selectMaximum(
                List.of(new MachineIngredientSelector.Ingredient<>(alternatives, false), catalyst("lens", 1)),
                key -> key.equals("available") ? 40 : key.equals("lens") ? 1 : 0,
                64, 2304).orElseThrow();
        assertEquals(20, result.batches());
        assertTrue(result.selections().contains(new MachineIngredientSelector.Selection<>("available", 40)));
    }

    @Test
    void diodeShortfallReportsTheMissingItemsForTheFullOrder() {
        var ingredients = List.of(input("wire", 4), input("dust", 1));
        java.util.function.ToLongFunction<String> stock = key -> key.equals("wire") ? 8 : 1;
        var plan = MachineIngredientSelector.selectMaximum(ingredients, stock, 64, 2304).orElseThrow();

        assertEquals(1, plan.batches());
        assertEquals(List.of(new MachineIngredientSelector.Selection<>("wire", 248),
                        new MachineIngredientSelector.Selection<>("dust", 63)),
                MachineIngredientSelector.shortfall(ingredients, stock, 64));
    }

    @Test
    void missingCatalystIsReportedOnceAndBlocksCompleteBatches() {
        var ingredients = List.of(input("wafer", 1), catalyst("lens", 1));
        java.util.function.ToLongFunction<String> stock = key -> key.equals("wafer") ? 47 : 0;
        assertTrue(MachineIngredientSelector.selectMaximum(ingredients, stock, 47, 2304).isEmpty());
        assertEquals(List.of(new MachineIngredientSelector.Selection<>("lens", 1)),
                MachineIngredientSelector.shortfall(ingredients, stock, 47));
    }

    @Test
    void shortfallAggregatesRepeatedInputsWithoutDoubleCountingStock() {
        var ingredients = List.of(input("plate", 2), input("plate", 1));
        assertEquals(List.of(new MachineIngredientSelector.Selection<>("plate", 7)),
                MachineIngredientSelector.shortfall(ingredients, key -> 5, 4));
    }

    @Test
    void smallOverlappingRecipesMatchExhaustiveAllocation() {
        for (int a = 1; a < 8; a++) {
            for (int b = 1; b < 8; b++) {
                for (int c = 1; c < 8; c++) {
                    var recipe = List.of(alternatives(a), alternatives(b), alternatives(c));
                    for (int inventory = 0; inventory < 27; inventory++) {
                        int[] stock = {inventory % 3, inventory / 3 % 3, inventory / 9};
                        boolean expected = allocate(recipe, stock.clone(), 0);
                        var result = MachineIngredientSelector.select(recipe, key -> stock[key], 64);
                        assertEquals(expected, result.isPresent(), () -> recipe + " / " + java.util.Arrays.toString(stock));
                        if (result.isPresent()) {
                            assertEquals(3, result.get().stream().mapToLong(MachineIngredientSelector.Selection::amount).sum());
                            result.get().forEach(item -> assertTrue(item.amount() <= stock[item.key()]));
                        }
                    }
                }
            }
        }
    }

    private static List<MachineIngredientSelector.Alternative<Integer>> alternatives(int mask) {
        var result = new ArrayList<MachineIngredientSelector.Alternative<Integer>>();
        for (int key = 0; key < 3; key++) {
            if ((mask & (1 << key)) != 0) {
                result.add(new MachineIngredientSelector.Alternative<>(key, 1));
            }
        }
        return result;
    }

    private static boolean allocate(List<List<MachineIngredientSelector.Alternative<Integer>>> recipe,
            int[] stock, int slot) {
        if (slot == recipe.size()) {
            return true;
        }
        for (var item : recipe.get(slot)) {
            if (stock[item.key()] > 0) {
                stock[item.key()]--;
                boolean found = allocate(recipe, stock, slot + 1);
                stock[item.key()]++;
                if (found) {
                    return true;
                }
            }
        }
        return false;
    }

    @Test
    void impossibleHighlyOverlappingRecipeHasBoundedSearch() {
        var alternatives = new ArrayList<MachineIngredientSelector.Alternative<Integer>>();
        for (int key = 0; key < 19; key++) {
            alternatives.add(new MachineIngredientSelector.Alternative<>(key, 1));
        }
        var recipe = java.util.Collections.nCopies(20, List.copyOf(alternatives));
        assertTimeoutPreemptively(Duration.ofSeconds(5),
                () -> assertTrue(MachineIngredientSelector.select(recipe, key -> 1, 64).isEmpty()));
    }

    @Test
    void overlappingAlternativesCanReassignAnEarlierChoice() {
        var result = MachineIngredientSelector.select(
                List.of(List.of(item("a", 1), item("b", 1)),
                        List.of(item("a", 1), item("c", 1)),
                        List.of(item("a", 1), item("c", 1))),
                key -> 1, 64).orElseThrow();

        assertEquals(3, result.size());
        assertTrue(result.contains(new MachineIngredientSelector.Selection<>("a", 1)));
        assertTrue(result.contains(new MachineIngredientSelector.Selection<>("b", 1)));
        assertTrue(result.contains(new MachineIngredientSelector.Selection<>("c", 1)));
    }

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
        assertTrue(result.selections().contains(new MachineIngredientSelector.Selection<>("lens", 1, 1)));
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
                        new MachineIngredientSelector.Selection<>("lens", 1, 1)),
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
