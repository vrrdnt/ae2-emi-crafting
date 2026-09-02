package org.blocovermelho.ae2emi.network;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;

/** Balances repeated, identical ingredients without depending on Minecraft's runtime. */
final class BalancedGridFiller {
    private BalancedGridFiller() {
    }

    record Slot<K>(K key, int count, int limit) {
        Slot {
            Objects.requireNonNull(key);
            if (count < 1 || limit < count) {
                throw new IllegalArgumentException("Expected an occupied slot within its stack limit");
            }
        }
    }

    @FunctionalInterface
    interface ItemSource<K> {
        /** Returns up to amount items; simulation must not change the source. */
        int extract(K key, int amount, boolean simulate);
    }

    static <K> int[] fill(List<Slot<K>> slots, int requested, ItemSource<K> source) {
        int[] result = slots.stream().mapToInt(Slot::count).toArray();
        if (requested <= 1) {
            return result;
        }

        var groups = new LinkedHashMap<K, List<Integer>>();
        for (int i = 0; i < slots.size(); i++) {
            groups.computeIfAbsent(slots.get(i).key(), key -> new ArrayList<>()).add(i);
        }

        for (var entry : groups.entrySet()) {
            var indices = entry.getValue();
            int total = 0;
            int target = requested;
            for (int index : indices) {
                var slot = slots.get(index);
                total += slot.count();
                target = Math.min(target, slot.limit());
            }

            int wanted = Math.max(0, target * indices.size() - total);
            if (wanted > 0) {
                int available = source.extract(entry.getKey(), wanted, true);
                int batches = Math.min(target, (total + available) / indices.size());
                int needed = Math.max(0, batches * indices.size() - total);
                if (needed > 0) {
                    total += source.extract(entry.getKey(), needed, false);
                }
            }

            // Preserve all existing and actually extracted items, even if extraction was partial.
            // Fill capped slots first, then divide the remaining pool evenly among the others.
            var unfilled = new ArrayList<>(indices);
            while (!unfilled.isEmpty()) {
                int share = total / unfilled.size();
                boolean capped = false;
                for (var iterator = unfilled.iterator(); iterator.hasNext();) {
                    int index = iterator.next();
                    int limit = slots.get(index).limit();
                    if (limit <= share) {
                        result[index] = limit;
                        total -= limit;
                        iterator.remove();
                        capped = true;
                    }
                }
                if (!capped) {
                    int remainder = total % unfilled.size();
                    for (int index : unfilled) {
                        result[index] = share + (remainder-- > 0 ? 1 : 0);
                    }
                    break;
                }
            }
        }
        return result;
    }
}
