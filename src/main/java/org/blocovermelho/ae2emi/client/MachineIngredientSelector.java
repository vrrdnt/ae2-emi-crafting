package org.blocovermelho.ae2emi.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.ToLongFunction;

/** Selects one concrete item for every alternative-based recipe ingredient. */
public final class MachineIngredientSelector {
    public record Alternative<K>(K key, long amount) {
    }

    public record Selection<K>(K key, long amount) {
    }

    private MachineIngredientSelector() {
    }

    public static <K> Optional<List<Selection<K>>> select(
            List<List<Alternative<K>>> ingredients,
            ToLongFunction<K> availability,
            long maxTotal) {
        if (ingredients.isEmpty() || maxTotal <= 0) {
            return Optional.empty();
        }

        var remaining = new ArrayList<>(ingredients);
        var available = new HashMap<K, Long>();
        var used = new HashMap<K, Long>();
        var selected = new LinkedHashMap<K, Long>();
        long total = 0;

        while (!remaining.isEmpty()) {
            int bestIngredient = -1;
            List<Alternative<K>> bestCandidates = List.of();

            for (int ingredientIndex = 0; ingredientIndex < remaining.size(); ingredientIndex++) {
                var candidates = new ArrayList<Alternative<K>>();
                for (var alternative : remaining.get(ingredientIndex)) {
                    if (alternative.key() == null || alternative.amount() <= 0 || alternative.amount() > maxTotal) {
                        continue;
                    }

                    long inInventory = available.computeIfAbsent(
                            alternative.key(), key -> Math.max(0, availability.applyAsLong(key)));
                    long alreadyUsed = used.getOrDefault(alternative.key(), 0L);
                    if (inInventory - alreadyUsed >= alternative.amount()) {
                        candidates.add(alternative);
                    }
                }

                if (candidates.isEmpty()) {
                    return Optional.empty();
                }
                if (bestIngredient < 0 || candidates.size() < bestCandidates.size()) {
                    bestIngredient = ingredientIndex;
                    bestCandidates = candidates;
                }
            }

            Alternative<K> choice = bestCandidates.get(0);
            long bestSpare = Long.MIN_VALUE;
            for (var candidate : bestCandidates) {
                long spare = available.get(candidate.key())
                        - used.getOrDefault(candidate.key(), 0L)
                        - candidate.amount();
                if (spare > bestSpare) {
                    choice = candidate;
                    bestSpare = spare;
                }
            }

            if (choice.amount() > maxTotal - total) {
                return Optional.empty();
            }
            total += choice.amount();
            used.merge(choice.key(), choice.amount(), Long::sum);
            selected.merge(choice.key(), choice.amount(), Long::sum);
            remaining.remove(bestIngredient);
        }

        return Optional.of(selected.entrySet().stream()
                .map(entry -> new Selection<>(entry.getKey(), entry.getValue()))
                .toList());
    }
}
