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

    /** Describes missing stock for one concrete alternative combination, not a minimum-cost shopping list. */
    public static <K> List<Selection<K>> shortfall(
            List<Ingredient<K>> ingredients, ToLongFunction<K> availability, int batches) {
        var needed = new LinkedHashMap<K, Long>();
        var stock = new HashMap<K, Long>();
        for (var ingredient : ingredients) {
            Alternative<K> choice = null;
            long bestRemaining = Long.MIN_VALUE;
            for (var alternative : ingredient.alternatives()) {
                if (alternative.key() == null || alternative.amount() <= 0) {
                    continue;
                }
                long available = stock.computeIfAbsent(alternative.key(),
                        key -> Math.max(0, availability.applyAsLong(key)));
                long remaining = available - needed.getOrDefault(alternative.key(), 0L);
                if (choice == null || remaining > bestRemaining) {
                    choice = alternative;
                    bestRemaining = remaining;
                }
            }
            if (choice != null) {
                long multiplier = ingredient.catalyst() ? 1 : Math.max(1, batches);
                long previous = needed.getOrDefault(choice.key(), 0L);
                long amount = Math.min(choice.amount(), (Long.MAX_VALUE - previous) / multiplier) * multiplier;
                needed.put(choice.key(), previous + amount);
            }
        }
        return needed.entrySet().stream()
                .filter(entry -> entry.getValue() > stock.get(entry.getKey()))
                .map(entry -> new Selection<>(entry.getKey(), entry.getValue() - stock.get(entry.getKey())))
                .toList();
    }

    public static <K> Optional<List<Selection<K>>> select(
            List<List<Alternative<K>>> ingredients,
            ToLongFunction<K> availability,
            long maxTotal) {
        var available = new HashMap<K, Long>();
        ToLongFunction<K> cached = key -> available.computeIfAbsent(
                key, candidate -> Math.max(0, availability.applyAsLong(candidate)));
        var greedy = selectGreedy(ingredients, cached, maxTotal);
        if (greedy.isPresent() || ingredients.isEmpty() || ingredients.size() > 64 || maxTotal <= 0) {
            return greedy;
        }

        // Overlapping alternatives may require undoing an earlier choice. Bound both
        // recursion depth and alternative inspections so an impossible recipe stays cheap.
        var selected = new LinkedHashMap<K, Long>();
        if (!search(new ArrayList<>(ingredients), cached, selected, maxTotal, new int[] {4096})) {
            return Optional.empty();
        }
        return Optional.of(selected.entrySet().stream()
                .map(entry -> new Selection<>(entry.getKey(), entry.getValue())).toList());
    }

    private static <K> boolean search(
            List<List<Alternative<K>>> remaining, ToLongFunction<K> available,
            Map<K, Long> selected, long capacity, int[] inspectionsLeft) {
        if (remaining.isEmpty()) {
            return true;
        }
        int bestIndex = -1;
        int bestCount = Integer.MAX_VALUE;
        for (int i = 0; i < remaining.size(); i++) {
            int count = 0;
            for (var alternative : remaining.get(i)) {
                if (--inspectionsLeft[0] < 0) {
                    return false;
                }
                if (fits(alternative, available, selected, capacity)) {
                    count++;
                }
            }
            if (count == 0) {
                return false;
            }
            if (count < bestCount) {
                bestIndex = i;
                bestCount = count;
            }
        }

        var alternatives = remaining.remove(bestIndex);
        for (var alternative : alternatives) {
            if (--inspectionsLeft[0] < 0) {
                break;
            }
            if (!fits(alternative, available, selected, capacity)) {
                continue;
            }
            long previous = selected.getOrDefault(alternative.key(), 0L);
            selected.put(alternative.key(), previous + alternative.amount());
            if (search(remaining, available, selected, capacity - alternative.amount(), inspectionsLeft)) {
                return true;
            }
            if (previous == 0) {
                selected.remove(alternative.key());
            } else {
                selected.put(alternative.key(), previous);
            }
        }
        remaining.add(bestIndex, alternatives);
        return false;
    }

    private static <K> boolean fits(
            Alternative<K> alternative, ToLongFunction<K> available, Map<K, Long> selected, long capacity) {
        return alternative.key() != null && alternative.amount() > 0 && alternative.amount() <= capacity
                && available.applyAsLong(alternative.key()) - selected.getOrDefault(alternative.key(), 0L)
                        >= alternative.amount();
    }

    private static <K> Optional<List<Selection<K>>> selectGreedy(
            List<List<Alternative<K>>> ingredients,
            ToLongFunction<K> availability,
            long maxTotal) {
        if (ingredients.isEmpty() || maxTotal <= 0) {
            return Optional.empty();
        }

        var remaining = new ArrayList<>(ingredients);
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

                    long inInventory = availability.applyAsLong(alternative.key());
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
