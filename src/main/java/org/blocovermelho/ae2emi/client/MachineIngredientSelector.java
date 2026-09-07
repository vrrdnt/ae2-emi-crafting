package org.blocovermelho.ae2emi.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.function.ToLongFunction;

/** Selects one concrete item for every alternative-based recipe ingredient. */
public final class MachineIngredientSelector {
    public record Alternative<K>(K key, long amount) {
    }

    public record Ingredient<K>(List<Alternative<K>> alternatives, boolean catalyst) {
        public Ingredient {
            alternatives = List.copyOf(alternatives);
        }
    }

    public record Selection<K>(K key, long amount) {
    }

    public record BatchSelection<K>(int batches, List<Selection<K>> selections) {
        public BatchSelection {
            selections = List.copyOf(selections);
        }
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
        var selected = new LinkedHashMap<K, Long>();
        long total = 0;

        while (!remaining.isEmpty()) {
            int bestIngredient = -1;
            int bestCandidateCount = Integer.MAX_VALUE;
            Alternative<K> choice = null;

            for (int ingredientIndex = 0; ingredientIndex < remaining.size(); ingredientIndex++) {
                int candidateCount = 0;
                Alternative<K> ingredientChoice = null;
                long bestSpare = Long.MIN_VALUE;
                for (var alternative : remaining.get(ingredientIndex)) {
                    if (alternative.key() == null || alternative.amount() <= 0 || alternative.amount() > maxTotal) {
                        continue;
                    }

                    long inInventory = available.computeIfAbsent(
                            alternative.key(), key -> Math.max(0, availability.applyAsLong(key)));
                    long spare = inInventory - selected.getOrDefault(alternative.key(), 0L) - alternative.amount();
                    if (spare >= 0) {
                        candidateCount++;
                        if (spare > bestSpare) {
                            ingredientChoice = alternative;
                            bestSpare = spare;
                        }
                    }
                }

                if (candidateCount == 0) {
                    return Optional.empty();
                }
                if (candidateCount < bestCandidateCount) {
                    bestIngredient = ingredientIndex;
                    bestCandidateCount = candidateCount;
                    choice = ingredientChoice;
                }
            }

            if (choice.amount() > maxTotal - total) {
                return Optional.empty();
            }
            total += choice.amount();
            selected.merge(choice.key(), choice.amount(), Long::sum);
            remaining.remove(bestIngredient);
        }

        return Optional.of(selected.entrySet().stream()
                .map(entry -> new Selection<>(entry.getKey(), entry.getValue()))
                .toList());
    }

    public static <K> Optional<BatchSelection<K>> selectMaximum(
            List<Ingredient<K>> ingredients,
            ToLongFunction<K> availability,
            int requestedBatches,
            long maxTotal) {
        if (ingredients.isEmpty() || requestedBatches <= 0 || maxTotal <= 0) {
            return Optional.empty();
        }

        int upperBound = (int) Math.min((long) requestedBatches, Math.min(maxTotal, Integer.MAX_VALUE));
        var availabilityCache = new HashMap<K, Long>();
        ToLongFunction<K> cachedAvailability = key -> availabilityCache.computeIfAbsent(
                key, candidate -> Math.max(0, availability.applyAsLong(candidate)));

        var best = selectScaled(ingredients, cachedAvailability, 1, maxTotal);
        if (best.isEmpty()) {
            return Optional.empty();
        }

        int lowerBound = 1;
        while (lowerBound < upperBound) {
            int candidateBatches = lowerBound + (upperBound - lowerBound + 1) / 2;
            var candidate = selectScaled(ingredients, cachedAvailability, candidateBatches, maxTotal);
            if (candidate.isPresent()) {
                lowerBound = candidateBatches;
                best = candidate;
            } else {
                upperBound = candidateBatches - 1;
            }
        }

        return Optional.of(new BatchSelection<>(lowerBound, best.orElseThrow()));
    }

    private static <K> Optional<List<Selection<K>>> selectScaled(
            List<Ingredient<K>> ingredients,
            ToLongFunction<K> availability,
            int batches,
            long maxTotal) {
        var scaled = new ArrayList<List<Alternative<K>>>(ingredients.size());
        for (var ingredient : ingredients) {
            long multiplier = ingredient.catalyst ? 1 : batches;
            var alternatives = new ArrayList<Alternative<K>>(ingredient.alternatives.size());
            for (var alternative : ingredient.alternatives) {
                if (alternative.amount > 0 && alternative.amount <= maxTotal / multiplier) {
                    alternatives.add(new Alternative<>(alternative.key, alternative.amount * multiplier));
                }
            }
            scaled.add(alternatives);
        }
        return select(scaled, availability, maxTotal);
    }
}
