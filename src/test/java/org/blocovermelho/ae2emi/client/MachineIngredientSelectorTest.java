package org.blocovermelho.ae2emi.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class MachineIngredientSelectorTest {
    private static MachineIngredientSelector.Alternative<String> item(String key, long amount) {
        return new MachineIngredientSelector.Alternative<>(key, amount);
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
}
